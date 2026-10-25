#!/usr/bin/env python3
"""Deterministic tiles from checksummed map originals; pad before downsampling."""
import argparse,hashlib,json,math
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
def prepare(output):
    count=0
    for row in json.loads((ROOT/'research/e10full/maps/inventory.json').read_text()):
        if row['uiMapId'] in (1411,1454,1413):continue # Published tile bytes are preserved.
        source=ROOT/'research/e10full/maps'/f'{row["uiMapId"]}.webp'
        assert hashlib.sha256(source.read_bytes()).hexdigest()==row['sha256']
        with Image.open(source) as original:image=original.convert('RGBA')
        assert image.size==(row['width'],row['height'])
        levels=math.ceil(math.log2(max(image.size)/256))+1;side=256*2**(levels-1)
        padded=Image.new('RGBA',(side,side));padded.paste(image,(0,0))
        for level in range(levels):
            d=2**(levels-1-level);scaled=padded.resize((side//d,side//d),Image.Resampling.LANCZOS)
            for y in range(math.ceil(image.height/d/256)):
                for x in range(math.ceil(image.width/d/256)):
                    p=output/f'maps/{row["uiMapId"]}/{level}/{y}/{x}.png';p.parent.mkdir(parents=True,exist_ok=True)
                    scaled.crop((x*256,y*256,(x+1)*256,(y+1)*256)).save(p,'PNG',compress_level=9);count+=1
    return count
if __name__=='__main__':
    a=argparse.ArgumentParser();a.add_argument('--output',type=Path,required=True);args=a.parse_args()
    print(json.dumps(dict(newTiles=prepare(args.output))))
