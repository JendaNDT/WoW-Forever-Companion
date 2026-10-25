#!/usr/bin/env python3
"""Prepare only the new reviewed Barrens art; E09's packaged fallback is unchanged."""
from pathlib import Path
import hashlib,json,math
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]

def main():
    src=ROOT/'research/e10/maps/the-barrens_1.60.1.69893.webp'
    p=json.loads((ROOT/'content/e10-additions.json').read_text());proof=next(s for s in p['sources'] if s['id']=='e10_map_1413')
    assert hashlib.sha256(src.read_bytes()).hexdigest()==json.loads(proof['payload'])['facts']['assetSha256']
    image=Image.open(src).convert('RGBA');assert image.size==(1002,668)
    padded=Image.new('RGBA',(1024,1024));padded.paste(image,(0,0));manifest=[]
    root=ROOT/'content/e10-maps'
    for level in range(3):
        divisor=2**(2-level);scaled=padded.resize((1024//divisor,1024//divisor),Image.Resampling.LANCZOS)
        for row in range(math.ceil(668/divisor/256)):
            for col in range(math.ceil(1002/divisor/256)):
                tile=scaled.crop((col*256,row*256,(col+1)*256,(row+1)*256));target=root/f'maps/1413/{level}/{row}/{col}.png'
                target.parent.mkdir(parents=True,exist_ok=True);tile.save(target,'PNG',compress_level=9)
                manifest.append({'path':target.relative_to(root).as_posix(),'sha256':hashlib.sha256(target.read_bytes()).hexdigest()})
    (ROOT/'research/e10/maps/tile-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    print(json.dumps({'new_map':1413,'tiles':len(manifest),'old_apk_tiles_changed':0}))

if __name__=='__main__':main()
