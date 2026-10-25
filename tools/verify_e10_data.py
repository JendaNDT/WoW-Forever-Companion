#!/usr/bin/env python3
"""Check the reviewed expanded distribution and new map without changing the E09 fallback."""
from pathlib import Path
import json,hashlib
from PIL import Image,ImageChops
from validate_content import validate

ROOT=Path(__file__).resolve().parents[1]

def main():
    old=json.loads((ROOT/'content/starter-v1.json').read_text());p=json.loads((ROOT/'content/e10-v4.json').read_text());validate(p)
    assert (p['generation'],len(p['maps']),len(p['entries']),len(p['locations']),len(p['relations']),len(p['sources']),len(p['guideRules']))==(4,5,139,62,106,140,36)
    actual={e['key']:e for e in p['entries']};assert all(e==actual[e['key']] for e in old['entries'])
    for kind in ['locations','relations','guideRules']:assert all(e in p[kind] for e in old[kind])
    assert all(not e['releaseConfirmed'] and e['verification']!='RELEASE' for e in p['entries'])
    assert not p['transportGraph']['edges'] and len(p['transportGraph']['nodes'])==3
    assert not any(l['entryKey']=='wow-forever:npc:5768' or l['entryKey'].startswith('wow-forever:quest:') for l in p['locations'])
    assert all(e['minimumLevel'] is None for e in p['entries'] if e['kind']=='QUEST' and any(s.startswith('e10_') for s in e['sourceIds']))
    assert (ROOT/'app/src/main/assets/content/starter.json').read_bytes()==(ROOT/'content/starter-v1.json').read_bytes()
    manifest=json.loads((ROOT/'research/e10/maps/tile-manifest.json').read_text());assert len(manifest)==17
    for f in manifest:
        path=ROOT/'content/e10-maps'/f['path'];assert hashlib.sha256(path.read_bytes()).hexdigest()==f['sha256']
        with Image.open(path) as image:assert image.size==(256,256);image.load()
    source=Image.open(ROOT/'research/e10/maps/the-barrens_1.60.1.69893.webp').convert('RGBA');assert source.size==(1002,668)
    rebuilt=Image.new('RGBA',source.size)
    for path in (ROOT/'content/e10-maps/maps/1413/2').glob('*/*.png'):
        with Image.open(path) as tile:rebuilt.paste(tile,(int(path.stem)*256,int(path.parent.name)*256))
    assert all(x==(0,0) for x in ImageChops.difference(source,rebuilt).getextrema())
    controls=[l for l in p['locations'] if l['entryKey'] in ['wow-forever:npc:3615','wow-forever:npc:3337','wow-forever:npc:5767','wow-forever:npc:3453','wow-forever:npc:3521']]
    assert len(controls)==5 and all(l['mapKey']=='wow-forever:map:1413' for l in controls)
    return {'passed':True,'generation':4,'entries':139,'locations':62,'maps':5,'new_tiles':17,'complete_distribution_tiles':85,'new_map_pixel_exact':True,'source_control_records':5,'independent_game_checks':0,'old_entries_unchanged':True,'fallback_generation':2,'verified_transport_edges':0}

if __name__=='__main__':print(json.dumps(main(),ensure_ascii=False,indent=2))
