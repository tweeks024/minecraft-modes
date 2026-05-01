#!/usr/bin/env python3
"""Generate Blockbench .bbmodel source files for the four wildwest mobs.

A .bbmodel is JSON. We programmatically build the modded_entity model with
the standard humanoid skeleton (head, body, arms, legs) plus per-mob
accessory cubes (hat, bandanna, star, cape) that match the Java model
classes one-for-one. The generated .bbmodel files are valid Blockbench
projects — open them in Blockbench (File → Open) for visual editing.

Coord system: Blockbench's modded_entity uses world coords where y=0 is
feet and y=32 is top of head. The conversion from Java HumanoidModel
addBox() coords is:
    bbmodel_from = (bone_x + jx, bone_y - (jy + jh), bone_z + jz)
    bbmodel_to   = (bone_x + jx + jw, bone_y - jy, bone_z + jz + jd)
(Y is inverted because HumanoidModel measures cube y from bone-pivot-down.)

Re-run after editing the script with:
    python3 wildwest/tools/gen_bbmodels.py wildwest/tools/
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

# Standard humanoid cubes: (parent_bone, java_addBox_args, uv_offset)
# java_addBox_args = (jx, jy, jz, jw, jh, jd)
HUMANOID_CUBES = [
    ('head',      HEAD_BONE,  (-4, -8, -4, 8, 8, 8),  (0, 0)),
    ('body',      BODY_BONE,  (-4,  0, -2, 8, 12, 4), (16, 16)),
    ('right_arm', RARM_BONE,  (-3, -2, -2, 4, 12, 4), (40, 16)),
    ('left_arm',  LARM_BONE,  (-1, -2, -2, 4, 12, 4), (32, 48)),
    ('right_leg', RLEG_BONE,  (-2,  0, -2, 4, 12, 4), (0, 16)),
    ('left_leg',  LLEG_BONE,  (-2,  0, -2, 4, 12, 4), (16, 48)),
]

# Per-mob accessory cubes. Each entry is the same shape as HUMANOID_CUBES.
DEPUTY_ACCESSORIES = [
    ('cap_brim',  HEAD_BONE, (-4.5, -9.0, -4.5, 9, 1, 9), (32, 0)),
    ('cap_crown', HEAD_BONE, (-3.0, -11.0, -3.0, 6, 2, 6), (32, 10)),
]

SHERRIF_ACCESSORIES = [
    ('cap_brim',  HEAD_BONE, (-5.0, -9.0, -5.0, 10, 1, 10), (32, 0)),
    ('cap_crown', HEAD_BONE, (-3.5, -12.0, -3.5, 7, 3, 7), (32, 11)),
    ('star',      BODY_BONE, (-1.5,  1.0, -2.5, 3, 3, 1), (56, 0)),
]

BANDIT_ACCESSORIES = [
    ('bandanna', HEAD_BONE, (-4.5, -4.0, -4.6, 9, 4, 1), (32, 0)),
]

BANDIT_LEADER_ACCESSORIES = [
    ('cap_brim',  HEAD_BONE, (-5.5, -9.0, -5.5, 11, 1, 11), (32, 0)),
    ('cap_crown', HEAD_BONE, (-3.5, -12.0, -3.5, 7, 3, 7), (32, 12)),
    ('bandanna',  HEAD_BONE, (-4.5, -4.0, -4.6, 9, 4, 1), (48, 4)),
    ('cape',      BODY_BONE, (-4.5,  0.0,  2.1, 9, 12, 1), (40, 32)),
]


def java_to_bbmodel(bone, java):
    """Convert Java HumanoidModel addBox args to bbmodel from/to world coords."""
    bx, by, bz = bone
    jx, jy, jz, jw, jh, jd = java
    from_pt = [bx + jx, by - (jy + jh), bz + jz]
    to_pt   = [bx + jx + jw, by - jy, bz + jz + jd]
    return from_pt, to_pt


def make_cube(name, mob_name, bone, java, uv_offset):
    """Build the bbmodel `elements` entry for one cube."""
    from_pt, to_pt = java_to_bbmodel(bone, java)
    return {
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


def build_bbmodel(mob_name, accessory_cubes, texture_path):
    """Return the full bbmodel dict for one mob."""
    elements = []
    # bone_name -> [child cube UUID]
    bone_children = {
        'head':      [],
        'body':      [],
        'right_arm': [],
        'left_arm':  [],
        'right_leg': [],
        'left_leg':  [],
    }

    def parent_bone_name(bone_origin):
        if bone_origin == HEAD_BONE: return 'head'
        if bone_origin == BODY_BONE: return 'body'
        if bone_origin == RARM_BONE: return 'right_arm'
        if bone_origin == LARM_BONE: return 'left_arm'
        if bone_origin == RLEG_BONE: return 'right_leg'
        if bone_origin == LLEG_BONE: return 'left_leg'
        raise ValueError(bone_origin)

    # Standard humanoid cubes.
    for name, bone, java, uv in HUMANOID_CUBES:
        cube = make_cube(name, mob_name, bone, java, uv)
        elements.append(cube)
        bone_children[parent_bone_name(bone)].append(cube["uuid"])

    # Accessory cubes.
    for name, bone, java, uv in accessory_cubes:
        cube = make_cube(name, mob_name, bone, java, uv)
        elements.append(cube)
        bone_children[parent_bone_name(bone)].append(cube["uuid"])

    outliner = [
        make_bone_group(mob_name, 'head',      HEAD_BONE, bone_children['head']),
        make_bone_group(mob_name, 'body',      BODY_BONE, bone_children['body']),
        make_bone_group(mob_name, 'right_arm', RARM_BONE, bone_children['right_arm']),
        make_bone_group(mob_name, 'left_arm',  LARM_BONE, bone_children['left_arm']),
        make_bone_group(mob_name, 'right_leg', RLEG_BONE, bone_children['right_leg']),
        make_bone_group(mob_name, 'left_leg',  LLEG_BONE, bone_children['left_leg']),
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
        "namespace": "wildwest",
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
        "relative_path": f"../src/main/resources/assets/wildwest/textures/entity/{mob_name}.png",
        "use_as_default": False,
        "layers_enabled": False,
        "sync_to_project": "",
        "width": 64,
        "height": 64,
        "uv_width": 64,
        "uv_height": 64,
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
        "resolution": {"width": 64, "height": 64},
        "elements": elements,
        "outliner": outliner,
        "textures": [texture],
    }


def write_bbmodel(out_dir, mob_name, accessories, texture_dir):
    texture_path = os.path.join(texture_dir, f"{mob_name}.png")
    bbmodel = build_bbmodel(mob_name, accessories, texture_path)
    out_path = os.path.join(out_dir, f"{mob_name}.bbmodel")
    with open(out_path, 'w') as f:
        json.dump(bbmodel, f, indent=2)
        f.write('\n')
    print(f"  wrote {out_path}")


if __name__ == '__main__':
    out_dir = sys.argv[1] if len(sys.argv) > 1 else '.'
    # Look for textures relative to the tools/ dir.
    tools_dir = os.path.dirname(os.path.abspath(out_dir.rstrip('/')))
    texture_dir = os.path.join(tools_dir, 'src/main/resources/assets/wildwest/textures/entity')
    if not os.path.isdir(texture_dir):
        # Fallback if invoked from worktree root.
        texture_dir = os.path.abspath(
            'wildwest/src/main/resources/assets/wildwest/textures/entity')
    print(f"Texture dir: {texture_dir}")

    write_bbmodel(out_dir, 'deputy',        DEPUTY_ACCESSORIES,        texture_dir)
    write_bbmodel(out_dir, 'sherrif',       SHERRIF_ACCESSORIES,       texture_dir)
    write_bbmodel(out_dir, 'bandit',        BANDIT_ACCESSORIES,        texture_dir)
    write_bbmodel(out_dir, 'bandit_leader', BANDIT_LEADER_ACCESSORIES, texture_dir)
    print('OK')
