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

def write_png(path, rgba, width=None, height=None):
    """Write an RGBA PNG from a flat byte buffer. Defaults to the WxH mob
    canvas; pass width/height explicitly for other sizes (e.g. the 64x32
    worn-armor equipment sheets)."""
    w = width if width is not None else W
    h = height if height is not None else H
    sig = b'\x89PNG\r\n\x1a\n'
    def chunk(tag, data):
        crc = zlib.crc32(tag + data) & 0xffffffff
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', crc)

    ihdr = struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)  # 8-bit RGBA
    raw = bytearray()
    for y in range(h):
        raw.append(0)  # filter type 'none' per scanline
        raw.extend(rgba[4*w*y : 4*w*(y+1)])
    idat = zlib.compress(bytes(raw))
    iend = b''
    out = sig + chunk(b'IHDR', ihdr) + chunk(b'IDAT', idat) + chunk(b'IEND', iend)
    with open(path, 'wb') as f:
        f.write(out)

def fill_buf(rgba, color):
    """Fill an arbitrarily-sized RGBA buffer (not just the WxH mob canvas)."""
    r, g, b, a = color
    for i in range(len(rgba) // 4):
        rgba[4*i + 0] = r
        rgba[4*i + 1] = g
        rgba[4*i + 2] = b
        rgba[4*i + 3] = a

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

# Luke Skywalker (ROTJ): near-black tunic w/ subtle gloss highlights, dark
# trousers with darker boots, sand-blond hair, fair skin + blue eyes, and a
# tan glove stripe on the right wrist (mechanical hand, post-Bespin).
LUKE_TUNIC    = (0x1C, 0x1C, 0x20, 0xFF)
LUKE_TUNIC_HI = (0x30, 0x30, 0x36, 0xFF)
LUKE_BOOT     = (0x10, 0x10, 0x12, 0xFF)
LUKE_HAIR     = (0xC8, 0xA8, 0x60, 0xFF)
LUKE_SKIN     = (0xE8, 0xC0, 0x98, 0xFF)
LUKE_EYE      = (0x30, 0x60, 0xC0, 0xFF)
LUKE_GLOVE    = (0x9A, 0x84, 0x60, 0xFF)

def paint_luke_skywalker(rgba):
    fill(rgba, LUKE_TUNIC)
    # Head: sand-blond hair on top/sides/back, fair face with a hairline
    # fringe and blue eyes.
    rect(rgba, 0, 0, 32, 8, LUKE_HAIR)        # head top: hair
    rect(rgba, 8, 8, 16, 16, LUKE_SKIN)       # face
    rect(rgba, 8, 8, 16, 10, LUKE_HAIR)       # hairline
    rect(rgba, 10, 11, 11, 12, LUKE_EYE)      # left eye
    rect(rgba, 13, 11, 14, 12, LUKE_EYE)      # right eye
    # Body: black tunic with a top-edge gloss highlight.
    rect(rgba, 16, 16, 40, 32, LUKE_TUNIC)
    rect(rgba, 16, 16, 40, 17, LUKE_TUNIC_HI)
    # Arms: black tunic sleeves with top gloss highlight; the right arm
    # (mechanical hand) gets a tan glove stripe on the wrist rows instead of
    # the usual dark cuff.
    for (u0, v0) in ((40, 16), (32, 48)):
        rect(rgba, u0, v0, u0 + 16, v0 + 16, LUKE_TUNIC)
        rect(rgba, u0, v0, u0 + 16, v0 + 1, LUKE_TUNIC_HI)
    rect(rgba, 40, 30, 56, 32, LUKE_GLOVE)    # right-arm wrist rows: glove stripe
    # Legs: black trousers, boots darker on the bottom 4 rows.
    for (u0, v0) in ((0, 16), (16, 48)):
        rect(rgba, u0, v0, u0 + 16, v0 + 16, LUKE_TUNIC)
        rect(rgba, u0, v0, u0 + 16, v0 + 1, LUKE_TUNIC_HI)
        rect(rgba, u0, v0 + 12, u0 + 16, v0 + 16, LUKE_BOOT)

# Obi-Wan Kenobi: the Jedi Knight paint job with an older-Jedi palette —
# auburn-gray hair + full lower-face beard, a lighter cream tunic, and a
# grayer robe (vs. the Jedi Knight's plain brown robe).
OBIWAN_TUNIC   = (0xE2, 0xD8, 0xC0, 0xFF)
OBIWAN_TUNIC_S = (0xBE, 0xB4, 0x9C, 0xFF)
OBIWAN_ROBE    = (0x74, 0x5C, 0x42, 0xFF)
OBIWAN_ROBE_S  = (0x58, 0x46, 0x34, 0xFF)
OBIWAN_HAIR    = (0x8A, 0x6A, 0x4A, 0xFF)
OBIWAN_BEARD   = (0x7A, 0x5E, 0x42, 0xFF)

def paint_obi_wan(rgba):
    fill(rgba, OBIWAN_TUNIC)
    rect(rgba, 0, 0, 32, 8, OBIWAN_HAIR)   # head top: auburn-gray hair
    rect(rgba, 8, 8, 16, 16, SKIN)         # face
    rect(rgba, 8, 8, 16, 10, OBIWAN_HAIR)  # hairline
    rect(rgba, 10, 11, 11, 12, (0x20, 0x30, 0x50, 0xFF))  # eyes
    rect(rgba, 13, 11, 14, 12, (0x20, 0x30, 0x50, 0xFF))
    rect(rgba, 8, 13, 16, 16, OBIWAN_BEARD)  # full beard, lower face rows 13..16
    rect(rgba, 32, 0, 64, 16, OBIWAN_ROBE)   # hood overlay strip
    rect(rgba, 40, 8, 48, 16, (0, 0, 0, 0))  # hood front opening (transparent)
    rect(rgba, 16, 16, 40, 32, OBIWAN_TUNIC) # body
    rect(rgba, 20, 28, 36, 30, BELT)         # belt
    rect(rgba, 16, 30, 40, 32, OBIWAN_TUNIC_S)
    for (u0, v0) in ((40, 16), (32, 48)):    # arms: robe sleeves
        rect(rgba, u0, v0, u0 + 16, v0 + 16, OBIWAN_ROBE)
        rect(rgba, u0, v0 + 13, u0 + 16, v0 + 16, OBIWAN_ROBE_S)
    for (u0, v0) in ((0, 16), (16, 48)):     # legs: dark trousers
        rect(rgba, u0, v0, u0 + 16, v0 + 16, (0x50, 0x46, 0x38, 0xFF))
        rect(rgba, u0, v0 + 14, u0 + 16, v0 + 16, (0x38, 0x30, 0x26, 0xFF))
    rect(rgba, 32, 32, 64, 48, OBIWAN_ROBE)  # robe skirt region (u32.., v32..)
    rect(rgba, 32, 45, 64, 48, OBIWAN_ROBE_S)

# Astromech droid: white body w/ blue vertical panel stripes + vents, blue
# dome w/ a silver ring band + black eye lens, silver legs w/ a dark joint row.
ASTRO_WHITE     = (0xE8, 0xE8, 0xEC, 0xFF)
ASTRO_WHITE_HI  = (0xFA, 0xFA, 0xFC, 0xFF)
ASTRO_WHITE_SH  = (0xC4, 0xC4, 0xCC, 0xFF)
ASTRO_BLUE      = (0x28, 0x50, 0xA0, 0xFF)
ASTRO_BLUE_HI   = (0x40, 0x68, 0xB8, 0xFF)
ASTRO_VENT      = (0x90, 0x98, 0xA0, 0xFF)
ASTRO_SILVER    = (0xC0, 0xC8, 0xD0, 0xFF)
ASTRO_SILVER_SH = (0x90, 0x98, 0xA0, 0xFF)
ASTRO_JOINT     = (0x30, 0x34, 0x3A, 0xFF)
ASTRO_LENS      = (0x10, 0x10, 0x10, 0xFF)

def paint_astromech(rgba):
    fill(rgba, (0, 0, 0, 0))                  # custom UV layout — transparent base
    # Dome head, UV (0,0)..(32,12): blue dome, silver ring band, black eye lens.
    rect(rgba, 0, 0, 32, 12, ASTRO_BLUE)
    rect(rgba, 0, 0, 32, 1, ASTRO_BLUE_HI)      # top gloss
    rect(rgba, 0, 5, 32, 7, ASTRO_SILVER)       # silver ring band, wraps the dome
    rect(rgba, 0, 10, 32, 12, ASTRO_SILVER_SH)  # lower dome shade
    rect(rgba, 32, 0, 35, 3, ASTRO_LENS)        # eye lens
    # Body, UV (0,20)..(32,38): white with two blue vertical panel stripes + vents.
    rect(rgba, 0, 20, 32, 38, ASTRO_WHITE)
    rect(rgba, 0, 20, 32, 21, ASTRO_WHITE_HI)   # top highlight
    rect(rgba, 0, 36, 32, 38, ASTRO_WHITE_SH)   # bottom shade
    rect(rgba, 4, 22, 7, 36, ASTRO_BLUE)        # left panel stripe
    rect(rgba, 25, 22, 28, 36, ASTRO_BLUE)      # right panel stripe
    rect(rgba, 12, 24, 15, 27, ASTRO_VENT)      # vent rect
    rect(rgba, 17, 24, 20, 27, ASTRO_VENT)      # vent rect
    # Legs, UV (32,20).. (one 10x15 footprint per leg): silver w/ a dark joint row.
    for u0 in (32, 42):
        rect(rgba, u0, 20, u0 + 10, 35, ASTRO_SILVER)
        rect(rgba, u0, 20, u0 + 10, 21, ASTRO_WHITE_HI)   # top highlight sliver
        rect(rgba, u0, 26, u0 + 10, 27, ASTRO_JOINT)      # joint row
        rect(rgba, u0, 33, u0 + 10, 35, ASTRO_SILVER_SH)  # foot shade

# Boba Fett: sage-green armor plates over a gray flightsuit, a black T-visor
# on the green helmet, rust weathering dots, and a back-mounted jetpack with
# silver nozzles + red shoulder accent stripes.
BOBA_GREEN     = (0x5A, 0x6E, 0x50, 0xFF)
BOBA_GREEN_HI  = (0x72, 0x86, 0x66, 0xFF)
BOBA_GREEN_SH  = (0x44, 0x56, 0x3C, 0xFF)
BOBA_SUIT      = (0x60, 0x60, 0x5C, 0xFF)
BOBA_SUIT_SH   = (0x48, 0x48, 0x44, 0xFF)
BOBA_VISOR     = (0x10, 0x10, 0x12, 0xFF)
BOBA_RUST      = (0x7A, 0x4A, 0x30, 0xFF)
BOBA_SILVER    = (0xB0, 0xB4, 0xB8, 0xFF)
BOBA_RED       = (0xA0, 0x30, 0x28, 0xFF)
BOBA_RANGEFINDER = (0x30, 0x30, 0x34, 0xFF)

def paint_boba_fett(rgba):
    fill(rgba, BOBA_SUIT)
    # Head front (u 8..16, v 8..16): green faceplate with a black T-visor —
    # a 2px vertical bar down the face center (rows 10..15) crossed by a
    # 6px horizontal bar (rows 10..11).
    rect(rgba, 8, 8, 16, 16, BOBA_GREEN)
    rect(rgba, 11, 10, 13, 15, BOBA_VISOR)   # vertical bar
    rect(rgba, 9, 10, 15, 11, BOBA_VISOR)    # horizontal bar
    # Helmet-overlay region (u 32..64, v 0..16): green shell, gloss top,
    # shaded bottom rim.
    rect(rgba, 32, 0, 64, 16, BOBA_GREEN)
    rect(rgba, 32, 0, 64, 1, BOBA_GREEN_HI)
    rect(rgba, 32, 14, 64, 16, BOBA_GREEN_SH)
    # Rangefinder UV footprint (56,16)-(60,22): dark-gray strip.
    rect(rgba, 56, 16, 60, 22, BOBA_RANGEFINDER)
    # Body (u 16..40, v 16..32): sage-green chest plate over the flightsuit.
    rect(rgba, 16, 16, 40, 32, BOBA_GREEN)
    rect(rgba, 16, 16, 40, 17, BOBA_GREEN_HI)
    rect(rgba, 16, 30, 40, 32, BOBA_GREEN_SH)
    # Arms/legs: gray flightsuit, shaded lower edge.
    for (u0, v0) in ((40, 16), (32, 48), (0, 16), (16, 48)):
        rect(rgba, u0, v0, u0 + 16, v0 + 16, BOBA_SUIT)
        rect(rgba, u0, v0 + 14, u0 + 16, v0 + 16, BOBA_SUIT_SH)
    # Arms only: green shoulder plate + red accent stripe.
    for (u0, v0) in ((40, 16), (32, 48)):
        rect(rgba, u0, v0, u0 + 16, v0 + 4, BOBA_GREEN)
        rect(rgba, u0, v0 + 4, u0 + 16, v0 + 5, BOBA_RED)
    # Jetpack UV footprint (44..62, 32..43): green tanks, silver nozzle
    # rows, gloss top / shaded bottom.
    rect(rgba, 44, 32, 62, 43, BOBA_GREEN)
    rect(rgba, 44, 32, 62, 33, BOBA_GREEN_HI)
    rect(rgba, 47, 35, 50, 41, BOBA_SILVER)   # left tank nozzle
    rect(rgba, 56, 35, 59, 41, BOBA_SILVER)   # right tank nozzle
    rect(rgba, 44, 41, 62, 43, BOBA_GREEN_SH)
    # Weathering: 4 scattered rust dots on the chest + helmet.
    rect(rgba, 20, 20, 21, 21, BOBA_RUST)
    rect(rgba, 34, 24, 35, 25, BOBA_RUST)
    rect(rgba, 9, 9, 10, 10, BOBA_RUST)
    rect(rgba, 38, 5, 39, 6, BOBA_RUST)

# Han Solo: off-white shirt under a layered black vest (open front), navy
# trousers with a Corellian bloodstripe seam, a holstered blaster at the
# right hip, dark brown hair, and fair skin.
HAN_SHIRT      = (0xE8, 0xE0, 0xD0, 0xFF)   # off-white shirt
HAN_SHIRT_DIM  = (0xC9, 0xC0, 0xAE, 0xFF)   # shirt shadow
HAN_VEST       = (0x2B, 0x2B, 0x2B, 0xFF)   # black vest
HAN_VEST_HI    = (0x45, 0x45, 0x48, 0xFF)   # vest highlight
HAN_VEST_DK    = (0x17, 0x17, 0x19, 0xFF)   # vest deep shadow
HAN_TROUSER    = (0x2E, 0x3A, 0x52, 0xFF)   # navy trousers
HAN_TROUSER_DK = (0x1F, 0x28, 0x3A, 0xFF)
HAN_STRIPE     = (0xB0, 0x30, 0x30, 0xFF)   # Corellian bloodstripe (trouser seam)
HAN_SKIN       = (0xC8, 0x9E, 0x7A, 0xFF)
HAN_SKIN_DK    = (0xA8, 0x80, 0x60, 0xFF)
HAN_HAIR       = (0x4A, 0x35, 0x22, 0xFF)   # dark brown hair
HAN_BELT       = (0x5A, 0x40, 0x28, 0xFF)   # holster belt, right hip detail
HAN_BUCKLE     = (0x9A, 0x8A, 0x60, 0xFF)

def paint_han_solo(rgba):
    fill(rgba, HAN_HAIR)
    # Head: dark brown hair top/sides, fair skin face w/ jaw shading, eyes.
    rect(rgba, 0, 0, 32, 8, HAN_HAIR)          # head top: hair
    rect(rgba, 8, 8, 16, 16, HAN_SKIN)         # face
    rect(rgba, 8, 8, 16, 10, HAN_HAIR)         # hairline
    rect(rgba, 8, 13, 16, 16, HAN_SKIN_DK)     # jaw shading
    rect(rgba, 10, 11, 11, 12, (0x20, 0x30, 0x50, 0xFF))  # left eye
    rect(rgba, 13, 11, 14, 12, (0x20, 0x30, 0x50, 0xFF))  # right eye
    # Body: off-white shirt with a chest fold shadow, belt row + buckle at
    # the waist (the vest sits over this as a separate inflated cube).
    rect(rgba, 16, 16, 40, 32, HAN_SHIRT)
    rect(rgba, 16, 22, 40, 23, HAN_SHIRT_DIM)  # chest fold line
    rect(rgba, 16, 30, 40, 32, HAN_BELT)       # waist belt row
    rect(rgba, 26, 30, 30, 32, HAN_BUCKLE)     # belt buckle
    # Arms: shirt sleeves with a vest-shoulder-strap peek + elbow fold.
    for (u0, v0) in ((40, 16), (32, 48)):
        rect(rgba, u0, v0, u0 + 16, v0 + 16, HAN_SHIRT)
        rect(rgba, u0, v0, u0 + 16, v0 + 2, HAN_VEST)          # shoulder strap peek
        rect(rgba, u0, v0 + 6, u0 + 16, v0 + 7, HAN_SHIRT_DIM)  # elbow fold
    # Legs: navy trousers, inner shadow column, Corellian bloodstripe seam.
    for (u0, v0) in ((0, 16), (16, 48)):
        rect(rgba, u0, v0, u0 + 16, v0 + 16, HAN_TROUSER)
        rect(rgba, u0 + 1, v0, u0 + 3, v0 + 16, HAN_TROUSER_DK)  # inner shadow
        rect(rgba, u0 + 14, v0, u0 + 15, v0 + 16, HAN_STRIPE)    # bloodstripe
    # Right hip: holster drop + dark gun grip, over the right leg's top rows.
    rect(rgba, 0, 16, 2, 19, HAN_BELT)         # holster drop
    rect(rgba, 0, 19, 1, 20, HAN_VEST_DK)      # gun grip
    # Vest cube UV block (32,32..56,44): black vest body, highlight on the
    # top/lapel edge, deep shadow under the arms, and an open-front column
    # left transparent so the shirt shows through underneath.
    rect(rgba, 32, 32, 56, 44, HAN_VEST)
    rect(rgba, 32, 32, 56, 33, HAN_VEST_HI)    # top/lapel highlight
    rect(rgba, 32, 40, 56, 44, HAN_VEST_DK)    # under-arm deep shadow
    rect(rgba, 42, 33, 46, 44, (0, 0, 0, 0))   # open front (shirt shows through)

# Princess Leia: white senatorial robe/gown with painted folds, a
# silver-grey belt, side hair buns (geometry cubes), and fair skin.
LEIA_ROBE     = (0xF2, 0xEE, 0xE6, 0xFF)    # white senatorial robe
LEIA_ROBE_DIM = (0xD8, 0xD2, 0xC4, 0xFF)    # robe fold shadow
LEIA_ROBE_DK  = (0xB8, 0xB0, 0xA0, 0xFF)    # deep fold
LEIA_BELT     = (0x8A, 0x86, 0x7A, 0xFF)    # silver-grey belt
LEIA_SKIN     = (0xD8, 0xB0, 0x8E, 0xFF)
LEIA_SKIN_DK  = (0xB6, 0x92, 0x74, 0xFF)
LEIA_HAIR     = (0x5A, 0x40, 0x30, 0xFF)    # brown hair
LEIA_HAIR_DK  = (0x42, 0x2E, 0x22, 0xFF)    # bun shadow / parting
LEIA_HAIR_HI  = (0x74, 0x54, 0x40, 0xFF)    # bun top/outer highlight

def paint_princess_leia(rgba):
    fill(rgba, LEIA_HAIR)
    # Head: brown hair crown w/ a dark center part, skin face, jaw shading, eyes.
    rect(rgba, 0, 0, 32, 8, LEIA_HAIR)          # head top: hair crown
    rect(rgba, 8, 8, 16, 16, LEIA_SKIN)         # face
    rect(rgba, 8, 8, 16, 10, LEIA_HAIR)         # hairline
    rect(rgba, 11, 8, 13, 16, LEIA_HAIR_DK)     # center part
    rect(rgba, 8, 13, 16, 16, LEIA_SKIN_DK)     # jaw shading
    rect(rgba, 10, 11, 11, 12, (0x20, 0x30, 0x50, 0xFF))  # left eye
    rect(rgba, 13, 11, 14, 12, (0x20, 0x30, 0x50, 0xFF))  # right eye
    # Side hair buns (UV 54,0 and 54,6 — a 2x3x3 box unwraps 10px wide, so
    # u=54 puts the full footprint at 54..64, exactly on the canvas edge):
    # brown hair base, darker spiral hint on the inner edge, lighter
    # highlight along the top face row (3 tones).
    rect(rgba, 54, 0, 64, 6, LEIA_HAIR)
    rect(rgba, 54, 0, 56, 6, LEIA_HAIR_DK)     # spiral shadow
    rect(rgba, 56, 0, 64, 1, LEIA_HAIR_HI)     # top highlight
    rect(rgba, 54, 6, 64, 12, LEIA_HAIR)
    rect(rgba, 54, 6, 56, 12, LEIA_HAIR_DK)    # spiral shadow
    rect(rgba, 56, 6, 64, 7, LEIA_HAIR_HI)     # top highlight
    # Body: white senatorial robe, vertical fold shadows, under-arm shade,
    # silver-grey belt row at the waist.
    rect(rgba, 16, 16, 40, 32, LEIA_ROBE)
    rect(rgba, 22, 16, 23, 30, LEIA_ROBE_DIM)   # vertical fold
    rect(rgba, 30, 16, 31, 30, LEIA_ROBE_DIM)   # vertical fold
    rect(rgba, 16, 20, 18, 26, LEIA_ROBE_DK)    # under-arm shadow (left)
    rect(rgba, 38, 20, 40, 26, LEIA_ROBE_DK)    # under-arm shadow (right)
    rect(rgba, 16, 28, 40, 30, LEIA_BELT)       # waist belt row
    # Arms: robe sleeves with a top fold + darker fold-shadow line.
    for (u0, v0) in ((40, 16), (32, 48)):
        rect(rgba, u0, v0, u0 + 16, v0 + 16, LEIA_ROBE)
        rect(rgba, u0, v0, u0 + 16, v0 + 1, LEIA_ROBE_DIM)      # top fold
        rect(rgba, u0, v0 + 6, u0 + 16, v0 + 7, LEIA_ROBE_DK)   # fold shadow
    # Legs: robe hem continues down, top fold + darker lower shading.
    for (u0, v0) in ((0, 16), (16, 48)):
        rect(rgba, u0, v0, u0 + 16, v0 + 16, LEIA_ROBE)
        rect(rgba, u0, v0, u0 + 16, v0 + 1, LEIA_ROBE_DIM)
        rect(rgba, u0, v0 + 14, u0 + 16, v0 + 16, LEIA_ROBE_DK)
    # Robe skirt UV block (32,32..64,48): white robe base with narrow fold
    # columns — a 1px DIM fold every 4px, deepened to DK on every other fold
    # — so all three robe tones survive in the block.
    rect(rgba, 32, 32, 64, 48, LEIA_ROBE)
    for x in range(34, 64, 4):
        rect(rgba, x, 32, x + 1, 48, LEIA_ROBE_DIM)
    for x in range(38, 64, 8):
        rect(rgba, x, 32, x + 1, 48, LEIA_ROBE_DK)

MOBS = {
    'stormtrooper': paint_stormtrooper,
    'battle_droid': paint_battle_droid,
    'jedi_knight': paint_jedi_knight,
    'darth_vader': paint_darth_vader,
    'luke_skywalker': paint_luke_skywalker,
    'obi_wan': paint_obi_wan,
    'astromech': paint_astromech,
    'boba_fett': paint_boba_fett,
    'han_solo': paint_han_solo,
    'princess_leia': paint_princess_leia,
}

# Worn-armor equipment layers: standard 64x32 vanilla armor-sheet UV layout
# (same box UV as the humanoid skin's top half — head 0..32,0..16;
# body 16..40,16..32; arm 40..56,16..32; leg 0..16,16..32). Uses the same
# WHITE/WHITE_SH/SUIT_BLACK palette as paint_stormtrooper above.
ARMOR_W, ARMOR_H = 64, 32

def paint_stormtrooper_armor_layers(humanoid_rgba, leggings_rgba):
    """Paint the two worn-armor sheets for the stormtrooper equipment asset.

    Both sheets are left fully transparent outside the UV regions vanilla's
    box-UV format actually samples: for each body-part block, the top rows
    hold only an inset top/bottom-face strip (not the full block width) and
    the remaining rows hold the full-width wraparound strip. This mirrors
    the exact extents painted in craftee's own worn-armor sheets
    (assets/craftee/textures/entity/equipment/{humanoid,humanoid_leggings}/
    craftee.png) rather than flat-filling whole UV blocks.

    Layer 1 ("humanoid", assets/.../equipment/humanoid/stormtrooper.png)
    covers the helmet (head UV block), chestplate (body + arm UV blocks),
    and boots (leg UV block) — everything except leggings.

    Layer 2 ("humanoid_leggings", .../equipment/humanoid_leggings/
    stormtrooper.png) covers only the leg UV block, matching the visible
    extent of craftee's own humanoid_leggings/craftee.png (bbox (0,16)-
    (16,32) — the waist/body region is left transparent there too).

    Every region gets 3 white tones (top-highlight / base / bottom-shade)
    plus black inter-plate seams at the joints, with a sliver of GRAY
    under-suit showing through at each seam's end gaps.
    """
    fill_buf(humanoid_rgba, (0, 0, 0, 0))
    fill_buf(leggings_rgba, (0, 0, 0, 0))

    # ---- Head block (helmet), UV 0..32,0..16 ----
    # Top/bottom faces: y0..7, inset to x8..24 (craftee's exact extent),
    # 3-tone gradient left-to-right.
    rect(humanoid_rgba, 8, 0, 13, 8, WHITE_HI)
    rect(humanoid_rgba, 13, 0, 19, 8, WHITE)
    rect(humanoid_rgba, 19, 0, 24, 8, WHITE_SH)
    # Wraparound sides: y8..16, full x0..32.
    rect(humanoid_rgba, 0, 8, 32, 9, WHITE_HI)     # dome top highlight
    rect(humanoid_rgba, 0, 9, 32, 13, WHITE)       # base band
    rect(humanoid_rgba, 0, 13, 32, 15, WHITE_SH)   # lower dome shade
    rect(humanoid_rgba, 0, 15, 32, 16, GRAY)       # neck seam: under-suit peek
    # Front face detail (x8..16,y8..16): eye slits + aerator + chin vent.
    rect(humanoid_rgba, 9, 10, 11, 11, SUIT_BLACK)   # left eye slit
    rect(humanoid_rgba, 13, 10, 15, 11, SUIT_BLACK)  # right eye slit
    rect(humanoid_rgba, 10, 13, 14, 14, GRAY)        # aerator frown
    rect(humanoid_rgba, 11, 14, 13, 15, SUIT_BLACK)  # chin vent

    # ---- Body block (chestplate), UV 16..40,16..32 ----
    # Top/bottom faces: y16..19, inset to x20..28.
    rect(humanoid_rgba, 20, 16, 24, 20, WHITE_HI)
    rect(humanoid_rgba, 24, 16, 28, 20, WHITE_SH)
    # Wraparound: y20..32, full x16..40 — ab-plate seam (2 horizontal +
    # 1 vertical) with under-suit peeking through at the seam ends.
    rect(humanoid_rgba, 16, 20, 40, 21, WHITE_HI)    # shoulder top highlight
    rect(humanoid_rgba, 16, 21, 40, 26, WHITE)       # chest base
    rect(humanoid_rgba, 24, 21, 32, 24, GRAY)        # chest control box
    rect(humanoid_rgba, 16, 26, 40, 27, SUIT_BLACK)  # ab-plate seam 1
    rect(humanoid_rgba, 16, 26, 18, 27, GRAY)        # seam gap: under-suit peek
    rect(humanoid_rgba, 38, 26, 40, 27, GRAY)
    rect(humanoid_rgba, 27, 21, 29, 30, SUIT_BLACK)  # ab-plate vertical seam
    rect(humanoid_rgba, 16, 27, 40, 29, WHITE)       # lower ab plate
    rect(humanoid_rgba, 16, 29, 40, 30, SUIT_BLACK)  # ab-plate seam 2
    rect(humanoid_rgba, 16, 30, 40, 32, WHITE_SH)    # waist shade

    # ---- Arm block (chestplate sleeve), UV 40..56,16..32 ----
    # Top/bottom faces: y16..19, inset to x44..52.
    rect(humanoid_rgba, 44, 16, 48, 20, WHITE_HI)
    rect(humanoid_rgba, 48, 16, 52, 20, WHITE_SH)
    # Wraparound: y20..32, full x40..56 — elbow seam with under-suit peek.
    rect(humanoid_rgba, 40, 20, 56, 21, WHITE_HI)    # shoulder highlight
    rect(humanoid_rgba, 40, 21, 56, 25, WHITE)       # upper sleeve
    rect(humanoid_rgba, 40, 25, 56, 26, SUIT_BLACK)  # elbow seam
    rect(humanoid_rgba, 40, 25, 41, 26, GRAY)        # seam gap: under-suit peek
    rect(humanoid_rgba, 55, 25, 56, 26, GRAY)
    rect(humanoid_rgba, 40, 26, 56, 30, WHITE)       # forearm
    rect(humanoid_rgba, 40, 30, 56, 32, WHITE_SH)    # wrist shade

    # ---- Leg block (boots, on the humanoid sheet), UV 0..16,16..32 ----
    # Top/bottom faces: y16..19, inset to x4..12.
    rect(humanoid_rgba, 4, 16, 8, 20, WHITE_HI)
    rect(humanoid_rgba, 8, 16, 12, 20, WHITE_SH)
    # Wraparound: y20..32, full x0..16 — cuff seam, under-suit peek, sole.
    rect(humanoid_rgba, 0, 20, 16, 21, WHITE_HI)     # ankle top highlight
    rect(humanoid_rgba, 0, 21, 16, 23, WHITE)        # boot cuff
    rect(humanoid_rgba, 0, 23, 16, 24, SUIT_BLACK)   # cuff seam
    rect(humanoid_rgba, 0, 23, 1, 24, GRAY)          # seam gap: under-suit peek
    rect(humanoid_rgba, 15, 23, 16, 24, GRAY)
    rect(humanoid_rgba, 0, 24, 16, 29, WHITE_SH)     # boot body, shaded
    rect(humanoid_rgba, 0, 29, 16, 32, GRAY)         # sole, darker gray

    # ---- Leggings sheet: leg block only, UV 0..16,16..32 ----
    # Top/bottom faces: y16..19, inset to x4..12.
    rect(leggings_rgba, 4, 16, 8, 20, WHITE_HI)
    rect(leggings_rgba, 8, 16, 12, 20, WHITE_SH)
    # Wraparound: y20..32, full x0..16 — knee seam, thigh/shin tone split.
    rect(leggings_rgba, 0, 20, 16, 21, WHITE_HI)     # thigh top highlight
    rect(leggings_rgba, 0, 21, 16, 24, WHITE)        # thigh
    rect(leggings_rgba, 0, 24, 16, 25, SUIT_BLACK)   # knee seam
    rect(leggings_rgba, 0, 24, 1, 25, GRAY)          # seam gap: under-suit peek
    rect(leggings_rgba, 15, 24, 16, 25, GRAY)
    rect(leggings_rgba, 0, 25, 16, 30, WHITE_SH)     # shin (darker than thigh)
    rect(leggings_rgba, 0, 30, 16, 32, GRAY)         # ankle cuff, darker

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

    humanoid_dir = os.path.join(out_dir, 'equipment', 'humanoid')
    leggings_dir = os.path.join(out_dir, 'equipment', 'humanoid_leggings')
    os.makedirs(humanoid_dir, exist_ok=True)
    os.makedirs(leggings_dir, exist_ok=True)
    humanoid_rgba = bytearray(ARMOR_W * ARMOR_H * 4)
    leggings_rgba = bytearray(ARMOR_W * ARMOR_H * 4)
    paint_stormtrooper_armor_layers(humanoid_rgba, leggings_rgba)
    write_png(os.path.join(humanoid_dir, 'stormtrooper.png'), humanoid_rgba,
              width=ARMOR_W, height=ARMOR_H)
    write_png(os.path.join(leggings_dir, 'stormtrooper.png'), leggings_rgba,
              width=ARMOR_W, height=ARMOR_H)
    print('stormtrooper armor layers')
    print('OK')
