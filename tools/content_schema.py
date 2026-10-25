#!/usr/bin/env python3
"""Validate the canonical content contract; keep the historical command without overwriting later extensions."""
import json
from pathlib import Path
from jsonschema import Draft202012Validator

ROOT=Path(__file__).resolve().parents[1]

def schema():
    return json.loads((ROOT/'content/schema-v1.json').read_text())

if __name__=='__main__':
    contract=schema();Draft202012Validator.check_schema(contract)
    print(json.dumps({'passed':True,'schema':contract['$id'],'source':'content/schema-v1.json','file_overwritten':False}))
