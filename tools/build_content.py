#!/usr/bin/env python3
"""Normalize E00 and apply the reviewed E07 additions; no network is needed."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
E00 = ROOT / "research/e00"

def load(p): return json.loads((E00 / p).read_text())
def dump(value): return json.dumps(value, ensure_ascii=False, separators=(",", ":"))
def verification(status):
    if "conflict" in status: return "CONFLICT"
    if "classic" in status: return "CLASSIC"
    if "community" in status: return "COMMUNITY"
    if "beta" in status or "database_record" in status: return "BETA"
    return "UNVERIFIED"

def build():
    original = load("content/sample_catalog.json")
    sources = [{"id": s["id"], "url": s["url"], "kind": s["kind"], "checkedOn": s["checked_on"],
                "build": s.get("reported_build"), "payload": dump(s)} for s in load("content/sources.json")["sources"]]
    maps = [{"key": m["id"], "uiMapId": m["ui_map_id"], "floorId": m["floor_id"],
             "name": m["name"], "width": m["width"], "height": m["height"], "levels": 3,
             "build": m["reported_build"], "coordinateVersion": f"{m['id']}:{m['reported_build']}:v1",
             "sourceId": "recorder_maps", "sourceUrl": m["source_page"],
             "transform": {"scaleX": 1.0, "scaleY": 1.0, "offsetX": 0.0, "offsetY": 0.0}}
            for m in load("maps/inventory.json")["maps"]]
    entries, locations, relations = [], [], []
    for section, kind in [("npcs", "NPC"), ("quests", "QUEST"), ("items", "ITEM"), ("spells", "SPELL"), ("travel", "TRAVEL")]:
        for e in original[section]:
            name = e.get("name_original") or f"Durotar → {e['destination_name']}"
            desc = e.get("summary_cs") or e.get("note") or {
                "NPC": "Postava z publikovaných podkladů. Stav ověření a případná poloha jsou vedené samostatně.",
                "ITEM": "Předmět uváděný v beta katalogu; různé způsoby získání mají vlastní zdroje.",
                "SPELL": "Kouzlo Summon Imp související s Warlockovými úkoly. Dostupnost v aktuální hře čeká na ověření."
            }[kind]
            aliases = ([str(e["game_id"])] if "game_id" in e else [])
            if kind == "SPELL": aliases += ["přivolání impa", "warlock"]
            if kind == "ITEM": aliases += ["hůl", "odměna"]
            if e.get("classes") == ["WARLOCK"]: aliases += ["warlock"]
            entries.append({"key": e["id"], "kind": kind, "gameId": e.get("game_id"), "nameOriginal": name,
                "descriptionCs": desc, "aliases": aliases, "verification": verification(e["verification_status"]),
                "releaseConfirmed": False, "minimumLevel": e.get("minimum_level"),
                "reportedLevel": e.get("quest_level_reported"), "sourceIds": e["source_ids"], "payload": dump(e)})
            if e.get("location"):
                l = e["location"]; m = next(m for m in maps if m["key"] == l["map_key"])
                locations.append({"key": e["id"]+":location:0", "entryKey": e["id"], "mapKey": l["map_key"],
                    "coordinateVersion": m["coordinateVersion"], "floorId": l["floor_id"],
                    "xPercent": l["x_percent"], "yPercent": l["y_percent"],
                    "precisionPercent": l["source_precision_percent"], "sourceId": l["source_id"]})
    for r in original["relations"]:
        relations.append({"key": r["id"], "kind": "PREREQUISITE", "ownerKey": r["quest_key"],
            "targetKeys": r["dependency_keys"], "mode": "ANY" if r["type"] == "ANY_COMPLETED" else "ALL",
            "verification": verification(r["verification_status"]), "routingEnabled": False, "sourceIds": r["source_ids"]})
    for q in original["quests"]:
        for field, kind in [("start_npc_key", "START_NPC"), ("end_npc_key", "END_NPC"), ("learns_spell_key", "LEARN_SPELL")]:
            if q.get(field): relations.append({"key": f"wow-forever:relation:{kind.lower()}:{q['game_id']}",
                "kind": kind, "ownerKey": q["id"], "targetKeys": [q[field]], "mode": "ALL",
                "verification": verification(q["verification_status"]), "routingEnabled": False, "sourceIds": q["source_ids"]})
    for item in original["items"]:
        for index, claim in enumerate(item.get("acquisition_claims", [])):
            relations.append({"key": f"wow-forever:relation:reward:{item['game_id']}:{index}", "kind": "REWARD_CHOICE",
                "ownerKey": item["id"], "targetKeys": [claim["quest_key"]], "mode": "ALL",
                "verification": verification(claim["verification_status"]), "routingEnabled": False, "sourceIds": [claim["source_id"]]})
    pack = {"schemaVersion": 1, "packageId": "wow-forever:starter:durotar", "generation": 1,
        "gameVariant": "wow-forever", "checkedOn": "2026-10-23", "maps": maps, "sources": sources,
        "entries": entries, "locations": locations, "relations": relations}
    additions = json.loads((ROOT / "content/e07-additions.json").read_text())
    for section in ["sources", "entries", "locations", "relations", "guideRules"]:
        pack.setdefault(section, []).extend(additions[section])
    for update in additions["entryUpdates"]:
        entry = next(e for e in pack["entries"] if e["key"] == update["key"])
        entry.update(update)
    pack["generation"] = 2
    path = ROOT / "app/src/main/assets/content/starter.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(pack, ensure_ascii=False, indent=2)+"\n")
    exported = ROOT / "content/starter-v1.json"
    exported.parent.mkdir(exist_ok=True)
    exported.write_bytes(path.read_bytes())
    print(f"Generation {pack['generation']}: {len(entries)} entries, {len(locations)} locations, {len(relations)} relations, {len(sources)} sources")
    return pack

if __name__ == "__main__": build()
