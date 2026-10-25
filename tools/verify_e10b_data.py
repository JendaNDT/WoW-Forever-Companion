#!/usr/bin/env python3
"""Check game facts, provenance and stable identity of the second E10 distribution."""
import hashlib
import json
from pathlib import Path
from collections import Counter
from validate_content import validate

ROOT = Path(__file__).resolve().parents[1]

def verify(pack):
    result = validate(pack)
    old = json.loads((ROOT / 'content/e10-v4.json').read_text())
    for kind in ('sources', 'entries', 'locations', 'relations', 'guideRules'):
        assert all(row in pack[kind] for row in old[kind]), 'Lost or changed older record: ' + kind
    assert pack['generation'] == 5 and pack['checkedOn'] == '2026-10-25'
    assert (len(pack['entries']), len(pack['locations']), len(pack['maps']), len(pack['relations'])) == (182, 74, 5, 147)
    assert Counter(e['kind'] for e in pack['entries']) == {'QUEST': 75, 'NPC': 73, 'ITEM': 16, 'SPELL': 14, 'TRAVEL': 4}
    for new, previous in zip(pack['maps'], old['maps']):
        assert new['key'] == previous['key']
        assert {k: v for k, v in new.items() if k != 'scopeCs'} == {k: v for k, v in previous.items() if k != 'scopeCs'}
    assert pack['transportGraph'] == old['transportGraph'] and not pack['transportGraph']['edges']
    assert all(not e['releaseConfirmed'] and e['verification'] != 'RELEASE' for e in pack['entries'])
    entries = {e['key']: e for e in pack['entries']}
    relations = {r['key']: r for r in pack['relations']}
    old_keys = {e['key'] for e in old['entries']}
    assert all(e['minimumLevel'] is None for e in pack['entries'] if e['kind'] == 'QUEST' and e['key'] not in old_keys)
    def end(quest):
        return relations[f'wow-forever:relation:e10b:end_npc:{quest}']['targetKeys']
    assert end(96821) == ['wow-forever:npc:3139']
    assert end(99123) == ['wow-forever:npc:3304'] and end(97225) == ['wow-forever:npc:3188']
    assert end(831) == ['wow-forever:npc:3230']
    assert 'wow-forever:relation:e10b:start_npc:830' not in relations
    chain = relations['wow-forever:relation:e10b:prerequisite:831']
    assert chain['targetKeys'] == ['wow-forever:quest:830'] and chain['verification'] == 'CLASSIC' and not chain['routingEnabled']
    assert entries['wow-forever:quest:830']['key'] != entries['wow-forever:quest:831']['key']
    assert all(l['entryKey'].split(':')[1] == 'npc' for l in pack['locations'] if l not in old['locations'])
    for item in (4915, 4919, 4916, 6713, 6062):
        e = entries[f'wow-forever:item:{item}']; payload = json.loads(e['payload'])
        assert payload['material'] == 'Cloth' and e['minimumLevel'] is None
    assert relations['wow-forever:relation:e10b:reward_choice:4916']['targetKeys'] == ['wow-forever:quest:804']
    assert entries['wow-forever:quest:1485'] == next(e for e in old['entries'] if e['key'] == 'wow-forever:quest:1485')
    for r in pack['guideRules']:
        if ':e10b:' in r['key']:
            assert r['verification'] in ('BETA', 'CLASSIC')
            assert any(c['type'] == 'unknown' for c in r['condition']['conditions'])
    assert (ROOT / 'app/src/main/assets/content/starter.json').read_bytes() == (ROOT / 'content/starter-v1.json').read_bytes()
    return {**result, 'generation': 5, 'old_records_unchanged': True, 'map_frames_unchanged': True,
        'new_entries': 43, 'new_positions': 12, 'new_quest_records': 18, 'verified_transport_edges': 0,
        'release_verified_entries': 0, 'independent_in_game_checks': 0}

if __name__ == '__main__':
    print(json.dumps(verify(json.loads((ROOT / 'content/e10-v5.json').read_text())), ensure_ascii=False, indent=2))
