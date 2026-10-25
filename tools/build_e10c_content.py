#!/usr/bin/env python3
"""Curate E10.3 (generation 6) without changing any published generation-5 record."""
import hashlib
import json
from pathlib import Path
from validate_content import validate

ROOT = Path(__file__).resolve().parents[1]

def encoded(value):
    return json.dumps(value, ensure_ascii=False, separators=(',', ':'))

def build():
    pack = json.loads((ROOT / 'content/e10-v5.json').read_text())
    facts = json.loads((ROOT / 'research/e10c/facts.json').read_text())
    audit = []
    existing = {e['key'] for e in pack['entries']}

    def key(kind, value):
        return f'wow-forever:{kind.lower()}:{value}'

    def source(sid, url, kind, fact, build=None):
        pack['sources'].append(dict(id=sid, url=url, kind=kind, checkedOn=facts['checkedOn'], build=build,
            payload=encoded(dict(facts=fact, retrieval='public_page_review',
                caveatCs='Kontrola veřejného zdroje, nikoli vlastní ověření vydané hry. Původ tooltipu neověřuje získání předmětu.'))))
        audit.append(dict(sourceId=sid, url=url, kind=kind, build=build,
            selectedFactsSha256=hashlib.sha256(encoded(fact).encode()).hexdigest()))

    def entry(kind, fact):
        owner = key(kind, fact['id'])
        assert owner not in existing, 'Published identity must not be replaced: ' + owner
        existing.add(owner)
        sid = f'e10c_{kind.lower()}_{fact["id"]}'
        source_kind = ('classic_reference' if fact['verification'] == 'CLASSIC' else
            'secondary_reported_beta_database' if kind == 'QUEST' else
            'publisher_reported_beta_observation' if kind == 'NPC' else 'publisher_reported_beta_extraction')
        source(sid, fact['sourceUrl'], source_kind, fact, fact.get('build'))
        ids = [sid]
        if fact.get('identitySourceUrl'):
            identity_sid = sid + '_identity'
            source(identity_sid, fact['identitySourceUrl'], 'identity_reference', dict(id=fact['id'], name=fact['name']))
            ids.append(identity_sid)
        description = ('Classic reference: ' if fact['verification'] == 'CLASSIC' else '') + fact['descriptionCs']
        pack['entries'].append(dict(key=owner, kind=kind, gameId=fact['id'], nameOriginal=fact['name'],
            descriptionCs=description, aliases=[str(fact['id']), *fact['aliases']], verification=fact['verification'],
            releaseConfirmed=False, minimumLevel=fact.get('requiredLevel'), reportedLevel=fact.get('level'),
            sourceIds=ids, payload=encoded(fact)))
        return owner, sid

    def relation(kind, owner, target, sid, verification):
        pack['relations'].append(dict(key=f'wow-forever:relation:e10c:{kind.lower()}:{owner.removeprefix("wow-forever:")}',
            kind=kind, ownerKey=owner, targetKeys=[target], mode='ALL', verification=verification,
            routingEnabled=False, sourceIds=[sid]))

    def rule(owner, fact, sid, importance, verification=None):
        areas = [1411, 1454] if fact.get('warlock') or fact['id'] in (812, 813, 829, 832) else [1411]
        conditions = [dict(type='faction', values=['HORDE']), dict(type='area', uiMapIds=areas)]
        if fact.get('warlock'):
            conditions.append(dict(type='class', values=['WARLOCK']))
            if fact['id'] in (1506, 1501, 1504, 6535, 7464):
                conditions.append(dict(type='level', minimum=10, maximum=None))
        conditions.append(dict(type='unknown', reasonCs='Ověř zadání a dostupnost ve svém klientu. Úplné podmínky vydané Forever nejsou potvrzené.'))
        pack['guideRules'].append(dict(key='wow-forever:rule:e10c:' + owner.removeprefix('wow-forever:'),
            entryKey=owner, condition=dict(type='all', conditions=conditions), importance=importance,
            reasonCs=fact.get('reasonCs', fact['descriptionCs']), verification=verification or fact['verification'],
            sourceIds=[sid], areaIds=areas))

    for fact in facts['npcs']:
        owner, sid = entry('NPC', fact)
        frame = next(m for m in pack['maps'] if m['uiMapId'] == fact['uiMapId'])
        pack['locations'].append(dict(key=owner + ':location:0', entryKey=owner, mapKey=frame['key'],
            coordinateVersion=frame['coordinateVersion'], floorId=0, xPercent=fact['x'], yPercent=fact['y'],
            precisionPercent=0.1, sourceId=sid))

    for fact in facts['quests']:
        owner, sid = entry('QUEST', fact)
        if fact['startNpcId'] is not None:
            relation('START_NPC', owner, key('npc', fact['startNpcId']), sid, fact['verification'])
        relation('END_NPC', owner, key('npc', fact['endNpcId']), sid, fact['verification'])
        if fact.get('previousQuestId'):
            relation('PREREQUISITE', owner, key('quest', fact['previousQuestId']), sid, fact['verification'])
        rule(owner, fact, sid, 'CLASS' if fact.get('warlock') else 'DISCOVERY')

    for fact in facts['items']:
        owner, sid = entry('ITEM', fact)
        quest = next(q for q in facts['quests'] if q['id'] == fact['questId'])
        # A beta tooltip cannot confirm a Classic quest's reward or unlock conditions.
        relationship_sid = f'e10c_quest_{quest["id"]}'
        if fact['role'] == 'CHOICE_REWARD':
            relation('REWARD_CHOICE', owner, key('quest', fact['questId']), relationship_sid, quest['verification'])
        rule(owner, fact, relationship_sid, 'REWARD' if fact['role'].endswith('REWARD') else 'CLASS' if fact.get('warlock') else 'DISCOVERY', quest['verification'])
        pack['guideRules'][-1]['sourceIds'].append(sid)

    relation('LEARN_SPELL', key('quest', 1504), key('spell', 'book:warlock:summon-voidwalker'), 'e10c_quest_1504', 'CLASSIC')
    relation('PREREQUISITE', key('quest', 809), key('quest', 829), 'e10c_quest_829', 'CLASSIC')
    next(m for m in pack['maps'] if m['uiMapId'] == 1411)['scopeCs'] = (
        'Výběr startu 1–12: Valley of Trials, Sen’jin Village, Razor Hill, Echo Isles, Tiragarde Keep, '
        'Thunder Ridge, Razorwind Canyon a okolí Skull Rock. Obsahuje orčí cestu k Voidwalkerovi, '
        'Rezlaka, brašnu a vybrané odměny. Beta záznamy a označené Classic reference; úplnost Durotaru se netvrdí. '
        'Některé nové NPC mají pouze Classic polohu. Jeskyně nemají vnitřní mapu, pozorovaná poloha není vchod ani trasa. '
        'Vile Familiars 1485 má dál rozpor. Ověřené dopravní linky chybí.')
    pack['generation'] = 6
    pack['checkedOn'] = facts['checkedOn']
    result = validate(pack)
    (ROOT / 'content/e10-v6.json').write_text(json.dumps(pack, ensure_ascii=False, indent=2) + '\n')
    (ROOT / 'research/e10c/facts-audit.json').write_text(json.dumps(dict(checkedOn=facts['checkedOn'],
        method='Direct public-page review; Classic positions explicitly retained as CLASSIC; selected facts only',
        gameChecksPerformed=False, pages=audit, limitsCs=facts['notesCs']), ensure_ascii=False, indent=2) + '\n')
    return dict(generation=6, new_entries=34, new_positions=10, **result)

if __name__ == '__main__':
    print(json.dumps(build(), ensure_ascii=False, indent=2))
