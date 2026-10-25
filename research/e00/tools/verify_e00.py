"""Kontrola předávacího balíčku E00. Vyžaduje pouze standardní Python 3."""
from pathlib import Path
import json,hashlib
from collections import Counter

ROOT=Path(__file__).resolve().parents[1]
def read(path):return json.loads((ROOT/path).read_text(encoding='utf-8'))
def require(condition,message):
    if not condition:raise ValueError(message)

def main():
    inventory=read('maps/inventory.json')
    catalog=read('content/sample_catalog.json')
    sources=read('content/sources.json')['sources']
    controls=read('maps/control_points.json')['points']
    source_ids={s['id'] for s in sources}
    require(len(source_ids)==len(sources),'Duplicitní ID zdrojů')
    maps=inventory['maps']; map_ids={m['id'] for m in maps}
    require(len(map_ids)==len(maps),'Duplicitní klíč mapy')
    for m in maps:
        p=ROOT/m['file']
        require(p.is_file(),f'Chybí mapa {m["file"]}')
        require(hashlib.sha256(p.read_bytes()).hexdigest()==m['sha256'],f'Nesedí otisk {p.name}')
        require(p.stat().st_size==m['bytes'],f'Nesedí velikost {p.name}')
        require(m['width']>0 and m['height']>0,'Neplatné rozměry')
        require(m['parent_map_key'] is None or m['parent_map_key'] in map_ids,'Neexistující rodič mapy')
    durotar=next(m for m in maps if m['ui_map_id']==1411)
    require(durotar['area_id']==14,'Záměna areaId a uiMapId')
    groups=['npcs','quests','items','spells','relations','travel']
    records=[r for g in groups for r in catalog[g]]
    all_ids=[r['id'] for r in records]
    require(len(set(all_ids))==len(all_ids),'Duplicitní klíč herního záznamu')
    npc_ids={r['id'] for r in catalog['npcs']}
    quest_ids={r['id'] for r in catalog['quests']}
    for r in records:
        require(bool(r.get('source_ids')),'Záznam bez zdroje')
        require(set(r['source_ids'])<=source_ids,f'Chybějící zdroj u {r["id"]}')
        require(bool(r.get('verification_status')),'Chybí stav ověření')
    for n in catalog['npcs']:
        if n['location'] is None:continue
        loc=n['location']
        require(loc['map_key'] in map_ids,'NPC na neexistující mapě')
        require(0<=loc['x_percent']<=100 and 0<=loc['y_percent']<=100,'Souřadnice mimo mapu')
        require(loc['source_id'] in source_ids,'Poloha bez zdroje')
    for q in catalog['quests']:
        require(q['start_npc_key'] in npc_ids and q['end_npc_key'] in npc_ids,'Úkol odkazuje na neznámé NPC')
        require(q['minimum_level'] is None,'Minimum nelze odvodit z úrovně úkolu')
        require(q['release_confirmed'] is False and q['eligible_for_release_verified_recommendations'] is False,'Neověřený záznam označen jako ověřený pro vydání')
    for rel in catalog['relations']:
        require(rel['quest_key'] in quest_ids and set(rel['dependency_keys'])<=quest_ids,'Návaznost odkazuje mimo vzorek')
        require(rel['active_for_verified_routing'] is False,'Referenční podmínka omylem aktivována')
    q1485=next(q for q in catalog['quests'] if q['game_id']==1485)
    require(q1485['objective_count'] is None,'Rozporný počet byl předčasně rozhodnut')
    require({v['value'] for v in q1485['objective_count_claims']}=={6,8},'Chybí evidence rozporu')
    for claim in q1485['objective_count_claims']:require(claim['source_id'] in source_ids,'Tvrzení bez zdroje')
    for item in catalog['items']:
        for claim in item['acquisition_claims']:
            require(claim['quest_key'] in quest_ids and claim['source_id'] in source_ids,'Neplatný zdroj předmětu')
    for edge in catalog['travel']:
        require(edge['origin_npc_key'] in npc_ids and edge['origin_map_key'] in map_ids,'Neplatná dopravní vazba')
        require(edge['routing_enabled'] is False and edge['destination_coordinates'] is None,'Neověřená doprava aktivována')
    require(len(controls)==8 and all(c['native_render_verified'] is False for c in controls),'Kontrolní místa chybí nebo jsou falešně označena jako zkalibrovaná')
    same_names=Counter(q['name_original'] for q in catalog['quests'])
    require(same_names['Vile Familiars']==3,'Tři různé úkoly stejného jména byly sloučeny')
    report={'status':'passed','maps_with_valid_checksums':len(maps),'catalog_record_count':len(records),'unique_catalog_keys':len(set(all_ids)),'source_count':len(sources),'records_with_source':len(records),'npc_beta_positions':sum(n['location'] is not None for n in catalog['npcs']),'npc_position_total':len(catalog['npcs']),'quest_minimum_level_unknown':sum(q['minimum_level'] is None for q in catalog['quests']),'quest_count':len(catalog['quests']),'release_verified_quests':0,'native_calibrated_points':0,'same_name_different_quest_ids':[792,1485,1499],'interpretation':'Strukturální kontrola a integrita souborů prošla. Neprokazuje kalibraci mapy ani správnost ve vydaném klientu hry.'}
    target=ROOT/'docs/quality_report.json'
    target.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,ensure_ascii=False,indent=2))

if __name__=='__main__':main()
