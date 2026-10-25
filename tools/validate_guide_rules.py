"""Semantic checks after the content JSON schema; mirrors the Android import boundary."""
from zoneinfo import ZoneInfo
import re

def validate_guide_rules(pack):
    rules=pack.get('guideRules',[])
    assert len(rules)<=6000
    assert len({r['key'] for r in rules})==len(rules) and len({r['entryKey'] for r in rules})==len(rules)
    entries={e['key']:e for e in pack['entries']};sources={s['id']:s for s in pack['sources']}
    relations={r['key']:r for r in pack['relations']}
    for r in rules:
        owner=entries[r['entryKey']]
        assert r['reasonCs'].strip() and r['sourceIds'] and len(set(r['sourceIds']))==len(r['sourceIds']) and all(s in sources for s in r['sourceIds'])
        if r['verification']=='RELEASE':
            assert owner['verification']=='RELEASE' and owner['releaseConfirmed']
            assert any(sources[s]['kind']=='in_game_release_observation' for s in r['sourceIds'])
        w=r.get('window')
        if w:
            assert 0<=w['fromEpochMillis']<w['untilEpochMillis']
            ZoneInfo(w['timeZone'])
        if r.get('exclusiveRelationKey'):
            rel=relations[r['exclusiveRelationKey']]
            assert rel['kind']=='EXCLUSIVE_QUESTS' and r['entryKey'] in [rel['ownerKey'],*rel['targetKeys']]
            if r['verification']=='RELEASE':assert rel['verification']=='RELEASE' and rel['routingEnabled']
        nodes=0
        def target(k,kind):
            assert entries[k]['kind']==kind
            if r['verification']=='RELEASE':assert entries[k]['verification']=='RELEASE' and entries[k]['releaseConfirmed']
        def condition(c,depth=0):
            nonlocal nodes
            nodes+=1;assert nodes<=200 and depth<=8
            kind=c['type']
            if kind in ('all','any'):
                assert c['conditions']
                for child in c['conditions']:condition(child,depth+1)
            elif kind=='unknown':assert c['reasonCs'].strip()
            elif kind in ('faction','class','race','mode','profession'):
                assert all(v.strip() for v in c['values'])
                if kind=='faction':assert set(c['values'])<={'HORDE','ALLIANCE'}
                if kind in ('class','race'):assert all(re.fullmatch('[A-Z][A-Z_]*',v) for v in c['values'])
            elif kind=='level':assert c.get('maximum') is None or c['minimum']<=c['maximum']
            elif kind=='area':assert set(c['uiMapIds']) <= {m['uiMapId'] for m in pack['maps']}
            elif kind=='quest_completed':target(c['questKey'],'QUEST')
            elif kind=='item_acquired':target(c['itemKey'],'ITEM')
            elif kind=='reward_selected':
                target(c['questKey'],'QUEST');target(c['itemKey'],'ITEM')
                assert any(rel['kind']=='REWARD_CHOICE' and rel['ownerKey']==c['itemKey'] and c['questKey'] in rel['targetKeys'] and (r['verification']!='RELEASE' or (rel['verification']=='RELEASE' and rel['routingEnabled'])) for rel in pack['relations'])
        condition(r['condition'])
