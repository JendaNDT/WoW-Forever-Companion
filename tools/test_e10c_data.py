import json
import unittest
from verify_e10c_data import ROOT, verify

class E10Batch3RegressionTest(unittest.TestCase):
    def setUp(self):
        self.pack=json.loads((ROOT/'content/e10-v6.json').read_text())
    def test_reviewed_distribution_retains_published_content(self):
        self.assertEqual(34,verify(self.pack)['new_entries'])
    def test_classic_position_cannot_silently_become_beta_observation(self):
        next(e for e in self.pack['entries'] if e['key']=='wow-forever:npc:5875')['verification']='BETA'
        with self.assertRaises(AssertionError):verify(self.pack)
    def test_fixed_reward_cannot_become_an_optional_choice(self):
        row=next(r for r in self.pack['relations'] if r['kind']=='REWARD_CHOICE' and r['ownerKey']=='wow-forever:item:4938')
        row['ownerKey']='wow-forever:item:281274';row['targetKeys']=['wow-forever:quest:97282']
        with self.assertRaises(AssertionError):verify(self.pack)
    def test_classic_chain_cannot_unlock_live_content(self):
        next(r for r in self.pack['relations'] if r['ownerKey']=='wow-forever:quest:1504' and r['kind']=='PREREQUISITE')['routingEnabled']=True
        with self.assertRaises(AssertionError):verify(self.pack)

if __name__=='__main__':unittest.main()
