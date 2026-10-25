#!/usr/bin/env python3
"""Verify a CI debug APK against the exact checked-out sources and offline assets."""
import argparse
import hashlib
import json
import zipfile
from pathlib import Path

from release_identity import APP_ID, verify_identity
from verify_release import apk_info

ROOT = Path(__file__).resolve().parents[1]


def verify(apk, aapt, apksigner):
    info, _, _ = apk_info(apk, aapt, apksigner)
    if info['package'] != APP_ID or not info['debuggable']:
        raise ValueError('Expected the development APK for WoW Forever Companion.')
    with zipfile.ZipFile(apk) as z:
        meta = json.loads(z.read('assets/release-info.json'))
        identity = verify_identity(meta, ROOT)
        if (info['versionCode'], info['versionName']) != (meta['versionCode'], meta['versionName']):
            raise ValueError('APK version differs from its build identity.')
        assets = ROOT / 'app/src/main/assets'
        checked = 0
        for source in assets.rglob('*'):
            if not source.is_file():
                continue
            name = 'assets/' + source.relative_to(assets).as_posix()
            if z.read(name) != source.read_bytes():
                raise ValueError('Bundled asset differs: ' + name)
            if source.suffix in ('.wfp', '.wfpart') and z.getinfo(name).compress_type != zipfile.ZIP_STORED:
                raise ValueError('Offline package is compressed again: ' + name)
            checked += 1
        if any(name.endswith(('.jks', '.keystore', '.pem', '.p12', '.pfx')) for name in z.namelist()):
            raise ValueError('A private signing file was packaged in the APK.')
    return {
        'passed': True, **info, **identity, 'verifiedAssets': checked,
        'offlinePackagesStoredForStreaming': True,
        'apkBytes': apk.stat().st_size,
        'apkSha256': hashlib.sha256(apk.read_bytes()).hexdigest(),
        'physicalPhoneTested': False,
        'ownerSignedUpdate': False,
    }


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk', type=Path, required=True)
    parser.add_argument('--aapt', required=True)
    parser.add_argument('--apksigner', required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    result = verify(args.apk, args.aapt, args.apksigner)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result))
