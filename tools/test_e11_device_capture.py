#!/usr/bin/env python3
import unittest
from e11_device_capture import frame_stats,memory_pss

class DeviceEvidenceParsingTest(unittest.TestCase):
    def test_frame_statistics_ignore_incomplete_flagged_and_invalid_rows(self):
        raw='''---PROFILEDATA---
Flags,IntendedVsync,FrameCompleted
0,1000000000,1010000000
0,2000000000,2020000000
1,1000000000,1030000000
0,1000000000,9223372036854775807
0,2000000000,1000000000
bad,row
---PROFILEDATA---'''
        r=frame_stats(raw);self.assertEqual(2,r['usable_frames']);self.assertEqual(20,r['p95_ms']);self.assertEqual(1,r['over_16_67ms']);self.assertFalse(r['goal_60fps_verified'])
    def test_missing_frame_data_stays_unknown(self):
        r=frame_stats('Permission denied');self.assertEqual(0,r['usable_frames']);self.assertIsNone(r['p95_ms']);self.assertFalse(r['goal_60fps_verified'])
    def test_memory_supports_both_android_formats_and_missing_is_not_zero(self):
        self.assertEqual(12345,memory_pss('TOTAL PSS: 12345 TOTAL RSS: 22222'))
        self.assertEqual(23456,memory_pss('     TOTAL 23456 0 2000'))
        self.assertIsNone(memory_pss('No process found'))

if __name__=='__main__':unittest.main()
