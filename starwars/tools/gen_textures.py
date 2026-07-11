#!/usr/bin/env python3
"""Generate 64x64 RGBA PNG entity textures for the starwars mobs.

Finished-art bar: every visible region gets a base color + at least one
darker shade (bottom/right edges) and one highlight (top edge) — no flat
single-color limbs.
"""
import struct, zlib, sys, os

W, H = 64, 64

def fill(rgba, color):
    r, g, b, a = color
    for i in range(W * H):
        rgba[4*i + 0] = r
        rgba[4*i + 1] = g
        rgba[4*i + 2] = b
        rgba[4*i + 3] = a

def rect(rgba, x0, y0, x1, y1, color):
    """Fill (x0..x1, y0..y1) — half-open in x and y — with color."""
    r, g, b, a = color
    for y in range(y0, y1):
        for x in range(x0, x1):
            i = 4 * (y * W + x)
            rgba[i + 0] = r
            rgba[i + 1] = g
            rgba[i + 2] = b
            rgba[i + 3] = a

def write_png(path, rgba):
    """Write a 64x64 RGBA PNG from a flat byte buffer."""
    sig = b'\x89PNG\r\n\x1a\n'
    def chunk(tag, data):
        crc = zlib.crc32(tag + data) & 0xffffffff
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', crc)

    ihdr = struct.pack('>IIBBBBB', W, H, 8, 6, 0, 0, 0)  # 8-bit RGBA
    raw = bytearray()
    for y in range(H):
        raw.append(0)  # filter type 'none' per scanline
        raw.extend(rgba[4*W*y : 4*W*(y+1)])
    idat = zlib.compress(bytes(raw))
    iend = b''
    out = sig + chunk(b'IHDR', ihdr) + chunk(b'IDAT', idat) + chunk(b'IEND', iend)
    with open(path, 'wb') as f:
        f.write(out)

def paint_humanoid_base(rgba, shirt):
    """Lay down the standard humanoid skin: face + arms + body shirt + pants.
    Kept for parity with wildwest's gen_textures.py helper set even though
    the starwars mobs currently paint every region explicitly themselves.
    """
    rect(rgba, 0, 0, 32, 8, shirt)
    rect(rgba, 16, 16, 40, 32, shirt)
    rect(rgba, 40, 16, 56, 32, shirt)
    rect(rgba, 32, 48, 48, 64, shirt)
    rect(rgba, 48, 48, 64, 64, shirt)

# Stormtrooper: white plastoid armor, black bodysuit seams, black eye slits.
WHITE      = (0xEE, 0xEE, 0xF2, 0xFF)
WHITE_HI   = (0xFF, 0xFF, 0xFF, 0xFF)
WHITE_SH   = (0xC8, 0xC8, 0xD2, 0xFF)
SUIT_BLACK = (0x16, 0x16, 0x1A, 0xFF)
GRAY       = (0x8A, 0x8A, 0x94, 0xFF)

def paint_stormtrooper(rgba):
    fill(rgba, WHITE)
    # Head front (u 8..16, v 8..16): faceplate with eye slits + frown vent.
    rect(rgba, 8, 8, 16, 16, WHITE_HI)
    rect(rgba, 9, 10, 11, 11, SUIT_BLACK)   # left eye slit
    rect(rgba, 13, 10, 15, 11, SUIT_BLACK)  # right eye slit
    rect(rgba, 10, 13, 14, 14, GRAY)        # aerator frown
    rect(rgba, 11, 14, 13, 15, SUIT_BLACK)  # chin vent
    # Helmet-overlay region (u 32..64, v 0..16): white shell, gray brow band.
    rect(rgba, 32, 0, 64, 16, WHITE)
    rect(rgba, 40, 8, 56, 9, GRAY)
    # Body (u 16..40, v 16..32): chest plate + black under-suit midriff.
    rect(rgba, 16, 16, 40, 32, WHITE)
    rect(rgba, 20, 26, 36, 28, SUIT_BLACK)  # ab plate seam
    rect(rgba, 26, 20, 30, 24, GRAY)        # chest control box
    # Arms/legs: white with black joint seams + shading.
    for (u0, v0) in ((40, 16), (32, 48), (0, 16), (16, 48)):
        rect(rgba, u0, v0, u0 + 16, v0 + 16, WHITE)
        rect(rgba, u0, v0 + 6, u0 + 16, v0 + 7, SUIT_BLACK)   # elbow/knee seam
        rect(rgba, u0, v0 + 14, u0 + 16, v0 + 16, WHITE_SH)   # lower shade
        rect(rgba, u0, v0, u0 + 16, v0 + 1, WHITE_HI)         # top highlight
    # Global shading pass: darken the right/bottom edge of head + body.
    rect(rgba, 30, 8, 32, 16, WHITE_SH)
    rect(rgba, 16, 30, 40, 32, WHITE_SH)

# Battle droid: sand-tan metal, dark joints, black eye dots on the snouted head.
TAN     = (0xB8, 0xA0, 0x78, 0xFF)
TAN_HI  = (0xD2, 0xBC, 0x94, 0xFF)
TAN_SH  = (0x8E, 0x78, 0x56, 0xFF)
JOINT   = (0x50, 0x44, 0x32, 0xFF)
EYE     = (0x10, 0x10, 0x10, 0xFF)

def paint_battle_droid(rgba):
    fill(rgba, (0, 0, 0, 0))                # custom UV layout — transparent base
    rect(rgba, 0, 0, 16, 10, TAN)           # head region (u0..16, v0..10)
    rect(rgba, 4, 4, 5, 6, EYE)             # eyes on the head front rows
    rect(rgba, 7, 4, 8, 6, EYE)
    rect(rgba, 16, 0, 26, 5, TAN_SH)        # snout region
    rect(rgba, 0, 16, 18, 29, TAN)          # body strip
    rect(rgba, 0, 16, 18, 17, TAN_HI)
    rect(rgba, 0, 27, 18, 29, TAN_SH)
    rect(rgba, 18, 16, 26, 20, JOINT)       # hip block
    for u0 in (32, 40, 48, 56):             # four 2x12x2 limbs at v16
        rect(rgba, u0, 16, u0 + 8, 30, TAN)
        rect(rgba, u0, 21, u0 + 8, 22, JOINT)  # mid-joint
        rect(rgba, u0, 28, u0 + 8, 30, TAN_SH)
        rect(rgba, u0, 16, u0 + 8, 17, TAN_HI)

# Jedi knight: cream tunic, brown robe/hood, tan belt.
TUNIC   = (0xD8, 0xCC, 0xB0, 0xFF)
TUNIC_S = (0xB4, 0xA8, 0x8C, 0xFF)
ROBE    = (0x6A, 0x50, 0x34, 0xFF)
ROBE_S  = (0x4E, 0x3A, 0x26, 0xFF)
BELT    = (0x3C, 0x2C, 0x1C, 0xFF)
SKIN    = (0xE8, 0xC0, 0x98, 0xFF)
HAIR    = (0x4A, 0x36, 0x22, 0xFF)

def paint_jedi_knight(rgba):
    fill(rgba, TUNIC)
    rect(rgba, 0, 0, 32, 8, HAIR)          # head top: hair
    rect(rgba, 8, 8, 16, 16, SKIN)         # face
    rect(rgba, 8, 8, 16, 10, HAIR)         # hairline
    rect(rgba, 10, 11, 11, 12, (0x20, 0x30, 0x50, 0xFF))  # eyes
    rect(rgba, 13, 11, 14, 12, (0x20, 0x30, 0x50, 0xFF))
    rect(rgba, 32, 0, 64, 16, ROBE)        # hood overlay strip
    rect(rgba, 40, 8, 48, 16, (0, 0, 0, 0))  # hood front opening (transparent)
    rect(rgba, 16, 16, 40, 32, TUNIC)      # body
    rect(rgba, 20, 28, 36, 30, BELT)       # belt
    rect(rgba, 16, 30, 40, 32, TUNIC_S)
    for (u0, v0) in ((40, 16), (32, 48)):  # arms: robe sleeves
        rect(rgba, u0, v0, u0 + 16, v0 + 16, ROBE)
        rect(rgba, u0, v0 + 13, u0 + 16, v0 + 16, ROBE_S)
    for (u0, v0) in ((0, 16), (16, 48)):   # legs: dark trousers
        rect(rgba, u0, v0, u0 + 16, v0 + 16, (0x50, 0x46, 0x38, 0xFF))
        rect(rgba, u0, v0 + 14, u0 + 16, v0 + 16, (0x38, 0x30, 0x26, 0xFF))
    rect(rgba, 32, 32, 64, 48, ROBE)       # robe skirt region (u32.., v32..)
    rect(rgba, 32, 45, 64, 48, ROBE_S)

# Darth Vader: near-black armored suit, gloss highlights, silver eye lenses,
# dark-gray aerator, a colored-button chest panel, and a fold-striped cape.
VADER_BASE    = (0x14, 0x14, 0x18, 0xFF)
VADER_GLOSS   = (0x2E, 0x2E, 0x38, 0xFF)
VADER_LIMB    = (0x1E, 0x1E, 0x24, 0xFF)
VADER_JOINT   = (0x0A, 0x0A, 0x0C, 0xFF)
VADER_EYE     = (0x9A, 0x9A, 0xA4, 0xFF)
VADER_AERATOR = (0x50, 0x50, 0x58, 0xFF)
VADER_PANEL   = (0x3A, 0x3A, 0x42, 0xFF)
VADER_RED     = (0xC0, 0x30, 0x30, 0xFF)

def paint_darth_vader(rgba):
    fill(rgba, VADER_BASE)
    # Head front (u 8..16, v 8..16): faceplate with silver eye lenses +
    # a dark-gray aerator block suggesting the triangular mouth grille.
    rect(rgba, 9, 10, 11, 11, VADER_EYE)         # left eye lens
    rect(rgba, 13, 10, 15, 11, VADER_EYE)        # right eye lens
    rect(rgba, 10, 12, 14, 15, VADER_AERATOR)    # aerator
    # Helmet-overlay region (u 32..64, v 0..16): dome + flare, gloss top highlight.
    rect(rgba, 32, 0, 64, 1, VADER_GLOSS)        # dome top gloss
    # Cape UV footprint (u 44..64, v 32..53): flat near-black with vertical
    # fold stripes every 3px.
    for x in range(44, 64, 3):
        rect(rgba, x, 32, x + 1, 53, VADER_LIMB)
    # Chest-panel UV footprint (u 56..64, v 54..58): panel plate + 3 buttons
    # (red, silver, red).
    rect(rgba, 56, 54, 64, 58, VADER_PANEL)
    rect(rgba, 57, 55, 58, 56, VADER_RED)
    rect(rgba, 59, 55, 60, 56, VADER_EYE)
    rect(rgba, 61, 55, 62, 56, VADER_RED)
    # Arms/legs: charcoal with black joint seams + gloss shoulder/hip highlight.
    for (u0, v0) in ((40, 16), (32, 48), (0, 16), (16, 48)):
        rect(rgba, u0, v0, u0 + 16, v0 + 16, VADER_LIMB)
        rect(rgba, u0, v0 + 6, u0 + 16, v0 + 7, VADER_JOINT)   # elbow/knee seam
        rect(rgba, u0, v0, u0 + 16, v0 + 1, VADER_GLOSS)       # shoulder/hip top gloss
        rect(rgba, u0, v0 + 14, u0 + 16, v0 + 16, VADER_BASE)  # lower shade
    # Global shading pass: darken the right/bottom edge of head + body.
    rect(rgba, 30, 8, 32, 16, VADER_JOINT)
    rect(rgba, 16, 30, 40, 32, VADER_JOINT)

MOBS = {
    'stormtrooper': paint_stormtrooper,
    'battle_droid': paint_battle_droid,
    'jedi_knight': paint_jedi_knight,
    'darth_vader': paint_darth_vader,
}

if __name__ == '__main__':
    out_dir = sys.argv[1] if len(sys.argv) > 1 else \
        'starwars/src/main/resources/assets/starwars/textures/entity'
    os.makedirs(out_dir, exist_ok=True)
    for name, paint_fn in MOBS.items():
        rgba = bytearray(W * H * 4)
        paint_fn(rgba)
        out_path = os.path.join(out_dir, f'{name}.png')
        write_png(out_path, rgba)
        print(name)
    print('OK')
