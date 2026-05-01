#!/usr/bin/env python3
"""
Regenerates the Blockbench .bbmodel project files for :craftee armor.

Four bbmodels are produced, one per armor item:

    craftee_armor_helmet.bbmodel      -> head cube,
                                         humanoid/craftee.png
    craftee_armor_chestplate.bbmodel  -> body + both arms,
                                         humanoid/craftee.png
    craftee_armor_boots.bbmodel       -> boot cubes (left + right),
                                         humanoid/craftee.png
    craftee_armor_leggings.bbmodel    -> leg cubes (left + right),
                                         humanoid_leggings/craftee.png

The first three all read from the SAME humanoid/craftee.png because
that PNG carries three pieces' worth of pixels (helmet UV at top-left,
chestplate UV at center, boots UV at left of leggings row). Edit any
of those three bbmodels and Blockbench saves to the same shared PNG;
the per-piece split exists for visual focus, not for separate textures.
Only the leggings bbmodel writes to a different PNG (humanoid_leggings).

Each bbmodel embeds its linked PNG as base64 so the file is portable —
Blockbench can show the texture immediately on first open. When the user
edits and saves the texture in Blockbench (right-click thumbnail -> Save),
the linked PNG file under src/main/resources/.../textures/entity/equipment/
is overwritten in place; the next gradle build picks it up automatically.

Run with: python3 craftee/tools/generate_bbmodels.py
Idempotent: re-running overwrites the bbmodels with regenerated UUIDs and
re-embedded PNG bytes (so a texture change in the PNG flows back into the
bbmodel preview after a re-run).
"""

import base64
import json
import uuid
from pathlib import Path

# -----------------------------------------------------------------------------
# Paths
# -----------------------------------------------------------------------------

ROOT = Path(__file__).resolve().parents[1]
TOOLS_DIR = ROOT / "tools"
RESOURCES = ROOT / "src/main/resources/assets/craftee"
HUMANOID_PNG = RESOURCES / "textures/entity/equipment/humanoid/craftee.png"
LEGGINGS_PNG = RESOURCES / "textures/entity/equipment/humanoid_leggings/craftee.png"

HELMET_BBMODEL     = TOOLS_DIR / "craftee_armor_helmet.bbmodel"
CHESTPLATE_BBMODEL = TOOLS_DIR / "craftee_armor_chestplate.bbmodel"
BOOTS_BBMODEL      = TOOLS_DIR / "craftee_armor_boots.bbmodel"
LEGGINGS_BBMODEL   = TOOLS_DIR / "craftee_armor_leggings.bbmodel"

# -----------------------------------------------------------------------------
# Cube layout — matches vanilla HumanoidModel armor pivots / sizes
# -----------------------------------------------------------------------------
# Tuple format: (name, from, to, origin, (uv_u, uv_v))
# Sizes:
#   head      8 x 8 x 8   uv (0, 0)   on humanoid layer
#   body      8 x 12 x 4  uv (16, 16) on humanoid layer
#   r_arm     4 x 12 x 4  uv (40, 16) on humanoid layer
#   l_arm     4 x 12 x 4  uv (40, 16) on humanoid layer (mirrored)
#   r_boot    4 x 12 x 4  uv (0, 16)  on humanoid layer  (legs region of body sheet)
#   l_boot    4 x 12 x 4  uv (0, 16)  on humanoid layer  (mirrored)
#   r_leg     4 x 12 x 4  uv (0, 16)  on leggings layer
#   l_leg     4 x 12 x 4  uv (0, 16)  on leggings layer  (mirrored)

HELMET_CUBES = [
    ("head", [-4, 24, -4], [4, 32, 4], [0, 24, 0], (0, 0)),
]

CHESTPLATE_CUBES = [
    ("body",      [-4, 12, -2], [4, 24, 2],  [0, 24, 0], (16, 16)),
    ("right_arm", [-8, 12, -2], [-4, 24, 2], [-5, 22, 0], (40, 16)),
    ("left_arm",  [4, 12, -2],  [8, 24, 2],  [5, 22, 0],  (40, 16)),
]

BOOTS_CUBES = [
    ("right_boot", [-3.9, 0, -2], [0.1, 12, 2], [-1.9, 12, 0], (0, 16)),
    ("left_boot",  [-0.1, 0, -2], [3.9, 12, 2], [1.9, 12, 0],  (0, 16)),
]

LEGGINGS_CUBES = [
    ("right_leg", [-3.9, 0, -2], [0.1, 12, 2], [-1.9, 12, 0], (0, 16)),
    ("left_leg",  [-0.1, 0, -2], [3.9, 12, 2], [1.9, 12, 0],  (0, 16)),
]

# -----------------------------------------------------------------------------
# Box-UV face computation (Blockbench / vanilla Minecraft player-skin layout)
# -----------------------------------------------------------------------------

def box_faces(size_w, size_h, size_d, uv_u, uv_v):
    """Return the 6-face UV map for a box_uv cube of size (W, H, D) at
    uv_offset (U, V). Mirrors the convention used in :creeperskin."""
    u, v, w, h, d = uv_u, uv_v, size_w, size_h, size_d
    return {
        "north": {"uv": [u + d,         v + d, u + d + w,         v + d + h], "texture": 0},
        "east":  {"uv": [u,             v + d, u + d,             v + d + h], "texture": 0},
        "south": {"uv": [u + d + w + d, v + d, u + d + w + d + w, v + d + h], "texture": 0},
        "west":  {"uv": [u + d + w,     v + d, u + d + w + d,     v + d + h], "texture": 0},
        "up":    {"uv": [u + d + w,     v + d, u + d,             v],         "texture": 0},
        "down":  {"uv": [u + d + w + w, v + d, u + d + w,         v],         "texture": 0},
    }


def make_cube(name, fr, to, origin, uv_offset):
    w, h, d = to[0] - fr[0], to[1] - fr[1], to[2] - fr[2]
    return {
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
        "uuid": str(uuid.uuid4()),
        "faces": box_faces(w, h, d, *uv_offset),
    }


# -----------------------------------------------------------------------------
# Texture record
# -----------------------------------------------------------------------------

def png_data_url(path: Path) -> str:
    with open(path, "rb") as f:
        return "data:image/png;base64," + base64.b64encode(f.read()).decode("ascii")


def texture_record(png_path: Path, folder: str) -> dict:
    rel = "../" + str(png_path.relative_to(ROOT)).replace("\\", "/")
    return {
        "path": None,
        "name": png_path.name,
        "folder": folder,
        "namespace": "craftee",
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
        "uuid": str(uuid.uuid4()),
        "relative_path": rel,
        "use_as_default": False,
        "layers_enabled": False,
        "sync_to_project": "",
        "width": 64,
        "height": 32,
        "uv_width": 64,
        "uv_height": 32,
        "source": png_data_url(png_path),
    }


# -----------------------------------------------------------------------------
# Bbmodel assembly
# -----------------------------------------------------------------------------

def build_bbmodel(name: str, cube_specs, png_path: Path, folder: str) -> dict:
    elements = [make_cube(*spec) for spec in cube_specs]
    outliner = [e["uuid"] for e in elements]
    return {
        "meta": {
            "format_version": "5.0",
            "model_format": "modded_entity",
            "box_uv": True,
        },
        "name": name,
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
        "textures": [texture_record(png_path, folder)],
    }


def write(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent="\t")
    print(f"wrote {path.relative_to(ROOT.parent)}")


def main() -> None:
    if not HUMANOID_PNG.exists() or not LEGGINGS_PNG.exists():
        raise SystemExit(
            f"Texture PNGs not found. Run "
            f"`python3 craftee/tools/generate_textures.py` first."
        )

    write(
        HELMET_BBMODEL,
        build_bbmodel(
            name="craftee_armor_helmet",
            cube_specs=HELMET_CUBES,
            png_path=HUMANOID_PNG,
            folder="entity/equipment/humanoid",
        ),
    )
    write(
        CHESTPLATE_BBMODEL,
        build_bbmodel(
            name="craftee_armor_chestplate",
            cube_specs=CHESTPLATE_CUBES,
            png_path=HUMANOID_PNG,
            folder="entity/equipment/humanoid",
        ),
    )
    write(
        BOOTS_BBMODEL,
        build_bbmodel(
            name="craftee_armor_boots",
            cube_specs=BOOTS_CUBES,
            png_path=HUMANOID_PNG,
            folder="entity/equipment/humanoid",
        ),
    )
    write(
        LEGGINGS_BBMODEL,
        build_bbmodel(
            name="craftee_armor_leggings",
            cube_specs=LEGGINGS_CUBES,
            png_path=LEGGINGS_PNG,
            folder="entity/equipment/humanoid_leggings",
        ),
    )


if __name__ == "__main__":
    main()
