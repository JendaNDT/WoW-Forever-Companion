import unittest
from e11_ci_device import instrumentation_count


class InstrumentationCompletionTest(unittest.TestCase):
    def test_completed_single_and_multiple_tests(self):
        self.assertEqual(1, instrumentation_count("Time: 1.3\n\nOK (1 test)\n\nINSTRUMENTATION_CODE: -1\n"))
        self.assertEqual(2, instrumentation_count("OK (2 tests)\n"))

    def test_shell_success_does_not_hide_test_failure_or_process_crash(self):
        for output in ["FAILURES!!!\nTests run: 1, Failures: 1\n", "INSTRUMENTATION_FAILED: failed\n",
                       "INSTRUMENTATION_RESULT: shortMsg=Process crashed.\n", "INSTRUMENTATION_CODE: 0\n"]:
            with self.subTest(output=output), self.assertRaises(RuntimeError):
                instrumentation_count(output)

    def test_partial_and_zero_detail_output_is_not_a_success(self):
        for output in ["", "INSTRUMENTATION_STATUS: numtests=2\n", "OK (1 test)\nINSTRUMENTATION_ABORTED: stopped"]:
            with self.subTest(output=output), self.assertRaises(RuntimeError):
                instrumentation_count(output)


if __name__ == "__main__":
    unittest.main()
