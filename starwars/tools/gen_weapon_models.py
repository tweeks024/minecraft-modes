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


# ─── UV helpers (mirrors gen_meteor_staff.py's uv_for / uv_for_model_json) ─
def uv_for(uv_regions, group):
    x1, y1, x2, y2 = uv_regions[group]
    return [x1, y1, x2 + 1, y2 + 1]


def uv_for_model_json(uv_regions, group, texture_size):
    scale = 16.0 / texture_size
    return [v * scale for v in uv_for(uv_regions, group)]


# ─── Model / bbmodel builders ────────────────────────────────────────────
def build_model_json(name, cubes, uv_regions, texture_size, texture_path):
    elements = []
    for cube_name, frm, to, group in cubes:
        uv = uv_for_model_json(uv_regions, group, texture_size)
        elements.append({
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
        })
    return {
        "credit": "Generated by starwars/tools/gen_weapon_models.py",
        "texture_size": [texture_size, texture_size],
        "textures": {
            "0": texture_path,
            "particle": texture_path,
        },
        "elements": elements,
        "display": dict(DISPLAY),
    }


def png_to_data_url(img):
    import io
    buf = io.BytesIO()
    img.save(buf, format='PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode('ascii')


def build_bbmodel(name, cubes, uv_regions, texture_img, texture_relpath, texture_size, model_identifier):
    elements = []
    outliner = []
    for cube_name, frm, to, group in cubes:
        eid = det_uuid(f'{name}/element/{cube_name}')
        uv = uv_for(uv_regions, group)
        elements.append({
            "name": cube_name,
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
        "display": dict(DISPLAY),
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

    print('OK')


if __name__ == '__main__':
    main()
