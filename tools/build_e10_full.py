#!/usr/bin/env python3
"""Reproducible, literal-only import of a pinned reference catalog. No Lua execution."""
import collections, hashlib, json, math, re, zipfile, gzip
from pathlib import Path
from questie_tables import effective_data, literal_table

ROOT=Path(__file__).resolve().parents[1]
UP=ROOT/'research/e10full/upstream'
PIN='5c094f6e9a264f0dc54e2fde9731a37a97ed197c'
DATE='2026-10-25'
def encoded(value): return json.dumps(value,ensure_ascii=False,separators=(',',':'))
def vals(value): return [v for _,v in sorted((value or {}).items()) if v is not None]
def key(kind,ident): return f'wow-forever:{kind.lower()}:{ident}'
def build(generation=7, extra_root=None):
    snapshot=json.loads((UP/'snapshot.json').read_text())
    assert snapshot['revision']==PIN
    for item in snapshot['files']:
        assert hashlib.sha256((UP/item['path']).read_bytes()).hexdigest()==item['sha256'],item['path']
    data,changed,patch_audit=effective_data(UP, extra_root=extra_root)
    with zipfile.ZipFile(ROOT/'app/src/main/assets/content/bundled-data-6.wfp') as z: p=json.loads(z.read('catalog.json'))
    p.update(generation=generation,checkedOn=DATE)
    entries={e['key']:e for e in p['entries']}; old=set(entries); maps={m['uiMapId']:m for m in p['maps']}
    sources={s['id']:s for s in p['sources']}; rules={r['entryKey']:r for r in p['guideRules']}
    inventory=json.loads((ROOT/'research/e10full/maps/inventory.json').read_text())
    area_to_ui=literal_table((UP/'support/Forever/Zones/areaIdToUiMapId.lua').read_text(),'ZoneDB.private.areaIdToUiMapId = ')[0]
    for row in inventory:
        ident=row['uiMapId'];sid=f'e10full_map_{ident}'
        sources[sid]=dict(id=sid,url=row['sourceUrl'],kind='extracted_map_reference',checkedOn=DATE,build=row['build'],payload=encoded(row))
        scope=('Přehled vnitřku instance; samostatný obrazový rámec bez převodu souřadnic NPC. Vstupy a úkoly hledej v katalogu.' if row.get('overviewOnly') else
            'Referenční katalog Horda / Warlock 1–60. Souřadnice převzaté z komunitní databáze; nejde o potvrzení vydané hry. Rozsah a mezery viz Pokrytí obsahu.')
        if ident in maps: maps[ident]['scopeCs']=scope;continue
        maps[ident]=dict(key=key('map',ident),uiMapId=ident,floorId=0,name=row['name'],width=row['width'],height=row['height'],levels=math.ceil(math.log2(max(row['width'],row['height'])/256))+1,
            build=row['build'],coordinateVersion=f'wow-forever:map:{ident}:{row["build"]}:v1',sourceId=sid,sourceUrl=row['sourceUrl'],transform=dict(scaleX=1.0,scaleY=1.0,offsetX=0.0,offsetY=0.0),scopeCs=scope)
    def area_ui(area):
        ident=area_to_ui.get(area)
        return ident if ident in maps and ident<900000 else None
    def selected(q):
        races=q.get(6) or 0; classes=q.get(7) or 0
        return bool(q.get(1)) and 0<=(q.get(4) or 0)<=60 and -1<=(q.get(5) or 0)<=60 and (not races or races&146) and (not classes or classes&256) and (q.get(17) not in {-81,-82,-141,-161,-162,-261,-262,-263} or classes&256)
    selected_ids={i for i,q in data['Quest'].items() if selected(q)}
    selected_ids.update(e['gameId'] for e in entries.values() if e['kind']=='QUEST' and e.get('gameId') in data['Quest'])
    needed={k:set() for k in data};needed['Quest']=selected_ids
    giver=set(); reward_for=collections.defaultdict(list); item_uses=collections.defaultdict(set)
    for i in selected_ids:
        q=data['Quest'][i]
        for f in (2,3):
            g=q.get(f) or {}
            for n in vals(g.get(1)):needed['Npc'].add(n);giver.add(n)
            needed['Object'].update(vals(g.get(2)));needed['Item'].update(vals(g.get(3)))
        o=q.get(10) or {}
        for group,kind in [(1,'Npc'),(2,'Object'),(3,'Item')]:
            needed[kind].update(t[1] for t in vals(o.get(group)) if isinstance(t,dict) and isinstance(t.get(1),int) and t[1]>0)
        for it in [q.get(11)]+vals(q.get(21)):
            if isinstance(it,int) and it>0:needed['Item'].add(it);item_uses[it].add(i)
    # A reward association does not say whether the item is fixed or one choice.
    for ident,item in data['Item'].items():
        quests=sorted(set(vals(item.get(6)))&selected_ids)
        useful=item.get(12)==1 or (item.get(12)==4 and item.get(13) in (0,1)) or (item.get(12)==2 and item.get(13) in (7,10,15,19))
        if quests and useful:needed['Item'].add(ident);reward_for[ident]=quests
    # Item drop sources are explicit references, never inferred drop chances.
    for ident in needed['Item']:
        item=data['Item'].get(ident,{})
        needed['Npc'].update(vals(item.get(2)));needed['Object'].update(vals(item.get(3)))
    # Horde-friendly services, including flight masters, inns, banks, trainers and vendors.
    forever_enum=(UP/'src/corrections/enum/expansions.lua').read_text().split('-- Forever',1)[1]
    service_flags,unknown=literal_table(forever_enum,'npcFlags = ')
    assert not unknown
    service_names=dict(VENDOR='obchodník',FLIGHT_MASTER='letový mistr',TRAINER='trenér',INNKEEPER='hostinský',BANKER='banka',AUCTIONEER='aukce',REPAIR='opravy')
    service_bits=sum(service_flags[n] for n in service_names)
    services=set()
    for ident,n in data['Npc'].items():
        if n.get(13) in ('H','AH') and isinstance(n.get(15),int) and n[15]&service_bits and any(area_ui(a) for a in (n.get(7) or {})):
            services.add(ident);needed['Npc'].add(ident)
    missing=[]
    def ver(kind,ident):return 'COMMUNITY' if ident in changed[kind] else 'CLASSIC'
    def source(kind,ident,row):
        sid=f'e10full_{kind.lower()}_{ident}'
        # Raw spawn vectors remain in the checksummed upstream snapshot, not duplicated in memory.
        fields={k:v for k,v in row.items() if not (kind=='Npc' and k in (7,8)) and not (kind=='Object' and k in (4,7))}
        sources[sid]=dict(id=sid,url=f'https://github.com/Questie/QuestieDB/tree/{PIN}',kind='questie_pinned_reference',checkedOn=DATE,
            payload=encoded(dict(entity=kind,id=ident,revision=PIN,fields=fields,rowSha256=hashlib.sha256(encoded(row).encode()).hexdigest(),staticForeverPatch=ident in changed[kind])))
        return sid
    def names(kind,ids):return ', '.join(data[kind].get(i,{}).get(1,f'ID {i}') for i in ids)
    for kind in ('Quest','Npc','Object','Item'):
        for ident in sorted(needed[kind]):
            row=data[kind].get(ident)
            if not row or not row.get(1):missing.append(dict(kind=kind,id=ident,reason='missing source row'));continue
            ek=key(kind,ident)
            if ek in old:continue
            sid=source(kind,ident,row); description=[];aliases=[str(ident)]
            if kind=='Quest':
                if row.get(7,0) and row[7]&256:description.append('Třídní úkol pro Warlocka.');aliases+=['Warlock','třídní úkol']
                for field,label in [(2,'Začátek'),(3,'Odevzdání')]:
                    group=row.get(field) or {};parts=[]
                    for n,k in [(1,'Npc'),(2,'Object'),(3,'Item')]:
                        if group.get(n):parts.append(names(k,vals(group[n])))
                    if parts:description.append(label+': '+'; '.join(parts)+'.')
                o=row.get(10) or {}
                for group,k,label in [(1,'Npc','Cílové postavy'),(2,'Object','Cílové objekty'),(3,'Item','Potřebné předměty')]:
                    ids=[t[1] for t in vals(o.get(group)) if isinstance(t,dict) and t.get(1) in data[k]]
                    if ids:description.append(label+': '+names(k,ids)+'.')
                if row.get(12):description.append('Předchozí úkoly, všechny: '+names('Quest',vals(row[12]))+'.')
                if row.get(13):description.append('Předchozí úkoly, alespoň jeden: '+names('Quest',vals(row[13]))+'.')
                if row.get(25):description.append('Podklad uvádí aktivní nadřazený úkol: '+names('Quest',[row[25]])+'.')
                if row.get(27):description.append('Volitelná naváděcí návaznost; není považována za povinný předpoklad.')
                if row.get(18) or row.get(19) or row.get(20) or row.get(30) or row.get(34) or row.get(35) or row.get(36):description.append('Další podmínky (profese, reputace, fáze nebo jiný stav) ověř v zadání; nejsou automaticky vyhodnocené.')
                if (row.get(24) or 0)&1:description.append('Podklad označuje úkol jako opakovatelný.')
                description.append('Počty cílů a úplné podmínky zkontroluj ve hře. Jde o referenci, nikoli potvrzenou dostupnost.')
            elif kind=='Npc':
                if row.get(14):description.append('Role v podkladu: '+str(row[14])+'.')
                if ident in giver:description.append('Zadává nebo přijímá úkol z katalogu.')
                if ident in services:
                    labels=[label for name,label in service_names.items() if row[15]&service_flags[name]]
                    description.append('Služby podle příznaků Forever: '+', '.join(labels)+'. Dostupnost a konkrétní nabídku ověř ve hře.');aliases+=['služby']+labels
                else:description.append('Postava související s úkolem nebo zdrojem předmětu.')
                description.append('Značky jsou explicitní místa výskytu ze zdroje; u mnoha výskytů je zobrazen omezený vzorek, nikoli trasa pohybu.')
            elif kind=='Object':description.append('Herní objekt související s úkolem nebo získáním předmětu. Značky uvádějí referenční místa výskytu, nikoli záruku právě dostupného objektu.');aliases+=['objekt']
            else:
                if reward_for[ident]:description.append('Podklad spojuje předmět s odměnou za: '+names('Quest',reward_for[ident])+'. Nerozlišuje pevnou odměnu a volbu; ověř při odevzdání.');aliases+=['odměna']
                else:description.append('Předmět použitý v úkolu nebo při jeho zahájení.')
                if vals(row.get(2)):description.append('Zdroje mezi postavami: '+names('Npc',vals(row[2]))+'.')
                if vals(row.get(3)):description.append('Zdroje mezi objekty: '+names('Object',vals(row[3]))+'.')
                if row.get(10):description.append(f'Požadovaná úroveň podle zdroje: {row[10]}.')
            entries[ek]=dict(key=ek,kind=kind.upper(),gameId=ident,nameOriginal=row[1],descriptionCs=' '.join(description),aliases=aliases,verification=ver(kind,ident),releaseConfirmed=False,
                minimumLevel=(row.get(4) if kind=='Quest' else row.get(10) if kind=='Item' else None),reportedLevel=(row.get(5) if kind=='Quest' else None),
                sourceIds=[sid],payload=encoded(dict(importer='E10-full',entity=kind,id=ident,revision=PIN)),referencePosition=kind in ('Npc','Object'))
            for field in ('minimumLevel','reportedLevel'):
                if entries[ek][field] not in range(1,61):entries[ek][field]=None
    sample_audit=[];unmapped=collections.Counter()
    for kind,field in [('Npc',7),('Object',4)]:
        for ident in sorted(needed[kind]):
            ek=key(kind,ident)
            if ek not in entries or ek in old:continue
            row=data[kind][ident];by_map=collections.defaultdict(list)
            for area,points in (row.get(field) or {}).items():
                ui=area_ui(area)
                if not ui:unmapped[area]+=len(vals(points));continue
                for pair in vals(points):
                    if not isinstance(pair,dict):continue
                    x,y=pair.get(1),pair.get(2)
                    if isinstance(x,(int,float)) and isinstance(y,(int,float)) and math.isfinite(x) and math.isfinite(y) and 0<x<100 and 0<y<100 and (x,y) not in by_map[ui]:by_map[ui].append((x,y))
            for ui,points in sorted(by_map.items()):
                original=len(points);limit=original if ident in giver and kind=='Npc' else 6
                if len(points)>limit:points=[points[round(i*(len(points)-1)/(limit-1))] for i in range(limit)]
                if original>len(points):sample_audit.append(dict(entryKey=ek,uiMapId=ui,sourceCount=original,includedCount=len(points)))
                m=maps[ui]
                for index,(x,y) in enumerate(points):p['locations'].append(dict(key=f'{ek}:location:full:{ui}:{index}',entryKey=ek,mapKey=m['key'],coordinateVersion=m['coordinateVersion'],floorId=0,xPercent=x,yPercent=y,precisionPercent=0.1,sourceId=entries[ek]['sourceIds'][0]))
    relations=p['relations'];edges=collections.defaultdict(set)
    for r in relations:
        if r['kind']=='PREREQUISITE':edges[r['ownerKey']].update(r['targetKeys'])
    cycles=[];missing_relation_targets=[]
    def reaches(a,b):
        todo=[a];seen=set()
        while todo:
            n=todo.pop()
            if n==b:return True
            if n not in seen:seen.add(n);todo.extend(edges[n]-seen)
        return False
    def relation(kind,owner,targets,mode='ALL'):
        absent=sorted(set(targets)-entries.keys())
        if absent:
            missing_relation_targets.append(dict(kind=kind,ownerKey=owner,missingTargetKeys=absent,wholeGroupOmitted=kind=='PREREQUISITE'))
            if kind=='PREREQUISITE':return
        targets=sorted(set(targets)&entries.keys())
        if owner not in entries or not targets:return
        if kind=='PREREQUISITE':
            if any(reaches(t,owner) for t in targets):cycles.append(dict(ownerKey=owner,targetKeys=targets));return
            edges[owner].update(targets)
        relations.append(dict(key=f'wow-forever:relation:full:{kind.lower()}:{owner.split(":")[-2]}:{owner.split(":")[-1]}:{mode.lower()}',kind=kind,ownerKey=owner,targetKeys=targets,mode=mode,
            verification=entries[owner]['verification'],routingEnabled=False,sourceIds=entries[owner]['sourceIds']))
    direct=collections.defaultdict(set)
    for l in p['locations']:direct[l['entryKey']].add(int(l['mapKey'].split(':')[-1]))
    for ident in sorted(selected_ids):
        ek=key('Quest',ident)
        if ek not in entries or ek in old:continue
        q=data['Quest'][ident];areas=set();o=q.get(10) or {}
        for field,label in [(2,'START'),(3,'END')]:
            for group,kind in [(1,'Npc'),(2,'Object'),(3,'Item')]:
                targets=[key(kind,i) for i in vals((q.get(field) or {}).get(group))]
                relation(f'{label}_{kind.upper()}',ek,targets)
                for target in targets:areas.update(direct[target])
        for group,kind in [(1,'Npc'),(2,'Object'),(3,'Item')]:
            relation(f'OBJECTIVE_{kind.upper()}',ek,[key(kind,t[1]) for t in vals(o.get(group)) if isinstance(t,dict) and t.get(1)])
        for field,mode in [(12,'ALL'),(13,'ANY')]:relation('PREREQUISITE',ek,[key('Quest',i) for i in vals(q.get(field))],mode)
        relation('EXCLUSIVE_QUESTS',ek,[key('Quest',i) for i in vals(q.get(16)) if i!=ident],mode='ANY')
        if area_ui(q.get(17)):areas.add(area_ui(q[17]))
        races=q.get(6) or 0;conditions=[dict(type='faction',values=['HORDE']),dict(type='class',values=['WARLOCK'])]
        if races:conditions.append(dict(type='race',values=[name for name,mask in [('ORC',2),('UNDEAD',16),('TROLL',128)] if races&mask]))
        conditions.append(dict(type='level',minimum=max(1,q.get(4) or 1)))
        conditions.append(dict(type='unknown',reasonCs='Referenční statická databáze; úplné živé podmínky, fáze a dostupnost vydané Forever nejsou ověřené.'))
        rules[ek]=dict(key=f'wow-forever:rule:full:quest:{ident}',entryKey=ek,condition=dict(type='all',conditions=conditions),importance='CLASS' if (q.get(7) or 0)&256 or q.get(17)==-61 else 'DISCOVERY',
            reasonCs='Třídní návaznost Warlocka podle podkladu; ověř ve hře.' if (q.get(7) or 0)&256 or q.get(17)==-61 else 'Úkol z referenčního katalogu Hordy 1–60; úplné podmínky ověř ve hře.',verification=entries[ek]['verification'],sourceIds=entries[ek]['sourceIds'],areaIds=sorted(areas))
    for ident in sorted(needed['Item']):
        ek=key('Item',ident)
        if ek not in entries or ek in old:continue
        item=data['Item'].get(ident,{})
        for field,kind in [(2,'Npc'),(3,'Object')]:relation(f'ITEM_SOURCE_{kind.upper()}',ek,[key(kind,n) for n in vals(item.get(field))],mode='ANY')
    rewards_by_quest=collections.defaultdict(list)
    for ident,quests in reward_for.items():
        for quest in quests:rewards_by_quest[quest].append(key('Item',ident))
    for quest,items in sorted(rewards_by_quest.items()):
        if key('Quest',quest) not in old:relation('QUEST_REWARD',key('Quest',quest),items,'ANY')
    # Only the reviewed Classic/Forever subset, never later-expansion dungeon entrances.
    dungeon_text=(UP/'support/Forever/Zones/dungeons.lua').read_text()
    dungeon_text=re.sub(r'\(isHorde and (\{[^}]+\})\) or \{[^}]+\}',r'\1',dungeon_text)
    dungeons,unknown=literal_table(dungeon_text,'local dungeons = ')
    assert not unknown
    dungeon_ids=[209,491,718,719,721,722,796,1176,1337,1477,1581,1583,1584,1977,2017,2057,2100,2159,2437,2557,2677,2717,3428,3429,3456,3457,16544,16611,16732,16919]
    for ident in dungeon_ids:
        row=dungeons[ident];ek=key('Travel',f'dungeon_{ident}')
        sid=f'e10full_dungeon_{ident}';src=dict(id=sid,url=f'https://github.com/Questie/QuestieDB/blob/{PIN}/support/Forever/Zones/dungeons.lua',kind='questie_pinned_reference',checkedOn=DATE,payload=encoded(dict(areaId=ident,fields=row,revision=PIN)))
        sources[sid]=src
        aliases=[ident]+vals(row.get(2));quests=sorted(i for i in selected_ids if data['Quest'][i].get(17) in aliases)
        entries[ek]=dict(key=ek,kind='TRAVEL',gameId=None,nameOriginal=row[1],descriptionCs='Příprava na dungeon nebo raid: před odchodem projdi připojené úkoly a jejich zadavatele. Vstup je pouze komunitní reference, nikoli ověřená dopravní trasa. Podmínky vstupu, skupinu a dostupnost ověř ve hře.',
            aliases=['dungeon','instance','příprava'],verification='COMMUNITY',releaseConfirmed=False,minimumLevel=None,reportedLevel=None,sourceIds=[sid],payload=encoded(dict(importer='E10-full',areaId=ident)),referencePosition=True)
        areas=set()
        for index,point in enumerate(vals(row.get(4))):
            ui=area_ui(point.get(1))
            if not ui:continue
            m=maps[ui];areas.add(ui)
            p['locations'].append(dict(key=f'{ek}:location:{index}',entryKey=ek,mapKey=m['key'],coordinateVersion=m['coordinateVersion'],floorId=0,xPercent=point[2],yPercent=point[3],precisionPercent=0.1,sourceId=sid))
        relation('DUNGEON_QUEST',ek,[key('Quest',q) for q in quests],'ANY')
        rules[ek]=dict(key=f'wow-forever:rule:full:dungeon:{ident}',entryKey=ek,condition=dict(type='unknown',reasonCs='Vstup a nabídku dungeonových úkolů je nutné potvrdit v aktuální Forever.'),importance='DUNGEON',reasonCs='Projdi zadavatele připojených úkolů před cestou do instance.',verification='COMMUNITY',sourceIds=[sid],areaIds=sorted(areas))
    p['entries']=list(entries.values());p['maps']=list(maps.values());p['sources']=list(sources.values());p['guideRules']=list(rules.values())
    audit=dict(revision=PIN,checkedOn=DATE,layers=patch_audit,counts={s:len(p[s]) for s in ['maps','entries','locations','relations','sources','guideRules']},kinds=dict(collections.Counter(e['kind'] for e in p['entries'])),
        verification=dict(collections.Counter(e['verification'] for e in p['entries'])),selectedQuestIds=sorted(selected_ids),warlockQuestIds=sorted(i for i in selected_ids if (data['Quest'][i].get(7) or 0)&256 or data['Quest'][i].get(17)==-61),
        sampledLocations=sample_audit,unmappedAreaPointCounts=dict(sorted(unmapped.items())),missingRows=missing,omittedCyclicPrerequisiteGroups=cycles,missingRelationTargets=missing_relation_targets,serviceFlagsUsed={n:service_flags[n] for n in service_names},
        limitations=['No in-game release observations or verified transport edges added.',('No dynamic runtime corrections; static legacy, delta, traces and authored corrections applied.' if extra_root else 'No inherited legacy corrections, generated traces or runtime Lua execution.'),'Reward association does not identify fixed versus choice.','Interior coordinates are never placed on overview images.','Enemy/object spawns are limited to six explicit points per map; questgiver spawns are retained.','Objective counts and live conditions require checking in game.'])
    raw=(encoded(p)+'\n').encode()
    (ROOT/f'content/e10-v{generation}.json').write_bytes(raw)
    (ROOT/f'content/e10-v{generation}.json.gz').write_bytes(gzip.compress(raw,compresslevel=9,mtime=0))
    (ROOT/('research/e10audit/import-audit.json' if extra_root else 'research/e10full/import-audit.json')).write_text(json.dumps(audit,ensure_ascii=False,indent=2)+'\n')
    (ROOT/'app/src/main/assets/content/supported-maps.json').write_text(json.dumps(p['maps'],ensure_ascii=False,indent=2)+'\n')
    frames=',\n'.join(f'        {m["uiMapId"]} to Triple({m["width"]}, {m["height"]}, {m["levels"]})' for m in p['maps'])
    (ROOT/'app/src/main/java/cz/jenda/wowforever/content/SupportedMapFrames.kt').write_text('package cz.jenda.wowforever.content\n\n// Generated from checksummed E10 map inventory by tools/build_e10_full.py.\nobject SupportedMapFrames {\n    val frames = mapOf(\n'+frames+'\n    )\n    val ids = frames.keys\n}\n')
    class_keys=',\n'.join('        "'+key('Quest',ident)+'"' for ident in audit['warlockQuestIds'])
    (ROOT/'app/src/main/java/cz/jenda/wowforever/content/WarlockQuestIndex.kt').write_text('package cz.jenda.wowforever.content\n\n// Generated class/sort index from pinned QuestieDB '+PIN+'.\n// Classification is independent of availability and does not change older records.\nobject WarlockQuestIndex {\n    val keys = setOf(\n'+class_keys+'\n    )\n}\n')
    print(encoded({**audit['counts'],'kinds':audit['kinds'],'catalogBytes':(ROOT/f'content/e10-v{generation}.json').stat().st_size,'missingRows':len(missing),'cycles':len(cycles),'warlockQuests':len(audit['warlockQuestIds'])}))
    return p,audit
if __name__=='__main__':build()
