# Meteor Staff Visual Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the placeholder flat handheld model for `wildwest:meteor_staff` with a 3D voxel staff (charred wood + magma core) for the player's hand and a dedicated 2D pixel-art icon for the inventory/GUI. Ship a `.bbmodel` source so the 3D model is editable in Blockbench, plus a Python generator so the assets can be regenerated deterministically.

**Architecture:** A single Python generator (`wildwest/tools/gen_meteor_staff.py`) is the source of truth. It emits five files: the 32×32 3D texture, the 16×16 GUI sprite, the Blockbench `.bbmodel`, the 3D model JSON, and the flat GUI model JSON. The existing `assets/wildwest/items/meteor_staff.json` ItemModelDefinition gets a `display_context=gui` selector to route inventory rendering to the 2D model while every other context (held, ground, head, item-frame) falls through to the 3D model.

**Tech Stack:** Python 3 + Pillow (already a build-time dep — see [tools/gen_textures.py](wildwest/tools/gen_textures.py)). NeoForge 26.x ItemModelDefinition format. JSON model files. Blockbench 4.5 `.bbmodel` JSON.

**Spec:** [docs/superpowers/specs/2026-06-03-meteor-staff-visuals-design.md](docs/superpowers/specs/2026-06-03-meteor-staff-visuals-design.md)

**Existing files this plan touches:**
- `wildwest/src/main/resources/assets/wildwest/items/meteor_staff.json` — current contents: `{ "model": { "type": "minecraft:model", "model": "wildwest:item/meteor_staff" } }`. Becomes a selector.
- `wildwest/src/main/resources/assets/wildwest/models/item/meteor_staff.json` — current contents: `{ "parent": "minecraft:item/handheld", "textures": { "layer0": "wildwest:item/meteor_staff" } }`. Becomes the full 3D voxel model JSON.
- `wildwest/src/main/resources/assets/wildwest/textures/item/meteor_staff.png` — current: 16×16 orange placeholder. Becomes the 32×32 3D atlas.

**New files this plan creates:**
- `wildwest/tools/gen_meteor_staff.py`
- `wildwest/tools/meteor_staff.bbmodel`
- `wildwest/src/main/resources/assets/wildwest/models/item/meteor_staff_gui.json`
- `wildwest/src/main/resources/assets/wildwest/textures/item/meteor_staff_gui.png`

---

## Task 1: Scaffold generator with cube data

**Why this first:** Every other artifact (textures, bbmodel, model JSON) derives from the same list of cubes + UV regions. Centralising that data in one Python module first means later tasks consume a single source-of-truth.

**Files:**
- Create: `wildwest/tools/gen_meteor_staff.py`

### Steps

- [ ] **Step 1: Create the file with the cube schema, palette, UV regions, and main entrypoint**

Create `wildwest/tools/gen_meteor_staff.py`:

```python
#!/usr/bin/env python3
"""Generate Meteor Staff assets: 3D voxel model, GUI sprite, .bbmodel source.

A single source of truth for the cube list, UV regions, and palette feeds
five output files:
    - meteor_staff.png        (32x32 3D atlas)
    - meteor_staff_gui.png    (16x16 inventory icon)
    - meteor_staff.bbmodel    (Blockbench source)
    - meteor_staff.json       (3D item model JSON)
    - meteor_staff_gui.json   (flat 2D GUI model JSON)

Re-run with:
    python3 wildwest/tools/gen_meteor_staff.py
"""
import base64
import json
import os
import sys
import uuid

from PIL import Image, ImageDraw

# Deterministic UUIDs so re-runs produce byte-identical output.
NAMESPACE = uuid.UUID('00000000-0000-0000-0000-000000000001')


def det_uuid(name):
    return str(uuid.uuid5(NAMESPACE, name))


# ─── Palette ──────────────────────────────────────────────────────────────
WOOD_BASE    = (0x3a, 0x24, 0x12, 0xff)
WOOD_GRAIN   = (0x1a, 0x0e, 0x08, 0xff)
WOOD_HI      = (0x5a, 0x38, 0x20, 0xff)
IRON_BASE    = (0x4a, 0x45, 0x40, 0xff)
IRON_RIVET   = (0x70, 0x70, 0x70, 0xff)
IRON_SHADE   = (0x2a, 0x25, 0x20, 0xff)
MAGMA_OUTER  = (0xff, 0x5a, 0x1f, 0xff)
MAGMA_HOT    = (0xff, 0xb0, 0x2e, 0xff)
MAGMA_CRACK  = (0x00, 0x00, 0x00, 0xff)
EMBER        = (0xff, 0x85, 0x33, 0xff)
EMBER_HOT    = (0xff, 0xd0, 0x66, 0xff)
TRANSPARENT  = (0, 0, 0, 0)

# ─── Cube list ────────────────────────────────────────────────────────────
# (name, from_xyz, to_xyz, group). group → which palette + UV atlas region.
CUBES = [
    # Shaft: 7 stacked 2x2x2 cubes climbing diagonally
    ('shaft_0', (4.0, 1.0, 8.0), (6.0,  3.0, 10.0), 'wood'),
    ('shaft_1', (4.5, 3.0, 8.0), (6.5,  5.0, 10.0), 'wood'),
    ('shaft_2', (5.0, 5.0, 8.0), (7.0,  7.0, 10.0), 'wood'),
    ('shaft_3', (5.5, 7.0, 8.0), (7.5,  9.0, 10.0), 'wood'),
    ('shaft_4', (6.0, 9.0, 8.0), (8.0, 11.0, 10.0), 'wood'),
    ('shaft_5', (6.5, 11.0, 8.0), (8.5, 13.0, 10.0), 'wood'),
    ('shaft_6', (7.0, 13.0, 8.0), (9.0, 15.0, 10.0), 'wood'),
    # Iron bands
    ('band_low', (3.5, 2.5, 7.5), (6.5, 3.5, 10.5), 'iron'),
    ('band_mid', (5.5, 8.5, 7.5), (8.5, 9.5, 10.5), 'iron'),
    # Magma head: central cube + 4 radiating spikes
    ('core_main',   (6.5, 14.5, 7.5), (9.5, 17.5, 10.5), 'magma'),
    ('spike_top',   (7.5, 17.5, 8.5), (8.5, 18.5,  9.5), 'ember'),
    ('spike_front', (7.5, 15.5, 6.5), (8.5, 16.5,  7.5), 'ember'),
    ('spike_back',  (7.5, 15.5, 10.5), (8.5, 16.5, 11.5), 'ember'),
    ('spike_right', (5.5, 15.5, 8.5), (6.5, 16.5,  9.5), 'ember'),
    # Ember wisps (floating sparkles)
    ('wisp_a', (6.0, 19.0, 9.0), (7.0, 20.0, 10.0), 'ember'),
    ('wisp_b', (8.5, 18.5, 8.0), (9.5, 19.5,  9.0), 'ember'),
]

# ─── UV atlas regions (in 32×32 texture) ─────────────────────────────────
# Each group's faces share these UV rectangles. 1-pixel margins between
# groups prevent mipmap bleeding (see spec §Texture layout).
UV_REGIONS = {
    'wood':  (0,  0,  7,  23),   # 8x24 strip on the left
    'iron':  (9,  0,  15, 7),    # 7x8 block
    'magma': (0,  25, 31, 31),   # full-width 32x7 strip at the bottom
    'ember': (17, 0,  23, 7),    # 7x8 block on the right
}

# ─── Display transforms ──────────────────────────────────────────────────
DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, 90, -35], "translation": [0, 1.25, 1.5], "scale": [0.85, 0.85, 0.85]},
    "thirdperson_lefthand":  {"rotation": [0, 90, -35], "translation": [0, 1.25, 1.5], "scale": [0.85, 0.85, 0.85]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand":  {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "ground":                {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed":                 {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1.0, 1.0, 1.0]},
    "gui":                   {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1.0, 1.0, 1.0]},
    "head":                  {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1.0, 1.0, 1.0]},
}

# ─── Path helpers ────────────────────────────────────────────────────────
def resolve_paths(arg):
    """Return (tools_dir, assets_dir) given the CLI arg or sensible default."""
    if arg is None:
        # Invoked from repo root with no arg
        tools_dir = os.path.abspath('wildwest/tools')
    else:
        tools_dir = os.path.abspath(arg)
    repo_root = os.path.dirname(tools_dir)
    assets_dir = os.path.join(repo_root, 'src/main/resources/assets/wildwest')
    return tools_dir, assets_dir


def main():
    arg = sys.argv[1] if len(sys.argv) > 1 else None
    tools_dir, assets_dir = resolve_paths(arg)
    print(f"tools_dir: {tools_dir}")
    print(f"assets_dir: {assets_dir}")
    print(f"cubes: {len(CUBES)}")
    print(f"uv groups: {list(UV_REGIONS.keys())}")
    # Subsequent tasks fill in: paint textures, write bbmodel, write JSON.


if __name__ == '__main__':
    main()
```

- [ ] **Step 2: Verify it runs and prints sane paths**

Run: `python3 wildwest/tools/gen_meteor_staff.py`

Expected output: prints `tools_dir`, `assets_dir`, `cubes: 16`, `uv groups: ['wood', 'iron', 'magma', 'ember']`. No exceptions.

- [ ] **Step 3: Verify cube/UV invariants**

Run:

```bash
python3 -c "
import sys; sys.path.insert(0, 'wildwest/tools')
import gen_meteor_staff as g
assert len(g.CUBES) == 16, f'expected 16 cubes, got {len(g.CUBES)}'
names = [c[0] for c in g.CUBES]
assert len(names) == len(set(names)), 'duplicate cube names'
for name, frm, to, group in g.CUBES:
    assert group in g.UV_REGIONS, f'{name} uses unknown group {group}'
    assert all(t > f for t, f in zip(to, frm)), f'{name} has degenerate volume'
print('cube/uv invariants OK')
"
```

Expected: `cube/uv invariants OK`.

- [ ] **Step 4: Commit**

```bash
git add wildwest/tools/gen_meteor_staff.py
git commit -m "feat(wildwest): meteor staff generator scaffold + cube schema"
```

---

## Task 2: Paint the 32×32 3D texture atlas

**Files:**
- Modify: `wildwest/tools/gen_meteor_staff.py` (add `paint_3d_texture` function + main wiring)
- Create: `wildwest/src/main/resources/assets/wildwest/textures/item/meteor_staff.png` (regenerated)

### Steps

- [ ] **Step 1: Add the `paint_3d_texture` function**

Append to `wildwest/tools/gen_meteor_staff.py` *before* `def main():`:

```python
def paint_3d_texture():
    """Paint the 32x32 atlas with wood/iron/magma/ember regions.

    Margin pixels around each UV group are filled with the dominant adjacent
    palette colour (#1a0e08 dark wood streak) so any mipmap bleed at distance
    is invisible (see spec §Texture layout).
    """
    img = Image.new('RGBA', (32, 32), TRANSPARENT)
    draw = ImageDraw.Draw(img)

    # Margin fill: paint columns 8 and 16 + row 24 with WOOD_GRAIN so any
    # mipmap bleed reads as dark wood, not pink/transparent.
    draw.rectangle((8,  0,  8,  31), fill=WOOD_GRAIN)
    draw.rectangle((16, 0,  16, 31), fill=WOOD_GRAIN)
    draw.rectangle((0,  24, 31, 24), fill=WOOD_GRAIN)

    # ── Wood region: (0,0)–(7,23). Each shaft cube reuses this strip. ──
    draw.rectangle((0, 0, 7, 23), fill=WOOD_BASE)
    # Vertical grain streaks
    for x in (1, 4, 6):
        draw.line(((x, 0), (x, 23)), fill=WOOD_GRAIN)
    # Highlight column
    draw.line(((2, 0), (2, 23)), fill=WOOD_HI)
    # Knot speckles
    for (kx, ky) in ((3, 4), (5, 11), (3, 17), (6, 20)):
        img.putpixel((kx, ky), WOOD_GRAIN)

    # ── Iron band region: (9,0)–(15,7). ──
    draw.rectangle((9, 0, 15, 7), fill=IRON_BASE)
    # Top + bottom shade lines
    draw.line(((9, 0), (15, 0)), fill=IRON_SHADE)
    draw.line(((9, 7), (15, 7)), fill=IRON_SHADE)
    # Rivet dots
    for rx in (10, 13):
        img.putpixel((rx, 3), IRON_RIVET)
        img.putpixel((rx, 4), IRON_RIVET)

    # ── Magma core region: (0,25)–(31,31). Full-width strip. ──
    draw.rectangle((0, 25, 31, 31), fill=MAGMA_OUTER)
    # Hot pixels scattered
    for (mx, my) in ((3, 27), (7, 29), (12, 26), (15, 28), (19, 27),
                     (22, 30), (25, 26), (28, 28)):
        img.putpixel((mx, my), MAGMA_HOT)
    # Crack lines
    for (cx, cy) in ((5, 28), (11, 29), (17, 27), (23, 26), (27, 30)):
        img.putpixel((cx, cy), MAGMA_CRACK)

    # ── Ember spike region: (17,0)–(23,7). ──
    draw.rectangle((17, 0, 23, 7), fill=EMBER)
    # Hot pixels
    for (ex, ey) in ((18, 1), (21, 3), (19, 5), (22, 6)):
        img.putpixel((ex, ey), EMBER_HOT)
    # Shadow on one edge for depth
    draw.line(((17, 7), (23, 7)), fill=MAGMA_OUTER)

    return img
```

- [ ] **Step 2: Wire `paint_3d_texture` into `main`**

Replace the body of `main()` in `wildwest/tools/gen_meteor_staff.py` (steps may be added to `main` cumulatively across tasks):

```python
def main():
    arg = sys.argv[1] if len(sys.argv) > 1 else None
    tools_dir, assets_dir = resolve_paths(arg)

    os.makedirs(os.path.join(assets_dir, 'textures/item'), exist_ok=True)
    os.makedirs(os.path.join(assets_dir, 'models/item'), exist_ok=True)
    os.makedirs(tools_dir, exist_ok=True)

    texture_3d = paint_3d_texture()
    texture_3d_path = os.path.join(assets_dir, 'textures/item/meteor_staff.png')
    texture_3d.save(texture_3d_path)
    print(f"  wrote {texture_3d_path}")
```

- [ ] **Step 3: Run the generator**

Run: `python3 wildwest/tools/gen_meteor_staff.py`

Expected output: prints `wrote .../textures/item/meteor_staff.png`. No exceptions.

- [ ] **Step 4: Verify PNG dimensions + structure**

Run:

```bash
file wildwest/src/main/resources/assets/wildwest/textures/item/meteor_staff.png
python3 -c "
from PIL import Image
img = Image.open('wildwest/src/main/resources/assets/wildwest/textures/item/meteor_staff.png')
assert img.size == (32, 32), img.size
assert img.mode == 'RGBA', img.mode
# Wood region sample
assert img.getpixel((0, 0)) == (0x3a, 0x24, 0x12, 0xff)
# Margin column 8 = WOOD_GRAIN
assert img.getpixel((8, 0)) == (0x1a, 0x0e, 0x08, 0xff)
# Magma region sample
assert img.getpixel((0, 25)) == (0xff, 0x5a, 0x1f, 0xff)
# Ember region sample
assert img.getpixel((17, 0)) == (0xff, 0x85, 0x33, 0xff)
print('3D texture OK')
"
```

Expected: `... 32 x 32, 8-bit/color RGBA, non-interlaced` and `3D texture OK`.

- [ ] **Step 5: Determinism check — re-run and confirm byte-identical**

```bash
shasum wildwest/src/main/resources/assets/wildwest/textures/item/meteor_staff.png > /tmp/staff_sha_1
python3 wildwest/tools/gen_meteor_staff.py
shasum wildwest/src/main/resources/assets/wildwest/textures/item/meteor_staff.png > /tmp/staff_sha_2
diff /tmp/staff_sha_1 /tmp/staff_sha_2 && echo "deterministic"
```

Expected: `deterministic`.

- [ ] **Step 6: Commit**

```bash
git add wildwest/tools/gen_meteor_staff.py \
        wildwest/src/main/resources/assets/wildwest/textures/item/meteor_staff.png
git commit -m "feat(wildwest): meteor staff 32x32 3D texture atlas"
```

---

## Task 3: Paint the 16×16 GUI sprite

**Files:**
- Modify: `wildwest/tools/gen_meteor_staff.py` (add `paint_gui_icon` + wire into `main`)
- Create: `wildwest/src/main/resources/assets/wildwest/textures/item/meteor_staff_gui.png`

### Steps

- [ ] **Step 1: Add `paint_gui_icon` function**

Append to `wildwest/tools/gen_meteor_staff.py` (just below `paint_3d_texture`):

```python
def paint_gui_icon():
    """Hand-composed 16x16 pixel-art icon.

    PIL pixel coordinates are top-left origin; spec coords match.
    """
    img = Image.new('RGBA', (16, 16), TRANSPARENT)

    # Diagonal shaft: Bresenham line from (3,14) to (11,5). dx=8, dy=-9.
    shaft_pts = []
    x0, y0, x1, y1 = 3, 14, 11, 5
    dx = abs(x1 - x0); dy = -abs(y1 - y0)
    sx = 1 if x0 < x1 else -1; sy = 1 if y0 < y1 else -1
    err = dx + dy
    x, y = x0, y0
    while True:
        shaft_pts.append((x, y))
        if x == x1 and y == y1:
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy; x += sx
        if e2 <= dx:
            err += dx; y += sy
    for (sx_, sy_) in shaft_pts:
        img.putpixel((sx_, sy_), WOOD_BASE)
        # Shadow stroke offset 1 down-left
        if 0 <= sx_ - 1 < 16 and 0 <= sy_ + 1 < 16:
            # Don't overwrite the magma blob area
            if not (sx_ - 1 >= 11 and sy_ + 1 <= 5):
                img.putpixel((sx_ - 1, sy_ + 1), WOOD_GRAIN)

    # Iron band 1: (4,12)-(6,13)
    for bx in range(4, 7):
        for by in range(12, 14):
            img.putpixel((bx, by), IRON_BASE)
    img.putpixel((5, 12), IRON_RIVET)

    # Iron band 2: (7,9)-(9,10)
    for bx in range(7, 10):
        for by in range(9, 11):
            img.putpixel((bx, by), IRON_BASE)
    img.putpixel((8, 9), IRON_RIVET)

    # Magma blob: (11,3)-(13,5)
    for mx in range(11, 14):
        for my in range(3, 6):
            img.putpixel((mx, my), MAGMA_OUTER)
    img.putpixel((12, 4), MAGMA_HOT)
    img.putpixel((13, 3), MAGMA_CRACK)

    # Glow halo: 4 dim ember pixels at the corners around the blob
    for (gx, gy) in ((10, 2), (14, 2), (10, 6), (14, 6)):
        img.putpixel((gx, gy), EMBER)

    # Wisp pixel
    img.putpixel((13, 1), EMBER_HOT)

    return img
```

- [ ] **Step 2: Wire into `main`**

After the `texture_3d.save(...)` line in `main()`, append:

```python
    texture_gui = paint_gui_icon()
    texture_gui_path = os.path.join(assets_dir, 'textures/item/meteor_staff_gui.png')
    texture_gui.save(texture_gui_path)
    print(f"  wrote {texture_gui_path}")
```

- [ ] **Step 3: Run the generator**

Run: `python3 wildwest/tools/gen_meteor_staff.py`

Expected: prints both `wrote .../meteor_staff.png` and `wrote .../meteor_staff_gui.png`.

- [ ] **Step 4: Verify GUI PNG**

Run:

```bash
file wildwest/src/main/resources/assets/wildwest/textures/item/meteor_staff_gui.png
python3 -c "
from PIL import Image
img = Image.open('wildwest/src/main/resources/assets/wildwest/textures/item/meteor_staff_gui.png')
assert img.size == (16, 16), img.size
assert img.mode == 'RGBA', img.mode
# Magma centre is hot
assert img.getpixel((12, 4)) == (0xff, 0xb0, 0x2e, 0xff)
# Wisp
assert img.getpixel((13, 1)) == (0xff, 0xd0, 0x66, 0xff)
# Shaft start
assert img.getpixel((3, 14)) == (0x3a, 0x24, 0x12, 0xff)
# Background transparent
assert img.getpixel((0, 0)) == (0, 0, 0, 0)
print('GUI icon OK')
"
```

Expected: `... 16 x 16, 8-bit/color RGBA, non-interlaced` and `GUI icon OK`.

- [ ] **Step 5: Commit**

```bash
git add wildwest/tools/gen_meteor_staff.py \
        wildwest/src/main/resources/assets/wildwest/textures/item/meteor_staff_gui.png
git commit -m "feat(wildwest): meteor staff 16x16 GUI pixel-art icon"
```

---

## Task 4: Build the 3D model JSON

**Files:**
- Modify: `wildwest/tools/gen_meteor_staff.py` (add `build_model_json` + UV helper)
- Modify: `wildwest/src/main/resources/assets/wildwest/models/item/meteor_staff.json` (overwrite)

### Steps

- [ ] **Step 1: Add UV mapping + model JSON builder**

Append to `wildwest/tools/gen_meteor_staff.py` (after `paint_gui_icon`):

```python
def uv_for(group):
    """Return the 4-tuple [u1, v1, u2, v2] for a given UV group.

    All faces of all cubes in the same group share the same UV rect — the
    geometry is small enough that texture detail comes from group-level
    palette/pattern, not per-face uniqueness.
    """
    x1, y1, x2, y2 = UV_REGIONS[group]
    # +1 on the upper bounds because UV_REGIONS stores inclusive pixel
    # coords but model JSON uv is half-open like a slice end.
    return [x1, y1, x2 + 1, y2 + 1]


def build_model_json():
    elements = []
    for name, frm, to, group in CUBES:
        uv = uv_for(group)
        elements.append({
            "name": name,
            "from": list(frm),
            "to": list(to),
            "faces": {
                "north": {"uv": uv, "texture": "#0"},
                "south": {"uv": uv, "texture": "#0"},
                "east":  {"uv": uv, "texture": "#0"},
                "west":  {"uv": uv, "texture": "#0"},
                "up":    {"uv": uv, "texture": "#0"},
                "down":  {"uv": uv, "texture": "#0"},
            },
        })
    return {
        "credit": "Generated by wildwest/tools/gen_meteor_staff.py",
        "texture_size": [32, 32],
        "textures": {
            "0": "wildwest:item/meteor_staff",
            "particle": "wildwest:item/meteor_staff",
        },
        "elements": elements,
        "display": DISPLAY,
    }
```

- [ ] **Step 2: Wire into `main`**

After the `texture_gui.save(...)` block in `main()`, append:

```python
    model_3d = build_model_json()
    model_3d_path = os.path.join(assets_dir, 'models/item/meteor_staff.json')
    with open(model_3d_path, 'w') as f:
        json.dump(model_3d, f, indent=2)
        f.write('\n')
    print(f"  wrote {model_3d_path}")
```

- [ ] **Step 3: Run the generator**

Run: `python3 wildwest/tools/gen_meteor_staff.py`

Expected: prints `wrote .../models/item/meteor_staff.json` along with prior outputs.

- [ ] **Step 4: Verify model JSON structure**

Run:

```bash
python3 -c "
import json
m = json.load(open('wildwest/src/main/resources/assets/wildwest/models/item/meteor_staff.json'))
assert m['textures']['0'] == 'wildwest:item/meteor_staff'
assert m['textures']['particle'] == 'wildwest:item/meteor_staff', 'particle slot missing'
assert len(m['elements']) == 16, f'expected 16 elements, got {len(m[\"elements\"])}'
for el in m['elements']:
    assert set(el['faces'].keys()) == {'north','south','east','west','up','down'}, el['name']
    for f in el['faces'].values():
        assert f['texture'] == '#0'
# Display transforms include all 8 contexts
expected_ctx = {'thirdperson_righthand','thirdperson_lefthand','firstperson_righthand',
                'firstperson_lefthand','ground','fixed','gui','head'}
assert set(m['display'].keys()) == expected_ctx
print('3D model JSON OK')
"
```

Expected: `3D model JSON OK`.

- [ ] **Step 5: Commit**

```bash
git add wildwest/tools/gen_meteor_staff.py \
        wildwest/src/main/resources/assets/wildwest/models/item/meteor_staff.json
git commit -m "feat(wildwest): meteor staff 3D voxel model JSON"
```

---

## Task 5: Build the flat 2D GUI model JSON

**Files:**
- Modify: `wildwest/tools/gen_meteor_staff.py` (add `build_gui_model_json` + wire into `main`)
- Create: `wildwest/src/main/resources/assets/wildwest/models/item/meteor_staff_gui.json`

### Steps

- [ ] **Step 1: Add the builder**

Append to `wildwest/tools/gen_meteor_staff.py` (after `build_model_json`):

```python
def build_gui_model_json():
    return {
        "parent": "minecraft:item/generated",
        "textures": {
            "layer0": "wildwest:item/meteor_staff_gui",
        },
    }
```

- [ ] **Step 2: Wire into `main`**

Append to `main()` after the 3D-model-JSON write block:

```python
    model_gui = build_gui_model_json()
    model_gui_path = os.path.join(assets_dir, 'models/item/meteor_staff_gui.json')
    with open(model_gui_path, 'w') as f:
        json.dump(model_gui, f, indent=2)
        f.write('\n')
    print(f"  wrote {model_gui_path}")
```

- [ ] **Step 3: Run the generator**

Run: `python3 wildwest/tools/gen_meteor_staff.py`

Expected: now prints 4 `wrote ...` lines (3D texture, GUI texture, 3D model, GUI model).

- [ ] **Step 4: Verify the GUI model JSON**

Run:

```bash
python3 -c "
import json
m = json.load(open('wildwest/src/main/resources/assets/wildwest/models/item/meteor_staff_gui.json'))
assert m['parent'] == 'minecraft:item/generated', m['parent']
assert m['textures']['layer0'] == 'wildwest:item/meteor_staff_gui'
print('GUI model JSON OK')
"
```

Expected: `GUI model JSON OK`.

- [ ] **Step 5: Commit**

```bash
git add wildwest/tools/gen_meteor_staff.py \
        wildwest/src/main/resources/assets/wildwest/models/item/meteor_staff_gui.json
git commit -m "feat(wildwest): meteor staff 2D GUI model JSON"
```

---

## Task 6: Update the ItemModelDefinition selector

**Files:**
- Modify: `wildwest/src/main/resources/assets/wildwest/items/meteor_staff.json`

**Why a separate task:** This is a hand-edited JSON, not generator output. Keeping it separate from the generator commits makes its history easy to read.

### Steps

- [ ] **Step 1: Rewrite the selector**

Replace the contents of `wildwest/src/main/resources/assets/wildwest/items/meteor_staff.json` with:

```json
{
  "model": {
    "type": "minecraft:select",
    "property": "minecraft:display_context",
    "cases": [
      {
        "when": "gui",
        "model": {
          "type": "minecraft:model",
          "model": "wildwest:item/meteor_staff_gui"
        }
      }
    ],
    "fallback": {
      "type": "minecraft:model",
      "model": "wildwest:item/meteor_staff"
    }
  }
}
```

- [ ] **Step 2: Verify it parses**

Run:

```bash
python3 -c "
import json
d = json.load(open('wildwest/src/main/resources/assets/wildwest/items/meteor_staff.json'))
sel = d['model']
assert sel['type'] == 'minecraft:select'
assert sel['property'] == 'minecraft:display_context'
assert sel['cases'][0]['when'] == 'gui'
assert sel['cases'][0]['model']['model'] == 'wildwest:item/meteor_staff_gui'
assert sel['fallback']['model'] == 'wildwest:item/meteor_staff'
print('selector OK')
"
```

Expected: `selector OK`.

- [ ] **Step 3: Commit**

```bash
git add wildwest/src/main/resources/assets/wildwest/items/meteor_staff.json
git commit -m "feat(wildwest): route meteor staff GUI to 2D, hand to 3D"
```

---

## Task 7: Build the .bbmodel source file

**Files:**
- Modify: `wildwest/tools/gen_meteor_staff.py` (add `build_bbmodel` + wire into `main`)
- Create: `wildwest/tools/meteor_staff.bbmodel`

**Why last among the generator tasks:** The bbmodel is the most format-sensitive output — it embeds the texture as base64 and has to match Blockbench's expected `java_block` project schema. We want the other artifacts working first so the bbmodel is the only moving piece if Blockbench complains.

### Steps

- [ ] **Step 1: Add the bbmodel builder**

Append to `wildwest/tools/gen_meteor_staff.py` (after `build_gui_model_json`):

```python
def png_to_data_url(img):
    import io
    buf = io.BytesIO()
    img.save(buf, format='PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode('ascii')


def build_bbmodel(texture_3d_img, texture_3d_relpath):
    elements = []
    outliner = []
    for name, frm, to, group in CUBES:
        eid = det_uuid(f'element/{name}')
        uv = uv_for(group)
        elements.append({
            "name": name,
            "rescale": False,
            "locked": False,
            "from": list(frm),
            "to": list(to),
            "autouv": 0,
            "color": 0,
            "origin": [0, 0, 0],
            "faces": {
                "north": {"uv": uv, "texture": 0},
                "east":  {"uv": uv, "texture": 0},
                "south": {"uv": uv, "texture": 0},
                "west":  {"uv": uv, "texture": 0},
                "up":    {"uv": uv, "texture": 0},
                "down":  {"uv": uv, "texture": 0},
            },
            "type": "cube",
            "uuid": eid,
        })
        outliner.append(eid)

    texture = {
        "path": "",
        "name": "meteor_staff.png",
        "folder": "item",
        "namespace": "wildwest",
        "id": "0",
        "particle": True,
        "render_mode": "default",
        "render_sides": "auto",
        "frame_time": 1,
        "frame_order_type": "loop",
        "frame_order": "",
        "frame_interpolate": False,
        "visible": True,
        "internal": True,
        "saved": False,
        "uuid": det_uuid('texture/meteor_staff'),
        "relative_path": texture_3d_relpath,
        "use_as_default": True,
        "layers_enabled": False,
        "sync_to_project": "",
        "width": 32,
        "height": 32,
        "uv_width": 32,
        "uv_height": 32,
        "source": png_to_data_url(texture_3d_img),
    }

    return {
        "meta": {
            "format_version": "4.5",
            "model_format": "java_block",
            "box_uv": False,
        },
        "name": "meteor_staff",
        "model_identifier": "wildwest:meteor_staff",
        "visible_box": [2, 2, 0],
        "variable_placeholders": "",
        "variable_placeholder_buttons": [],
        "unhandled_root_fields": {},
        "resolution": {"width": 32, "height": 32},
        "elements": elements,
        "outliner": outliner,
        "textures": [texture],
        "display": DISPLAY,
    }
```

- [ ] **Step 2: Wire into `main`**

Append to `main()` after the GUI-model-JSON write block:

```python
    bbmodel_relpath = '../src/main/resources/assets/wildwest/textures/item/meteor_staff.png'
    bbmodel = build_bbmodel(texture_3d, bbmodel_relpath)
    bbmodel_path = os.path.join(tools_dir, 'meteor_staff.bbmodel')
    with open(bbmodel_path, 'w') as f:
        json.dump(bbmodel, f, indent=2)
        f.write('\n')
    print(f"  wrote {bbmodel_path}")
```

- [ ] **Step 3: Run the generator**

Run: `python3 wildwest/tools/gen_meteor_staff.py`

Expected: now prints 5 `wrote ...` lines. No exceptions.

- [ ] **Step 4: Verify bbmodel structure**

Run:

```bash
python3 -c "
import json
m = json.load(open('wildwest/tools/meteor_staff.bbmodel'))
assert m['meta']['format_version'] == '4.5'
assert m['meta']['model_format'] == 'java_block'
assert m['name'] == 'meteor_staff'
assert len(m['elements']) == 16
assert len(m['outliner']) == 16
assert len(m['textures']) == 1
tex = m['textures'][0]
assert tex['source'].startswith('data:image/png;base64,')
assert tex['width'] == 32 and tex['height'] == 32
assert tex['particle'] is True
assert m['display'].keys() == set(['thirdperson_righthand','thirdperson_lefthand','firstperson_righthand','firstperson_lefthand','ground','fixed','gui','head'])
# Determinism: UUIDs are stable
e0_uuid = m['elements'][0]['uuid']
print(f'bbmodel OK (first element UUID: {e0_uuid})')
"
```

Expected: `bbmodel OK (first element UUID: ...)`.

- [ ] **Step 5: Determinism check across whole pipeline**

```bash
shasum wildwest/tools/meteor_staff.bbmodel \
       wildwest/src/main/resources/assets/wildwest/textures/item/meteor_staff.png \
       wildwest/src/main/resources/assets/wildwest/textures/item/meteor_staff_gui.png \
       wildwest/src/main/resources/assets/wildwest/models/item/meteor_staff.json \
       wildwest/src/main/resources/assets/wildwest/models/item/meteor_staff_gui.json > /tmp/staff_all_1
python3 wildwest/tools/gen_meteor_staff.py
shasum wildwest/tools/meteor_staff.bbmodel \
       wildwest/src/main/resources/assets/wildwest/textures/item/meteor_staff.png \
       wildwest/src/main/resources/assets/wildwest/textures/item/meteor_staff_gui.png \
       wildwest/src/main/resources/assets/wildwest/models/item/meteor_staff.json \
       wildwest/src/main/resources/assets/wildwest/models/item/meteor_staff_gui.json > /tmp/staff_all_2
diff /tmp/staff_all_1 /tmp/staff_all_2 && echo "all outputs deterministic"
```

Expected: `all outputs deterministic`.

- [ ] **Step 6: Commit**

```bash
git add wildwest/tools/gen_meteor_staff.py wildwest/tools/meteor_staff.bbmodel
git commit -m "feat(wildwest): meteor staff .bbmodel source + generator wiring"
```

---

## Task 8: Datagen + build smoke test

**Why:** Confirm NeoForge's datagen + resource loading accepts the new model files and selector before declaring done. Catches schema typos that the Python validation doesn't catch (e.g. NeoForge's stricter selector schema).

### Steps

- [ ] **Step 1: Run datagen**

Run: `./gradlew :wildwest:runData`

Expected: BUILD SUCCESSFUL. No `model validation failed` or `Couldn't parse element` warnings in the output.

- [ ] **Step 2: Run the main build**

Run: `./gradlew :wildwest:build`

Expected: BUILD SUCCESSFUL. No new compile-time warnings related to meteor staff assets.

- [ ] **Step 3: Confirm the staff still tests cleanly**

Run: `./gradlew :wildwest:test --tests "*Meteor*"`

Expected: PASS (the existing MeteorStaffItem tests are gameplay-only and shouldn't be affected by visual changes — this is a regression check).

- [ ] **Step 4: If any tests fail**

Investigate the failure. The visual changes should not affect logic — if a test broke, something unexpected is happening. Do not skip or modify the tests; report the failure.

- [ ] **Step 5: No commit needed**

(No files changed in this task; it's verification only.)

---

## Task 9: Manual smoke test note + final commit

**Why:** The repo convention for visual mob/item work (see [docs/superpowers/specs/2026-04-29-thief-mob-design.md](docs/superpowers/specs/2026-04-29-thief-mob-design.md) etc.) is to note when an interactive dev-client smoke test is deferred so it doesn't get lost. We can't auto-launch the dev client, so the task is to record the deferred check.

### Steps

- [ ] **Step 1: Append a deferred-smoke-test note to the project memory**

Update `~/.claude/projects/-Users-tweeks-code-minecraft-mods/memory/MEMORY.md` to add a new line for this work, pointing at a new memory file recording what manual testing is needed.

Create `~/.claude/projects/-Users-tweeks-code-minecraft-mods/memory/project_meteor_staff_visuals.md`:

```markdown
---
name: Meteor Staff visual upgrade complete
description: 3D voxel staff model + 2D GUI icon shipped 2026-06-03; manual dev-client smoke test deferred
type: project
---

3D voxel meteor staff (charred wood + magma core, 16 cubes) plus dedicated 16×16 pixel-art GUI icon shipped on `main` 2026-06-03. Generator at [wildwest/tools/gen_meteor_staff.py](wildwest/tools/gen_meteor_staff.py); editable Blockbench source at [wildwest/tools/meteor_staff.bbmodel](wildwest/tools/meteor_staff.bbmodel). Routing via `display_context=gui` selector in [items/meteor_staff.json](wildwest/src/main/resources/assets/wildwest/items/meteor_staff.json).

**Why:** Original 16×16 placeholder was always intended to be replaced (noted in [2026-05-10-herobrine.md](docs/superpowers/plans/2026-05-10-herobrine.md) Task 22). User wanted a 3D held weapon plus a sharper inventory icon.

**How to apply:** Manual dev-client smoke test deferred (`./gradlew :wildwest:runClient` + `/give @s wildwest:meteor_staff`, eyeball hotbar icon, first-person, third-person, ground drop, item frame). If user reports clipping or float, adjust `firstperson_righthand.translation.y` or `ground.translation.y` in [gen_meteor_staff.py](wildwest/tools/gen_meteor_staff.py) and re-run the generator — never edit the generated JSON directly.
```

Then append this line to `MEMORY.md`:

```
- [Meteor Staff visual upgrade complete](project_meteor_staff_visuals.md) — 3D voxel + 2D GUI shipped 2026-06-03; manual dev-client smoke deferred
```

- [ ] **Step 2: No git commit for memory updates**

(Memory files are in the user's home directory, not the repo. The work is already committed via Tasks 1–7.)

---

## Self-review checklist (run BEFORE handoff)

- ✅ Spec covers `items/meteor_staff.json` selector → Task 6.
- ✅ Spec covers 3D model JSON with `textures.0` + `textures.particle` → Task 4 step 4 explicitly asserts both.
- ✅ Spec covers 32×32 atlas with 1-pixel margins → Task 2 paints margin columns/rows with `WOOD_GRAIN`.
- ✅ Spec covers 16×16 GUI icon with exact pixel coords → Task 3 uses those coords verbatim.
- ✅ Spec covers `.bbmodel` with `java_block` format, embedded PNG, deterministic UUIDs → Task 7.
- ✅ Spec covers `gen_meteor_staff.py` outline → Tasks 1–7 build it up incrementally.
- ✅ Spec testing section (datagen + manual smoke) → Tasks 8 + 9.
- ✅ Ground-context float risk noted → Task 9 memory entry mentions adjustment path.
- ✅ Type/name consistency: `paint_3d_texture`, `paint_gui_icon`, `build_model_json`, `build_gui_model_json`, `build_bbmodel`, `uv_for`, `resolve_paths`, `CUBES`, `UV_REGIONS`, `DISPLAY`, `det_uuid` — used identically across every task that references them.
