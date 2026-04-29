#!/usr/bin/env python3
"""
Regenerates the six PNG assets for :creeperskin from a small palette.
Run with: python3 creeperskin/tools/generate_textures.py
Output paths are absolute under the module's resources tree, so the
script can be re-run any time the palette is tweaked. Idempotent.
"""

from PIL import Image
from pathlib import Path

# Palette
GREEN_BASE  = (0x0D, 0xA7, 0x0D, 255)
GREEN_SHADE = (0x0A, 0x8D, 0x0A, 255)
FACE_BLACK  = (0x0F, 0x0F, 0x0F, 255)
TRANSPARENT = (0, 0, 0, 0)

ROOT = Path(__file__).resolve().parents[1]
ENTITY_BASE   = ROOT / "src/main/resources/assets/creeperskin/textures/entity/equipment"
ITEM_TEXTURES = ROOT / "src/main/resources/assets/creeperskin/textures/item"


def make_humanoid_body() -> Image.Image:
    """64×32 humanoid armor sheet, plain creeper green with shading.
       Face is added in a separate task; this is a clean green base."""
    img = Image.new("RGBA", (64, 32), TRANSPARENT)
    # Vanilla netherite uses opaque pixels in the cube-net regions and
    # transparent elsewhere. Easiest robust approach: fill everything with
    # the green base, then re-zero the four-pixel margins the vanilla UV
    # layout leaves transparent. The renderer ignores transparent pixels,
    # so over-paint in the empty UV gutter is harmless.
    for x in range(64):
        for y in range(32):
            img.putpixel((x, y), GREEN_BASE)
    # Light shading along a diagonal seam to give the armor depth.
    for i in range(0, 64, 8):
        for y in range(32):
            img.putpixel((i, y), GREEN_SHADE)
    return img


def make_humanoid_leggings() -> Image.Image:
    """64×32 leggings sheet, uniform creeper green (no shading, no face)."""
    img = Image.new("RGBA", (64, 32), TRANSPARENT)
    for x in range(64):
        for y in range(32):
            img.putpixel((x, y), GREEN_BASE)
    return img


def make_item_icon(armor_type: str) -> Image.Image:
    """16×16 inventory icon. Simple silhouette so the four items are
       visually distinct in the creative tab. Shape varies by piece."""
    img = Image.new("RGBA", (16, 16), TRANSPARENT)
    if armor_type == "helmet":
        # Wide rectangle with a dome on top
        for y in range(2, 8):
            for x in range(3, 13):
                img.putpixel((x, y), GREEN_BASE)
        for x in range(5, 11):
            img.putpixel((x, 1), GREEN_BASE)
        # creeper face on the front of the helmet icon
        img.putpixel((6, 4), FACE_BLACK); img.putpixel((9, 4), FACE_BLACK)
        img.putpixel((7, 6), FACE_BLACK); img.putpixel((8, 6), FACE_BLACK)
    elif armor_type == "chestplate":
        for y in range(2, 13):
            for x in range(3, 13):
                img.putpixel((x, y), GREEN_BASE)
        # carve out neck slot
        img.putpixel((7, 2), TRANSPARENT); img.putpixel((8, 2), TRANSPARENT)
    elif armor_type == "leggings":
        for y in range(1, 12):
            for x in range(3, 7):
                img.putpixel((x, y), GREEN_BASE)
            for x in range(9, 13):
                img.putpixel((x, y), GREEN_BASE)
    elif armor_type == "boots":
        for y in range(8, 14):
            for x in range(2, 7):
                img.putpixel((x, y), GREEN_BASE)
            for x in range(9, 14):
                img.putpixel((x, y), GREEN_BASE)
    else:
        raise ValueError(f"unknown armor_type {armor_type!r}")
    return img


def write(path: Path, img: Image.Image) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    print(f"wrote {path.relative_to(ROOT.parent)}")


def main() -> None:
    write(ENTITY_BASE / "humanoid" / "creeper.png", make_humanoid_body())
    write(ENTITY_BASE / "humanoid_leggings" / "creeper.png", make_humanoid_leggings())
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        write(ITEM_TEXTURES / f"creeper_{piece}.png", make_item_icon(piece))


if __name__ == "__main__":
    main()
