#!/usr/bin/env python3
"""Build deterministic offline tiles from the unchanged, checksummed E00 inputs."""
import hashlib
import json
import math
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
INPUT = ROOT / "research/e00"
OUTPUT = ROOT / "app/src/main/assets"
TILE, LEVELS = 256, 3

def main():
    inventory = json.loads((INPUT / "maps/inventory.json").read_text())
    control = json.loads((INPUT / "maps/control_points.json").read_text())
    maps, manifest = [], []
    for item in inventory["maps"]:
        source = INPUT / item["file"]
        assert hashlib.sha256(source.read_bytes()).hexdigest() == item["sha256"]
        image = Image.open(source).convert("RGBA")
        assert image.size == (item["width"], item["height"])
        # Pad BEFORE scaling: 1002/4=250.5, not 251. This preserves the coordinate frame.
        side = TILE * 2 ** (LEVELS - 1)
        padded = Image.new("RGBA", (side, side))
        padded.paste(image, (0, 0))
        for level in range(LEVELS):
            divisor = 2 ** (LEVELS - level - 1)
            scaled = padded.resize((side // divisor, side // divisor), Image.Resampling.LANCZOS)
            for row in range(math.ceil(image.height / divisor / TILE)):
                for col in range(math.ceil(image.width / divisor / TILE)):
                    tile = scaled.crop((col*TILE, row*TILE, (col+1)*TILE, (row+1)*TILE))
                    path = OUTPUT / f"maps/{item['ui_map_id']}/{level}/{row}/{col}.png"
                    path.parent.mkdir(parents=True, exist_ok=True)
                    tile.save(path, "PNG", compress_level=9)
                    manifest.append({"path": str(path.relative_to(OUTPUT)),
                                     "sha256": hashlib.sha256(path.read_bytes()).hexdigest()})
        maps.append({"uiMapId": item["ui_map_id"], "name": item["name"],
                     "width": image.width, "height": image.height, "levels": LEVELS,
                     "build": item["reported_build"], "sourceUrl": item["source_page"]})
    points = [{"id": p["npc_key"], "name": p["name"], "uiMapId": 1411,
               "xPercent": p["x_percent"], "yPercent": p["y_percent"],
               "sourceUrl": f"https://warcraftforever.games/npcs/{p['npc_key'].split(':')[-1]}"}
              for p in control["points"]]
    (OUTPUT / "atlas.json").write_text(json.dumps({"maps": maps, "points": points}, ensure_ascii=False, indent=2)+"\n")
    (ROOT / "docs/tile_manifest.json").write_text(json.dumps(manifest, indent=2)+"\n")
    print(f"Prepared {len(maps)} maps, {len(points)} beta NPC points, {len(manifest)} tiles")

if __name__ == "__main__":
    main()
