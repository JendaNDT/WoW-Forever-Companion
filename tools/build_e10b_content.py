#!/usr/bin/env python3
"""Build the second E10 batch (generation 5) for the unchanged E12 application."""
import json
from pathlib import Path
from validate_content import validate

ROOT = Path(__file__).resolve().parents[1]
DATE = '2026-10-25'

def encoded(value):
    return json.dumps(value, ensure_ascii=False, separators=(',', ':'))

def build():
    pack = json.loads((ROOT / 'content/e10-v4.json').read_text())
    facts = json.loads((ROOT / 'research/e10b/facts.json').read_text())
    existing = {e['key'] for e in pack['entries']}

    def key(kind, value):
        return f'wow-forever:{kind.lower()}:{value}'

    def source(source_id, url, kind, fact, build=None):
        pack['sources'].append({'id': source_id, 'url': url, 'kind': kind,
            'checkedOn': DATE, 'build': build, 'payload': encoded({'facts': fact,
            'retrieval': 'public_page_review', 'caveatCs': 'Záznam zdroje není nezávislé ověření vydané hry. Úplné podmínky dostupnosti mohou chybět.'})})

    def entry(kind, value, name, description, fact, source_id, verification='BETA', minimum=None, level=None, aliases=()):
        entry_key = key(kind, value)
        assert entry_key not in existing, 'Existing identity must never be replaced: ' + entry_key
        existing.add(entry_key)
        pack['entries'].append({'key': entry_key, 'kind': kind, 'gameId': value if isinstance(value, int) else None,
            'nameOriginal': name, 'descriptionCs': description, 'aliases': [str(value), *aliases],
            'verification': verification, 'releaseConfirmed': False, 'minimumLevel': minimum,
            'reportedLevel': level, 'sourceIds': [source_id], 'payload': encoded(fact)})
        return entry_key

    def relation(kind, owner, target, source_id, verification='BETA'):
        pack['relations'].append({'key': f'wow-forever:relation:e10b:{kind.lower()}:{owner.rsplit(":", 1)[-1]}',
            'kind': kind, 'ownerKey': owner, 'targetKeys': [target], 'mode': 'ALL',
            'verification': verification, 'routingEnabled': False, 'sourceIds': [source_id]})

    def rule(owner, reason, source_id, importance, verification='BETA', minimum=None, warlock=False, areas=(1411,)):
        conditions = [{'type': 'faction', 'values': ['HORDE']}, {'type': 'area', 'uiMapIds': list(areas)}]
        if warlock:
            conditions.append({'type': 'class', 'values': ['WARLOCK']})
        if minimum is not None:
            conditions.append({'type': 'level', 'minimum': minimum, 'maximum': None})
        conditions.append({'type': 'unknown', 'reasonCs': 'Ověř dostupnost a případné předchozí kroky ve svém klientu; úplné podmínky pro vydanou Forever nejsou potvrzené.'})
        pack['guideRules'].append({'key': 'wow-forever:rule:e10b:' + owner.removeprefix('wow-forever:'),
            'entryKey': owner, 'condition': {'type': 'all', 'conditions': conditions},
            'importance': importance, 'reasonCs': reason, 'verification': verification,
            'sourceIds': [source_id], 'areaIds': list(areas)})

    for fact in facts['npcs']:
        value = fact['id']; sid = f'e10b_npc_{value}'
        source(sid, f'https://warcraftforever.games/npcs/{value}', 'publisher_reported_beta_observation', fact)
        owner = entry('NPC', value, fact['name'], fact['descriptionCs'], fact, sid, aliases=fact['aliases'])
        map_key = key('map', fact['uiMapId'])
        frame = next(m for m in pack['maps'] if m['key'] == map_key)
        pack['locations'].append({'key': owner + ':location:0', 'entryKey': owner,
            'mapKey': map_key, 'coordinateVersion': frame['coordinateVersion'], 'floorId': 0,
            'xPercent': fact['x'], 'yPercent': fact['y'], 'precisionPercent': 0.1, 'sourceId': sid})

    for fact in facts['quests']:
        value = fact['id']; sid = f'e10b_quest_{value}'
        classic = fact.get('classicReference', False)
        verification = 'CLASSIC' if classic else 'BETA'
        url = f'https://foreverchanges.pro/quest/{value}' if classic else f'https://warcraftforever.games/quests/{value}'
        source(sid, url, 'classic_reference' if classic else 'publisher_reported_beta_observation', fact)
        owner = entry('QUEST', value, fact['name'], fact['descriptionCs'], fact, sid, verification,
            level=fact['level'], aliases=['Durotar', 'začátek 1–10', *fact['aliases']])
        if fact['startNpcId'] is not None:
            relation('START_NPC', owner, key('npc', fact['startNpcId']), sid, verification)
        relation('END_NPC', owner, key('npc', fact['endNpcId']), sid, verification)
        rule(owner, fact['reasonCs'], sid, 'DISCOVERY', verification)

    for fact in facts['items']:
        value = fact['id']; sid = f'e10b_item_{value}'
        source(sid, f'https://warcraftforever.games/items/{value}', 'publisher_reported_beta_extraction', fact, '1.60.1.70009')
        owner = entry('ITEM', value, fact['name'], fact['descriptionCs'], fact, sid,
            aliases=['odměna', 'látka', 'výbava', *fact['aliases']])
        relation('REWARD_CHOICE', owner, key('quest', fact['questId']), sid)
        rule(owner, fact['descriptionCs'] + ' Před volbou porovnej s tím, co už máš.', sid, 'REWARD', warlock=True)

    sid = 'e10b_warlock_book'
    source(sid, 'https://foreverchanges.pro/spellbook/warlock', 'publisher_reported_beta_extraction',
        {'spells': facts['spells'], 'fullAccessConditionsUnknown': True}, '1.60.1.70532')
    for fact in facts['spells']:
        owner = entry('SPELL', 'book:warlock:' + fact['slug'], fact['name'],
            fact['descriptionCs'] + f" Beta kniha kouzel uvádí první rank od úrovně {fact['firstRankLevel']}. Ověř způsob získání v klientu.",
            fact, sid, minimum=fact['firstRankLevel'], aliases=['warlock', 'kouzlo', 'výcvik', *fact['aliases']])
        rule(owner, fact['descriptionCs'] + ' Uvedený výcvik není potvrzením konkrétního trenéra.', sid,
            'CLASS', minimum=fact['firstRankLevel'], warlock=True, areas=(1411, 1454))

    relation('PREREQUISITE', key('quest', 831), key('quest', 830), 'e10b_quest_831', 'CLASSIC')
    durotar = next(m for m in pack['maps'] if m['uiMapId'] == 1411)
    durotar['scopeCs'] = ('Startovní výběr 1–10: Valley of Trials, Sen’jin Village, Razor Hill, Kolkar Crag, Echo Isles a Tiragarde Keep. '
        'Úkoly, pět vybraných látkových odměn a milníky Warlocka. Beta pozorování a označené Classic návaznosti; '
        'nejde o úplný seznam Durotaru. Jeskyně mají pouze pozorované polohy na mapě oblasti, bez vnitřních pater. '
        'Vile Familiars 1485 má dál rozpor. Ověřené dopravní linky chybí.')
    pack['generation'] = 5
    pack['checkedOn'] = DATE
    result = validate(pack)
    target = ROOT / 'content/e10-v5.json'
    target.write_text(json.dumps(pack, ensure_ascii=False, indent=2) + '\n')
    return {'generation': 5, **result}

if __name__ == '__main__':
    print(json.dumps(build(), ensure_ascii=False, indent=2))
