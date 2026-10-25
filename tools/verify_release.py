#!/usr/bin/env python3
"""Check APK signature, embedded build identity, source revision and optional update/data inputs."""
import argparse,hashlib,json,re,subprocess,zipfile,tempfile
from pathlib import Path
from release_identity import APP_ID,verify_identity,sha
from validate_package import validate_package

ROOT=Path(__file__).resolve().parents[1]

def command(*args):
    p=subprocess.run([str(a) for a in args],capture_output=True,text=True)
    if p.returncode:raise ValueError('Nástroj odmítl APK: '+p.stderr.strip()[:300])
    return p.stdout

def apk_info(apk,aapt,apksigner):
    signature=command(apksigner,'verify','--verbose','--print-certs',apk)
    certificate=re.search(r'Signer #1 certificate SHA-256 digest: ([a-f0-9]{64})',signature)
    if not certificate:raise ValueError('Chybí ověřený podpis APK.')
    badging=command(aapt,'dump','badging',apk)
    package=re.search(r"package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']+)'",badging)
    if not package:raise ValueError('Chybí identifikace aplikace.')
    return {'package':package[1],'versionCode':int(package[2]),'versionName':package[3],'certificateSha256':certificate[1],'debuggable':'application-debuggable' in badging},signature,badging

def verify(apk,root,aapt,apksigner,previous=None,data_package=None):
    root=Path(root);info,_,_=apk_info(apk,aapt,apksigner)
    if info['package']!=APP_ID or info['debuggable']:raise ValueError('APK není vlastní release sestavení průvodce.')
    if info['certificateSha256']!=(root/'release/owner-certificate.sha256').read_text().strip():raise ValueError('Cizí certifikát APK.')
    with zipfile.ZipFile(apk) as z:
        entry=z.getinfo('assets/release-info.json')
        if not 0<entry.file_size<=16_384:raise ValueError('Neplatná velikost identity.')
        meta=json.loads(z.read(entry));identity=verify_identity(meta,root)
        if (info['versionCode'],info['versionName'])!=(meta['versionCode'],meta['versionName']):raise ValueError('Verze APK a identity se liší.')
        for f in (root/'app/src/main/assets').rglob('*'):
            if f.is_file() and z.read('assets/'+f.relative_to(root/'app/src/main/assets').as_posix())!=f.read_bytes():raise ValueError('Neshodná přibalená data.')
        if any(p.endswith(('.jks','.keystore','.pem')) or 'legacy-e09' in p or p=='assets/content/e10-v4.json' for p in z.namelist()):raise ValueError('Do APK se dostal soukromý nebo testovací soubor.')
    upgrade=None
    if previous:
        old,_,_=apk_info(previous,aapt,apksigner)
        if old['package']!=info['package'] or old['certificateSha256']!=info['certificateSha256'] or old['versionCode']>=info['versionCode']:raise ValueError('Není zachovaná kompatibilita aktualizace.')
        upgrade={'fromVersionCode':old['versionCode'],'certificateAndPackageMatch':True,'physicalInstallationVerified':False}
    catalog=json.loads((root/'app/src/main/assets/content/starter.json').read_text())
    receipt={'formatVersion':1,'apk':Path(apk).name,'apkSha256':sha(Path(apk).read_bytes()),'apkBytes':Path(apk).stat().st_size,**info,**identity,
             'fallbackGeneration':catalog['generation'],'fallbackCatalogSha256':meta['fallbackCatalogSha256'],'publisherKeySha256':meta['publisherKeySha256'],'upgrade':upgrade,
             'physicalPhoneAcceptance':'pending','performancePhoneGoalsVerified':False,'contentMilestonesComplete':False}
    if data_package:
        data=validate_package(Path(data_package),root/'app/src/main/assets/content/publisher.der');receipt['optionalDataPackage']={'file':Path(data_package).name,'sha256':sha(Path(data_package).read_bytes()),'validation':data}
    if info['versionCode']>=13:
        from verify_bundled import verify_bundled,ASSET
        with tempfile.TemporaryDirectory(prefix='forever-bundled-') as tmp:
            path=Path(tmp)/'bundled.wfp'
            with zipfile.ZipFile(apk) as z:
                index_name='assets/'+ASSET+'.index.json'
                if index_name in z.namelist():
                    index=json.loads(z.read(index_name))
                    with path.open('wb') as out:
                        for name in index['parts']:
                            assert Path(name).name==name
                            with z.open('assets/content/'+name) as part:
                                import shutil
                                shutil.copyfileobj(part,out)
                    assert path.stat().st_size==index['bytes'] and sha(path.read_bytes())==index['sha256']
                else:path.write_bytes(z.read('assets/'+ASSET))
            receipt['bundledContent']=verify_bundled(path,root)
        if info['versionCode']>=16:
            with zipfile.ZipFile(apk) as z:
                index_name='assets/'+ASSET+'.index.json'
                names=['assets/content/'+n for n in json.loads(z.read(index_name))['parts']] if index_name in z.namelist() else ['assets/'+ASSET]
                if any(z.getinfo(n).compress_type!=zipfile.ZIP_STORED for n in names):raise ValueError('Offline data musí být uložená bez druhé komprese.')
            receipt['bundledArchiveStoredForStreaming']=True
    return receipt

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--apk',type=Path,required=True);p.add_argument('--source-root',type=Path,default=ROOT);p.add_argument('--aapt',required=True);p.add_argument('--apksigner',required=True);p.add_argument('--previous-apk',type=Path);p.add_argument('--data-package',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
    result=verify(a.apk,a.source_root,a.aapt,a.apksigner,a.previous_apk,a.data_package)
    a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps({'passed':True,'apkSha256':result['apkSha256'],'sourceRevision':result['sourceRevision']}))
