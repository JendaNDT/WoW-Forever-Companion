#!/usr/bin/env python3
"""Build the reviewed E10 generation separately from the immutable E09 fallback."""
from pathlib import Path
import json
from validate_content import validate

ROOT=Path(__file__).resolve().parents[1]

def build():
    p=json.loads((ROOT/'content/starter-v1.json').read_text())
    assert p['generation']==2
    extra=json.loads((ROOT/'content/e10-additions.json').read_text())
    for kind in ['sources','entries','locations','relations','guideRules','maps']:p.setdefault(kind,[]).extend(extra[kind])
    for update in extra['mapUpdates']:next(m for m in p['maps'] if m['key']==update['key']).update(update)
    p['transportGraph']=extra['transportGraph'];p['generation']=4;p['checkedOn']='2026-10-24'
    validate(p)
    path=ROOT/'content/e10-v4.json';path.write_text(json.dumps(p,ensure_ascii=False,indent=2)+'\n')
    # The real current distribution is available to host tests, never substituted into the fallback APK.
    (ROOT/'app/src/test/resources/e10-v4.json').write_bytes(path.read_bytes())
    print(json.dumps({'generation':4,'maps':len(p['maps']),'entries':len(p['entries']),'locations':len(p['locations']),'relations':len(p['relations']),'rules':len(p['guideRules'])}))
    return p

if __name__=='__main__':build()
