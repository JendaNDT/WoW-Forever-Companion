"""Lossless small-file storage of signed WFP archives."""
import hashlib,json,tempfile
from contextlib import contextmanager
from pathlib import Path
def split(source,target,chunk=8*1024*1024):
 source=Path(source);target=Path(target);target.parent.mkdir(parents=True,exist_ok=True);parts=[]
 with source.open('rb') as f:
  while data:=f.read(chunk):
   p=Path(str(target)+f'.part{len(parts):03d}.wfpart');p.write_bytes(data);parts.append(p.name)
 raw=source.read_bytes();index={'parts':parts,'bytes':len(raw),'sha256':hashlib.sha256(raw).hexdigest()}
 Path(str(target)+'.index.json').write_text(json.dumps(index,indent=2)+'\n');return index
@contextmanager
def materialized(path):
 path=Path(path)
 if path.is_file():yield path;return
 index=json.loads(Path(str(path)+'.index.json').read_text())
 with tempfile.TemporaryDirectory(prefix='forever-parts-') as d:
  out=Path(d)/'bundle.wfp';h=hashlib.sha256();size=0
  with out.open('wb') as f:
   for name in index['parts']:
    assert Path(name).name==name
    with (path.parent/name).open('rb') as part:
     while data:=part.read(65536):f.write(data);h.update(data);size+=len(data)
  assert size==index['bytes'] and h.hexdigest()==index['sha256'],'Corrupt bundle parts'
  yield out
