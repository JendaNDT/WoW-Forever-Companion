"""Exact catalog bytes, losslessly compressed for source control."""
import gzip,json
from pathlib import Path
def catalog_bytes(path):
 path=Path(path)
 if path.is_file():return gzip.decompress(path.read_bytes()) if path.suffix=='.gz' else path.read_bytes()
 return gzip.decompress(Path(str(path)+'.gz').read_bytes())
def catalog_json(path):return json.loads(catalog_bytes(path))
