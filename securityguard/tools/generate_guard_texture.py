#!/usr/bin/env python3
"""
Deterministic generator for the Security Guard entity texture.

Run:
    python3 generate_guard_texture.py

Writes ../src/main/resources/assets/securityguard/textures/entity/security_guard.png.
Re-running produces byte-identical output as long as this script is unchanged.
"""

from __future__ import annotations

from pathlib import Path
from PIL import Image, ImageDraw

# ---------- Palette (RGBA) ----------

SKIN           = (0xA3, 0x7D, 0x5B, 0xFF)
BROW           = (0x3A, 0x26, 0x18, 0xFF)
EYE            = (0x00, 0x00, 0x00, 0xFF)
NAVY           = (0x1F, 0x2A, 0x55, 0xFF)
NAVY_HIGHLIGHT = (0x2D, 0x3D, 0x70, 0xFF)
NAVY_SHADOW    = (0x15, 0x20, 0x40, 0xFF)
TROUSERS       = (0x15, 0x20, 0x3F, 0xFF)
GOLD           = (0xD4, 0xA8, 0x2A, 0xFF)
GOLD_HIGHLIGHT = (0xF1, 0xC8, 0x4A, 0xFF)
GOLD_SHADOW    = (0x9C, 0x7A, 0x1F, 0xFF)
TRANSPARENT    = (0x00, 0x00, 0x00, 0x00)

# ---------- Region rectangles (x0, y0, x1, y1), half-open ----------

# Head (outer, not the hat overlay)
HEAD_FRONT  = (8,  8, 16, 16)
HEAD_RIGHT  = (0,  8,  8, 16)
HEAD_BACK   = (24, 8, 32, 16)
HEAD_LEFT   = (16, 8, 24, 16)
HEAD_TOP    = (8,  0, 16,  8)
HEAD_BOTTOM = (16, 0, 24,  8)

# Body (torso outer)
BODY_FRONT = (20, 20, 28, 32)
BODY_BACK  = (32, 20, 40, 32)
BODY_RIGHT = (16, 20, 20, 32)
BODY_LEFT  = (28, 20, 32, 32)
BODY_TOP   = (20, 16, 28, 20)
BODY_BOT   = (28, 16, 36, 20)

# Right arm (outer)
RIGHT_ARM_OUTER  = (40, 20, 44, 32)
RIGHT_ARM_FRONT  = (44, 20, 48, 32)
RIGHT_ARM_INNER  = (48, 20, 52, 32)
RIGHT_ARM_BACK   = (52, 20, 56, 32)
RIGHT_ARM_TOP    = (44, 16, 48, 20)
RIGHT_ARM_BOTTOM = (48, 16, 52, 20)

# Left arm (outer, lower half of sheet)
LEFT_ARM_OUTER  = (32, 52, 36, 64)
LEFT_ARM_FRONT  = (36, 52, 40, 64)
LEFT_ARM_INNER  = (40, 52, 44, 64)
LEFT_ARM_BACK   = (44, 52, 48, 64)
LEFT_ARM_TOP    = (36, 48, 40, 52)
LEFT_ARM_BOTTOM = (40, 48, 44, 52)

# Right leg (outer)
RIGHT_LEG_OUTER  = (0,  20,  4, 32)
RIGHT_LEG_FRONT  = (4,  20,  8, 32)
RIGHT_LEG_INNER  = (8,  20, 12, 32)
RIGHT_LEG_BACK   = (12, 20, 16, 32)
RIGHT_LEG_TOP    = (4,  16,  8, 20)
RIGHT_LEG_BOTTOM = (8,  16, 12, 20)

# Left leg (outer, lower half of sheet)
LEFT_LEG_OUTER  = (16, 52, 20, 64)
LEFT_LEG_FRONT  = (20, 52, 24, 64)
LEFT_LEG_INNER  = (24, 52, 28, 64)
LEFT_LEG_BACK   = (28, 52, 32, 64)
LEFT_LEG_TOP    = (20, 48, 24, 52)
LEFT_LEG_BOTTOM = (24, 48, 28, 52)

# Cap (existing UVs from the model)
CAP_BRIM_UNWRAP  = (32,  0, 64, 10)
CAP_CROWN_UNWRAP = (32, 10, 60, 19)

# Nose (NEW — matches texOffs(56, 16) for a 2x2x2 cube)
NOSE_UNWRAP = (56, 16, 64, 20)

OUTPUT = Path(__file__).resolve().parent.parent / \
    "src/main/resources/assets/securityguard/textures/entity/security_guard.png"

# ---------- Painting helpers ----------

def fill(img: Image.Image, rect: tuple[int, int, int, int], color):
    """Fill a half-open rect (x0, y0, x1, y1) with a solid color."""
    x0, y0, x1, y1 = rect
    ImageDraw.Draw(img).rectangle((x0, y0, x1 - 1, y1 - 1), fill=color)


def pixel(img: Image.Image, x: int, y: int, color):
    img.putpixel((x, y), color)


# ---------- Main paint pipeline ----------

def paint_skin(img: Image.Image) -> None:
    """Fill all exposed-skin regions with villager skin tone."""
    for rect in (HEAD_FRONT, HEAD_RIGHT, HEAD_BACK, HEAD_LEFT, HEAD_TOP, HEAD_BOTTOM):
        fill(img, rect, SKIN)
    fill(img, NOSE_UNWRAP, SKIN)
    for rect in (RIGHT_ARM_BOTTOM, LEFT_ARM_BOTTOM):
        fill(img, rect, SKIN)


def paint_uniform_base(img: Image.Image) -> None:
    """Solid navy fill across torso, arms (except hands), and the cap."""
    for rect in (BODY_FRONT, BODY_BACK, BODY_RIGHT, BODY_LEFT, BODY_TOP, BODY_BOT):
        fill(img, rect, NAVY)
    for rect in (RIGHT_ARM_OUTER, RIGHT_ARM_FRONT, RIGHT_ARM_INNER, RIGHT_ARM_BACK, RIGHT_ARM_TOP):
        fill(img, rect, NAVY)
    for rect in (LEFT_ARM_OUTER, LEFT_ARM_FRONT, LEFT_ARM_INNER, LEFT_ARM_BACK, LEFT_ARM_TOP):
        fill(img, rect, NAVY)
    for rect in (RIGHT_LEG_OUTER, RIGHT_LEG_FRONT, RIGHT_LEG_INNER, RIGHT_LEG_BACK,
                 RIGHT_LEG_TOP, RIGHT_LEG_BOTTOM):
        fill(img, rect, TROUSERS)
    for rect in (LEFT_LEG_OUTER, LEFT_LEG_FRONT, LEFT_LEG_INNER, LEFT_LEG_BACK,
                 LEFT_LEG_TOP, LEFT_LEG_BOTTOM):
        fill(img, rect, TROUSERS)
    fill(img, CAP_BRIM_UNWRAP, NAVY)
    fill(img, CAP_CROWN_UNWRAP, NAVY)


def paint_face(img: Image.Image) -> None:
    """Paint villager-style face features into HEAD_FRONT (8x8 panel at (8, 8)).

    Layout inside the 8x8 panel (x relative to panel origin):
      row 2-3: unibrow stripe spanning x=1..7 (7px wide, 2px tall — high visibility)
      row 4-5: eye blocks 2px wide x 2px tall at x=1..3 and x=4..6 (flanking the nose)
      row 6-7: blank (cheeks/chin — no mouth, matching vanilla villagers)
    """
    panel_x, panel_y = HEAD_FRONT[0], HEAD_FRONT[1]
    # Unibrow: 7 pixels wide x 2 pixels tall
    for dx in range(1, 8):
        for dy in range(2, 4):
            pixel(img, panel_x + dx, panel_y + dy, BROW)
    # Eye blocks: 2x2 each, flanking the nose center column
    for dx, dy in [(1, 4), (2, 4), (1, 5), (2, 5),
                   (5, 4), (6, 4), (5, 5), (6, 5)]:
        pixel(img, panel_x + dx, panel_y + dy, EYE)


def paint_uniform_shading(img: Image.Image) -> None:
    """Add 1px shoulder highlights, 1px hem shadows, and the gold cap band.

    Highlights run along the top row of BODY_FRONT/BODY_BACK and the
    inner-shoulder edge. Shadows run along the bottom row of the torso
    where the uniform meets the trousers. The cap gets a single-row gold
    band along the top of the brim unwrap.
    """
    # Body shoulder highlight: top row of front and back
    x0, y0, x1, _ = BODY_FRONT
    for x in range(x0, x1):
        pixel(img, x, y0, NAVY_HIGHLIGHT)
    x0, y0, x1, _ = BODY_BACK
    for x in range(x0, x1):
        pixel(img, x, y0, NAVY_HIGHLIGHT)

    # Body hem shadow: bottom row of front and back
    x0, _, x1, y1 = BODY_FRONT
    for x in range(x0, x1):
        pixel(img, x, y1 - 1, NAVY_SHADOW)
    x0, _, x1, y1 = BODY_BACK
    for x in range(x0, x1):
        pixel(img, x, y1 - 1, NAVY_SHADOW)

    # Right arm shoulder highlight: top row of arm front
    x0, y0, x1, _ = RIGHT_ARM_FRONT
    for x in range(x0, x1):
        pixel(img, x, y0, NAVY_HIGHLIGHT)

    # Left arm shoulder highlight: top row of arm front
    x0, y0, x1, _ = LEFT_ARM_FRONT
    for x in range(x0, x1):
        pixel(img, x, y0, NAVY_HIGHLIGHT)

    # Cap brim — gold band along the row where brim meets crown.
    # CAP_BRIM_UNWRAP is (32, 0, 64, 10); the bottom row at y=9 reads as
    # the visible band where the brim front face transitions to the crown.
    x0, _, x1, y1 = CAP_BRIM_UNWRAP
    for x in range(x0, x1):
        pixel(img, x, y1 - 1, GOLD)


def paint_shield(img: Image.Image) -> None:
    """Paint a 3-wide x 4-tall gold shield on the left chest of BODY_FRONT.

    BODY_FRONT is (20, 20)..(28, 32) — an 8x12 panel. The shield sits
    centered vertically and slightly left of center horizontally so it
    reads as "left chest" the way real police badges do.

    Shield silhouette (rows top to bottom):
      row 0: . X .
      row 1: X X X
      row 2: X X X
      row 3: . X .

    Top-left highlight pixel and bottom-right shadow pixel give it a
    1-pixel pop without losing the silhouette at distance.
    """
    panel_x, panel_y = BODY_FRONT[0], BODY_FRONT[1]
    # Shield top-left corner relative to BODY_FRONT origin
    sx, sy = 2, 4

    shield_pixels = [
        (1, 0),                 # row 0: . X .
        (0, 1), (1, 1), (2, 1),  # row 1: X X X
        (0, 2), (1, 2), (2, 2),  # row 2: X X X
        (1, 3),                 # row 3: . X .
    ]
    for dx, dy in shield_pixels:
        pixel(img, panel_x + sx + dx, panel_y + sy + dy, GOLD)

    # Highlight: top-left of the central body
    pixel(img, panel_x + sx + 0, panel_y + sy + 1, GOLD_HIGHLIGHT)
    # Shadow: bottom-right of the central body
    pixel(img, panel_x + sx + 2, panel_y + sy + 2, GOLD_SHADOW)


def main() -> None:
    img = Image.new("RGBA", (64, 64), TRANSPARENT)
    # paint_uniform_base first; its CAP_CROWN_UNWRAP rectangle bleeds into the
    # nose UV slot at (56..60, 16..19), so paint_skin (which covers the nose)
    # must run AFTER it to win the overlap.
    paint_uniform_base(img)
    paint_skin(img)
    paint_face(img)
    paint_uniform_shading(img)
    paint_shield(img)
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUTPUT, format="PNG", optimize=True)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    main()
