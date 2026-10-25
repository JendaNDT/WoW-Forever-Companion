#!/usr/bin/env python3
"""Package tracked source, verified git history and an APK-bound restoration identity. No signing keys."""
import argparse,json,subprocess,zipfile,tempfile,hashlib,os
from pathlib import Path
from release_identity import verify_identity,source_manifest
ROOT=Path(__file__).resolve().parents[1]

def package(apk,output,root=ROOT):
    root=Path(root).resolve()
    with zipfile.ZipFile(apk) as z:meta=json.loads(z.read('assets/release-info.json'))
    verify_identity(meta,root)
    if subprocess.check_output(['git','-C',str(root),'status','--porcelain','--untracked-files=no']).strip():raise ValueError('Nejdřív ulož sledované zdroje do Git historie.')
    files=subprocess.check_output(['git','-C',str(root),'ls-files','-z']).decode().split('\0');files=list(filter(None,files))
    password=os.environ.get('FOREVER_SIGNING_PASSWORD','').encode()
    for name in files:
        path=root/name
        if path.is_symlink() or name.endswith(('.jks','.keystore','.pem','.p12')) or name=='local.properties' or '/build/' in name:raise ValueError('Do zdrojů nepatří tento soubor: '+name)
        raw=path.read_bytes()
        if any(header in raw for header in (b'-----BEGIN '+b'PRIVATE KEY-----',b'-----BEGIN RSA '+b'PRIVATE KEY-----')) or (password and password in raw):raise ValueError('Zdroj obsahuje soukromé údaje: '+name)
    output=Path(output);output.parent.mkdir(parents=True,exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='forever-source-') as tmp:
        bundle=Path(tmp)/'history.bundle'
        subprocess.run(['git','-C',str(root),'bundle','create',str(bundle),'--all'],check=True)
        subprocess.run(['git','-C',str(root),'bundle','verify',str(bundle)],check=True,capture_output=True)
        state={'formatVersion':1,'sourceRevision':meta['sourceRevision'],'buildInputsSha256':meta['buildInputsSha256'],
               'archiveRevision':subprocess.check_output(['git','-C',str(root),'rev-parse','HEAD']).decode().strip(),
               'apkSha256':hashlib.sha256(Path(apk).read_bytes()).hexdigest(),'files':dict(source_manifest(root))}
        with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED,compresslevel=7) as z:
            # A restored checkout can already contain the previous archive receipt.
            # Write only the fresh receipt and bundle below, with no duplicate ZIP names.
            for name in sorted(files):
                if name not in ('source-state.json','history.bundle'):z.write(root/name,'WoWForeverPruvodce/'+name)
            z.write(bundle,'WoWForeverPruvodce/history.bundle')
            z.writestr('WoWForeverPruvodce/source-state.json',json.dumps(state,ensure_ascii=False,indent=2)+'\n')
        with zipfile.ZipFile(output) as z:
            if z.testzip() is not None:raise ValueError('Poškozený zdrojový archiv.')
    return {'passed':True,'files':len(files)+2,'sourceRevision':meta['sourceRevision'],'archiveRevision':state['archiveRevision'],'bytes':output.stat().st_size,'sha256':hashlib.sha256(output.read_bytes()).hexdigest()}

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--apk',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();print(json.dumps(package(a.apk,a.output)))
