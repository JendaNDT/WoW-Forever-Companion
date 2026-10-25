#!/usr/bin/env python3
"""Reproduce the pre-launch audit; downloaded Lua is read as literal data only."""
import collections,hashlib,json,gzip
from pathlib import Path
from build_e10_full import build,ROOT,PIN,encoded
from validate_content import validate
from catalog_io import catalog_json
AUDIT=ROOT/'research/e10audit'
def load(path):return catalog_json(path)
def run():
 for base in (ROOT/'research/e10full/upstream',AUDIT/'upstream'):
  snapshot=load(base/'snapshot.json');assert snapshot['revision']==PIN
  for row in snapshot['files']:
   raw=(base/row['path']).read_bytes();assert hashlib.sha256(raw).hexdigest()==row['sha256'],row['path'];assert hashlib.sha1(b'blob '+str(len(raw)).encode()+b'\0'+raw).hexdigest()==row['gitBlob'],row['path']
 p,import_audit=build(generation=8,extra_root=AUDIT/'upstream')
 before=load(ROOT/'content/e10-v7.json');old={e['key']:e for e in before['entries']};entries={e['key']:e for e in p['entries']}
 recorder=load(AUDIT/'recorder-quest-index.json');by_id={r['id']:r for r in recorder};assert len(by_id)==len(recorder)==1609
 rank_data=load(AUDIT/'foreverchanges-warlock-ranks.json');ranks=rank_data['ranks'];first={r['name']:r for r in ranks if r['rank'] in (None,1)}
 overlap=[];conflicts=[];spell_checks=[]
 for e in p['entries']:
  if e['kind']=='QUEST' and e['gameId'] in by_id:
   row=by_id[e['gameId']];overlap.append(e['key']);fields={}
   if e['nameOriginal']!=row['title']:fields['name']=[e['nameOriginal'],row['title']]
   if e.get('reportedLevel') and (row.get('level') or 0)>0 and e['reportedLevel']!=row['level']:fields['reportedLevel']=[e['reportedLevel'],row['level']]
   if fields:
    sid=f'audit_recorder_quest_{e["gameId"]}'
    p['sources'].append(dict(id=sid,url=f'https://warcraftforever.games/quests/{e["gameId"]}',kind='beta_recorder_index',checkedOn='2026-10-25',payload=encoded(row)))
    e['sourceIds'].append(sid);e['verification']='CONFLICT';e['descriptionCs']+=' Audit 25. 10. 2026: komunitní databáze a beta záznam se rozcházejí v úrovni nebo názvu. Hodnota z databáze zůstává referenční; potvrď ji až ve vydané hře.'
    conflicts.append(dict(entryKey=e['key'],fields=fields,status='open_beta_source_disagreement',sourceId=sid))
  if e['kind']=='SPELL':
   row=first.get(e['nameOriginal']);assert row,e['key'];assert e.get('minimumLevel') in (None,row['level']),e['key']
   if e.get('minimumLevel') is None:e['minimumLevel']=row['level']
   sid=f'audit_fc_spell_{row["id"]}';p['sources'].append(dict(id=sid,url=rank_data['sourceUrl'],kind='beta_client_spellbook',checkedOn='2026-10-25',build=rank_data['build'],payload=encoded(row)));e['sourceIds'].append(sid)
   spell_checks.append(dict(entryKey=e['key'],id=row['id'],level=row['level'],status='matches_client_reference_not_release_availability'))
 conflict_keys={r['entryKey'] for r in conflicts}
 for r in p['guideRules']:
  if r['entryKey'] in conflict_keys:r['verification']='CONFLICT'
 for r in p['relations']:
  if r['ownerKey'] in conflict_keys:r['verification']='CONFLICT'
 assert entries['wow-forever:quest:1485']['verification']=='CONFLICT'
 assert not any(e['releaseConfirmed'] or e['verification']=='RELEASE' for e in p['entries']);assert not any(r['routingEnabled'] for r in p['relations']);validate(p)
 differences=[]
 for k in sorted(old.keys()&entries.keys()):
  fields={f:{'before':old[k].get(f),'after':entries[k].get(f)} for f in ('nameOriginal','minimumLevel','reportedLevel','verification') if old[k].get(f)!=entries[k].get(f)}
  if fields:differences.append(dict(entryKey=k,fields=fields))
 result=dict(checkedOn='2026-10-25',generation=8,revision=PIN,fullStaticLayers=True,dynamicRuntimeApplied=False,counts={s:len(p[s]) for s in ['entries','locations','relations','sources','guideRules','maps']},sourceInventory={'questieFiles':29+len(load(AUDIT/'upstream/snapshot.json')['files']),'recorderIndexRows':len(recorder),'spellRanks':len(ranks)},kindCounts=dict(collections.Counter(e['kind'] for e in p['entries'])),verification=dict(collections.Counter(e['verification'] for e in p['entries'])),addedKeys=sorted(entries.keys()-old.keys()),removedKeys=sorted(old.keys()-entries.keys()),changedFacts=differences,recorderOverlap=len(overlap),recorderNameMatches=len(overlap)-sum('name' in c['fields'] for c in conflicts),recorderConflicts=conflicts,spellChecks=spell_checks,knownObjectiveConflict='wow-forever:quest:1485',limitations=['No release or in-game verification before launch.','Common Classic lineage is not independent confirmation.','Runtime faction/class/phase corrections are not executed.','Recorder absence is unknown, not evidence a quest was removed.','ForeverChanges full quest JSON could not be fetched; no claim of full cross-site comparison.','Objective counts, reward choice, drops, live travel and current spawns remain unverified.','Curated beta records are retained over conflicting Classic references; keys and personal database remain stable.'])
 raw=(encoded(p)+'\n').encode();(ROOT/'content/e10-v8.json').write_bytes(raw);(ROOT/'content/e10-v8.json.gz').write_bytes(gzip.compress(raw,compresslevel=9,mtime=0))
 (AUDIT/'audit-results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:result[k] for k in ['counts','recorderOverlap','recorderConflicts']},ensure_ascii=False));return result
if __name__=='__main__':run()
