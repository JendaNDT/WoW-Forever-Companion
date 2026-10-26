import unittest
from unittest.mock import patch
import tablet_dev as dev


class TabletDevelopmentTest(unittest.TestCase):
    def test_usb_selection_is_explicit_and_never_picks_the_emulator(self):
        with patch.object(dev, 'command', side_effect=['LENOVO\n', 'List of devices attached\nLENOVO device product:tablet\nemulator-5554 device\n']) as run:
            self.assertEqual('LENOVO', dev.select_device('adb'))
            self.assertEqual(['adb', '-d', 'get-serialno'], run.call_args_list[0].args[0])

    def test_unauthorized_or_missing_tablet_is_rejected_before_mutations(self):
        for output in ['List of devices attached\nLENOVO unauthorized\n', 'List of devices attached\nOTHER device\n']:
            with patch.object(dev, 'command', return_value=output):
                with self.assertRaises(RuntimeError):
                    dev.select_device('adb', 'LENOVO')

    def test_instrumentation_exit_zero_without_passed_tests_is_not_success(self):
        self.assertTrue(dev.instrumentation_passed('OK (3 tests)\n'))
        for text in ['INSTRUMENTATION_CODE: 0', 'FAILURES!!!\nTests run: 3', 'Process crashed', 'OK (3 tests)\nINSTRUMENTATION_FAILED']:
            self.assertFalse(dev.instrumentation_passed(text))

    def test_signature_failure_never_uninstalls_or_clears_user_data(self):
        from pathlib import Path
        import tempfile
        with tempfile.TemporaryDirectory() as folder:
            apk = Path(folder) / 'app.apk'
            apk.write_bytes(b'apk')
            with patch.object(dev, 'find_adb', return_value='adb'), patch.object(dev, 'select_device', return_value='LENOVO'), patch.object(dev, 'STATE', Path(folder)), patch.object(dev, 'command', side_effect=RuntimeError('INSTALL_FAILED_UPDATE_INCOMPATIBLE')) as run:
                with self.assertRaises(RuntimeError):
                    dev.main(['install', '--apk', str(apk)])
                self.assertEqual(['adb', '-s', 'LENOVO', 'install', '-r', str(apk)], run.call_args.args[0])
                self.assertEqual(1, run.call_count)


if __name__ == '__main__':
    unittest.main()
