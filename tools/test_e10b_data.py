import json
import unittest
from verify_e10b_data import ROOT, verify

class E10Batch2RegressionTest(unittest.TestCase):
    def setUp(self):
        self.pack=json.loads((ROOT/'content/e10-v5.json').read_text())
    def test_reviewed_distribution_preserves_older_ids(self):
        self.assertEqual(43,verify(self.pack)['new_entries'])
    def test_wrong_reward_quest_cannot_silently_attach_vest_to_kill_step(self):
        row=next(r for r in self.pack['relations'] if r['key']=='wow-forever:relation:e10b:reward_choice:4916')
        row['targetKeys']=['wow-forever:quest:790']
        with self.assertRaises(AssertionError):verify(self.pack)
    def test_reported_level_cannot_become_minimum_or_classic_chain_become_enabled(self):
        entry=next(e for e in self.pack['entries'] if e['key']=='wow-forever:quest:96821')
        entry['minimumLevel']=entry['reportedLevel']
        with self.assertRaises(AssertionError):verify(self.pack)
        self.setUp()
        row=next(r for r in self.pack['relations'] if r['key']=='wow-forever:relation:e10b:prerequisite:831')
        row['routingEnabled']=True
        with self.assertRaises(AssertionError):verify(self.pack)

if __name__=='__main__':unittest.main()
