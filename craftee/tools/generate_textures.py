#!/usr/bin/env python3
"""
Regenerates the seven PNG assets for :craftee from a small palette.
Run with: python3 craftee/tools/generate_textures.py
Output paths are absolute under the module's resources tree, so the
script can be re-run any time the palette is tweaked. Idempotent.
"""

from PIL import Image
from pathlib import Path

# Palette
BLACK_BASE   = (0x0F, 0x0F, 0x0F, 255)
ORANGE_STRIP = (0xF0, 0x8A, 0x1F, 255)
WHITE_MARK   = (0xFF, 0xFF, 0xFF, 255)
TRANSPARENT  = (0, 0, 0, 0)

ROOT = Path(__file__).resolve().parents[1]
ENTITY_BASE   = ROOT / "src/main/resources/assets/craftee/textures/entity/equipment"
ITEM_TEXTURES = ROOT / "src/main/resources/assets/craftee/textures/item"


def fill(img: Image.Image, color):
    for x in range(img.width):
        for y in range(img.height):
            img.putpixel((x, y), color)


def make_humanoid_body() -> Image.Image:
    """64×32 humanoid armor sheet. Black base with a 2px-wide vertical
       orange filmstrip stripe down the centre-front of the chestplate
       and helmet UVs. Sprocket holes are 1×1 black pixels every 4 rows.

       Vanilla armor UV layout (top half is helmet + chest + arms layer):
         - helmet front face: x=8..15, y=8..15 (8×8 block)
         - chestplate front:  x=20..27, y=20..31 (8×12 block)
         - arms-front:        x=44..47, y=20..31 (split L/R)
       The "centre-front" of helmet is x=11..12, of chest is x=23..24."""
    img = Image.new("RGBA", (64, 32), TRANSPARENT)
    fill(img, BLACK_BASE)

    # Helmet front stripe (x=11..12, y=8..15)
    for y in range(8, 16):
        img.putpixel((11, y), ORANGE_STRIP)
        img.putpixel((12, y), ORANGE_STRIP)
    # Helmet sprocket holes (every 4 rows starting y=10)
    for y in (10, 14):
        img.putpixel((11, y), BLACK_BASE)
        img.putpixel((12, y), BLACK_BASE)

    # Chestplate front stripe (x=23..24, y=20..31)
    for y in range(20, 32):
        img.putpixel((23, y), ORANGE_STRIP)
        img.putpixel((24, y), ORANGE_STRIP)
    # Chest sprocket holes (every 4 rows starting y=22)
    for y in (22, 26, 30):
        img.putpixel((23, y), BLACK_BASE)
        img.putpixel((24, y), BLACK_BASE)

    return img


def make_humanoid_leggings() -> Image.Image:
    """64×32 leggings sheet. Black base with two parallel vertical
       orange stripes — one for each leg.
         - left  leg front: x=4..7,  y=20..31, centre x=5..6
         - right leg front: x=20..23,y=20..31, centre x=21..22
       NOTE: the topmost row of the leggings stripe (y=20) must align
       with the bottommost row of the chestplate stripe (y=31 in body
       sheet) so the filmstrip reads as one continuous line. Vanilla
       UV mapping puts both at the player's waist; visually verify in
       dev client."""
    img = Image.new("RGBA", (64, 32), TRANSPARENT)
    fill(img, BLACK_BASE)

    # Left leg stripe
    for y in range(20, 32):
        img.putpixel((5, y), ORANGE_STRIP)
        img.putpixel((6, y), ORANGE_STRIP)
    # Right leg stripe
    for y in range(20, 32):
        img.putpixel((21, y), ORANGE_STRIP)
        img.putpixel((22, y), ORANGE_STRIP)

    return img


def make_item_helmet() -> Image.Image:
    """16×16 helmet inventory icon — black helmet silhouette with
       single-px orange stripe down the centre-front."""
    img = Image.new("RGBA", (16, 16), TRANSPARENT)
    # Dome
    for x in range(5, 11):
        img.putpixel((x, 1), BLACK_BASE)
    # Body of helmet
    for y in range(2, 8):
        for x in range(3, 13):
            img.putpixel((x, y), BLACK_BASE)
    # Orange stripe
    for y in range(1, 8):
        img.putpixel((8, y), ORANGE_STRIP)
    return img


def make_item_chestplate() -> Image.Image:
    """16×16 chestplate icon — black T-silhouette with central orange stripe."""
    img = Image.new("RGBA", (16, 16), TRANSPARENT)
    # Top yoke
    for x in range(2, 14):
        for y in range(2, 4):
            img.putpixel((x, y), BLACK_BASE)
    # Body
    for x in range(4, 12):
        for y in range(4, 14):
            img.putpixel((x, y), BLACK_BASE)
    # Orange stripe
    for y in range(2, 14):
        img.putpixel((8, y), ORANGE_STRIP)
    return img


def make_item_leggings() -> Image.Image:
    """16×16 leggings icon — two black legs with one orange stripe each."""
    img = Image.new("RGBA", (16, 16), TRANSPARENT)
    # Waistband
    for x in range(3, 13):
        for y in range(2, 4):
            img.putpixel((x, y), BLACK_BASE)
    # Left leg
    for x in range(3, 7):
        for y in range(4, 14):
            img.putpixel((x, y), BLACK_BASE)
    # Right leg
    for x in range(9, 13):
        for y in range(4, 14):
            img.putpixel((x, y), BLACK_BASE)
    # Stripes
    for y in range(2, 14):
        img.putpixel((4, y), ORANGE_STRIP)
        img.putpixel((11, y), ORANGE_STRIP)
    return img


def make_item_boots() -> Image.Image:
    """16×16 boots icon — two short black blocks with orange dashes."""
    img = Image.new("RGBA", (16, 16), TRANSPARENT)
    # Two boots
    for x in range(3, 7):
        for y in range(8, 14):
            img.putpixel((x, y), BLACK_BASE)
    for x in range(9, 13):
        for y in range(8, 14):
            img.putpixel((x, y), BLACK_BASE)
    # Orange dashes on the front
    for y in (10, 11):
        img.putpixel((4, y), ORANGE_STRIP)
        img.putpixel((10, y), ORANGE_STRIP)
    return img


def make_item_template() -> Image.Image:
    """16×16 smithing template icon — black plate with orange diamond
       outline and a white 'C' marker centred."""
    img = Image.new("RGBA", (16, 16), TRANSPARENT)
    # Black square plate
    for x in range(2, 14):
        for y in range(2, 14):
            img.putpixel((x, y), BLACK_BASE)
    # Orange diamond outline (corners cut)
    diamond = [
        (8, 2), (7, 3), (9, 3), (6, 4), (10, 4),
        (5, 5), (11, 5), (4, 6), (12, 6), (3, 7), (13, 7),
        (4, 8), (12, 8), (5, 9), (11, 9), (6, 10), (10, 10),
        (7, 11), (9, 11), (8, 12)
    ]
    for px in diamond:
        img.putpixel(px, ORANGE_STRIP)
    # White 'C' centred (4×5)
    c_pixels = [
        (7, 6), (8, 6), (9, 6),
        (7, 7),
        (7, 8),
        (7, 9),
        (7, 10), (8, 10), (9, 10),
    ]
    for px in c_pixels:
        img.putpixel(px, WHITE_MARK)
    return img


def write(img: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, "PNG")
    print(f"wrote {path.relative_to(ROOT.parent)}")


def main() -> None:
    write(make_humanoid_body(),     ENTITY_BASE / "humanoid"          / "craftee.png")
    write(make_humanoid_leggings(), ENTITY_BASE / "humanoid_leggings" / "craftee.png")
    write(make_item_helmet(),       ITEM_TEXTURES / "craftee_helmet.png")
    write(make_item_chestplate(),   ITEM_TEXTURES / "craftee_chestplate.png")
    write(make_item_leggings(),     ITEM_TEXTURES / "craftee_leggings.png")
    write(make_item_boots(),        ITEM_TEXTURES / "craftee_boots.png")
    write(make_item_template(),     ITEM_TEXTURES / "craftee_upgrade_smithing_template.png")


if __name__ == "__main__":
    main()
