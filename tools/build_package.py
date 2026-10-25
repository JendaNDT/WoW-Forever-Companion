#!/usr/bin/env python3
"""Build a signed, self-contained E09 content generation. Private keys stay outside the project."""
import argparse,hashlib,json,subprocess,zipfile
from pathlib import Path
from validate_content import validate

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'

def build(catalog,output,signing_key,manifest_prefix=None,tile_root=None):
    raw=catalog.read_bytes();p=json.loads(raw);validate(p)
    public=subprocess.check_output(['openssl','pkey','-in',str(signing_key),'-pubout','-outform','DER'],stderr=subprocess.DEVNULL)
    assert public==(ASSETS/'content/publisher.der').read_bytes(),'Signing key does not match the pinned publisher'
    files={'catalog.json':raw}
    for m in p['maps']:
        prefix=f"maps/{m['uiMapId']}"+(f"/floor_{m['floorId']}" if m['floorId'] else '')
        for z in range(m['levels']):
            d=1 << (m['levels']-1-z)
            for row in range((m['height']+256*d-1)//(256*d)):
                for col in range((m['width']+256*d-1)//(256*d)):
                    name=f'{prefix}/{z}/{row}/{col}.png'
                    source=(ASSETS/name) if (ASSETS/name).is_file() else ROOT/'content/e10-maps'/name
                    if not source.is_file() and tile_root is not None:source=tile_root/name
                    files[name]=source.read_bytes()
    entries={e['key']:e for e in p['entries']}
    minimum_code=15 if p['generation']>=7 or any('referencePosition' in e or e['kind']=='OBJECT' for e in p['entries']) or any(m['uiMapId'] not in (947,1414,1411,1454,1413) for m in p['maps']) else 14 if any(entries[l['entryKey']]['verification']=='CLASSIC' for l in p['locations']) else 10 if any(m['uiMapId']==1413 or m.get('scopeCs') is not None for m in p['maps']) else 9
    manifest={'formatVersion':1,'packageId':p['packageId'],'generation':p['generation'],'gameVariant':p['gameVariant'],
        'contentSchema':p['schemaVersion'],'minimumAppCode':minimum_code,'checkedOn':p['checkedOn'],'dependencies':[],
        'frames':[{'mapKey':m['key'],'floorId':m['floorId'],'coordinateVersion':m['coordinateVersion'],'build':m['build']} for m in p['maps']],
        'files':[{'path':name,'size':len(data),'sha256':hashlib.sha256(data).hexdigest()} for name,data in sorted(files.items())]}
    encoded=(json.dumps(manifest,ensure_ascii=False,sort_keys=True,separators=(',',':'))+'\n').encode()
    signature=subprocess.run(['openssl','dgst','-sha256','-sign',str(signing_key)],input=encoded,check=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE).stdout
    if manifest_prefix is not None:
        Path(str(manifest_prefix)+'.json').write_bytes(encoded)
        Path(str(manifest_prefix)+'.sig').write_bytes(signature)
    output.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED,compresslevel=7) as z:
        z.writestr('manifest.json',encoded);z.writestr('manifest.sig',signature)
        for name,data in sorted(files.items()):z.writestr(name,data)
    with zipfile.ZipFile(output) as z:assert z.testzip() is None
    from validate_package import validate_package
    validate_package(output)
    return {'generation':p['generation'],'files':len(files),'bytes':output.stat().st_size,'sha256':hashlib.sha256(output.read_bytes()).hexdigest()}

if __name__=='__main__':
    a=argparse.ArgumentParser(description=__doc__);a.add_argument('--catalog',type=Path,default=ROOT/'content/starter-v1.json')
    a.add_argument('--output',type=Path,required=True);a.add_argument('--signing-key',type=Path,required=True);a.add_argument('--manifest-prefix',type=Path);a.add_argument('--tile-root',type=Path);args=a.parse_args()
    print(json.dumps(build(args.catalog,args.output,args.signing_key,args.manifest_prefix,args.tile_root),ensure_ascii=False))
