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
