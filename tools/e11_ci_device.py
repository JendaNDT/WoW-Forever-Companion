#!/usr/bin/env python3
"""Destructive reliability checks on an explicitly selected disposable emulator only."""
import argparse
import json
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

PACKAGE = "cz.jenda.wowforever.guide"
RUNNER = PACKAGE + ".test/androidx.test.runner.AndroidJUnitRunner"
TEST_PACKAGE = "cz.jenda.wowforever."


def instrumentation_count(output):
    if re.search(r"FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|Process crashed|shortMsg=", output):
        raise RuntimeError("Android instrumentation reported a failure or crash")
    match = re.search(r"^OK \((\d+) tests?\)\s*$", output, re.MULTILINE)
    if not match:
        raise RuntimeError("Instrumentation did not report a completed, successful test count")
    return int(match.group(1))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--soak-seconds", type=int, default=1200)
    args = parser.parse_args()
    if not 0 <= args.soak_seconds <= 1800:
        parser.error("soak-seconds must be between 0 and 1800")
    args.output.mkdir(parents=True, exist_ok=True)
    adb = ["adb", "-s", args.serial]
    completed = []

    def run(*command, timeout=90, check=True):
        result = subprocess.run([*adb, *command], capture_output=True, text=True, timeout=timeout, check=False)
        if check and result.returncode:
            raise RuntimeError(f"adb {' '.join(command)} failed: {result.stdout[-3000:]} {result.stderr[-3000:]}")
        return result

    def instrument(test, count, name, soak=False):
        options = ["shell", "am", "instrument", "-w", "-r", "-e", "class", TEST_PACKAGE + test]
        if soak:
            options += ["-e", "e11SoakSeconds", str(args.soak_seconds)]
        result = run(*options, RUNNER, timeout=args.soak_seconds + 900 if soak else 900)
        output = result.stdout + result.stderr
        (args.output / (name + ".txt")).write_text(output)
        actual = instrumentation_count(output)
        if actual != count:
            raise RuntimeError(f"{name}: expected {count} tests, received {actual}")
        completed.append({"phase": name, "tests": actual, "passed": True})
        print(json.dumps(completed[-1]), flush=True)

    def force_stop_after_ui_launch(name):
        run("shell", "am", "start", "-W", "-n", PACKAGE + "/cz.jenda.wowforever.MainActivity")
        deadline = time.monotonic() + 180
        while time.monotonic() < deadline:
            result = run("shell", "uiautomator", "dump", "/sdcard/e11-ui.xml", timeout=30, check=False)
            xml = run("shell", "cat", "/sdcard/e11-ui.xml").stdout
            (args.output / (name + "-ui.xml")).write_text(xml)
            if "Uložená data se nepodařilo načíst" in xml:
                raise RuntimeError("The app displayed its data loading error")
            try:
                nodes = list(ET.fromstring(xml).iter("node"))
            except ET.ParseError:
                nodes = []
            if any(n.get("text") == "Mapa" for n in nodes) and any(n.get("text") == "Úkoly" for n in nodes):
                break
            time.sleep(1)
        else:
            raise RuntimeError("The real app UI did not become ready before force-stop")
        with (args.output / (name + ".png")).open("wb") as stream:
            subprocess.run([*adb, "exec-out", "screencap", "-p"], stdout=stream, check=True, timeout=30)
        pid = run("shell", "pidof", PACKAGE).stdout.strip()
        if not pid:
            raise RuntimeError("Target process is not running")
        run("shell", "am", "force-stop", PACKAGE)
        if run("shell", "pidof", PACKAGE, check=False).stdout.strip():
            raise RuntimeError("Target process survived force-stop")
        completed.append({"phase": name, "process_before": pid, "process_stopped": True})
        print(json.dumps(completed[-1]), flush=True)

    report = {"serial": args.serial, "physical_phone_tested": False,
              "scope": "disposable emulator / debug APK; no physical-phone FPS or latency claim", "passed": False}
    try:
        if run("shell", "getprop", "ro.kernel.qemu").stdout.strip() != "1":
            raise RuntimeError("Refusing destructive tests on a physical or unidentified device")
        report["android_api"] = run("shell", "getprop", "ro.build.version.sdk").stdout.strip()
        run("install", "-t", "-r", "app/build/outputs/apk/debug/app-debug.apk", timeout=180)
        run("install", "-t", "-r", "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk", timeout=180)
        run("shell", "pm", "clear", PACKAGE)
        # Some old system images refuse clearing a log buffer. Collection is diagnostic;
        # the instrumentation results and persisted-data assertions remain mandatory.
        clear_logs = run("logcat", "-c", check=False)
        (args.output / "logcat-clear.txt").write_text(clear_logs.stdout + clear_logs.stderr)
        radio_requests = {}
        for radio in ("wifi", "data"):
            result = run("shell", "svc", radio, "disable", check=False)
            radio_requests[radio] = {"returncode": result.returncode, "output": result.stdout + result.stderr}
        report["radio_disable_requests"] = radio_requests
        report["offline_radio_state_verified"] = False
        instrument("E11ProcessReliabilityDeviceTest#prepareState", 1, "01-prepare")
        force_stop_after_ui_launch("02-first-force-stop")
        instrument("E11ProcessReliabilityDeviceTest#verifyAfterForceStopAndRestoreBackup", 1, "03-restore")
        force_stop_after_ui_launch("04-second-force-stop")
        instrument("E11ProcessReliabilityDeviceTest#verifyRestoredStateAfterSecondForceStop", 1, "05-restart-restored")
        instrument("E11FullCatalogDeviceTest", 2, "06-full-catalog-and-map-soak", soak=True)
        report["passed"] = True
    except Exception as exc:
        report["error"] = str(exc)
        raise
    finally:
        report["phases"] = completed
        (args.output / "summary.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
        logs = run("logcat", "-d", check=False).stdout
        (args.output / "logcat.txt").write_text(logs)
        (args.output / "meminfo.txt").write_text(run("shell", "dumpsys", "meminfo", PACKAGE, check=False).stdout)
        with (args.output / "app-evidence.tar").open("wb") as stream:
            subprocess.run([*adb, "exec-out", "run-as", PACKAGE, "tar", "-cf", "-", "files/e11"], stdout=stream, check=False, timeout=60)


if __name__ == "__main__":
    main()
