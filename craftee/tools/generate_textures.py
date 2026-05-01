#!/usr/bin/env python3
"""
Regenerates the worn-armor + inventory-icon PNGs for :craftee from a
single source skin file.

Source: craftee/tools/craftee_source_skin.png
        (a vanilla 64x64 Minecraft player skin — the Craftee character)

The vanilla skin UV layout aligns 1:1 with the vanilla armor sheet UV
layout for the regions we care about (head front, body front, arms,
legs). So we simply copy the skin's top half (y=0..31) onto the
worn-armor "humanoid" 64x32 sheet and copy the right-leg region onto
the "humanoid_leggings" sheet.  The five 16x16 inventory icons are
hand-painted from the same palette so they read at small scale.

Run with:  python3 craftee/tools/generate_textures.py
Idempotent.  Outputs:

    src/main/resources/assets/craftee/textures/entity/equipment/
        humanoid/craftee.png            (64x32 — helmet/chest/arms/boots)
        humanoid_leggings/craftee.png   (64x32 — legs only)
    src/main/resources/assets/craftee/textures/item/
        craftee_helmet.png              (16x16)
        craftee_chestplate.png          (16x16)
        craftee_leggings.png            (16x16)
        craftee_boots.png               (16x16)
        craftee_upgrade_smithing_template.png (16x16)
"""

from PIL import Image
from pathlib import Path

# Palette — sampled directly from craftee_source_skin.png
CYAN_LIGHT  = (0xB3, 0xFC, 0xF5, 255)
CYAN_MID    = (0x81, 0xFF, 0xF4, 255)
CYAN_DARK   = (0x4F, 0xF9, 0xE9, 255)
BLACK_MARK  = (0x00, 0x00, 0x00, 255)
ORANGE_MID  = (0xF2, 0x87, 0x27, 255)
TRANSPARENT = (0, 0, 0, 0)

ROOT = Path(__file__).resolve().parents[1]
SOURCE_SKIN   = ROOT / "tools/craftee_source_skin.png"
ENTITY_BASE   = ROOT / "src/main/resources/assets/craftee/textures/entity/equipment"
ITEM_TEXTURES = ROOT / "src/main/resources/assets/craftee/textures/item"


def humanoid_body_from_skin(skin: Image.Image) -> Image.Image:
    """64x32 worn-armor 'humanoid' sheet.

    Vanilla armor UV layout matches the player skin's top-half layout:
      head      x=0..31,  y=0..15   (helmet)
      body      x=16..39, y=16..31  (chestplate)
      r_arm     x=40..55, y=16..31  (chestplate sleeves)
      r_leg     x=0..15,  y=16..31  (boots)
    The skin has all four regions in its top half, so a verbatim crop
    of the top half gives us a faithful 'wear the Craftee skin as
    armor' projection."""
    return skin.crop((0, 0, 64, 32))


def humanoid_leggings_from_skin(skin: Image.Image) -> Image.Image:
    """64x32 worn-armor 'humanoid_leggings' sheet.

    Only the right-leg UV region (x=0..15, y=16..31) is sampled by the
    leggings layer renderer; the rest of the sheet is unused but must
    be transparent so it doesn't bleed into other layers."""
    out = Image.new("RGBA", (64, 32), TRANSPARENT)
    leg = skin.crop((0, 16, 16, 32))
    out.paste(leg, (0, 16))
    return out


def make_item_helmet() -> Image.Image:
    """16x16 helmet icon — cyan dome with a black 'C' on the front."""
    img = Image.new("RGBA", (16, 16), TRANSPARENT)
    for x in range(5, 11):
        img.putpixel((x, 1), CYAN_LIGHT)
    for y in range(2, 8):
        for x in range(3, 13):
            img.putpixel((x, y), CYAN_MID)
    # Black "C" on front face (3x3 area)
    for y, xs in [(3, [6, 7, 8]), (4, [6]), (5, [6]), (6, [6, 7, 8])]:
        for x in xs:
            img.putpixel((x, y), BLACK_MARK)
    return img


def make_item_chestplate() -> Image.Image:
    """16x16 chestplate icon — cyan body with a black 'C' emblem."""
    img = Image.new("RGBA", (16, 16), TRANSPARENT)
    # Top yoke
    for x in range(2, 14):
        for y in range(2, 4):
            img.putpixel((x, y), CYAN_LIGHT)
    # Body
    for x in range(4, 12):
        for y in range(4, 14):
            img.putpixel((x, y), CYAN_MID)
    # Big black "C" centred
    c_pixels = [
        (6, 5), (7, 5), (8, 5), (9, 5),
        (5, 6),
        (5, 7),
        (5, 8),
        (5, 9),
        (5, 10),
        (6, 11), (7, 11), (8, 11), (9, 11),
    ]
    for px in c_pixels:
        img.putpixel(px, BLACK_MARK)
    return img


def make_item_leggings() -> Image.Image:
    """16x16 leggings icon — cyan legs."""
    img = Image.new("RGBA", (16, 16), TRANSPARENT)
    for x in range(3, 13):
        for y in range(2, 4):
            img.putpixel((x, y), CYAN_LIGHT)
    for x in range(3, 7):
        for y in range(4, 14):
            img.putpixel((x, y), CYAN_MID)
    for x in range(9, 13):
        for y in range(4, 14):
            img.putpixel((x, y), CYAN_MID)
    return img


def make_item_boots() -> Image.Image:
    """16x16 boots icon — cyan boots with an orange ankle band."""
    img = Image.new("RGBA", (16, 16), TRANSPARENT)
    for x in range(3, 7):
        for y in range(8, 14):
            img.putpixel((x, y), CYAN_MID)
    for x in range(9, 13):
        for y in range(8, 14):
            img.putpixel((x, y), CYAN_MID)
    # Orange ankle band — echoes the wristband on the source skin
    for x in range(3, 7):
        img.putpixel((x, 8), ORANGE_MID)
    for x in range(9, 13):
        img.putpixel((x, 8), ORANGE_MID)
    return img


def make_item_template() -> Image.Image:
    """16x16 smithing template icon — black plate, cyan diamond outline,
       cyan 'C' marker centred."""
    img = Image.new("RGBA", (16, 16), TRANSPARENT)
    for x in range(2, 14):
        for y in range(2, 14):
            img.putpixel((x, y), BLACK_MARK)
    diamond = [
        (8, 2), (7, 3), (9, 3), (6, 4), (10, 4),
        (5, 5), (11, 5), (4, 6), (12, 6), (3, 7), (13, 7),
        (4, 8), (12, 8), (5, 9), (11, 9), (6, 10), (10, 10),
        (7, 11), (9, 11), (8, 12),
    ]
    for px in diamond:
        img.putpixel(px, CYAN_MID)
    c_pixels = [
        (7, 6), (8, 6), (9, 6),
        (7, 7),
        (7, 8),
        (7, 9),
        (7, 10), (8, 10), (9, 10),
    ]
    for px in c_pixels:
        img.putpixel(px, CYAN_LIGHT)
    return img


def write(img: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, "PNG")
    print(f"wrote {path.relative_to(ROOT.parent)}")


def main() -> None:
    if not SOURCE_SKIN.exists():
        raise SystemExit(
            f"Missing source skin at {SOURCE_SKIN.relative_to(ROOT.parent)}. "
            f"Drop a 64x64 Minecraft skin PNG there and re-run."
        )

    skin = Image.open(SOURCE_SKIN).convert("RGBA")
    if skin.size != (64, 64):
        raise SystemExit(
            f"Source skin must be 64x64; got {skin.size}. Use a vanilla "
            f"Minecraft skin format."
        )

    write(humanoid_body_from_skin(skin),     ENTITY_BASE / "humanoid"          / "craftee.png")
    write(humanoid_leggings_from_skin(skin), ENTITY_BASE / "humanoid_leggings" / "craftee.png")
    write(make_item_helmet(),                ITEM_TEXTURES / "craftee_helmet.png")
    write(make_item_chestplate(),            ITEM_TEXTURES / "craftee_chestplate.png")
    write(make_item_leggings(),              ITEM_TEXTURES / "craftee_leggings.png")
    write(make_item_boots(),                 ITEM_TEXTURES / "craftee_boots.png")
    write(make_item_template(),              ITEM_TEXTURES / "craftee_upgrade_smithing_template.png")


if __name__ == "__main__":
    main()
