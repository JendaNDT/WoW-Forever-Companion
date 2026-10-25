#!/usr/bin/env python3
"""Verify packaged tile integrity and exact highest-resolution map reconstruction."""
import hashlib
import json
from pathlib import Path
from PIL import Image, ImageChops

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"

def main():
    manifest = json.loads((ROOT / "docs/tile_manifest.json").read_text())
    for entry in manifest:
        p = ASSETS / entry["path"]
        assert hashlib.sha256(p.read_bytes()).hexdigest() == entry["sha256"], p
        with Image.open(p) as tile:
            tile.load()
            assert tile.size == (256, 256), p
    inventory = json.loads((ROOT / "research/e00/maps/inventory.json").read_text())
    for m in inventory["maps"]:
        source = Image.open(ROOT / "research/e00" / m["file"]).convert("RGBA")
        rebuilt = Image.new("RGBA", source.size)
        for path in (ASSETS / f"maps/{m['ui_map_id']}/2").glob("*/*.png"):
            row, col = int(path.parent.name), int(path.stem)
            with Image.open(path) as tile:
                rebuilt.paste(tile, (col*256, row*256))
        difference = ImageChops.difference(source, rebuilt)
        assert all(extrema == (0, 0) for extrema in difference.getextrema()), m["name"]
    active = json.loads((ASSETS / "atlas.json").read_text())
    controls = json.loads((ROOT / "research/e00/maps/control_points.json").read_text())["points"]
    assert len(active["points"]) == len(controls) == 8
    for p in controls:
        point = next(x for x in active["points"] if x["id"] == p["npc_key"])
        assert (point["xPercent"], point["yPercent"]) == (p["x_percent"], p["y_percent"])
        assert point["uiMapId"] == 1411
    result = {"tile_checksums": len(manifest), "decoded_tiles": len(manifest),
              "pixel_exact_map_reconstructions": 4, "preserved_source_points": 8,
              "independent_in_game_checks": 0, "passed": True}
    print(json.dumps(result, ensure_ascii=False, indent=2))

if __name__ == "__main__":
    main()
