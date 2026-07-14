#!/usr/bin/env python3
"""Generate 3D voxel weapon assets: lightsaber (x4 blade colors) + both blasters.

Mirrors wildwest/tools/gen_meteor_staff.py's approach — a single cube list
per weapon feeds both the element-based `models/item/<name>.json` and a
matching `tools/<name>.bbmodel` (deterministic uuid5 namespace so re-runs are
byte-identical). Cube coordinates are in the same 0-16 model-pixel space as
wildwest's flat pistol.json elements (y up).

Item-model GUI handling: unlike meteor_staff.json (which swaps between a
flat 2D icon and the 3D model at the `assets/starwars/items/*.json` selector
level via `minecraft:display_context`), these weapons are small enough to
render acceptably as full 3D voxels in every display context, including the
inventory slot. So each generated model is fully self-contained (its own
`texture_size` + full `display` block covering every context, including a
`gui` entry using vanilla's standard angled "item in a box" transform)
instead of splitting into two model files. `assets/starwars/items/*.json`
for the blasters are therefore left untouched (Task 6 already points them
at the same model path); only `assets/starwars/items/lightsaber.json` gets
its own (component-select) selector, written by this script.

Re-run with:
    python3 starwars/tools/gen_weapon_models.py
"""
import json
import os
import sys
import uuid
import base64

from PIL import Image, ImageDraw

# Deterministic UUIDs so re-runs produce byte-identical output.
NAMESPACE = uuid.UUID('00000000-0000-0000-0000-000000000001')


def det_uuid(name):
    return str(uuid.uuid5(NAMESPACE, name))


TRANSPARENT = (0, 0, 0, 0)

# ─── Lightsaber hilt palette (shared by all 4 blade colors) ─────────────
HILT_METAL       = (0x9A, 0x9A, 0xA4, 0xFF)  # 'hilt' region base (grip cube)
HILT_HIGHLIGHT   = (0xB8, 0xB8, 0xC2, 0xFF)  # 'hilt' top edge
HILT_GRIP_ACCENT = (0x2A, 0x2A, 0x2E, 0xFF)  # 'hilt' grip ridge lines
HILT_DARK        = (0x3A, 0x3A, 0x40, 0xFF)  # 'hilt_dark' region base (pommel/emitter)
HILT_DARK_SHADE  = (0x20, 0x20, 0x24, 0xFF)  # 'hilt_dark' bottom edge
WHITE_HOT        = (0xFF, 0xFF, 0xFF, 0xFF)  # blade core column

# SaberColor ordinal order matters — must match the Java enum exactly, since
# `items/lightsaber.json`'s "when" cases key off BLADE_COLOR's ordinal int.
SABER_COLORS = [
    ('blue',   (0x30, 0x60, 0xFF)),
    ('green',  (0x30, 0xE0, 0x50)),
    ('red',    (0xFF, 0x20, 0x20)),
    ('purple', (0xA0, 0x30, 0xE0)),
]

# ─── Blaster metal/wood palette (lifted from gen_item_textures.py) ──────
GUNMETAL   = (0x2A, 0x2A, 0x30, 0xFF)
HIGHLIGHT  = (0x55, 0x55, 0x60, 0xFF)
GRIP_BLACK = (0x18, 0x18, 0x18, 0xFF)
GRIP_HI    = (0x30, 0x30, 0x30, 0xFF)
OUTLINE    = (0x0A, 0x0A, 0x0C, 0xFF)
STOCK_WOOD = (0x3A, 0x30, 0x28, 0xFF)
STOCK_HI   = (0x55, 0x48, 0x38, 0xFF)
STOCK_DARK = (0x2A, 0x22, 0x1A, 0xFF)
METAL_DARK = (0x1A, 0x1A, 0x20, 0xFF)  # barrel/scope shading, distinct from grip black

# ─── Holocron palette ────────────────────────────────────────────────────
HOLOCRON_BLUE = (0x20, 0x38, 0x80, 0xFF)  # deep blue cube faces
HOLOCRON_CYAN = (0x60, 0xC8, 0xE0, 0xFF)  # cyan edge lines / corner studs
HOLOCRON_GLOW = (0xFF, 0xFF, 0xFF, 0xFF)  # glowing white core

# ─── Cube lists (units = model pixels, y up, wildwest pistol.json style) ─
LIGHTSABER_CUBES = [
    # (name, from, to, texture_region)
    ('pommel',  (7, 0, 7),        (9, 2, 9),        'hilt_dark'),
    ('grip',    (7, 2, 7),        (9, 7, 9),        'hilt'),
    ('emitter', (6.5, 7, 6.5),    (9.5, 9, 9.5),    'hilt_dark'),
    ('blade',   (7.25, 9, 7.25),  (8.75, 24, 8.75), 'blade'),
]

BLASTER_PISTOL_CUBES = [
    ('body',   (6, 5, 4),   (10, 8, 13),  'metal'),
    ('barrel', (7, 6, 13),  (9, 7.5, 16), 'metal_dark'),
    ('grip',   (6.5, 1, 4), (9.5, 5, 7),  'grip'),
]

BLASTER_RIFLE_CUBES = [
    ('stock',  (6.5, 4, 0), (9.5, 7, 5),   'wood'),
    ('body',   (6, 5, 5),   (10, 8, 12),   'metal'),
    ('barrel', (7, 6, 12),  (9, 7.5, 18),  'metal_dark'),
    ('scope',  (7, 8, 7),   (9, 9.5, 10),  'metal_dark'),
]

# 6x6x6 centered core cube + four 1x1x1 studs sitting on its top-face
# corners. All faces share the single 'core' UV region (procedural, not
# pixel-mapped) — the studs read as small glowing nubs on the cube corners.
HOLOCRON_CUBES = [
    ('core',     (5, 5, 5),   (11, 11, 11), 'core'),
    ('stud_nw',  (5, 11, 5),  (6, 12, 6),   'core'),
    ('stud_ne',  (10, 11, 5), (11, 12, 6),  'core'),
    ('stud_sw',  (5, 11, 10), (6, 12, 11),  'core'),
    ('stud_se',  (10, 11, 10), (11, 12, 11), 'core'),
]

# ─── UV atlas regions (inclusive pixel coords, half-open via uv_for) ────
SABER_TEXTURE_SIZE = 16
UV_REGIONS_SABER = {
    'hilt_dark': (0, 9, 7, 15),
    'hilt':      (8, 9, 15, 15),
    'blade':     (0, 0, 15, 8),
}

BLASTER_TEXTURE_SIZE = 32
UV_REGIONS_BLASTER = {
    'metal':      (0, 0, 15, 9),
    'metal_dark': (17, 0, 23, 7),
    'grip':       (25, 0, 31, 7),
    'wood':       (0, 11, 15, 19),
}

HOLOCRON_TEXTURE_SIZE = 16
UV_REGIONS_HOLOCRON = {
    'core': (0, 0, 15, 15),
}

# ─── Task-19 vehicle item models (voxel mini-silhouettes) ────────────────
# Each vehicle held as an item gets a compact 3D voxel model — mirroring the
# landspeeder's item file set (items/<id>.json + models/item/<id>.json +
# textures/item/<id>.png) but as a recognizable 3D silhouette rather than a
# flat sprite. Same self-contained model pattern as the weapons above (own
# texture_size + full display block). Cube coords are model-pixel space, y up.
VEHICLE_TEXTURE_SIZE = 16

# speeder_bike: long orange chassis, forward nose, rear black saddle, two
# silver steering vanes, two low rails.
SPEEDER_BIKE_ITEM_CUBES = [
    ('chassis', (5, 6, 2),    (11, 9, 14),  'body'),
    ('nose',    (6, 6.5, 0),  (10, 8.5, 2), 'body'),
    ('seat',    (6.5, 9, 8),  (9.5, 11, 13),'seat'),
    ('vane_l',  (3, 6.5, 1),  (5, 8.5, 5),  'vane'),
    ('vane_r',  (11, 6.5, 1), (13, 8.5, 5), 'vane'),
    ('rail_l',  (4, 5, 4),    (5, 6, 12),   'rail'),
    ('rail_r',  (11, 5, 4),   (12, 6, 12),  'rail'),
]
UV_REGIONS_SPEEDER_BIKE = {
    'body': (0, 0, 7, 7),
    'seat': (8, 0, 11, 3),
    'vane': (8, 4, 11, 7),
    'rail': (12, 0, 15, 3),
}

# xwing: a short fuselage + nose with two S-foil bars crossed at +/-45 deg
# about Z, forming an unmistakable X. Viewed near face-on via a custom gui
# transform so the X reads from the inventory slot.
XWING_ITEM_CUBES = [
    ('fuselage', (7, 6.5, 5),   (9, 9.5, 12),  'hull'),
    ('nose',     (7.25, 7, 2),  (8.75, 9, 5),  'hull'),
    ('cockpit',  (7.25, 9.5, 7),(8.75, 10.5, 9),'glass'),
    ('wing_a',   (7, 1, 7),     (9, 15, 9),    'wing',
     {'angle': 45, 'axis': 'z', 'origin': [8, 8, 8]}),
    ('wing_b',   (7, 1, 7),     (9, 15, 9),    'wing',
     {'angle': -45, 'axis': 'z', 'origin': [8, 8, 8]}),
]
UV_REGIONS_XWING = {
    'hull':  (0, 0, 7, 7),
    'wing':  (8, 0, 15, 7),
    'glass': (0, 8, 3, 11),
}

# tie_fighter: central gunmetal ball + red window, two short pylons, two big
# black outboard hex panels.
TIE_FIGHTER_ITEM_CUBES = [
    ('ball',    (5, 5, 5),   (11, 11, 11), 'metal'),
    ('window',  (6, 6, 4),   (10, 10, 5),  'window'),
    ('pylon_l', (3, 7, 7),   (5, 9, 9),    'metal'),
    ('pylon_r', (11, 7, 7),  (13, 9, 9),   'metal'),
    ('panel_l', (1, 2, 2),   (2, 14, 14),  'panel'),
    ('panel_r', (14, 2, 2),  (15, 14, 14), 'panel'),
]
UV_REGIONS_TIE_FIGHTER = {
    'metal':  (0, 0, 7, 7),
    'panel':  (8, 0, 15, 7),
    'window': (0, 8, 3, 11),
}

# ─── Display transforms (shared across all generated weapon models) ─────
# Structure mirrors gen_meteor_staff.py's DISPLAY dict (same 8 contexts).
# "gui" uses vanilla's standard angled item-in-a-box transform (matches
# minecraft:item/generated's builtin gui entry) so the voxel model reads
# well as an inventory icon without needing a separate flat 2D model.
DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, 90, -35], "translation": [0, 2.0, 1.0], "scale": [0.9, 0.9, 0.9]},
    "thirdperson_lefthand":  {"rotation": [0, 90, -35], "translation": [0, 2.0, 1.0], "scale": [0.9, 0.9, 0.9]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.0, 3.0, 1.0], "scale": [0.8, 0.8, 0.8]},
    "firstperson_lefthand":  {"rotation": [0, -90, 25], "translation": [1.0, 3.0, 1.0], "scale": [0.8, 0.8, 0.8]},
    "ground":                {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed":                 {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1.0, 1.0, 1.0]},
    "gui":                   {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
    "head":                  {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1.0, 1.0, 1.0]},
}

# X-wing item: the shared gui transform's 225 deg yaw would spin the S-foil X
# edge-on. Override gui to a near-face-on view (small pitch+yaw for depth) so
# the X reads clearly from the inventory slot. Other contexts inherit DISPLAY.
XWING_ITEM_DISPLAY = dict(DISPLAY)
XWING_ITEM_DISPLAY["gui"] = {"rotation": [14, -20, 0], "translation": [0, 0, 0], "scale": [0.72, 0.72, 0.72]}


def lighten(color, amount):
    r, g, b, a = color
    return (min(255, r + amount), min(255, g + amount), min(255, b + amount), a)


def darken(color, amount):
    r, g, b, a = color
    return (max(0, r - amount), max(0, g - amount), max(0, b - amount), a)


# ─── Path helpers ────────────────────────────────────────────────────────
def resolve_paths(arg):
    if arg is None:
        tools_dir = os.path.abspath('starwars/tools')
    else:
        tools_dir = os.path.abspath(arg)
    repo_root = os.path.dirname(tools_dir)
    assets_dir = os.path.join(repo_root, 'src/main/resources/assets/starwars')
    return tools_dir, assets_dir


# ─── Texture painting ────────────────────────────────────────────────────
def paint_saber_texture(blade_rgb):
    """16x16: hilt grays in the lower half, saturated blade + white-hot core above."""
    img = Image.new('RGBA', (16, 16), TRANSPARENT)
    draw = ImageDraw.Draw(img)

    # 'hilt_dark' region: pommel + emitter.
    draw.rectangle((0, 9, 7, 15), fill=HILT_DARK)
    draw.rectangle((0, 9, 7, 9), fill=lighten(HILT_DARK, 0x10))
    draw.rectangle((0, 15, 7, 15), fill=HILT_DARK_SHADE)

    # 'hilt' region: grip, with ridge accents for detail.
    draw.rectangle((8, 9, 15, 15), fill=HILT_METAL)
    draw.rectangle((8, 9, 15, 9), fill=HILT_HIGHLIGHT)
    draw.rectangle((8, 15, 15, 15), fill=darken(HILT_METAL, 0x30))
    for x in (10, 13):
        draw.line(((x, 10), (x, 14)), fill=HILT_GRIP_ACCENT)

    # 'blade' region: saturated color strip with a 1px white-hot core.
    blade = (blade_rgb[0], blade_rgb[1], blade_rgb[2], 0xFF)
    draw.rectangle((0, 0, 15, 8), fill=blade)
    draw.rectangle((0, 0, 15, 0), fill=lighten(blade, 0x30))
    draw.rectangle((0, 8, 15, 8), fill=darken(blade, 0x40))
    draw.rectangle((7, 0, 8, 8), fill=WHITE_HOT)

    return img


def paint_blaster_texture(include_wood):
    """32x32 atlas: metal body/barrel/grip regions, optional wood stock region."""
    img = Image.new('RGBA', (32, 32), TRANSPARENT)
    draw = ImageDraw.Draw(img)

    # 'metal' region: body.
    draw.rectangle((0, 0, 15, 9), fill=GUNMETAL)
    draw.rectangle((0, 0, 15, 0), fill=HIGHLIGHT)
    draw.rectangle((0, 9, 15, 9), fill=OUTLINE)

    # 'metal_dark' region: barrel/scope.
    draw.rectangle((17, 0, 23, 7), fill=METAL_DARK)
    draw.rectangle((17, 0, 23, 0), fill=GUNMETAL)
    draw.rectangle((17, 7, 23, 7), fill=OUTLINE)

    # 'grip' region.
    draw.rectangle((25, 0, 31, 7), fill=GRIP_BLACK)
    draw.rectangle((25, 0, 31, 0), fill=GRIP_HI)

    if include_wood:
        # 'wood' region: rifle stock.
        draw.rectangle((0, 11, 15, 19), fill=STOCK_WOOD)
        draw.rectangle((0, 11, 15, 11), fill=STOCK_HI)
        draw.rectangle((0, 19, 15, 19), fill=STOCK_DARK)
        for x in (3, 7, 11):
            draw.line(((x, 12), (x, 18)), fill=STOCK_DARK)

    return img


def paint_holocron_texture():
    """16x16: deep blue base, cyan border edge lines, glowing white 2x2 center."""
    img = Image.new('RGBA', (HOLOCRON_TEXTURE_SIZE, HOLOCRON_TEXTURE_SIZE), TRANSPARENT)
    draw = ImageDraw.Draw(img)

    # 'core' region: deep blue base fill.
    draw.rectangle((0, 0, 15, 15), fill=HOLOCRON_BLUE)

    # Cyan edge lines tracing the region border.
    draw.rectangle((0, 0, 15, 0), fill=HOLOCRON_CYAN)
    draw.rectangle((0, 15, 15, 15), fill=HOLOCRON_CYAN)
    draw.rectangle((0, 0, 0, 15), fill=HOLOCRON_CYAN)
    draw.rectangle((15, 0, 15, 15), fill=HOLOCRON_CYAN)

    # Glowing white 2x2 center.
    draw.rectangle((7, 7, 8, 8), fill=HOLOCRON_GLOW)

    return img


# ─── Vehicle item textures (small atlases, 3+ tones per region) ──────────
def _region3(draw, box, base, hi, sh):
    """Fill a UV region with base + a top-edge highlight + bottom-edge shade."""
    x1, y1, x2, y2 = box
    draw.rectangle((x1, y1, x2, y2), fill=base)
    draw.rectangle((x1, y1, x2, y1), fill=hi)
    draw.rectangle((x1, y2, x2, y2), fill=sh)


def paint_speeder_bike_item_texture():
    """16x16 atlas: orange body, black seat, silver vane, brown rail regions."""
    img = Image.new('RGBA', (VEHICLE_TEXTURE_SIZE, VEHICLE_TEXTURE_SIZE), TRANSPARENT)
    draw = ImageDraw.Draw(img)
    _region3(draw, (0, 0, 7, 7), (0xC2, 0x6A, 0x2E, 0xFF),
             (0xE0, 0x8A, 0x44, 0xFF), (0x8A, 0x46, 0x1E, 0xFF))   # body
    draw.rectangle((2, 5, 5, 5), fill=(0x6E, 0x40, 0x24, 0xFF))    # rust fleck
    _region3(draw, (8, 0, 11, 3), (0x1E, 0x1C, 0x1A, 0xFF),
             (0x3A, 0x36, 0x32, 0xFF), (0x10, 0x0E, 0x0C, 0xFF))   # seat
    _region3(draw, (8, 4, 11, 7), (0xB4, 0xB8, 0xC0, 0xFF),
             (0xD2, 0xD6, 0xDE, 0xFF), (0x82, 0x86, 0x90, 0xFF))   # vane
    _region3(draw, (12, 0, 15, 3), (0x5A, 0x3A, 0x22, 0xFF),
             (0x74, 0x4E, 0x30, 0xFF), (0x33, 0x24, 0x16, 0xFF))   # rail
    return img


def paint_xwing_item_texture():
    """16x16 atlas: off-white hull, white wing w/ a red squadron stripe, dark
    canopy glass."""
    img = Image.new('RGBA', (VEHICLE_TEXTURE_SIZE, VEHICLE_TEXTURE_SIZE), TRANSPARENT)
    draw = ImageDraw.Draw(img)
    _region3(draw, (0, 0, 7, 7), (0xE6, 0xE4, 0xDC, 0xFF),
             (0xF6, 0xF4, 0xEE, 0xFF), (0xB8, 0xB6, 0xAE, 0xFF))   # hull
    draw.rectangle((3, 3, 4, 7), fill=(0x7A, 0x7C, 0x82, 0xFF))    # hull panel line
    _region3(draw, (8, 0, 15, 7), (0xE6, 0xE4, 0xDC, 0xFF),
             (0xF6, 0xF4, 0xEE, 0xFF), (0xB8, 0xB6, 0xAE, 0xFF))   # wing base
    draw.rectangle((8, 3, 15, 4), fill=(0xC2, 0x30, 0x2E, 0xFF))   # red stripe
    draw.rectangle((8, 3, 15, 3), fill=(0xE0, 0x4A, 0x44, 0xFF))
    _region3(draw, (0, 8, 3, 11), (0x22, 0x2A, 0x36, 0xFF),
             (0x4C, 0x60, 0x78, 0xFF), (0x16, 0x1C, 0x26, 0xFF))   # canopy glass
    return img


def paint_tie_fighter_item_texture():
    """16x16 atlas: gunmetal ball/pylon, black panel w/ a rib, red window."""
    img = Image.new('RGBA', (VEHICLE_TEXTURE_SIZE, VEHICLE_TEXTURE_SIZE), TRANSPARENT)
    draw = ImageDraw.Draw(img)
    _region3(draw, (0, 0, 7, 7), (0x60, 0x64, 0x6C, 0xFF),
             (0x82, 0x86, 0x90, 0xFF), (0x44, 0x47, 0x4E, 0xFF))   # metal
    _region3(draw, (8, 0, 15, 7), (0x14, 0x15, 0x18, 0xFF),
             (0x2E, 0x30, 0x36, 0xFF), (0x0A, 0x0A, 0x0C, 0xFF))   # panel
    for rx in (10, 13):
        draw.rectangle((rx, 0, rx, 7), fill=(0x2E, 0x30, 0x36, 0xFF))  # ribs
    _region3(draw, (0, 8, 3, 11), (0x9A, 0x28, 0x22, 0xFF),
             (0xC6, 0x44, 0x3A, 0xFF), (0x5E, 0x18, 0x14, 0xFF))   # window
    return img


# ─── UV helpers (mirrors gen_meteor_staff.py's uv_for / uv_for_model_json) ─
def uv_for(uv_regions, group):
    x1, y1, x2, y2 = uv_regions[group]
    return [x1, y1, x2 + 1, y2 + 1]


def uv_for_model_json(uv_regions, group, texture_size):
    scale = 16.0 / texture_size
    return [v * scale for v in uv_for(uv_regions, group)]


# ─── Model / bbmodel builders ────────────────────────────────────────────
# Cube tuples are (name, from, to, group) or, for the Task-19 vehicle item
# models, (name, from, to, group, rotation) where rotation is a dict
# {'angle': deg, 'axis': 'x'|'y'|'z', 'origin': [x,y,z]} (the X-wing S-foils
# need a 45 deg tilt to read as an X). The rotation key is emitted only when
# present, so every pre-existing weapon's model/bbmodel stays byte-identical.
def _rot_vec(rotation):
    """Single-axis {'angle','axis'} -> a Blockbench [x,y,z]-euler vector."""
    vec = [0, 0, 0]
    vec[{'x': 0, 'y': 1, 'z': 2}[rotation['axis']]] = rotation['angle']
    return vec


def build_model_json(name, cubes, uv_regions, texture_size, texture_path,
                     display=None):
    elements = []
    for cube in cubes:
        cube_name, frm, to, group = cube[0], cube[1], cube[2], cube[3]
        rotation = cube[4] if len(cube) > 4 else None
        uv = uv_for_model_json(uv_regions, group, texture_size)
        element = {
            "name": cube_name,
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
        }
        if rotation:
            element["rotation"] = {
                "origin": list(rotation["origin"]),
                "axis": rotation["axis"],
                "angle": rotation["angle"],
            }
        elements.append(element)
    return {
        "credit": "Generated by starwars/tools/gen_weapon_models.py",
        "texture_size": [texture_size, texture_size],
        "textures": {
            "0": texture_path,
            "particle": texture_path,
        },
        "elements": elements,
        "display": dict(display if display is not None else DISPLAY),
    }


def png_to_data_url(img):
    import io
    buf = io.BytesIO()
    img.save(buf, format='PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode('ascii')


def build_bbmodel(name, cubes, uv_regions, texture_img, texture_relpath, texture_size,
                  model_identifier, display=None):
    elements = []
    outliner = []
    for cube in cubes:
        cube_name, frm, to, group = cube[0], cube[1], cube[2], cube[3]
        rotation = cube[4] if len(cube) > 4 else None
        eid = det_uuid(f'{name}/element/{cube_name}')
        uv = uv_for(uv_regions, group)
        element = {
            "name": cube_name,
            "rescale": False,
            "locked": False,
            "from": list(frm),
            "to": list(to),
            "autouv": 0,
            "color": 0,
            "origin": list(rotation["origin"]) if rotation else [0, 0, 0],
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
        }
        if rotation:
            element["rotation"] = _rot_vec(rotation)
        elements.append(element)
        outliner.append(eid)

    texture = {
        "path": "",
        "name": f"{name}.png",
        "folder": "item",
        "namespace": "starwars",
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
        "uuid": det_uuid(f'{name}/texture'),
        "relative_path": texture_relpath,
        "use_as_default": True,
        "layers_enabled": False,
        "sync_to_project": "",
        "width": texture_size,
        "height": texture_size,
        "uv_width": texture_size,
        "uv_height": texture_size,
        "source": png_to_data_url(texture_img),
    }

    return {
        "meta": {
            "format_version": "4.5",
            "model_format": "java_block",
            "box_uv": False,
        },
        "name": name,
        "model_identifier": model_identifier,
        "visible_box": [2, 2, 0],
        "variable_placeholders": "",
        "variable_placeholder_buttons": [],
        "unhandled_root_fields": {},
        "resolution": {"width": texture_size, "height": texture_size},
        "elements": elements,
        "outliner": outliner,
        "textures": [texture],
        "display": dict(display if display is not None else DISPLAY),
    }


def build_lightsaber_item_json():
    """`minecraft:select` on `minecraft:component` (confirmed against the
    vanilla 26.1.2 client jar's ComponentContents select property — see
    net/minecraft/client/renderer/item/properties/select/ComponentContents.java
    in minecraft-patched-26.1.2.30-beta-sources.jar). The "component" field
    names the DataComponentType id; "when" values are decoded with that
    component's own codec (Codec.INT for BLADE_COLOR), so plain integers
    work directly as case keys.
    """
    return {
        "model": {
            "type": "minecraft:select",
            "property": "minecraft:component",
            "component": "starwars:blade_color",
            "cases": [
                {"when": 0, "model": {"type": "minecraft:model", "model": "starwars:item/lightsaber_blue"}},
                {"when": 1, "model": {"type": "minecraft:model", "model": "starwars:item/lightsaber_green"}},
                {"when": 2, "model": {"type": "minecraft:model", "model": "starwars:item/lightsaber_red"}},
                {"when": 3, "model": {"type": "minecraft:model", "model": "starwars:item/lightsaber_purple"}},
            ],
            "fallback": {"type": "minecraft:model", "model": "starwars:item/lightsaber_blue"},
        }
    }


def build_holocron_item_json():
    """Plain model selector — the holocron has no per-instance visual variant."""
    return {
        "model": {
            "type": "minecraft:model",
            "model": "starwars:item/holocron",
        }
    }


def build_vehicle_item_json(vehicle_id):
    """Plain model selector for a vehicle item — identical shape to the
    landspeeder's committed items/landspeeder.json (a static item, no
    per-instance variant)."""
    return {
        "model": {
            "type": "minecraft:model",
            "model": f"starwars:item/{vehicle_id}",
        }
    }


def write_json(path, data):
    with open(path, 'w') as f:
        json.dump(data, f, indent=2)
        f.write('\n')
    print(f"  wrote {path}")


def main():
    arg = sys.argv[1] if len(sys.argv) > 1 else None
    tools_dir, assets_dir = resolve_paths(arg)

    textures_dir = os.path.join(assets_dir, 'textures/item')
    models_dir = os.path.join(assets_dir, 'models/item')
    items_dir = os.path.join(assets_dir, 'items')
    os.makedirs(textures_dir, exist_ok=True)
    os.makedirs(models_dir, exist_ok=True)
    os.makedirs(items_dir, exist_ok=True)
    os.makedirs(tools_dir, exist_ok=True)

    # ── Lightsaber: one texture + model per color, one shared bbmodel. ──
    for suffix, rgb in SABER_COLORS:
        tex = paint_saber_texture(rgb)
        tex_path = os.path.join(textures_dir, f'lightsaber_{suffix}.png')
        tex.save(tex_path)
        print(f"  wrote {tex_path}")

        model = build_model_json(
            f'lightsaber_{suffix}', LIGHTSABER_CUBES, UV_REGIONS_SABER,
            SABER_TEXTURE_SIZE, f'starwars:item/lightsaber_{suffix}')
        write_json(os.path.join(models_dir, f'lightsaber_{suffix}.json'), model)

    # bbmodel source uses the BLUE texture as the representative color —
    # geometry is shared across all four; recolor the texture in Blockbench
    # (or swap to another lightsaber_<color>.png) to preview other blades.
    blue_tex = paint_saber_texture(SABER_COLORS[0][1])
    bbmodel = build_bbmodel(
        'lightsaber', LIGHTSABER_CUBES, UV_REGIONS_SABER, blue_tex,
        '../src/main/resources/assets/starwars/textures/item/lightsaber_blue.png',
        SABER_TEXTURE_SIZE, 'starwars:lightsaber')
    write_json(os.path.join(tools_dir, 'lightsaber.bbmodel'), bbmodel)

    write_json(os.path.join(items_dir, 'lightsaber.json'), build_lightsaber_item_json())

    # ── Blaster pistol ──
    pistol_tex = paint_blaster_texture(include_wood=False)
    pistol_tex_path = os.path.join(textures_dir, 'blaster_pistol.png')
    pistol_tex.save(pistol_tex_path)
    print(f"  wrote {pistol_tex_path}")

    pistol_model = build_model_json(
        'blaster_pistol', BLASTER_PISTOL_CUBES, UV_REGIONS_BLASTER,
        BLASTER_TEXTURE_SIZE, 'starwars:item/blaster_pistol')
    write_json(os.path.join(models_dir, 'blaster_pistol.json'), pistol_model)

    pistol_bbmodel = build_bbmodel(
        'blaster_pistol', BLASTER_PISTOL_CUBES, UV_REGIONS_BLASTER, pistol_tex,
        '../src/main/resources/assets/starwars/textures/item/blaster_pistol.png',
        BLASTER_TEXTURE_SIZE, 'starwars:blaster_pistol')
    write_json(os.path.join(tools_dir, 'blaster_pistol.bbmodel'), pistol_bbmodel)

    # ── Blaster rifle ──
    rifle_tex = paint_blaster_texture(include_wood=True)
    rifle_tex_path = os.path.join(textures_dir, 'blaster_rifle.png')
    rifle_tex.save(rifle_tex_path)
    print(f"  wrote {rifle_tex_path}")

    rifle_model = build_model_json(
        'blaster_rifle', BLASTER_RIFLE_CUBES, UV_REGIONS_BLASTER,
        BLASTER_TEXTURE_SIZE, 'starwars:item/blaster_rifle')
    write_json(os.path.join(models_dir, 'blaster_rifle.json'), rifle_model)

    rifle_bbmodel = build_bbmodel(
        'blaster_rifle', BLASTER_RIFLE_CUBES, UV_REGIONS_BLASTER, rifle_tex,
        '../src/main/resources/assets/starwars/textures/item/blaster_rifle.png',
        BLASTER_TEXTURE_SIZE, 'starwars:blaster_rifle')
    write_json(os.path.join(tools_dir, 'blaster_rifle.bbmodel'), rifle_bbmodel)

    # ── Holocron ──
    holocron_tex = paint_holocron_texture()
    holocron_tex_path = os.path.join(textures_dir, 'holocron.png')
    holocron_tex.save(holocron_tex_path)
    print(f"  wrote {holocron_tex_path}")

    holocron_model = build_model_json(
        'holocron', HOLOCRON_CUBES, UV_REGIONS_HOLOCRON,
        HOLOCRON_TEXTURE_SIZE, 'starwars:item/holocron')
    write_json(os.path.join(models_dir, 'holocron.json'), holocron_model)

    holocron_bbmodel = build_bbmodel(
        'holocron', HOLOCRON_CUBES, UV_REGIONS_HOLOCRON, holocron_tex,
        '../src/main/resources/assets/starwars/textures/item/holocron.png',
        HOLOCRON_TEXTURE_SIZE, 'starwars:holocron')
    write_json(os.path.join(tools_dir, 'holocron.bbmodel'), holocron_bbmodel)

    write_json(os.path.join(items_dir, 'holocron.json'), build_holocron_item_json())

    # ── Vehicle item models (voxel mini-silhouettes) ──
    # Mirrors the landspeeder item file set (items/<id>.json + models/item/
    # <id>.json + textures/item/<id>.png) but as a 3D voxel model. Each also
    # gets an editable tools/<id>_item.bbmodel source — the `_item` suffix
    # keeps it distinct from the entity rig's tools/<id>.bbmodel (gen_bbmodels).
    vehicles = [
        ('speeder_bike', SPEEDER_BIKE_ITEM_CUBES, UV_REGIONS_SPEEDER_BIKE,
         paint_speeder_bike_item_texture(), None),
        ('xwing', XWING_ITEM_CUBES, UV_REGIONS_XWING,
         paint_xwing_item_texture(), XWING_ITEM_DISPLAY),
        ('tie_fighter', TIE_FIGHTER_ITEM_CUBES, UV_REGIONS_TIE_FIGHTER,
         paint_tie_fighter_item_texture(), None),
    ]
    for vid, cubes, uv_regions, tex, display in vehicles:
        tex_path = os.path.join(textures_dir, f'{vid}.png')
        tex.save(tex_path)
        print(f"  wrote {tex_path}")

        model = build_model_json(vid, cubes, uv_regions, VEHICLE_TEXTURE_SIZE,
                                 f'starwars:item/{vid}', display=display)
        write_json(os.path.join(models_dir, f'{vid}.json'), model)

        bbmodel = build_bbmodel(
            vid, cubes, uv_regions, tex,
            f'../src/main/resources/assets/starwars/textures/item/{vid}.png',
            VEHICLE_TEXTURE_SIZE, f'starwars:{vid}', display=display)
        write_json(os.path.join(tools_dir, f'{vid}_item.bbmodel'), bbmodel)

        write_json(os.path.join(items_dir, f'{vid}.json'), build_vehicle_item_json(vid))

    print('OK')


if __name__ == '__main__':
    main()
