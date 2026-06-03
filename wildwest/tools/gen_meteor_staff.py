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
    # First pass: shadow stroke offset 1 down-left
    for (px, py) in shaft_pts:
        if 0 <= px - 1 < 16 and 0 <= py + 1 < 16:
            img.putpixel((px - 1, py + 1), WOOD_GRAIN)
    # Second pass: shaft pixels overwrite shadow at overlaps
    for (px, py) in shaft_pts:
        img.putpixel((px, py), WOOD_BASE)

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


def build_gui_model_json():
    return {
        "parent": "minecraft:item/generated",
        "textures": {
            "layer0": "wildwest:item/meteor_staff_gui",
        },
    }


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

    texture_gui = paint_gui_icon()
    texture_gui_path = os.path.join(assets_dir, 'textures/item/meteor_staff_gui.png')
    texture_gui.save(texture_gui_path)
    print(f"  wrote {texture_gui_path}")

    model_3d = build_model_json()
    model_3d_path = os.path.join(assets_dir, 'models/item/meteor_staff.json')
    with open(model_3d_path, 'w') as f:
        json.dump(model_3d, f, indent=2)
        f.write('\n')
    print(f"  wrote {model_3d_path}")

    model_gui = build_gui_model_json()
    model_gui_path = os.path.join(assets_dir, 'models/item/meteor_staff_gui.json')
    with open(model_gui_path, 'w') as f:
        json.dump(model_gui, f, indent=2)
        f.write('\n')
    print(f"  wrote {model_gui_path}")

    bbmodel_relpath = '../src/main/resources/assets/wildwest/textures/item/meteor_staff.png'
    bbmodel = build_bbmodel(texture_3d, bbmodel_relpath)
    bbmodel_path = os.path.join(tools_dir, 'meteor_staff.bbmodel')
    with open(bbmodel_path, 'w') as f:
        json.dump(bbmodel, f, indent=2)
        f.write('\n')
    print(f"  wrote {bbmodel_path}")


if __name__ == '__main__':
    main()
