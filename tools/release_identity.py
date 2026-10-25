#!/usr/bin/env python3
"""Canonical APK build inputs, shared by release verification and source-archive restoration."""
import hashlib,json,subprocess,io,tarfile,re
from pathlib import Path

PREFIXES=('app/src/main/','app/src/release/','gradle/wrapper/')
NAMES={'app/build.gradle.kts','build.gradle.kts','settings.gradle.kts','gradle.properties','gradlew','gradlew.bat','release/owner-certificate.sha256'}
FIELDS={'schema','applicationId','versionCode','versionName','channel','sourceRevision','sourceDirty','buildInputsSha256','fallbackCatalogSha256','publisherKeySha256','ownerCertificateSha256'}
APP_ID='cz.jenda.wowforever.guide'

def sha(data):return hashlib.sha256(data).hexdigest()
def included(name):return name in NAMES or any(name.startswith(p) for p in PREFIXES)
def manifest_hash(rows):return sha(''.join(name+'\0'+digest+'\n' for name,digest in sorted(rows)).encode())
def source_manifest(root):
    root=Path(root);files=set()
    for name in NAMES:
        if (root/name).is_file():files.add(root/name)
    for prefix in PREFIXES:
        folder=root/prefix
        if folder.exists():files.update(p for p in folder.rglob('*') if p.is_file())
    if not files:raise ValueError('Chybí zdrojové soubory APK.')
    if any(p.is_symlink() for p in files):raise ValueError('Zdrojové vstupy nesmějí být symbolické odkazy.')
    return sorted((p.relative_to(root).as_posix(),sha(p.read_bytes())) for p in files)
def git(root,*args):
    try:p=subprocess.run(['git','-C',str(root),*args],capture_output=True)
    except OSError:raise ValueError('Git není dostupný pro kontrolu revize.') from None
    if p.returncode:raise ValueError('Nedohledaná revize zdrojového kódu.')
    return p.stdout

def verify_identity(meta,root):
    root=Path(root).resolve()
    if set(meta)!=FIELDS or type(meta['schema']) is not int or meta['schema']!=1 or meta['applicationId']!=APP_ID:raise ValueError('Chybí nebo nesouhlasí identita sestavení.')
    if type(meta['versionCode']) is not int or meta['versionCode']<12 or not isinstance(meta['versionName'],str) or not re.fullmatch(r'[0-9A-Za-z.\-]{1,80}',meta['versionName']):raise ValueError('Neplatná verze vydání.')
    if meta['channel']!='pilot' or meta['sourceDirty'] is not False or not re.fullmatch('[a-f0-9]{40}',meta['sourceRevision']):raise ValueError('Vydání nemá čistou dohledatelnou revizi.')
    for key in ('buildInputsSha256','fallbackCatalogSha256','publisherKeySha256','ownerCertificateSha256'):
        if not isinstance(meta[key],str) or not re.fullmatch('[a-f0-9]{64}',meta[key]):raise ValueError('Neplatný otisk sestavení.')
    rows=source_manifest(root);digest=manifest_hash(rows)
    if digest!=meta['buildInputsSha256']:raise ValueError('Zdrojový kód neodpovídá APK.')
    if sha((root/'app/src/main/assets/content/starter.json').read_bytes())!=meta['fallbackCatalogSha256']:raise ValueError('Neshodný přibalený katalog.')
    if sha((root/'app/src/main/assets/content/publisher.der').read_bytes())!=meta['publisherKeySha256']:raise ValueError('Neshodný vydavatel dat.')
    pin=(root/'release/owner-certificate.sha256').read_text().strip()
    if pin!=meta['ownerCertificateSha256']:raise ValueError('Neshodný vlastnický certifikát.')
    own=False
    try:own=Path(git(root,'rev-parse','--show-toplevel').decode().strip()).resolve()==root
    except ValueError:pass
    if own:
        git(root,'merge-base','--is-ancestor',meta['sourceRevision'],'HEAD')
        # Read the committed inputs, not only the working tree; no archive paths are extracted.
        archive=git(root,'archive','--format=tar',meta['sourceRevision'])
        with tarfile.open(fileobj=io.BytesIO(archive)) as tar:
            committed=[]
            for member in tar:
                if included(member.name):
                    if not member.isfile():
                        if member.isdir():continue
                        raise ValueError('Neplatný vstup v revizi.')
                    committed.append((member.name,sha(tar.extractfile(member).read())))
        if manifest_hash(committed)!=digest:raise ValueError('Uvedená revize neobsahuje stejné zdroje jako APK.')
    else:
        state=json.loads((root/'source-state.json').read_text())
        if state.get('sourceRevision')!=meta['sourceRevision'] or state.get('buildInputsSha256')!=digest:raise ValueError('Zdrojový archiv neodpovídá vydání.')
    return {'sourceRevision':meta['sourceRevision'],'buildInputsSha256':digest,'inputFiles':len(rows),'gitRevisionVerified':own}
