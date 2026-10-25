import hashlib,json,unittest
from pathlib import Path
from build_e10_full import ROOT
from questie_tables import literal_table,effective_data
from validate_content import validate
from bundle_parts import materialized
from catalog_io import catalog_json
class PrelaunchAuditTest(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.p=catalog_json(ROOT/'content/e10-v8.json');cls.a=json.loads((ROOT/'research/e10audit/audit-results.json').read_text());cls.entries={e['key']:e for e in cls.p['entries']}
 def test_signed_contract_no_release_claims_and_all_keys_resolve(self):
  self.assertTrue(validate(self.p)['passed']);self.assertEqual(8,self.p['generation']);self.assertEqual(11912,len(self.entries));self.assertFalse(any(e['releaseConfirmed'] or e['verification']=='RELEASE' for e in self.p['entries']));self.assertTrue(all(not r['routingEnabled'] for r in self.p['relations']))
 def test_cross_source_disagreements_stay_visible(self):
  self.assertEqual(749,self.a['recorderOverlap']);self.assertEqual(749,self.a['recorderNameMatches']);self.assertEqual({'wow-forever:quest:95328','wow-forever:quest:96130'},{r['entryKey'] for r in self.a['recorderConflicts']})
  for k in ['wow-forever:quest:1485','wow-forever:quest:95328','wow-forever:quest:96130']:self.assertEqual('CONFLICT',self.entries[k]['verification'])
  self.assertEqual(14,len(self.a['spellChecks']))
 def test_long_comments_cannot_activate_reused_objects(self):
  t,u=literal_table('return {[1]={name="a"},--[[[2]={name="wrong"},]]\n[3]={rank=-rankKeys.X}}','return',{'rankKeys.X':225});self.assertEqual({1,3},set(t));self.assertEqual(-225,t[3]['rank']);self.assertFalse(u)
 def test_static_trace_is_after_generated_base_and_before_authored_fix(self):
  data,_,layers=effective_data(ROOT/'research/e10full/upstream',ROOT/'research/e10audit/upstream');names=[r['file'] for r in layers];self.assertLess(names.index('src/corrections/Forever/generated/foreverBaseQuest.lua'),names.index('src/corrections/Forever/traces/foreverQuestTraces.lua'));self.assertLess(names.index('src/corrections/Forever/traces/foreverQuestTraces.lua'),names.index('src/corrections/Forever/foreverQuestFixes.lua'))
  for e in self.p['entries']:
   if e['kind']!='QUEST' or not json.loads(e['payload']).get('importer'):continue
   q=data['Quest'][e['gameId']]
   for app,field in [('minimumLevel',4),('reportedLevel',5)]:self.assertEqual(q.get(field) if q.get(field) in range(1,61) else None,e[app])
 def test_previous_bundle_keeps_signature_bytes(self):
  with materialized(ROOT/'research/e10full/bundled-data-7.wfp') as p:self.assertEqual('ba94fb218ad63cae250c7d32b32ef16532c4daa5b8ab822685d5b623a8cb563e',hashlib.sha256(p.read_bytes()).hexdigest())
