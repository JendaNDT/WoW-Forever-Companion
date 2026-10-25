#!/usr/bin/env python3
import json
import sys
import math
from pathlib import Path
from collections import deque
from jsonschema import Draft202012Validator, FormatChecker
from validate_guide_rules import validate_guide_rules
from validate_transport import validate_transport

ROOT = Path(__file__).resolve().parents[1]

def validate(pack):
    schema = json.loads((ROOT / "content/schema-v1.json").read_text())
    Draft202012Validator(schema, format_checker=FormatChecker()).validate(pack)
    for section in ["maps", "entries", "locations", "relations", "sources"]:
        key = "id" if section == "sources" else "key"
        values = [e[key] for e in pack[section]]
        assert len(values) == len(set(values)), "Duplicate identity: "+section
    maps = {e["key"]: e for e in pack["maps"]}; entries = {e["key"]: e for e in pack["entries"]}
    sources = {e["id"] for e in pack["sources"]}
    source_records = {e["id"]: e for e in pack["sources"]}
    map_ids={m["uiMapId"] for m in maps.values()}
    known={m["uiMapId"]:m for m in json.loads((ROOT/"app/src/main/assets/content/supported-maps.json").read_text())}
    assert {947,1414,1411,1454} <= map_ids <= known.keys() and len(map_ids)==len(maps)
    for m in maps.values():
        assert m["sourceId"] in sources
        assert all(m[k]==known[m["uiMapId"]][k] for k in ("width","height","levels","floorId","build"))
        assert m["key"] == f"wow-forever:map:{m['uiMapId']}"
    for e in entries.values():
        assert e["key"].startswith("wow-forever:"+e["kind"].lower()+":")
        assert e["sourceIds"] and set(e["sourceIds"]) <= sources
        assert not e.get("releaseConfirmed") or e["verification"] == "RELEASE"
        if e.get("gameId") is not None: assert e["key"] == f"wow-forever:{e['kind'].lower()}:{e['gameId']}"
        if e["verification"] == "RELEASE":
            assert e.get("releaseConfirmed") and any(source_records[s]["kind"] == "in_game_release_observation" for s in e["sourceIds"])
        assert not e.get("referencePosition") or (e["verification"] in ("CLASSIC","COMMUNITY") and e["kind"] in ("NPC","OBJECT","TRAVEL") and any(source_records[k]["kind"]=="questie_pinned_reference" for k in e["sourceIds"]))
        json.loads(e["payload"])
    for l in pack["locations"]:
        m = maps[l["mapKey"]]
        assert l["entryKey"] in entries and l["sourceId"] in sources
        assert l["floorId"] == m["floorId"] and l["coordinateVersion"] == m["coordinateVersion"]
        t = m.get("transform", {})
        assert all(math.isfinite(v) for v in t.values())
        assert t.get("scaleX", 1)>0 and t.get("scaleY", 1)>0
        assert 0 <= l["xPercent"]/100*t.get("scaleX", 1)+t.get("offsetX", 0) <= 1
        assert 0 <= l["yPercent"]/100*t.get("scaleY", 1)+t.get("offsetY", 0) <= 1
    graph = {}
    for r in pack["relations"]:
        assert r["ownerKey"] in entries and all(t in entries for t in r["targetKeys"])
        assert set(r["sourceIds"]) <= sources
        kinds = {"PREREQUISITE": ("QUEST","QUEST"), "START_NPC": ("QUEST","NPC"),
            "END_NPC": ("QUEST","NPC"), "LEARN_SPELL": ("QUEST","SPELL"), "REWARD_CHOICE": ("ITEM","QUEST"), "EXCLUSIVE_QUESTS": ("QUEST","QUEST")}
        kinds.update({"START_OBJECT":("QUEST","OBJECT"),"END_OBJECT":("QUEST","OBJECT"),"START_ITEM":("QUEST","ITEM"),"OBJECTIVE_NPC":("QUEST","NPC"),"OBJECTIVE_OBJECT":("QUEST","OBJECT"),"OBJECTIVE_ITEM":("QUEST","ITEM"),"ITEM_SOURCE_NPC":("ITEM","NPC"),"ITEM_SOURCE_OBJECT":("ITEM","OBJECT"),"DUNGEON_QUEST":("TRAVEL","QUEST"),"QUEST_REWARD":("QUEST","ITEM")})
        owner_kind, target_kind = kinds[r["kind"]]
        assert entries[r["ownerKey"]]["kind"] == owner_kind
        assert all(entries[t]["kind"] == target_kind for t in r["targetKeys"])
        if r["kind"] == "EXCLUSIVE_QUESTS":
            assert r["ownerKey"] not in r["targetKeys"] and r["mode"] == "ANY"
            if r["routingEnabled"]:
                assert any(source_records[key]["kind"] == "in_game_release_observation" for key in r["sourceIds"])
        if r.get("routingEnabled"):
            assert r["verification"] == "RELEASE" and entries[r["ownerKey"]]["releaseConfirmed"]
            assert all(entries[t]["releaseConfirmed"] for t in r["targetKeys"])
        if r["kind"] == "PREREQUISITE": graph.setdefault(r["ownerKey"], set()).update(r["targetKeys"])
    nodes = set(graph) | {n for ns in graph.values() for n in ns}
    degree = {n: 0 for n in nodes}
    for ns in graph.values():
        for n in ns: degree[n] += 1
    queue = deque(n for n, d in degree.items() if d == 0); seen = 0
    while queue:
        node = queue.popleft(); seen += 1
        for n in graph.get(node, []):
            degree[n] -= 1
            if degree[n] == 0: queue.append(n)
    assert seen == len(nodes), "Prerequisite cycle"
    validate_guide_rules(pack)
    validate_transport(pack)
    return {"passed": True, "guide_rules":len(pack.get("guideRules",[])), "entries": len(entries), "maps": len(maps), "locations": len(pack["locations"]),
            "relations": len(pack["relations"]), "sources": len(sources), "release_verified_entries": sum(e["releaseConfirmed"] for e in entries.values())}

if __name__ == "__main__":
    p = Path(sys.argv[1]) if len(sys.argv)>1 else ROOT / "app/src/main/assets/content/starter.json"
    print(json.dumps(validate(json.loads(p.read_text())), indent=2))
