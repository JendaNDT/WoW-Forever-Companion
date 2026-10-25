import hashlib,json,unittest
from catalog_io import catalog_json
from pathlib import Path
from questie_tables import TableReader,literal_table,patch_table,effective_data
from build_e10_full import ROOT,UP,PIN
from validate_content import validate

class LiteralImportTest(unittest.TestCase):
    def test_holes_strings_and_64_bit_race_mask_are_preserved(self):
        r=TableReader(r'''{[1]="a\"b",[2]=nil,[3]={1,2}, mask=178+8589934592}''')
        value=r.value();self.assertEqual('a"b',value[1]);self.assertIsNone(value[2]);self.assertEqual(8589934770,value['mask']);self.assertFalse(r.unknown)
    def test_coordinates_are_atomic_values_when_adding_or_removing(self):
        a={1:{1:10,2:20},2:{1:20,2:30}};b={1:{1:10,2:21}}
        merged=patch_table(a,b);self.assertEqual(3,len(merged));self.assertEqual(a[1],merged[1])
        self.assertEqual(a,patch_table(merged,b,remove=True))
        self.assertEqual({14:merged},patch_table({14:a},{14:b},nested=True))
    def test_unknown_identifiers_are_detected_and_code_is_never_executed(self):
        table,unknown=literal_table('return {[1]=system.erase}', 'return')
        self.assertEqual({'system.erase'},unknown);self.assertEqual('__unresolved:system.erase',table[1])
        with self.assertRaises((AssertionError,ValueError,IndexError)):literal_table('return {os.execute("x")}', 'return')
    def test_every_upstream_file_has_the_pinned_git_blob_and_sha256(self):
        snapshot=json.loads((UP/'snapshot.json').read_text());self.assertEqual(PIN,snapshot['revision']);self.assertEqual(29,len(snapshot['files']))
        for row in snapshot['files']:
            raw=(UP/row['path']).read_bytes();self.assertEqual(row['sha256'],hashlib.sha256(raw).hexdigest());self.assertEqual(row['gitBlob'],hashlib.sha1(b'blob '+str(len(raw)).encode()+b'\0'+raw).hexdigest())

class FullCatalogTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.p=catalog_json(ROOT/'content/e10-v7.json');cls.audit=json.loads((ROOT/'research/e10full/import-audit.json').read_text());cls.entries={e['key']:e for e in cls.p['entries']}
    def test_contract_and_all_older_reviewed_entries_are_preserved(self):
        self.assertTrue(validate(self.p)['passed']);old=json.loads((ROOT/'content/e10-v6.json').read_text())
        for e in old['entries']:self.assertEqual(e,self.entries[e['key']])
        for name in ['locations','relations','sources','guideRules']:
            by_key={e.get('key',e.get('id')):e for e in self.p[name]}
            for row in old[name]:self.assertEqual(row,by_key[row.get('key',row.get('id'))])
    def test_actual_import_includes_forever_delta_and_warlock_endgame(self):
        self.assertEqual(12352,len(self.p['entries']));self.assertEqual(61,len(self.p['maps']));self.assertEqual(2818,sum(e['kind']=='QUEST' for e in self.p['entries']))
        self.assertEqual(104,len(self.audit['warlockQuestIds']));self.assertIn(98575,self.audit['warlockQuestIds'])
        self.assertTrue(any(e['kind']=='QUEST' and 'Dreadsteed' in e['nameOriginal'] for e in self.p['entries']))
        self.assertTrue(any(e['kind']=='QUEST' and e['reportedLevel']==60 and e['minimumLevel']==60 for e in self.p['entries']))
    def test_references_never_become_release_observations_or_reward_choices(self):
        self.assertFalse(any(e['releaseConfirmed'] for e in self.p['entries']));self.assertFalse(self.p.get('transportGraph',{}).get('edges'))
        self.assertTrue(all(not r['routingEnabled'] for r in self.p['relations']))
        self.assertFalse(any(r['kind']=='REWARD_CHOICE' and ':full:' in r['key'] for r in self.p['relations']))
        self.assertTrue(any(r['kind']=='QUEST_REWARD' for r in self.p['relations']))
    def test_overviews_have_no_fabricated_interior_coordinates(self):
        self.assertEqual(5,sum(m['uiMapId']>=900000 for m in self.p['maps']))
        self.assertFalse(any(int(l['mapKey'].split(':')[-1])>=900000 for l in self.p['locations']))
        self.assertEqual(678,sum(e['kind']=='OBJECT' for e in self.p['entries']))
    def test_sampled_positions_are_actual_source_pairs_with_no_second_transform(self):
        data,_,_=effective_data(UP)
        frames={m['key']:m['uiMapId'] for m in self.p['maps']}
        area_ui=literal_table((UP/'support/Forever/Zones/areaIdToUiMapId.lua').read_text(),'ZoneDB.private.areaIdToUiMapId = ')[0]
        for l in self.p['locations']:
            if ':location:full:' not in l['key']:continue
            e=self.entries[l['entryKey']];kind='Npc' if e['kind']=='NPC' else 'Object';row=data[kind][e['gameId']];spawns=row.get(7 if kind=='Npc' else 4) or {}
            pairs={(v.get(1),v.get(2)) for area,coords in spawns.items() if area_ui.get(area)==frames[l['mapKey']] for v in coords.values() if isinstance(v,dict)}
            self.assertIn((l['xPercent'],l['yPercent']),pairs)
        for r in self.p['relations']:
            if ':full:' not in r['key'] or r['kind']!='PREREQUISITE':continue
            quest=int(r['ownerKey'].split(':')[-1]);field=12 if r['mode']=='ALL' else 13
            self.assertEqual({f'wow-forever:quest:{i}' for i in data['Quest'][quest][field].values()},set(r['targetKeys']))
    def test_services_use_forever_flag_bits_instead_of_era_flag_bits(self):
        self.assertEqual(8,self.audit['serviceFlagsUsed']['FLIGHT_MASTER']);self.assertEqual(128,self.audit['serviceFlagsUsed']['INNKEEPER'])
        self.assertTrue(any('letový mistr' in e['aliases'] for e in self.p['entries']))
        self.assertTrue(any('hostinský' in e['aliases'] for e in self.p['entries']))

if __name__=='__main__':unittest.main()
