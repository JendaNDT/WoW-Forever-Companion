#!/usr/bin/env python3
"""Local USB development for Warlock Forever; run on the computer with the tablet."""
import argparse
import datetime as dt
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
STATE = ROOT / '.local' / 'tablet'
APP = 'cz.jenda.wowforever.guide'
ACTIVITY = APP + '/cz.jenda.wowforever.MainActivity'
RUNNER = APP + '.test/androidx.test.runner.AndroidJUnitRunner'


def command(args, *, binary=False, timeout=120):
    result = subprocess.run([str(x) for x in args], capture_output=True,
                            text=not binary, timeout=timeout)
    if result.returncode:
        message = result.stderr or result.stdout
        if isinstance(message, bytes):
            message = message.decode('utf-8', errors='replace')
        raise RuntimeError(message.strip() or 'Příkaz selhal.')
    return result.stdout


def find_adb():
    candidates = [os.environ.get('ADB'), shutil.which('adb')]
    for variable in ('ANDROID_HOME', 'ANDROID_SDK_ROOT'):
        if os.environ.get(variable):
            candidates.append(str(Path(os.environ[variable]) / 'platform-tools' / 'adb'))
    candidates.append(str(Path.home() / 'Library/Android/sdk/platform-tools/adb'))
    for candidate in candidates:
        if candidate and Path(candidate).is_file():
            return candidate
    raise RuntimeError('ADB zde není dostupné. Spusť tento nástroj na počítači, kde funguje adb devices.')


def device_rows(output):
    rows = {}
    for line in output.splitlines():
        parts = line.split()
        if len(parts) >= 2 and not line.startswith(('List of devices', '*')):
            rows[parts[0]] = parts[1]
    return rows


def select_device(adb, requested=None):
    # -d selects a physical USB device; never silently pick an emulator/Wi-Fi target.
    serial = requested or command([adb, '-d', 'get-serialno']).strip()
    rows = device_rows(command([adb, 'devices', '-l']))
    if rows.get(serial) != 'device':
        raise RuntimeError('Vybraný tablet není autorizovaný a dostupný ve stavu device. Ověř adb devices -l.')
    return serial


def instrumentation_passed(output):
    import re
    return (re.search(r'OK \(\d+ tests?\)', output) is not None
            and not any(x in output for x in ('FAILURES', 'INSTRUMENTATION_FAILED', 'Process crashed')))


def gradle(*tasks):
    wrapper = ROOT / 'gradlew'
    if not wrapper.is_file():
        raise RuntimeError('Chybí celý projekt včetně gradlew. Použij místní kopii GitHub repozitáře.')
    subprocess.run(['bash', str(wrapper), '--no-daemon', '--max-workers=1', *tasks],
                   cwd=ROOT, check=True)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', help='Přesné zařízení z adb devices; jinak jediný USB tablet.')
    parser.add_argument('action', choices=['doctor', 'install', 'run', 'logs', 'screenshot', 'test', 'debug', 'debug-off'])
    parser.add_argument('--apk', type=Path, help='Existující APK místo místního sestavení.')
    args = parser.parse_args(argv)
    adb = find_adb()
    serial = select_device(adb, args.serial)
    prefix = [adb, '-s', serial]
    def shell(*words, **options):
        return command([*prefix, 'shell', *words], **options)
    def install(apk, test=False):
        if not apk.is_file():
            raise RuntimeError('APK neexistuje: ' + str(apk))
        try:
            output = command([*prefix, 'install', '-r', *(['-t'] if test else []), str(apk)])
        except RuntimeError as exc:
            raise RuntimeError(str(exc) + '\nAplikace nebyla automaticky odinstalována. Při rozdílném podpisu nejprve vyexportuj zálohu v aplikaci.') from exc
        if 'Success' not in output:
            raise RuntimeError(output)
        print(output.strip())
    STATE.mkdir(parents=True, exist_ok=True)
    stamp = dt.datetime.now(dt.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    if args.action == 'doctor':
        report = {'checkedAt': stamp, 'transport': 'local-adb', 'serial': serial}
        for key, prop in [('manufacturer', 'ro.product.manufacturer'), ('model', 'ro.product.model'),
                          ('android', 'ro.build.version.release'), ('api', 'ro.build.version.sdk')]:
            report[key] = shell('getprop', prop).strip()
        try:
            installed = bool(shell('pm', 'path', APP).strip())
        except RuntimeError:
            installed = False
        report.update(size=shell('wm', 'size').strip(), density=shell('wm', 'density').strip(),
                      fontScale=shell('settings', 'get', 'system', 'font_scale').strip(),
                      appInstalled=installed)
        (STATE / 'device.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
        print(json.dumps(report, ensure_ascii=False, indent=2))
    elif args.action == 'install':
        if args.apk is None:
            gradle(':app:assembleDebug')
        install(args.apk or ROOT / 'app/build/outputs/apk/debug/app-debug.apk')
    elif args.action in ('run', 'debug'):
        if args.action == 'debug':
            shell('am', 'set-debug-app', '-w', APP)
            shell('am', 'force-stop', APP)
        output = shell('am', 'start', *([] if args.action == 'debug' else ['-W']), '-n', ACTIVITY)
        if 'Error:' in output:
            raise RuntimeError(output)
        print(output.strip())
        if args.action == 'debug':
            print('Aplikace čeká na debugger. V Android Studiu použij Attach debugger to Android process. Ukončení: debug-off.')
    elif args.action == 'debug-off':
        shell('am', 'clear-debug-app')
        print('Čekání na debugger pro tuto aplikaci je vypnuté.')
    elif args.action == 'logs':
        pid = shell('pidof', '-s', APP).strip()
        if not pid.isdigit():
            raise RuntimeError('Aplikace neběží. Nejdřív použij run.')
        logs = command([*prefix, 'logcat', '-d', '--pid=' + pid, '-v', 'threadtime'])
        target = STATE / ('logcat-' + stamp + '.txt')
        target.write_text(logs)
        print(target)
    elif args.action == 'screenshot':
        image = command([*prefix, 'exec-out', 'screencap', '-p'], binary=True)
        if not image.startswith(b'\x89PNG\r\n\x1a\n'):
            raise RuntimeError('Zařízení nevrátilo PNG snímek.')
        target = STATE / ('screen-' + stamp + '.png')
        target.write_bytes(image)
        print(target)
    elif args.action == 'test':
        gradle(':app:assembleDebug', ':app:assembleDebugAndroidTest')
        install(ROOT / 'app/build/outputs/apk/debug/app-debug.apk')
        install(ROOT / 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk', test=True)
        output = shell('am', 'instrument', '-w', '-r', '-e', 'class',
                       'cz.jenda.wowforever.TabletLayoutDeviceTest', RUNNER, timeout=900)
        (STATE / ('test-' + stamp + '.txt')).write_text(output)
        print(output)
        if not instrumentation_passed(output):
            raise RuntimeError('Tabletové testy nebyly úspěšně dokončené. Tablet musí mít aplikaci na šířku v celém okně.')
    return 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (RuntimeError, OSError, subprocess.SubprocessError) as exc:
        print('Chyba: ' + str(exc), file=sys.stderr)
        sys.exit(1)
