#!/usr/bin/env python3
"""Validate a .wfp using the APK's public key, without extracting archive paths."""
import argparse,datetime,hashlib,io,json,re,subprocess,tempfile,zipfile
from pathlib import Path
from PIL import Image
from validate_content import validate as validate_content

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'

def validate_package(path,public_key=ASSETS/'content/publisher.der'):
    assert path.stat().st_size<=128*1024*1024,'Archive too large'
    with zipfile.ZipFile(path) as z:
        infos=z.infolist();names=[i.filename for i in infos]
        assert 4<=len(infos)<=8194 and len(set(names))==len(names) and not any(i.is_dir() for i in infos),'Invalid ZIP inventory'
        assert 0<z.getinfo('manifest.json').file_size<=512*1024 and 256<=z.getinfo('manifest.sig').file_size<=1024
        encoded=z.read('manifest.json');signature=z.read('manifest.sig')
        with tempfile.TemporaryDirectory() as tmp:
            d=Path(tmp);(d/'manifest').write_bytes(encoded);(d/'signature').write_bytes(signature)
            public=subprocess.check_output(['openssl','pkey','-pubin','-inform','DER','-in',str(public_key),'-outform','PEM'],stderr=subprocess.DEVNULL)
            (d/'public.pem').write_bytes(public)
            check=subprocess.run(['openssl','dgst','-sha256','-verify',str(d/'public.pem'),'-signature',str(d/'signature'),str(d/'manifest')],capture_output=True)
            assert check.returncode==0,'Publisher signature mismatch'
        m=json.loads(encoded)
        assert set(m)=={'formatVersion','packageId','generation','gameVariant','contentSchema','minimumAppCode','checkedOn','dependencies','frames','files'}
        assert all(type(m[k]) is int for k in ['formatVersion','contentSchema','minimumAppCode','generation'])
        assert m['formatVersion']==1 and m['contentSchema']==1 and 9<=m['minimumAppCode']<=15
        assert m['packageId']=='wow-forever:starter:durotar' and m['gameVariant']=='wow-forever' and type(m['generation']) is int and m['generation']>0
        assert re.fullmatch(r'\d{4}-\d{2}-\d{2}',m['checkedOn']);datetime.date.fromisoformat(m['checkedOn'])
        assert m['dependencies']==[] and 2<=len(m['files'])<=8192
        files=m['files'];assert len({f['path'] for f in files})==len(files)
        assert set(names)=={f['path'] for f in files}|{'manifest.json','manifest.sig'}
        assert sum(f['size'] for f in files)<=256*1024*1024
        for f in files:
            assert set(f)=={'path','size','sha256'}
            assert f['path']=='catalog.json' or re.fullmatch(r'maps/[1-9][0-9]{0,8}/(?:floor_[0-9]{1,3}/)?[0-7]/[0-9]{1,3}/[0-9]{1,3}\.png',f['path'])
            assert type(f['size']) is int and 0<f['size']<=(32 if f['path']=='catalog.json' else 4)*1024*1024
            assert re.fullmatch(r'[0-9a-f]{64}',f['sha256']) and z.getinfo(f['path']).file_size==f['size']
            raw=z.read(f['path']);assert len(raw)==f['size'] and hashlib.sha256(raw).hexdigest()==f['sha256']
            if f['path'].endswith('.png'):
                with Image.open(io.BytesIO(raw)) as image:
                    assert image.format=='PNG' and image.size==(256,256);image.load()
        p=json.loads(z.read('catalog.json'));validate_content(p)
        assert (m['generation'],m['packageId'],m['gameVariant'],m['contentSchema'],m['checkedOn'])==(p['generation'],p['packageId'],p['gameVariant'],p['schemaVersion'],p['checkedOn'])
        assert all(set(f)=={'mapKey','floorId','coordinateVersion','build'} for f in m['frames'])
        expected_frames=[{'mapKey':f['key'],'floorId':f['floorId'],'coordinateVersion':f['coordinateVersion'],'build':f['build']} for f in p['maps']]
        assert sorted(m['frames'],key=lambda f:f['mapKey'])==sorted(expected_frames,key=lambda f:f['mapKey'])
        known={f['key']:f for f in json.loads((ASSETS/'content/supported-maps.json').read_text())}
        if any(f['uiMapId']==1413 or f.get('scopeCs') is not None for f in p['maps']):assert m['minimumAppCode']>=10
        entries={e['key']:e for e in p['entries']}
        if any(entries[l['entryKey']]['verification']=='CLASSIC' for l in p['locations']):assert m['minimumAppCode']>=14
        if p['generation']>=7 or any('referencePosition' in e or e['kind']=='OBJECT' for e in p['entries']) or any(m['uiMapId'] not in (947,1414,1411,1454,1413) for m in p['maps']):assert m['minimumAppCode']>=15
        expected={'catalog.json'}
        for map in p['maps']:
            assert all(map.get(k)==known[map['key']].get(k) for k in ['uiMapId','floorId','width','height','levels','coordinateVersion'])
            identity={'scaleX':1.0,'scaleY':1.0,'offsetX':0.0,'offsetY':0.0}
            assert map.get('transform',identity)==known[map['key']].get('transform',identity)
            prefix=f"maps/{map['uiMapId']}"+(f"/floor_{map['floorId']}" if map['floorId'] else '')
            for level in range(map['levels']):
                d=1 << (map['levels']-1-level)
                for row in range((map['height']+256*d-1)//(256*d)):
                    for col in range((map['width']+256*d-1)//(256*d)):expected.add(f'{prefix}/{level}/{row}/{col}.png')
        assert expected=={f['path'] for f in files}
        return {'passed':True,'generation':m['generation'],'files':len(files),'maps':len(p['maps']),'entries':len(p['entries']),'publisher_signature_verified':True}

if __name__=='__main__':
    a=argparse.ArgumentParser(description=__doc__);a.add_argument('package',type=Path);args=a.parse_args()
    print(json.dumps(validate_package(args.package),ensure_ascii=False))
