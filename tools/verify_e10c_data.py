#!/usr/bin/env python3
"""Validate provenance, quest identity and the retained generation-5 content of E10.3."""
import json
from collections import Counter
from pathlib import Path
from validate_content import validate

ROOT = Path(__file__).resolve().parents[1]

def verify(pack):
    result = validate(pack)
    old = json.loads((ROOT / 'content/e10-v5.json').read_text())
    for section in ('entries', 'sources', 'locations', 'relations', 'guideRules'):
        assert all(row in pack[section] for row in old[section]), 'Changed published record: ' + section
    assert pack['generation'] == 6 and pack['checkedOn'] == '2026-10-25'
    assert (len(pack['entries']), len(pack['locations']), len(pack['relations']), len(pack['sources'])) == (216, 84, 188, 216)
    assert Counter(e['kind'] for e in pack['entries']) == dict(QUEST=91, NPC=83, ITEM=24, SPELL=14, TRAVEL=4)
    assert len(pack['guideRules']) == 91
    assert pack['transportGraph'] == old['transportGraph'] and not pack['transportGraph']['edges']
    for current, previous in zip(pack['maps'], old['maps']):
        assert {k:v for k,v in current.items() if k != 'scopeCs'} == {k:v for k,v in previous.items() if k != 'scopeCs'}
    assert all(not e['releaseConfirmed'] and e['verification'] != 'RELEASE' for e in pack['entries'])
    entries = {e['key']:e for e in pack['entries']}
    old_keys = {e['key'] for e in old['entries']}
    added = [e for e in pack['entries'] if e['key'] not in old_keys]
    assert all(e['minimumLevel'] is None for e in added if e['kind'] == 'QUEST')
    assert Counter(e['verification'] for e in added) == dict(CLASSIC=20, BETA=14)
    for r in pack['relations']:
        if ':e10c:' in r['key']:
            assert not r['routingEnabled'] and r['verification'] in ('CLASSIC', 'BETA')
    for r in pack['guideRules']:
        if ':e10c:' in r['key']:
            assert any(c['type']=='unknown' for c in r['condition']['conditions'])
    def relations(kind, owner):
        return [r for r in pack['relations'] if r['kind']==kind and r['ownerKey']==owner]
    def targets(kind, owner):
        return [target for r in relations(kind, owner) for target in r['targetKeys']]
    for quest in (832, 97281):
        assert not relations('START_NPC', f'wow-forever:quest:{quest}'), 'Item-started quest acquired a made-up giver'
    for quest, previous in ((1501,1506),(1504,1501),(835,834),(97282,97281),(809,829)):
        assert f'wow-forever:quest:{previous}' in targets('PREREQUISITE', f'wow-forever:quest:{quest}')
    assert not relations('PREREQUISITE','wow-forever:quest:832'), 'The found eye is not a collar-chain prerequisite'
    assert targets('END_NPC','wow-forever:quest:1504') == ['wow-forever:npc:5875']
    assert targets('END_NPC','wow-forever:quest:829') == ['wow-forever:npc:3216']
    assert targets('LEARN_SPELL','wow-forever:quest:1504') == ['wow-forever:spell:book:warlock:summon-voidwalker']
    for item in (11845,281274):
        assert not relations('REWARD_CHOICE',f'wow-forever:item:{item}'), 'Fixed reward must not become a choice'
    assert targets('REWARD_CHOICE','wow-forever:item:4938') == ['wow-forever:quest:835']
    for item in (4871,6658,4903,6535,7464):
        assert entries[f'wow-forever:item:{item}']['gameId'] == item
    bag=json.loads(entries['wow-forever:item:11845']['payload'])
    assert (bag['slots'],bag['classicSlots'],bag['questId']) == (6,4,791)
    storm=json.loads(entries['wow-forever:item:281274']['payload'])
    assert (storm['intellect'],storm['spellPower'],storm['itemLevel'],storm['questId']) == (2,4,11,97282)
    assert entries['wow-forever:item:281274']['minimumLevel'] is None
    classic_npcs = {3293,3193,3190,3294,5875,3147}
    assert all(entries[f'wow-forever:npc:{i}']['verification']=='CLASSIC' for i in classic_npcs)
    korghan=next(l for l in pack['locations'] if l['entryKey']=='wow-forever:npc:3189')
    assert (korghan['mapKey'],korghan['xPercent'],korghan['yPercent']) == ('wow-forever:map:1454',46.2,53.0)
    assert (ROOT/'app/src/main/assets/content/starter.json').read_bytes() == (ROOT/'content/starter-v1.json').read_bytes()
    return dict(**result, generation=6, new_entries=34, new_positions=10, old_records_unchanged=True,
        classic_reference_positions=6, beta_observed_positions=4, independent_in_game_checks=0, verified_transport_edges=0)

if __name__ == '__main__':
    print(json.dumps(verify(json.loads((ROOT/'content/e10-v6.json').read_text())), ensure_ascii=False, indent=2))
