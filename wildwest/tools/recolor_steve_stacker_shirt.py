#!/usr/bin/env python3
"""Recolor the Steve Stacker boss's shirt from vanilla teal to light green.

The vendored Steve skin uses 4 shaded teal variants for the shirt + sleeves:
  (0, 175, 175), (0, 164, 164), (4, 149, 149), (5, 136, 136)

We replace each with a green tone of equivalent brightness, preserving the
vanilla pixel-art shading.

Run this to regenerate the texture in place; commit the resulting PNG."""

from pathlib import Path
from PIL import Image

TEX = Path(__file__).resolve().parents[1] / "src/main/resources/assets/wildwest/textures/entity/steve_stacker.png"

# Map of {teal_rgb: light_green_rgb}. Brightness preserved per pair.
TEAL_TO_GREEN = {
    (0, 175, 175): (130, 220, 110),
    (0, 164, 164): (120, 205, 100),
    (4, 149, 149): (108, 186,  92),
    (5, 136, 136): ( 96, 168,  80),
}

def main():
    img = Image.open(TEX).convert("RGBA")
    w, h = img.size
    px = img.load()
    swapped = 0
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            key = (r, g, b)
            if key in TEAL_TO_GREEN:
                nr, ng, nb = TEAL_TO_GREEN[key]
                px[x, y] = (nr, ng, nb, a)
                swapped += 1
    img.save(TEX)
    print(f"Recolored {swapped} pixels in {TEX}")

if __name__ == "__main__":
    main()
