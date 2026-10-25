#!/usr/bin/env python3
"""Verify the signed current content bundled for automatic offline startup."""
import hashlib,io,json,zipfile
from pathlib import Path
from PIL import Image,ImageChops
from catalog_io import catalog_bytes
from validate_package import validate_package
ROOT=Path(__file__).resolve().parents[1]
ASSET='content/bundled-data-8.wfp'
EXPECTED='8c805d7f25a7d0e33b760026c32bb82216f153ed27dadfde2e3faa7f47d2b94e'
def verify_bundled(path,root=ROOT):
    root=Path(root);path=Path(path)
    assert hashlib.sha256(path.read_bytes()).hexdigest()==EXPECTED,'Bundled archive differs from reviewed data 8'
    result=validate_package(path,root/'app/src/main/assets/content/publisher.der')
    count=0
    with zipfile.ZipFile(path) as z:
        for name in ('catalog.json','manifest.json','manifest.sig'):
            original='e10-v8.json' if name=='catalog.json' else 'e10-v8.'+name
            assert z.read(name)==catalog_bytes(root/'content'/original)
        p=json.loads(z.read('catalog.json'))
        # All already published map tiles retain their bytes.
        with zipfile.ZipFile(root/'app/src/main/assets/content/bundled-data-6.wfp') as previous:
            for name in previous.namelist():
                if name.endswith('.png'):assert previous.read(name)==z.read(name),'Changed published map tile'
        inventory=json.loads((root/'research/e10full/maps/inventory.json').read_text())
        maps={m['uiMapId']:m for m in p['maps']}
        for row in inventory:
            if row['uiMapId'] in (1411,1454,1413):continue
            raw=(root/'research/e10full/maps'/f'{row["uiMapId"]}.webp').read_bytes()
            assert hashlib.sha256(raw).hexdigest()==row['sha256']
            with Image.open(io.BytesIO(raw)) as original:source=original.convert('RGBA')
            m=maps[row['uiMapId']];rebuilt=Image.new('RGBA',source.size)
            for y in range((m['height']+255)//256):
                for x in range((m['width']+255)//256):
                    with Image.open(io.BytesIO(z.read(f'maps/{m["uiMapId"]}/{m["levels"]-1}/{y}/{x}.png'))) as tile:rebuilt.paste(tile,(x*256,y*256))
            assert all(v==(0,0) for v in ImageChops.difference(source,rebuilt).getextrema())
            count+=1
        tiles=sum(n.endswith('.png') for n in z.namelist())
    assert (p['generation'],len(p['entries']),len(p['locations']),len(p['maps']))==(8,11912,17330,61)
    return {**result,'positions':17330,'sha256':EXPECTED,'tiles':tiles,'allPublishedMapTilesUnchanged':True,'pixelExactNewMapReconstructions':count}
if __name__=='__main__':
    from bundle_parts import materialized
    with materialized(ROOT/'app/src/main/assets'/ASSET) as p:print(json.dumps(verify_bundled(p),indent=2))
