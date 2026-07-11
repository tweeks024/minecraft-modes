#!/usr/bin/env python3
"""Generate Blockbench .bbmodel source files for the starwars mobs.

A .bbmodel is JSON. We programmatically build the modded_entity model with
the standard humanoid skeleton (head, body, arms, legs) plus per-mob
accessory cubes, or — for mobs with a fully custom skeleton — a cube table
that replaces the humanoid cubes outright. The generated .bbmodel files are
valid Blockbench projects — open them in Blockbench (File -> Open) for
visual editing.

Coord system: Blockbench's modded_entity uses world coords where y=0 is
feet and y=32 is top of head. The conversion from Java HumanoidModel
addBox() coords is:
    bbmodel_from = (bone_x + jx, bone_y - (jy + jh), bone_z + jz)
    bbmodel_to   = (bone_x + jx + jw, bone_y - jy, bone_z + jz + jd)
(Y is inverted because HumanoidModel measures cube y from bone-pivot-down.)

Re-run after editing the script with:
    python3 starwars/tools/gen_bbmodels.py starwars/tools/
"""
import base64
import json
import os
import sys
import uuid

# Deterministic UUIDs so re-runs produce byte-identical output.
NAMESPACE = uuid.UUID('00000000-0000-0000-0000-000000000001')

def det_uuid(name):
    return str(uuid.uuid5(NAMESPACE, name))

# Bones for a vanilla HumanoidModel. (origin = bone pivot in world coords)
HEAD_BONE  = (0,    24, 0)
BODY_BONE  = (0,    24, 0)
RARM_BONE  = (-5,   22, 0)
LARM_BONE  = (5,    22, 0)
RLEG_BONE  = (-1.9, 12, 0)
LLEG_BONE  = (1.9,  12, 0)

# Standard humanoid cubes: (name, parent_bone, java_addBox_args, uv_offset)
# java_addBox_args = (jx, jy, jz, jw, jh, jd)
HUMANOID_CUBES = [
    ('head',      HEAD_BONE,  (-4, -8, -4, 8, 8, 8),  (0, 0)),
    ('body',      BODY_BONE,  (-4,  0, -2, 8, 12, 4), (16, 16)),
    ('right_arm', RARM_BONE,  (-3, -2, -2, 4, 12, 4), (40, 16)),
    ('left_arm',  LARM_BONE,  (-1, -2, -2, 4, 12, 4), (32, 48)),
    ('right_leg', RLEG_BONE,  (-2,  0, -2, 4, 12, 4), (0, 16)),
    ('left_leg',  LLEG_BONE,  (-2,  0, -2, 4, 12, 4), (16, 48)),
]

# Per-mob accessory cubes. Each entry is the same shape as HUMANOID_CUBES,
# optionally with a 5th "inflate" element (default 0.0).
STORMTROOPER_ACCESSORIES = [
    # (name, parent_bone, (jx, jy, jz, jw, jh, jd), uv, inflate)
    ('helmet_shell', HEAD_BONE, (-4.0, -8.0, -4.0, 8, 8, 8), (32, 0), 0.6),
    ('chin_vent',    HEAD_BONE, (-1.5, -1.5, -5.0, 3, 2, 1), (56, 16), 0.0),
]

# Battle droid REPLACES the humanoid cubes (custom skeleton).
BATTLE_DROID_CUBES = [
    ('head',      HEAD_BONE, (-2.0, -6.0, -2.0, 4, 6, 4),  (0, 0)),
    ('snout',     HEAD_BONE, (-1.0, -3.0, -5.0, 2, 2, 3),  (16, 0)),
    ('body',      BODY_BONE, (-3.0,  0.0, -1.5, 6, 10, 3), (0, 16)),
    ('hip_block', BODY_BONE, (-1.0, 10.0, -1.0, 2, 2, 2),  (18, 16)),
    ('right_arm', RARM_BONE, (-1.0, -1.0, -1.0, 2, 12, 2), (32, 16)),
    ('left_arm',  LARM_BONE, (-1.0, -1.0, -1.0, 2, 12, 2), (40, 16)),
    ('right_leg', RLEG_BONE, (-1.0,  0.0, -1.0, 2, 12, 2), (48, 16)),
    ('left_leg',  LLEG_BONE, (-1.0,  0.0, -1.0, 2, 12, 2), (56, 16)),
]

JEDI_KNIGHT_ACCESSORIES = [
    ('robe_skirt', BODY_BONE, (-4.5, 12.0, -2.5, 9, 7, 5), (32, 32), 0.0),
    ('hood',       HEAD_BONE, (-4.0, -8.0, -4.0, 8, 8, 8), (32, 0), 0.5),
]

DARTH_VADER_ACCESSORIES = [
    ('helmet_dome',  HEAD_BONE, (-4.0, -8.0, -4.0, 8, 8, 8),  (32, 0), 0.7),
    ('helmet_flare', HEAD_BONE, (-5.0, -2.0, -5.0, 10, 2, 10),(32, 16), 0.0),
    ('cape',         BODY_BONE, (-4.5,  0.0,  2.1, 9, 20, 1), (44, 32), 0.0),
    ('chest_panel',  BODY_BONE, (-2.0,  3.0, -2.6, 4, 3, 1),  (56, 54), 0.0),
]

BOBA_FETT_ACCESSORIES = [
    ('helmet_shell', HEAD_BONE, (-4.0, -8.0, -4.0, 8, 8, 8),  (32, 0), 0.6),
    ('rangefinder',  HEAD_BONE, (3.8, -12.0, -0.5, 1, 4, 1),  (56, 16), 0.0),
    ('jetpack',      BODY_BONE, (-3.0,  0.5,  2.1, 6, 8, 3),  (44, 32), 0.0),
]

# Astromech droid: fully custom skeleton (body/head/legs only, no arms) at
# its own pivots — the exact PartPose.offset values from AstromechModel.java
# Step 3 — rather than the standard humanoid bone table above.
ASTRO_BODY_BONE = (0, 24, 0)
ASTRO_HEAD_BONE = (0, 10, 0)
ASTRO_RLEG_BONE = (-5, 12, 0)
ASTRO_LLEG_BONE = (5, 12, 0)

# (bone_name, origin) pairs, in outliner order — passed as build_bbmodel's
# bone_defs override since astromech's bone set/pivots don't match the
# standard humanoid HEAD_BONE/BODY_BONE/.../LLEG_BONE table above.
ASTROMECH_BONE_DEFS = [
    ('body', ASTRO_BODY_BONE),
    ('head', ASTRO_HEAD_BONE),
    ('right_leg', ASTRO_RLEG_BONE),
    ('left_leg', ASTRO_LLEG_BONE),
]

ASTROMECH_CUBES = [
    ('body',      ASTRO_BODY_BONE, (-4.0, -14.0, -4.0, 8, 10, 8), (0, 20)),
    ('head',      ASTRO_HEAD_BONE, (-4.0,  -4.0, -4.0, 8, 4, 8),  (0, 0)),
    ('eye_lens',  ASTRO_HEAD_BONE, (-1.5,  -3.0, -4.5, 3, 2, 1),  (32, 0)),
    ('right_leg', ASTRO_RLEG_BONE, (-1.0,   0.0, -1.5, 2, 12, 3), (32, 20)),
    ('left_leg',  ASTRO_LLEG_BONE, (-1.0,   0.0, -1.5, 2, 12, 3), (42, 20)),
]

# mob_name -> bone_defs override (only needed for mobs whose bone set/pivots
# aren't the standard humanoid table).
MOB_BONE_DEFS = {
    'astromech': ASTROMECH_BONE_DEFS,
}

MOBS = {
    'stormtrooper': HUMANOID_CUBES + STORMTROOPER_ACCESSORIES,
    'battle_droid': BATTLE_DROID_CUBES,
    'jedi_knight': HUMANOID_CUBES + JEDI_KNIGHT_ACCESSORIES,
    'darth_vader': HUMANOID_CUBES + DARTH_VADER_ACCESSORIES,
    # Plain humanoid — no accessory cubes. Luke's black tunic, blond hair,
    # and glove stripe are painted directly onto the standard UV layout.
    'luke_skywalker': HUMANOID_CUBES,
    # Obi-Wan reuses the Jedi Knight's robe_skirt + hood accessory geometry
    # exactly — only the paint differs.
    'obi_wan': HUMANOID_CUBES + JEDI_KNIGHT_ACCESSORIES,
    # Boba Fett: helmet shell (stormtrooper-style inflated head box),
    # rangefinder stalk, and a back-mounted jetpack.
    'boba_fett': HUMANOID_CUBES + BOBA_FETT_ACCESSORIES,
    # Astromech: fully custom skeleton (see ASTROMECH_BONE_DEFS above) —
    # body/head/legs only, no arms.
    'astromech': ASTROMECH_CUBES,
}


def java_to_bbmodel(bone, java):
    """Convert Java HumanoidModel addBox args to bbmodel from/to world coords."""
    bx, by, bz = bone
    jx, jy, jz, jw, jh, jd = java
    from_pt = [bx + jx, by - (jy + jh), bz + jz]
    to_pt   = [bx + jx + jw, by - jy, bz + jz + jd]
    return from_pt, to_pt


def make_cube(name, mob_name, bone, java, uv_offset, inflate=0.0):
    """Build the bbmodel `elements` entry for one cube."""
    from_pt, to_pt = java_to_bbmodel(bone, java)
    cube = {
        "name": name,
        "rescale": False,
        "locked": False,
        "mirror_uv": False,
        "from": from_pt,
        "to": to_pt,
        "autouv": 0,
        "color": 0,
        "origin": list(bone),
        "uv_offset": list(uv_offset),
        "type": "cube",
        "uuid": det_uuid(f"{mob_name}/cube/{name}"),
    }
    if inflate:
        cube["inflate"] = inflate
    return cube


def make_bone_group(mob_name, name, origin, child_uuids):
    """Build the outliner entry for one bone (head/body/arm/leg/etc)."""
    return {
        "name": name,
        "origin": list(origin),
        "color": 0,
        "uuid": det_uuid(f"{mob_name}/bone/{name}"),
        "export": True,
        "isOpen": True,
        "locked": False,
        "visibility": True,
        "autouv": 0,
        "children": child_uuids,
    }


# Default (standard humanoid) bone table, as (bone_name, origin) pairs in
# outliner order — same order as the original hardcoded if/elif chain this
# replaces, so lookups for the existing mobs are byte-for-byte unchanged
# (including that HEAD_BONE and BODY_BONE share the same (0, 24, 0) value:
# 'head' is checked first and wins the match for both, exactly as before).
DEFAULT_BONE_DEFS = [
    ('head', HEAD_BONE),
    ('body', BODY_BONE),
    ('right_arm', RARM_BONE),
    ('left_arm', LARM_BONE),
    ('right_leg', RLEG_BONE),
    ('left_leg', LLEG_BONE),
]


def _parent_bone_name(bone_origin, bone_defs):
    for name, origin in bone_defs:
        if bone_origin == origin:
            return name
    raise ValueError(bone_origin)


def build_bbmodel(mob_name, cubes, texture_path, tex_height=64, bone_defs=None):
    """Return the full bbmodel dict for one mob.

    `cubes` is a flat list of (name, bone, java, uv[, inflate]) tuples —
    the full cube table for the mob (humanoid cubes + accessories, or a
    fully custom skeleton). `bone_defs` is an optional (name, origin) list
    overriding the standard humanoid 6-bone table — required for mobs whose
    skeleton doesn't match it (see ASTROMECH_BONE_DEFS).
    """
    if bone_defs is None:
        bone_defs = DEFAULT_BONE_DEFS

    elements = []
    bone_children = {name: [] for name, _ in bone_defs}

    for entry in cubes:
        if len(entry) == 5:
            name, bone, java, uv, inflate = entry
        else:
            name, bone, java, uv = entry
            inflate = 0.0
        cube = make_cube(name, mob_name, bone, java, uv, inflate)
        elements.append(cube)
        bone_children[_parent_bone_name(bone, bone_defs)].append(cube["uuid"])

    outliner = [
        make_bone_group(mob_name, name, origin, bone_children[name])
        for name, origin in bone_defs
    ]

    # Embed the texture as base64.
    tex_data = ''
    if os.path.exists(texture_path):
        with open(texture_path, 'rb') as f:
            tex_data = 'data:image/png;base64,' + base64.b64encode(f.read()).decode('ascii')

    texture = {
        "path": os.path.abspath(texture_path),
        "name": f"{mob_name}.png",
        "folder": "entity",
        "namespace": "starwars",
        "id": "0",
        "particle": False,
        "render_mode": "default",
        "render_sides": "auto",
        "frame_time": 1,
        "frame_order_type": "loop",
        "frame_order": "",
        "frame_interpolate": False,
        "visible": True,
        "internal": True,
        "saved": True,
        "uuid": det_uuid(f"{mob_name}/texture"),
        "relative_path": f"../src/main/resources/assets/starwars/textures/entity/{mob_name}.png",
        "use_as_default": False,
        "layers_enabled": False,
        "sync_to_project": "",
        "width": 64,
        "height": tex_height,
        "uv_width": 64,
        "uv_height": tex_height,
        "source": tex_data,
    }

    return {
        "meta": {
            "format_version": "4.5",
            "model_format": "modded_entity",
            "box_uv": True,
        },
        "name": mob_name,
        "model_identifier": "",
        "visible_box": [4, 4, 0],
        "variable_placeholders": "",
        "variable_placeholder_buttons": [],
        "unhandled_root_fields": {},
        "resolution": {"width": 64, "height": tex_height},
        "elements": elements,
        "outliner": outliner,
        "textures": [texture],
    }


def write_bbmodel(out_dir, mob_name, cubes, texture_dir, tex_height=64, bone_defs=None):
    texture_path = os.path.join(texture_dir, f"{mob_name}.png")
    bbmodel = build_bbmodel(mob_name, cubes, texture_path, tex_height, bone_defs=bone_defs)
    out_path = os.path.join(out_dir, f"{mob_name}.bbmodel")
    with open(out_path, 'w') as f:
        json.dump(bbmodel, f, indent=2)
        f.write('\n')
    print(f"  wrote {out_path}")


# -----------------------------------------------------------------------------
# Armor bbmodels — standard armor-overlay geometry (helmet/chestplate/
# leggings/boots), one bbmodel per piece, following the repo convention set
# by craftee/tools/craftee_armor_{helmet,chestplate,leggings,boots}.bbmodel:
# format_version "5.0" (not "4.5"), explicit modded_entity_* root fields,
# per-face "faces" UV (not the simplified "uv_offset" the mob cubes above
# use), a flat outliner (no bone groups — armor cubes have no parent bone
# hierarchy), and a 64x32 resolution (the worn-armor equipment sheet size,
# not the mob's 64x64 skin). Cube from/to sizes match the vanilla humanoid
# armor pivots exactly (same coords craftee's own generator uses); unlike
# craftee, each cube also carries the standard vanilla ArmorModel "inflate"
# value for its layer (helmet/chestplate/boots outer layer ~1.0, leggings
# inner layer ~0.5) so the preview matches how the game actually renders
# worn armor.
# -----------------------------------------------------------------------------

HEAD_ARMOR_ORIGIN  = [0, 24, 0]
BODY_ARMOR_ORIGIN  = [0, 24, 0]
RARM_ARMOR_ORIGIN  = [-5, 22, 0]
LARM_ARMOR_ORIGIN  = [5, 22, 0]
RLEG_ARMOR_ORIGIN  = [-1.9, 12, 0]
LLEG_ARMOR_ORIGIN  = [1.9, 12, 0]

# (name, from, to, origin, uv_offset, inflate)
STORMTROOPER_HELMET_CUBES = [
    ("head", [-4, 24, -4], [4, 32, 4], HEAD_ARMOR_ORIGIN, (0, 0), 1.0),
]

STORMTROOPER_CHESTPLATE_CUBES = [
    ("body",      [-4, 12, -2], [4, 24, 2],  BODY_ARMOR_ORIGIN, (16, 16), 1.01),
    ("right_arm", [-8, 12, -2], [-4, 24, 2], RARM_ARMOR_ORIGIN, (40, 16), 1.0),
    ("left_arm",  [4, 12, -2],  [8, 24, 2],  LARM_ARMOR_ORIGIN, (40, 16), 1.0),
]

# Leggings cover both the waist (body cube) and both legs, per vanilla
# ArmorModel LEGGINGS geometry — unlike craftee's own leggings bbmodel
# (legs only), the task brief calls for the body cube too.
STORMTROOPER_LEGGINGS_CUBES = [
    ("body",      [-4, 12, -2],   [4, 24, 2],   BODY_ARMOR_ORIGIN, (16, 16), 0.51),
    ("right_leg", [-3.9, 0, -2],  [0.1, 12, 2], RLEG_ARMOR_ORIGIN, (0, 16),  0.5),
    ("left_leg",  [-0.1, 0, -2],  [3.9, 12, 2], LLEG_ARMOR_ORIGIN, (0, 16),  0.5),
]

STORMTROOPER_BOOTS_CUBES = [
    ("right_leg", [-3.9, 0, -2], [0.1, 12, 2], RLEG_ARMOR_ORIGIN, (0, 16), 1.0),
    ("left_leg",  [-0.1, 0, -2], [3.9, 12, 2], LLEG_ARMOR_ORIGIN, (0, 16), 1.0),
]

# piece_name -> (cubes, equipment texture subfolder)
ARMOR_PIECES = {
    'stormtrooper_armor_helmet':     (STORMTROOPER_HELMET_CUBES, 'humanoid'),
    'stormtrooper_armor_chestplate': (STORMTROOPER_CHESTPLATE_CUBES, 'humanoid'),
    'stormtrooper_armor_leggings':   (STORMTROOPER_LEGGINGS_CUBES, 'humanoid_leggings'),
    'stormtrooper_armor_boots':      (STORMTROOPER_BOOTS_CUBES, 'humanoid'),
}


def armor_box_faces(size_w, size_h, size_d, uv_u, uv_v):
    """Box-UV 6-face map for a cube of size (W, H, D) at uv_offset (U, V).
    Mirrors craftee/tools/generate_bbmodels.py's box_faces()."""
    u, v, w, h, d = uv_u, uv_v, size_w, size_h, size_d
    return {
        "north": {"uv": [u + d,         v + d, u + d + w,         v + d + h], "texture": 0},
        "east":  {"uv": [u,             v + d, u + d,             v + d + h], "texture": 0},
        "south": {"uv": [u + d + w + d, v + d, u + d + w + d + w, v + d + h], "texture": 0},
        "west":  {"uv": [u + d + w,     v + d, u + d + w + d,     v + d + h], "texture": 0},
        "up":    {"uv": [u + d + w,     v + d, u + d,             v],         "texture": 0},
        "down":  {"uv": [u + d + w + w, v + d, u + d + w,         v],         "texture": 0},
    }


def make_armor_cube(piece_name, name, fr, to, origin, uv_offset, inflate=0.0):
    w = to[0] - fr[0]
    h = to[1] - fr[1]
    d = to[2] - fr[2]
    cube = {
        "name": name,
        "from": fr,
        "to": to,
        "origin": origin,
        "autouv": 0,
        "box_uv": True,
        "uv_offset": list(uv_offset),
        "rotation": None,
        "rescale": None,
        "locked": False,
        "render_order": "default",
        "export": True,
        "scope": 0,
        "allow_mirror_modeling": True,
        "color": 0,
        "type": "cube",
        "uuid": det_uuid(f"{piece_name}/cube/{name}"),
        "faces": armor_box_faces(w, h, d, *uv_offset),
    }
    if inflate:
        cube["inflate"] = inflate
    return cube


def armor_texture_record(piece_name, texture_path, folder):
    tex_data = ''
    if os.path.exists(texture_path):
        with open(texture_path, 'rb') as f:
            tex_data = 'data:image/png;base64,' + base64.b64encode(f.read()).decode('ascii')
    rel_path = os.path.relpath(texture_path, start=os.path.dirname(os.path.abspath(__file__)))
    return {
        "path": None,
        "name": "stormtrooper.png",
        "folder": folder,
        "namespace": "starwars",
        "id": "0",
        "particle": False,
        "render_mode": "default",
        "render_sides": "auto",
        "frame_time": 1,
        "frame_order_type": "loop",
        "frame_order": "",
        "frame_interpolate": False,
        "visible": True,
        "internal": True,
        "saved": True,
        "uuid": det_uuid(f"{piece_name}/texture"),
        "relative_path": rel_path.replace("\\", "/"),
        "use_as_default": False,
        "layers_enabled": False,
        "sync_to_project": "",
        "width": 64,
        "height": 32,
        "uv_width": 64,
        "uv_height": 32,
        "source": tex_data,
    }


def build_armor_bbmodel(piece_name, cube_specs, texture_path, folder):
    elements = [make_armor_cube(piece_name, *spec) for spec in cube_specs]
    outliner = [e["uuid"] for e in elements]
    return {
        "meta": {
            "format_version": "5.0",
            "model_format": "modded_entity",
            "box_uv": True,
        },
        "name": piece_name,
        "model_identifier": "",
        "modded_entity_entity_class": "",
        "modded_entity_version": "1.21",
        "modded_entity_flip_y": True,
        "visible_box": [1, 1, 0],
        "variable_placeholders": "",
        "variable_placeholder_buttons": [],
        "timeline_setups": [],
        "unhandled_root_fields": {},
        "resolution": {"width": 64, "height": 32},
        "elements": elements,
        "groups": [],
        "outliner": outliner,
        "textures": [armor_texture_record(piece_name, texture_path, folder)],
    }


def write_armor_bbmodel(out_dir, piece_name, cube_specs, texture_dir, folder):
    texture_path = os.path.join(texture_dir, 'equipment', folder, 'stormtrooper.png')
    bbmodel = build_armor_bbmodel(piece_name, cube_specs, texture_path, folder=f"entity/equipment/{folder}")
    out_path = os.path.join(out_dir, f"{piece_name}.bbmodel")
    with open(out_path, 'w') as f:
        json.dump(bbmodel, f, indent=2)
        f.write('\n')
    print(f"  wrote {out_path}")


if __name__ == '__main__':
    out_dir = sys.argv[1] if len(sys.argv) > 1 else '.'
    # Look for textures relative to the tools/ dir.
    tools_dir = os.path.dirname(os.path.abspath(out_dir.rstrip('/')))
    texture_dir = os.path.join(tools_dir, 'src/main/resources/assets/starwars/textures/entity')
    if not os.path.isdir(texture_dir):
        # Fallback if invoked from worktree root.
        texture_dir = os.path.abspath(
            'starwars/src/main/resources/assets/starwars/textures/entity')
    print(f"Texture dir: {texture_dir}")

    for mob_name, cubes in MOBS.items():
        write_bbmodel(out_dir, mob_name, cubes, texture_dir,
                       bone_defs=MOB_BONE_DEFS.get(mob_name))

    for piece_name, (cubes, folder) in ARMOR_PIECES.items():
        write_armor_bbmodel(out_dir, piece_name, cubes, texture_dir, folder)
    print('OK')
