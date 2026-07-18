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

# Landspeeder (X-34 silhouette, spec §5.6): sand-orange hull w/ rust
# weathering blotches, dark open cockpit w/ a lighter seat-cushion tone, a
# 2-tone blue-gray windshield, and 3-tone gunmetal turbine pods each with a
# dark circular intake + a light rim highlight.
#
# UV LAYOUT (per gen_bbmodels.py's LANDSPEEDER_CUBES comment): the original
# single hull cube's box-uv footprint (2*(26+16)=84 wide) overflowed the
# 64px canvas by 20px — fixed (Task 13) by splitting it into hull_front +
# hull_rear (58x18 footprint each), painted here as two independent uv
# blocks with matching banding+rust so the seam between them (invisible —
# they abut seamlessly in world space) doesn't read as a visual break. The
# other 5 uv blocks are packed into the remaining y=[36,64) band in two
# columns (turbines left, nose/seats/windshield right) with zero overlaps;
# seat_right and turbine_r deliberately share seat_left/turbine_l's offset
# (mirror-image geometry, identical paint) rather than each claiming
# separate canvas space.
SPEEDER_BODY     = (0xC8, 0x86, 0x4A, 0xFF)  # sand-orange hull
SPEEDER_BODY_HI  = (0xE0, 0xA6, 0x6A, 0xFF)  # sun-lit top
SPEEDER_BODY_DK  = (0x9A, 0x64, 0x36, 0xFF)  # underside shadow
SPEEDER_RUST     = (0x7A, 0x52, 0x30, 0xFF)  # weathering blotches
SPEEDER_COCKPIT  = (0x2A, 0x2A, 0x30, 0xFF)  # dark cockpit interior / seats
SPEEDER_SEAT_HI  = (0x44, 0x44, 0x4C, 0xFF)
SPEEDER_GLASS    = (0xA8, 0xC8, 0xD8, 0xFF)  # windshield
SPEEDER_GLASS_HI = (0xD0, 0xE8, 0xF0, 0xFF)
SPEEDER_METAL    = (0x8A, 0x8A, 0x92, 0xFF)  # turbine housings
SPEEDER_METAL_DK = (0x5A, 0x5A, 0x62, 0xFF)  # turbine intake dark
SPEEDER_METAL_HI = (0xB4, 0xB4, 0xBC, 0xFF)

def _speeder_circle(rgba, cx, cy, r, color):
    """Small pixel-circle approximation (nested rows, half-open x ranges)
    used for the turbine intakes — reads as a dark disc, not a rectangle."""
    for dy in range(-r, r + 1):
        # Chord half-width shrinks toward the top/bottom rows.
        w = int(round((r * r - dy * dy) ** 0.5))
        if w <= 0:
            continue
        rect(rgba, cx - w, cy + dy, cx + w, cy + dy + 1, color)

def paint_landspeeder(rgba):
    fill(rgba, (0, 0, 0, 0))  # custom UV layout — transparent base

    # Hull front, UV (0,0)..(58,18): sand-orange base, sun-lit highlight
    # band across the top third, a dark shadow band across the bottom third
    # (underside), and rust weathering blotches. Painted with the exact same
    # banding treatment as hull_rear below so the seam between the two
    # cubes (invisible in world space — they abut exactly at z=0) doesn't
    # read as a visual break either.
    rect(rgba, 0, 0, 58, 18, SPEEDER_BODY)
    rect(rgba, 0, 0, 58, 6, SPEEDER_BODY_HI)    # sun-lit top third
    rect(rgba, 0, 12, 58, 18, SPEEDER_BODY_DK)  # underside shadow, bottom third
    for (bx, by, bw, bh) in ((6, 2, 2, 1), (33, 3, 1, 2), (47, 4, 2, 2), (10, 14, 2, 1)):
        rect(rgba, bx, by, bx + bw, by + bh, SPEEDER_RUST)

    # Hull rear, UV (0,18)..(58,36): same treatment as hull_front, shifted.
    rect(rgba, 0, 18, 58, 36, SPEEDER_BODY)
    rect(rgba, 0, 18, 58, 24, SPEEDER_BODY_HI)
    rect(rgba, 0, 30, 58, 36, SPEEDER_BODY_DK)
    for (bx, by, bw, bh) in ((18, 20, 2, 1), (44, 19, 1, 2), (10, 32, 2, 1), (40, 33, 1, 2)):
        rect(rgba, bx, by, bx + bw, by + bh, SPEEDER_RUST)

    # Turbine pods: gunmetal housing, rim highlight, dark circular intake.
    # Left column of the post-hull-split layout, x=[0,28).
    # turbine_c, UV (0,36)..(28,50).
    rect(rgba, 0, 36, 28, 50, SPEEDER_METAL)
    rect(rgba, 0, 36, 28, 37, SPEEDER_METAL_HI)   # rim highlight, top edge
    rect(rgba, 0, 49, 28, 50, SPEEDER_METAL_DK)
    _speeder_circle(rgba, 14, 43, 5, SPEEDER_METAL_HI)   # rim highlight
    _speeder_circle(rgba, 14, 43, 4, SPEEDER_METAL_DK)   # dark intake
    _speeder_circle(rgba, 14, 43, 1, SPEEDER_METAL)      # inner-hub glint

    # turbine_l/turbine_r (shared), UV (0,50)..(28,64).
    rect(rgba, 0, 50, 28, 64, SPEEDER_METAL)
    rect(rgba, 0, 50, 28, 51, SPEEDER_METAL_HI)
    rect(rgba, 0, 63, 28, 64, SPEEDER_METAL_DK)
    _speeder_circle(rgba, 14, 57, 5, SPEEDER_METAL_HI)   # rim highlight
    _speeder_circle(rgba, 14, 57, 4, SPEEDER_METAL_DK)   # dark intake
    _speeder_circle(rgba, 14, 57, 1, SPEEDER_METAL)      # inner-hub glint

    # Right column of the post-hull-split layout, x=[28,64).
    # Nose, UV (28,36)..(64,46): sand-orange wedge continuing the hull
    # tones, top highlight strip + a couple of weathering flecks.
    rect(rgba, 28, 36, 64, 46, SPEEDER_BODY)
    rect(rgba, 28, 36, 64, 39, SPEEDER_BODY_HI)
    rect(rgba, 28, 43, 64, 46, SPEEDER_BODY_DK)
    rect(rgba, 43, 40, 45, 41, SPEEDER_RUST)

    # Seats (seat_left/seat_right share this region), UV (28,46)..(52,54):
    # dark cockpit interior with a lighter seat-cushion highlight row.
    rect(rgba, 28, 46, 52, 54, SPEEDER_COCKPIT)
    rect(rgba, 28, 46, 52, 48, SPEEDER_SEAT_HI)

    # Windshield, UV (28,54)..(58,59): 2-tone glass — lighter top band (sky
    # reflection), darker base.
    rect(rgba, 28, 54, 58, 59, SPEEDER_GLASS)
    rect(rgba, 28, 54, 58, 56, SPEEDER_GLASS_HI)

# -----------------------------------------------------------------------------
# Task-14 mobs. Width-aware helpers: the originals above hardcode the 64x64
# canvas (rect() indexes with the global W), which the bantha/wampa (128x64)
# and bogwing (32x32) canvases can't use. All new painters go through these.
# -----------------------------------------------------------------------------

def rectb(rgba, bw, x0, y0, x1, y1, color):
    """rect() for an arbitrary buffer width `bw` (half-open in x and y)."""
    r, g, b, a = color
    for y in range(y0, y1):
        for x in range(x0, x1):
            i = 4 * (y * bw + x)
            rgba[i + 0] = r
            rgba[i + 1] = g
            rgba[i + 2] = b
            rgba[i + 3] = a

def shade_box(rgba, bw, u, v, w, h, d, base, hi, sh):
    """Paint the full box-UV footprint of a (w,h,d) cube at uv offset (u,v)
    with the finished-art minimum: base + top highlight + bottom shade.

    Layout (box UV): top/bottom faces occupy x[u+d, u+d+2w) rows [v, v+d);
    the side wraparound strip occupies x[u, u+2(w+d)) rows [v+d, v+d+h).
    The up face gets `hi`, the down face `sh`, the strip gets `base` with a
    1px `hi` top row and a shaded bottom (2px when the strip is tall enough).
    Mob painters layer their details on top of this.
    """
    rectb(rgba, bw, u + d, v, u + d + w, v + d, hi)            # up face
    rectb(rgba, bw, u + d + w, v, u + d + 2 * w, v + d, sh)    # down face
    strip_w = 2 * (w + d)
    rectb(rgba, bw, u, v + d, u + strip_w, v + d + h, base)    # side strip
    rectb(rgba, bw, u, v + d, u + strip_w, v + d + 1, hi)      # strip top row
    shade_rows = 2 if h >= 4 else 1
    rectb(rgba, bw, u, v + d + h - shade_rows, u + strip_w, v + d + h, sh)

def speckle(rgba, bw, x0, y0, x1, y1, color, mod=5, salt=0):
    """Deterministic scattered dots (hash pattern, no RNG) for fur streaks,
    scale noise, weathering, etc."""
    r, g, b, a = color
    for y in range(y0, y1):
        for x in range(x0, x1):
            if (x * 7 + y * 13 + salt) % mod == 0:
                i = 4 * (y * bw + x)
                rgba[i + 0] = r
                rgba[i + 1] = g
                rgba[i + 2] = b
                rgba[i + 3] = a

# Jawa: dark brown hooded robe, black face void with glowing yellow eyes,
# crossed light-tan bandolier straps.
# UV map (gen_bbmodels JAWA_CUBES == JawaModel.java): head 8x8x8 @(0,0);
# body 8x9x4 @(0,16); right_arm/left_arm 3x9x3 @(24,16)/(36,16);
# right_leg/left_leg 3x6x3 @(0,32)/(12,32).
JAWA_ROBE     = (0x52, 0x3A, 0x24, 0xFF)
JAWA_ROBE_HI  = (0x6B, 0x4E, 0x32, 0xFF)
JAWA_ROBE_SH  = (0x3A, 0x28, 0x18, 0xFF)
JAWA_VOID     = (0x0A, 0x08, 0x06, 0xFF)
JAWA_EYE      = (0xF2, 0xC1, 0x4E, 0xFF)   # glowing yellow
JAWA_EYE_CORE = (0xFF, 0xEE, 0x9A, 0xFF)   # hot core
JAWA_STRAP    = (0xC8, 0xB2, 0x8A, 0xFF)   # bandolier
JAWA_STRAP_SH = (0x9A, 0x86, 0x62, 0xFF)

def paint_jawa(rgba):
    fill(rgba, (0, 0, 0, 0))
    # Head 8x8x8 @(0,0): hood all around.
    shade_box(rgba, W, 0, 0, 8, 8, 8, JAWA_ROBE, JAWA_ROBE_HI, JAWA_ROBE_SH)
    speckle(rgba, W, 0, 8, 32, 16, JAWA_ROBE_SH, mod=7, salt=1)  # coarse weave
    # Face front (x8..16, y8..16): black void framed by a 1px hood rim,
    # two glowing yellow eyes with hot cores.
    rect(rgba, 9, 9, 15, 16, JAWA_VOID)
    rect(rgba, 10, 11, 12, 13, JAWA_EYE)
    rect(rgba, 13, 11, 15, 13, JAWA_EYE)
    rect(rgba, 10, 11, 11, 12, JAWA_EYE_CORE)
    rect(rgba, 13, 11, 14, 12, JAWA_EYE_CORE)
    # Body 8x9x4 @(0,16): robe + crossed bandolier straps on the front face
    # (x4..12 of the strip rows 20..29).
    shade_box(rgba, W, 0, 16, 8, 9, 4, JAWA_ROBE, JAWA_ROBE_HI, JAWA_ROBE_SH)
    speckle(rgba, W, 0, 20, 24, 29, JAWA_ROBE_SH, mod=7, salt=2)
    for i in range(8):                       # crossed straps, stepped diagonals
        x = 4 + i
        rect(rgba, x, 21 + i, x + 1, 22 + i, JAWA_STRAP)
        rect(rgba, x, 28 - i, x + 1, 29 - i, JAWA_STRAP)
    rect(rgba, 7, 24, 9, 25, JAWA_STRAP_SH)  # crossing-point shadow
    # Arms 3x9x3 @(24,16)/(36,16): robe sleeves, darker cuffs.
    for u0 in (24, 36):
        shade_box(rgba, W, u0, 16, 3, 9, 3, JAWA_ROBE, JAWA_ROBE_HI, JAWA_ROBE_SH)
        rect(rgba, u0, 26, u0 + 12, 28, JAWA_ROBE_SH)   # cuff
    # Legs 3x6x3 @(0,32)/(12,32): robe hem + near-black feet rows.
    for u0 in (0, 12):
        shade_box(rgba, W, u0, 32, 3, 6, 3, JAWA_ROBE, JAWA_ROBE_HI, JAWA_ROBE_SH)
        rect(rgba, u0, 39, u0 + 12, 41, JAWA_VOID)      # dark feet

# Tusken Raider: sand-tan head wraps w/ metal goggles + mouth spikes,
# layered tan/brown robes, dark gloves. Standard humanoid UV layout.
TUSKEN_WRAP    = (0xC8, 0xB2, 0x8A, 0xFF)
TUSKEN_WRAP_HI = (0xDE, 0xCC, 0xA6, 0xFF)
TUSKEN_WRAP_SH = (0xA6, 0x90, 0x6A, 0xFF)
TUSKEN_ROBE    = (0xA8, 0x92, 0x6C, 0xFF)
TUSKEN_ROBE_SH = (0x86, 0x72, 0x52, 0xFF)
TUSKEN_BROWN   = (0x6E, 0x58, 0x3E, 0xFF)
TUSKEN_METAL   = (0x8A, 0x84, 0x78, 0xFF)
TUSKEN_LENS    = (0x14, 0x12, 0x10, 0xFF)
TUSKEN_SPIKE   = (0x5A, 0x50, 0x44, 0xFF)
TUSKEN_GLOVE   = (0x3A, 0x30, 0x24, 0xFF)

def paint_tusken_raider(rgba):
    fill(rgba, TUSKEN_ROBE)
    # Head: sand-tan wraps with horizontal wrap seams.
    rect(rgba, 0, 0, 32, 8, TUSKEN_WRAP)          # top/bottom faces
    rect(rgba, 8, 0, 24, 1, TUSKEN_WRAP_HI)
    rect(rgba, 0, 8, 32, 16, TUSKEN_WRAP)         # wraparound sides
    rect(rgba, 0, 8, 32, 9, TUSKEN_WRAP_HI)
    for vy in (11, 14):                            # wrap seams all around
        rect(rgba, 0, vy, 32, vy + 1, TUSKEN_WRAP_SH)
    # Face front (8..16, 8..16): metal eye goggles + mouth spikes.
    rect(rgba, 9, 10, 12, 13, TUSKEN_METAL)       # left goggle ring
    rect(rgba, 12, 10, 15, 13, TUSKEN_METAL)      # right goggle ring
    rect(rgba, 10, 11, 11, 12, TUSKEN_LENS)       # left lens
    rect(rgba, 13, 11, 14, 12, TUSKEN_LENS)       # right lens
    rect(rgba, 11, 13, 13, 16, TUSKEN_SPIKE)      # breather block
    rect(rgba, 10, 14, 11, 16, TUSKEN_SPIKE)      # mouth spike L
    rect(rgba, 13, 14, 14, 16, TUSKEN_SPIKE)      # mouth spike R
    rect(rgba, 11, 14, 12, 15, TUSKEN_LENS)       # spike shadow
    # Wrap-shell overlay (32..64, 0..16, inflated head box): outer head-wrap
    # layer with a transparent front window (40..48, 8..16) so the goggles/
    # spikes on the base head show through.
    rect(rgba, 32, 0, 64, 16, TUSKEN_WRAP)
    rect(rgba, 40, 0, 48, 8, TUSKEN_WRAP_HI)      # up face
    rect(rgba, 48, 0, 56, 8, TUSKEN_WRAP_SH)      # down face
    rect(rgba, 32, 8, 64, 9, TUSKEN_WRAP_HI)      # crown highlight
    for vy in (11, 14):                            # wrap seams
        rect(rgba, 32, vy, 64, vy + 1, TUSKEN_WRAP_SH)
    rect(rgba, 32, 15, 64, 16, TUSKEN_BROWN)      # neck-wrap rim
    rect(rgba, 40, 8, 48, 16, (0, 0, 0, 0))       # front window (transparent)
    # chin_vent cube (56,16)-(64,19): breather spikes peeking below the wrap.
    rect(rgba, 56, 16, 64, 19, TUSKEN_SPIKE)
    rect(rgba, 56, 16, 64, 17, TUSKEN_METAL)
    rect(rgba, 60, 17, 61, 19, TUSKEN_LENS)
    # Body: layered tan/brown robes + bandolier + belt.
    rect(rgba, 16, 16, 40, 32, TUSKEN_ROBE)
    rect(rgba, 16, 16, 40, 17, TUSKEN_WRAP_HI)
    rect(rgba, 16, 20, 40, 22, TUSKEN_BROWN)      # shoulder layer seam
    rect(rgba, 22, 17, 26, 28, TUSKEN_BROWN)      # bandolier strap
    rect(rgba, 16, 27, 40, 29, TUSKEN_BROWN)      # belt
    rect(rgba, 27, 27, 30, 29, TUSKEN_METAL)      # buckle
    rect(rgba, 16, 30, 40, 32, TUSKEN_ROBE_SH)
    # Arms: robe sleeves, dark gloves on the wrist rows.
    for (u0, v0) in ((40, 16), (32, 48)):
        rect(rgba, u0, v0, u0 + 16, v0 + 16, TUSKEN_ROBE)
        rect(rgba, u0, v0, u0 + 16, v0 + 1, TUSKEN_WRAP_HI)
        rect(rgba, u0, v0 + 7, u0 + 16, v0 + 8, TUSKEN_ROBE_SH)   # sleeve fold
        rect(rgba, u0, v0 + 12, u0 + 16, v0 + 16, TUSKEN_GLOVE)   # gloves
    # Legs: wrapped tan with brown binding rows.
    for (u0, v0) in ((0, 16), (16, 48)):
        rect(rgba, u0, v0, u0 + 16, v0 + 16, TUSKEN_ROBE)
        rect(rgba, u0, v0, u0 + 16, v0 + 1, TUSKEN_WRAP_HI)
        for dy in (4, 8):
            rect(rgba, u0, v0 + dy, u0 + 16, v0 + dy + 1, TUSKEN_BROWN)
        rect(rgba, u0, v0 + 13, u0 + 16, v0 + 16, TUSKEN_ROBE_SH)  # boot wraps

# Rebel trooper (Endor/Yavin commando): tan shirt + olive combat vest,
# brown belt, tan helmet with band, determined face. Standard humanoid UV.
REBEL_SHIRT    = (0xC8, 0xB2, 0x8A, 0xFF)
REBEL_SHIRT_HI = (0xDE, 0xCC, 0xA6, 0xFF)
REBEL_SHIRT_SH = (0xA6, 0x90, 0x6A, 0xFF)
REBEL_VEST     = (0x66, 0x6B, 0x3F, 0xFF)   # olive
REBEL_VEST_HI  = (0x7A, 0x80, 0x50, 0xFF)
REBEL_VEST_SH  = (0x4E, 0x52, 0x30, 0xFF)
REBEL_HELMET   = (0xB4, 0x9E, 0x76, 0xFF)
REBEL_BELT     = (0x5A, 0x40, 0x28, 0xFF)
REBEL_TROUSER  = (0x8A, 0x78, 0x58, 0xFF)
REBEL_BOOT     = (0x4A, 0x38, 0x26, 0xFF)
REBEL_SKIN     = (0xE8, 0xC0, 0x98, 0xFF)
REBEL_SKIN_SH  = (0xC8, 0xA0, 0x7C, 0xFF)
REBEL_EYE      = (0x28, 0x30, 0x38, 0xFF)

def paint_rebel_trooper(rgba):
    fill(rgba, REBEL_SHIRT)
    # Head: tan helmet dome over a determined face strip.
    rect(rgba, 0, 0, 32, 8, REBEL_HELMET)         # helmet top/bottom faces
    rect(rgba, 8, 0, 24, 1, REBEL_SHIRT_HI)
    rect(rgba, 0, 8, 32, 12, REBEL_HELMET)        # helmet sides down to brow
    rect(rgba, 0, 8, 32, 9, REBEL_SHIRT_HI)       # dome highlight
    rect(rgba, 0, 11, 32, 12, REBEL_VEST_SH)      # helmet band (olive-dark)
    rect(rgba, 0, 12, 32, 16, REBEL_SKIN)         # face strip all around
    # Face front (8..16): brow shadow + eyes + set mouth = determined.
    rect(rgba, 8, 12, 16, 13, REBEL_SKIN_SH)      # brow shadow under band
    rect(rgba, 9, 13, 11, 14, REBEL_EYE)          # narrowed left eye
    rect(rgba, 13, 13, 15, 14, REBEL_EYE)         # narrowed right eye
    rect(rgba, 10, 15, 14, 16, REBEL_SKIN_SH)     # set jaw line
    # Helmet-shell overlay (32..64, 0..16, inflated head box): tan dome with
    # the olive band, open-faced — transparent front window (40..48, 8..16)
    # so the face strip shows through under the brim.
    rect(rgba, 32, 0, 64, 16, REBEL_HELMET)
    rect(rgba, 40, 0, 48, 8, REBEL_SHIRT_HI)      # up face
    rect(rgba, 48, 0, 56, 8, REBEL_SHIRT_SH)      # down face
    rect(rgba, 32, 8, 64, 9, REBEL_SHIRT_HI)      # dome highlight
    rect(rgba, 32, 11, 64, 12, REBEL_VEST_SH)     # helmet band
    rect(rgba, 32, 14, 64, 16, REBEL_SHIRT_SH)    # rim shade
    rect(rgba, 40, 8, 48, 16, (0, 0, 0, 0))       # front window (transparent)
    # chin_vent cube (56,16)-(64,19): brown chin strap.
    rect(rgba, 56, 16, 64, 19, REBEL_BELT)
    rect(rgba, 56, 16, 64, 17, REBEL_SHIRT_SH)
    rect(rgba, 59, 17, 61, 19, (0x9A, 0x8A, 0x60, 0xFF))  # strap buckle
    # Body: tan shirt under an olive combat vest + brown belt.
    rect(rgba, 16, 16, 40, 32, REBEL_SHIRT)
    rect(rgba, 16, 16, 40, 17, REBEL_SHIRT_HI)
    rect(rgba, 16, 18, 40, 28, REBEL_VEST)        # vest wrap
    rect(rgba, 16, 18, 40, 19, REBEL_VEST_HI)
    rect(rgba, 26, 19, 30, 27, REBEL_SHIRT)       # open vest front: shirt shows
    rect(rgba, 20, 20, 24, 23, REBEL_VEST_SH)     # chest pouch L
    rect(rgba, 32, 20, 36, 23, REBEL_VEST_SH)     # chest pouch R
    rect(rgba, 16, 26, 40, 28, REBEL_VEST_SH)     # vest hem shade
    rect(rgba, 16, 28, 40, 30, REBEL_BELT)        # brown belt
    rect(rgba, 26, 28, 29, 30, (0x9A, 0x8A, 0x60, 0xFF))  # buckle
    rect(rgba, 16, 30, 40, 32, REBEL_SHIRT_SH)
    # Arms: tan sleeves, rolled-cuff highlight, shaded wrists.
    for (u0, v0) in ((40, 16), (32, 48)):
        rect(rgba, u0, v0, u0 + 16, v0 + 16, REBEL_SHIRT)
        rect(rgba, u0, v0, u0 + 16, v0 + 1, REBEL_SHIRT_HI)
        rect(rgba, u0, v0 + 3, u0 + 16, v0 + 4, REBEL_VEST)     # shoulder strap
        rect(rgba, u0, v0 + 8, u0 + 16, v0 + 9, REBEL_SHIRT_SH) # elbow fold
        rect(rgba, u0, v0 + 14, u0 + 16, v0 + 16, REBEL_SHIRT_SH)
    # Legs: field trousers + dark boots.
    for (u0, v0) in ((0, 16), (16, 48)):
        rect(rgba, u0, v0, u0 + 16, v0 + 16, REBEL_TROUSER)
        rect(rgba, u0, v0, u0 + 16, v0 + 1, REBEL_SHIRT_HI)
        rect(rgba, u0, v0 + 6, u0 + 16, v0 + 7, REBEL_SHIRT_SH)  # knee seam
        rect(rgba, u0, v0 + 11, u0 + 16, v0 + 16, REBEL_BOOT)    # boots
        rect(rgba, u0, v0 + 11, u0 + 16, v0 + 12, REBEL_BELT)    # boot cuff

# Snowtrooper: stormtrooper-white armor w/ fabric-skirt shading on the lower
# body, a smooth helmet with a cold-blue visor slit + breather box. Reuses
# the stormtrooper rig (incl. helmet_shell overlay @(32,0) and chin_vent
# @(56,16)), so this paints the stormtrooper UV layout.
SNOW_WHITE    = (0xF2, 0xF2, 0xF2, 0xFF)
SNOW_WHITE_HI = (0xFF, 0xFF, 0xFF, 0xFF)
SNOW_WHITE_SH = (0xD2, 0xD4, 0xDA, 0xFF)
SNOW_FABRIC   = (0xE4, 0xE2, 0xDC, 0xFF)   # fabric skirt, warmer white
SNOW_FABRIC_SH= (0xC2, 0xBE, 0xB4, 0xFF)
SNOW_VISOR    = (0x4A, 0x74, 0x9E, 0xFF)   # cold-blue slit
SNOW_VISOR_DK = (0x2A, 0x44, 0x66, 0xFF)
SNOW_GRAY     = (0x8A, 0x8F, 0x99, 0xFF)

def paint_snowtrooper(rgba):
    fill(rgba, SNOW_WHITE)
    # Head front (8..16, 8..16): smooth faceplate, cold-blue visor slit.
    rect(rgba, 8, 8, 16, 16, SNOW_WHITE_HI)
    rect(rgba, 9, 10, 15, 11, SNOW_VISOR)        # visor slit
    rect(rgba, 12, 10, 15, 11, SNOW_VISOR_DK)    # slit gradient
    rect(rgba, 10, 13, 14, 15, SNOW_GRAY)        # breather box
    rect(rgba, 11, 14, 13, 15, SNOW_VISOR_DK)    # breather intake
    # Helmet-overlay region (32..64, 0..16): smooth white shell.
    rect(rgba, 32, 0, 64, 16, SNOW_WHITE)
    rect(rgba, 32, 0, 64, 1, SNOW_WHITE_HI)
    rect(rgba, 40, 9, 56, 10, SNOW_VISOR)        # visor slit on the shell front
    rect(rgba, 32, 14, 64, 16, SNOW_WHITE_SH)
    # chin_vent cube UV (56,16)-(64,19): gray breather.
    rect(rgba, 56, 16, 64, 19, SNOW_GRAY)
    rect(rgba, 56, 16, 64, 17, SNOW_WHITE_SH)
    # Body: armor chest over a fabric skirt on the lower rows.
    rect(rgba, 16, 16, 40, 32, SNOW_WHITE)
    rect(rgba, 16, 16, 40, 17, SNOW_WHITE_HI)
    rect(rgba, 26, 20, 30, 24, SNOW_GRAY)        # chest control box
    rect(rgba, 16, 24, 40, 32, SNOW_FABRIC)      # fabric skirt wrap
    for x in range(18, 40, 4):                   # skirt fold shading
        rect(rgba, x, 25, x + 1, 32, SNOW_FABRIC_SH)
    rect(rgba, 16, 24, 40, 25, SNOW_WHITE_SH)    # armor/fabric seam
    # Arms/legs: white armor, seams + snow-fabric cuffs.
    for (u0, v0) in ((40, 16), (32, 48), (0, 16), (16, 48)):
        rect(rgba, u0, v0, u0 + 16, v0 + 16, SNOW_WHITE)
        rect(rgba, u0, v0, u0 + 16, v0 + 1, SNOW_WHITE_HI)
        rect(rgba, u0, v0 + 6, u0 + 16, v0 + 7, SNOW_FABRIC_SH)  # joint seam
        rect(rgba, u0, v0 + 12, u0 + 16, v0 + 14, SNOW_FABRIC)   # fabric cuff
        rect(rgba, u0, v0 + 14, u0 + 16, v0 + 16, SNOW_WHITE_SH)
    rect(rgba, 30, 8, 32, 16, SNOW_WHITE_SH)     # head edge shade
    rect(rgba, 16, 30, 40, 32, SNOW_FABRIC_SH)   # skirt hem shade

# Bantha (128x64): shaggy chocolate-brown fur w/ streak banding, cream
# muzzle, dark curved horns, darker legs, wool skirt overlay.
# UV: body 14x10x22 @(0,0); head 10x9x10 @(72,0); right_horn/left_horn 2x2x6
# @(112,0)/(112,8); leg0..3 4x14x4 @(96,19)/(112,19)/(96,37)/(112,37);
# wool_skirt 16x5x24 @(0,32).
BANTHA_FUR    = (0x6B, 0x4F, 0x35, 0xFF)   # chocolate brown
BANTHA_FUR_HI = (0x86, 0x66, 0x46, 0xFF)
BANTHA_FUR_SH = (0x50, 0x3A, 0x26, 0xFF)
BANTHA_FUR_DK = (0x3C, 0x2B, 0x1C, 0xFF)   # deepest streaks / leg shade
BANTHA_CREAM  = (0xE8, 0xDC, 0xC0, 0xFF)   # muzzle
BANTHA_HORN   = (0x4A, 0x3E, 0x30, 0xFF)
BANTHA_HORN_HI= (0x66, 0x58, 0x46, 0xFF)
BANTHA_HORN_DK= (0x2E, 0x26, 0x1C, 0xFF)
BANTHA_EYE    = (0x18, 0x12, 0x0C, 0xFF)

def paint_bantha(rgba):
    bw = 128
    fill_buf(rgba, (0, 0, 0, 0))
    # Body @(0,0), fp 72x32: shaggy fur with horizontal streak banding.
    shade_box(rgba, bw, 0, 0, 14, 10, 22, BANTHA_FUR, BANTHA_FUR_HI, BANTHA_FUR_SH)
    for vy in range(24, 32, 3):                      # streak bands on the strip
        band = BANTHA_FUR_SH if (vy // 3) % 2 else BANTHA_FUR_DK
        rectb(rgba, bw, 0, vy, 72, vy + 1, band)
    speckle(rgba, bw, 0, 22, 72, 32, BANTHA_FUR_HI, mod=6, salt=3)
    speckle(rgba, bw, 22, 0, 36, 22, BANTHA_FUR_SH, mod=5, salt=4)  # up-face shag
    # Head @(72,0), fp 40x19: fur + cream muzzle + eyes on the front face
    # (x82..92, rows 10..19).
    shade_box(rgba, bw, 72, 0, 10, 9, 10, BANTHA_FUR, BANTHA_FUR_HI, BANTHA_FUR_SH)
    speckle(rgba, bw, 72, 10, 112, 19, BANTHA_FUR_SH, mod=6, salt=5)
    rectb(rgba, bw, 83, 14, 91, 19, BANTHA_CREAM)    # cream muzzle
    rectb(rgba, bw, 83, 18, 91, 19, (0xC6, 0xB8, 0x9A, 0xFF))  # muzzle shade
    rectb(rgba, bw, 85, 16, 86, 18, BANTHA_FUR_SH)   # nostril L
    rectb(rgba, bw, 88, 16, 89, 18, BANTHA_FUR_SH)   # nostril R
    rectb(rgba, bw, 83, 11, 85, 13, BANTHA_EYE)      # eye L
    rectb(rgba, bw, 89, 11, 91, 13, BANTHA_EYE)      # eye R
    rectb(rgba, bw, 84, 11, 85, 12, BANTHA_FUR_HI)   # eye glint
    rectb(rgba, bw, 89, 11, 90, 12, BANTHA_FUR_HI)
    # Horns @(112,0)/(112,8), fp 16x8 each: 3-tone horn, ridge rings, dark tip.
    for v0 in (0, 8):
        shade_box(rgba, bw, 112, v0, 2, 2, 6, BANTHA_HORN, BANTHA_HORN_HI, BANTHA_HORN_DK)
        for hx in (114, 117, 120, 123):              # ridge rings along z
            rectb(rgba, bw, hx, v0 + 6, hx + 1, v0 + 8, BANTHA_HORN_DK)
        rectb(rgba, bw, 118, v0 + 6, 120, v0 + 8, BANTHA_HORN_DK)  # front tip
    # Legs 4x14x4, fp 16x18: darker fur, banding, near-black hoof rows.
    for (u0, v0) in ((96, 19), (112, 19), (96, 37), (112, 37)):
        shade_box(rgba, bw, u0, v0, 4, 14, 4, BANTHA_FUR_SH, BANTHA_FUR, BANTHA_FUR_DK)
        speckle(rgba, bw, u0, v0 + 4, u0 + 16, v0 + 14, BANTHA_FUR_DK, mod=5, salt=u0 + v0)
        rectb(rgba, bw, u0, v0 + 15, u0 + 16, v0 + 18, BANTHA_FUR_DK)   # hoof
        rectb(rgba, bw, u0, v0 + 15, u0 + 16, v0 + 16, BANTHA_HORN)     # hoof rim
    # Wool skirt @(0,32), fp 80x29: extra-shaggy wool — deep vertical
    # streaks. Speckle stays inside the box-UV's sampled pixels (top faces
    # x24..56 rows 32..56, strip x0..80 rows 56..61) so it can't splatter
    # into the leg blocks that pack into the footprint's unused corners.
    shade_box(rgba, bw, 0, 32, 16, 5, 24, BANTHA_FUR, BANTHA_FUR_HI, BANTHA_FUR_SH)
    for x in range(0, 80, 2):                        # hanging wool strands
        c = BANTHA_FUR_DK if (x // 2) % 3 == 0 else BANTHA_FUR_SH
        rectb(rgba, bw, x, 58, x + 1, 61, c)
    speckle(rgba, bw, 24, 32, 56, 56, BANTHA_FUR_SH, mod=4, salt=6)
    speckle(rgba, bw, 0, 56, 80, 58, BANTHA_FUR_SH, mod=4, salt=6)

# Tauntaun (64x64): gray-taupe hide, cream belly, darker dorsal stripe with
# spots, dark horns/claws.
# UV: body 8x10x14 @(0,0); neck 4x6x4 @(44,0); right_arm/left_arm 3x6x3
# @(44,10)/(44,19); head 6x6x8 @(0,24); tail 3x3x8 @(28,28);
# right_hind/left_hind 4x12x4 @(0,40)/(16,40).
TAUN_HIDE    = (0x9A, 0x8C, 0x7A, 0xFF)   # gray-taupe
TAUN_HIDE_HI = (0xB2, 0xA6, 0x94, 0xFF)
TAUN_HIDE_SH = (0x7A, 0x6E, 0x5E, 0xFF)
TAUN_BELLY   = (0xE8, 0xDC, 0xC0, 0xFF)   # cream
TAUN_STRIPE  = (0x5E, 0x52, 0x44, 0xFF)   # dorsal stripe
TAUN_DARK    = (0x3E, 0x36, 0x2C, 0xFF)   # horns / claws
TAUN_EYE     = (0x1C, 0x16, 0x10, 0xFF)

def paint_tauntaun(rgba):
    fill(rgba, (0, 0, 0, 0))
    # Body @(0,0), fp 44x24: hide, dorsal stripe down the up face (x14..22,
    # rows 0..14), spots along it, cream belly on the down face + lower strip.
    shade_box(rgba, W, 0, 0, 8, 10, 14, TAUN_HIDE, TAUN_HIDE_HI, TAUN_HIDE_SH)
    rect(rgba, 17, 0, 19, 14, TAUN_STRIPE)          # dorsal stripe (up face)
    for y in range(1, 13, 3):                       # small flanking spots
        rect(rgba, 15, y, 16, y + 1, TAUN_STRIPE)
        rect(rgba, 20, y + 1, 21, y + 2, TAUN_STRIPE)
    rect(rgba, 22, 0, 30, 14, TAUN_BELLY)           # belly (down face)
    rect(rgba, 0, 20, 44, 22, TAUN_BELLY)           # belly rows on the strip
    rect(rgba, 0, 22, 44, 24, (0xC6, 0xB8, 0x9A, 0xFF))  # belly shade
    speckle(rgba, W, 0, 14, 44, 20, TAUN_HIDE_SH, mod=6, salt=7)
    # Neck @(44,0), fp 16x10: hide + stripe continuation on the up face.
    shade_box(rgba, W, 44, 0, 4, 6, 4, TAUN_HIDE, TAUN_HIDE_HI, TAUN_HIDE_SH)
    rect(rgba, 49, 0, 51, 4, TAUN_STRIPE)
    speckle(rgba, W, 44, 4, 60, 10, TAUN_HIDE_SH, mod=5, salt=8)
    # Arms @(44,10)/(44,19), fp 12x9: hide, dark claw tips.
    for v0 in (10, 19):
        shade_box(rgba, W, 44, v0, 3, 6, 3, TAUN_HIDE, TAUN_HIDE_HI, TAUN_HIDE_SH)
        rect(rgba, 44, v0 + 8, 56, v0 + 9, TAUN_DARK)   # claws
    # Head @(0,24), fp 28x14: hide, horn hints, eyes + nostrils on the front
    # face (x8..14, rows 32..38).
    shade_box(rgba, W, 0, 24, 6, 6, 8, TAUN_HIDE, TAUN_HIDE_HI, TAUN_HIDE_SH)
    rect(rgba, 8, 24, 10, 27, TAUN_DARK)            # horn base L (up face)
    rect(rgba, 12, 24, 14, 27, TAUN_DARK)           # horn base R
    rect(rgba, 8, 24, 9, 25, TAUN_HIDE_HI)          # horn glint
    rect(rgba, 9, 33, 10, 34, TAUN_EYE)             # eye L
    rect(rgba, 12, 33, 13, 34, TAUN_EYE)            # eye R
    rect(rgba, 9, 36, 13, 38, TAUN_BELLY)           # cream snout
    rect(rgba, 10, 36, 11, 37, TAUN_STRIPE)         # nostril
    rect(rgba, 11, 36, 12, 37, TAUN_STRIPE)
    # Tail @(28,28), fp 22x11: hide, stripe along the up face (x36..39,
    # rows 28..36), dark tip on the back face (x47..50, rows 36..39).
    shade_box(rgba, W, 28, 28, 3, 3, 8, TAUN_HIDE, TAUN_HIDE_HI, TAUN_HIDE_SH)
    rect(rgba, 37, 28, 38, 36, TAUN_STRIPE)         # up-face stripe
    rect(rgba, 47, 36, 50, 39, TAUN_HIDE_SH)        # tail tip shade (back face)
    # Hind legs @(0,40)/(16,40), fp 16x16: hide, hock banding, dark claws.
    for u0 in (0, 16):
        shade_box(rgba, W, u0, 40, 4, 12, 4, TAUN_HIDE, TAUN_HIDE_HI, TAUN_HIDE_SH)
        speckle(rgba, W, u0, 44, u0 + 16, 52, TAUN_HIDE_SH, mod=5, salt=u0)
        rect(rgba, u0, 49, u0 + 16, 50, TAUN_HIDE_SH)   # hock band
        rect(rgba, u0, 54, u0 + 16, 56, TAUN_DARK)      # claws

# Wampa (128x64): off-white shaggy fur w/ gray shading, blood-red claw tips,
# dark horns, fanged mouth.
# UV: body 14x12x8 @(0,0); head 10x8x8 @(44,0); right_horn/left_horn 2x3x2
# @(80,0)/(88,0); right_arm/left_arm 5x14x5 @(0,20)/(20,20);
# right_leg/left_leg 5x10x5 @(40,20)/(60,20).
WAMPA_FUR    = (0xF0, 0xED, 0xE6, 0xFF)   # off-white
WAMPA_FUR_HI = (0xFC, 0xFB, 0xF8, 0xFF)
WAMPA_FUR_SH = (0xC8, 0xC6, 0xC2, 0xFF)   # gray shading
WAMPA_FUR_DK = (0x9E, 0x9C, 0x9A, 0xFF)   # deep shag
WAMPA_HORN   = (0x4A, 0x40, 0x36, 0xFF)
WAMPA_HORN_HI= (0x6A, 0x5E, 0x50, 0xFF)
WAMPA_HORN_DK= (0x2E, 0x27, 0x20, 0xFF)
WAMPA_CLAW   = (0xB0, 0x30, 0x30, 0xFF)   # blood-red
WAMPA_CLAW_DK= (0x7A, 0x1E, 0x1E, 0xFF)
WAMPA_MOUTH  = (0x3A, 0x14, 0x14, 0xFF)
WAMPA_EYE    = (0x18, 0x14, 0x12, 0xFF)

def paint_wampa(rgba):
    bw = 128
    fill_buf(rgba, (0, 0, 0, 0))
    # Body @(0,0), fp 44x20: shaggy off-white fur, gray streaks.
    shade_box(rgba, bw, 0, 0, 14, 12, 8, WAMPA_FUR, WAMPA_FUR_HI, WAMPA_FUR_SH)
    speckle(rgba, bw, 0, 8, 44, 20, WAMPA_FUR_SH, mod=4, salt=9)
    speckle(rgba, bw, 0, 8, 44, 20, WAMPA_FUR_DK, mod=11, salt=10)
    for x in range(2, 44, 5):                       # hanging shag strands
        rectb(rgba, bw, x, 17, x + 1, 20, WAMPA_FUR_DK)
    # Head @(44,0), fp 36x16: fur; face on the front face (x52..62, rows
    # 8..16): sunken eyes, wide fanged mouth.
    shade_box(rgba, bw, 44, 0, 10, 8, 8, WAMPA_FUR, WAMPA_FUR_HI, WAMPA_FUR_SH)
    speckle(rgba, bw, 44, 8, 80, 16, WAMPA_FUR_SH, mod=5, salt=11)
    rectb(rgba, bw, 53, 9, 55, 11, WAMPA_EYE)       # eye L
    rectb(rgba, bw, 59, 9, 61, 11, WAMPA_EYE)       # eye R
    rectb(rgba, bw, 53, 9, 54, 10, WAMPA_FUR_DK)    # brow shadow
    rectb(rgba, bw, 60, 9, 61, 10, WAMPA_FUR_DK)
    rectb(rgba, bw, 53, 12, 61, 15, WAMPA_MOUTH)    # open mouth
    for fx in (53, 55, 57, 59):                     # upper fangs
        rectb(rgba, bw, fx, 12, fx + 1, 14, WAMPA_FUR_HI)
    rectb(rgba, bw, 54, 14, 55, 15, WAMPA_FUR_HI)   # lower fang
    rectb(rgba, bw, 58, 14, 59, 15, WAMPA_FUR_HI)
    rectb(rgba, bw, 53, 15, 61, 16, WAMPA_FUR_SH)   # chin
    # Horns @(80,0)/(88,0), fp 8x5: dark 3-tone horns.
    for u0 in (80, 88):
        shade_box(rgba, bw, u0, 0, 2, 3, 2, WAMPA_HORN, WAMPA_HORN_HI, WAMPA_HORN_DK)
        rectb(rgba, bw, u0, 4, u0 + 8, 5, WAMPA_HORN_DK)  # tip ring
    # Arms @(0,20)/(20,20), fp 20x19: fur + blood-red claw tips on the
    # bottom rows (3 claw stripes each on the front face).
    for u0 in (0, 20):
        shade_box(rgba, bw, u0, 20, 5, 14, 5, WAMPA_FUR, WAMPA_FUR_HI, WAMPA_FUR_SH)
        speckle(rgba, bw, u0, 25, u0 + 20, 37, WAMPA_FUR_SH, mod=5, salt=u0 + 1)
        for cx in (u0 + 6, u0 + 8, u0 + 10):        # claws on the front face
            rectb(rgba, bw, cx, 36, cx + 1, 39, WAMPA_CLAW)
            rectb(rgba, bw, cx, 38, cx + 1, 39, WAMPA_CLAW_DK)
    # Legs @(40,20)/(60,20), fp 20x15: fur + red toe-claw stripes.
    for u0 in (40, 60):
        shade_box(rgba, bw, u0, 20, 5, 10, 5, WAMPA_FUR, WAMPA_FUR_HI, WAMPA_FUR_SH)
        speckle(rgba, bw, u0, 25, u0 + 20, 33, WAMPA_FUR_SH, mod=5, salt=u0 + 2)
        for cx in (u0 + 6, u0 + 8, u0 + 10):        # toe claws
            rectb(rgba, bw, cx, 33, cx + 1, 35, WAMPA_CLAW)

# Probe droid (64x64): near-black/gunmetal pod w/ panel lines, single RED
# eye lens (bright + glow ring), gray legs.
# UV: pod 10x8x10 @(0,0); eye 4x4x2 @(40,0); antenna 1x6x1 @(52,0);
# leg0..3 1x8x1 @(0,18)/(4,18)/(8,18)/(12,18).
PROBE_POD      = (0x33, 0x36, 0x3B, 0xFF)   # gunmetal
PROBE_POD_HI   = (0x4A, 0x4E, 0x56, 0xFF)
PROBE_POD_DK   = (0x1E, 0x20, 0x24, 0xFF)   # near-black panel lines
PROBE_LEG      = (0x6A, 0x6E, 0x76, 0xFF)   # gray
PROBE_LEG_DK   = (0x44, 0x48, 0x4E, 0xFF)
PROBE_RED      = (0xE3, 0x3B, 0x3B, 0xFF)   # bright lens
PROBE_RED_GLOW = (0x8A, 0x1E, 0x1E, 0xFF)   # glow ring
PROBE_RED_HOT  = (0xFF, 0x8A, 0x7A, 0xFF)   # lens hot-spot

def paint_probe_droid(rgba):
    fill(rgba, (0, 0, 0, 0))
    # Pod @(0,0), fp 40x18: gunmetal with panel lines + rivet dots.
    shade_box(rgba, W, 0, 0, 10, 8, 10, PROBE_POD, PROBE_POD_HI, PROBE_POD_DK)
    for x in (5, 15, 25, 35):                       # vertical panel lines
        rect(rgba, x, 10, x + 1, 18, PROBE_POD_DK)
    rect(rgba, 0, 13, 40, 14, PROBE_POD_DK)         # horizontal panel line
    speckle(rgba, W, 0, 10, 40, 18, PROBE_POD_HI, mod=9, salt=12)  # rivets
    rect(rgba, 10, 0, 20, 10, PROBE_POD_HI)         # up face
    rect(rgba, 13, 3, 17, 7, PROBE_POD)             # dome hatch on top
    rect(rgba, 20, 0, 30, 10, PROBE_POD_DK)         # down face, darkest
    rect(rgba, 23, 3, 27, 7, PROBE_LEG_DK)          # belly sensor plate
    # Eye @(40,0), fp 12x6: gunmetal housing; front face (x42..46, rows 2..6)
    # = bright red lens with glow ring + hot-spot.
    shade_box(rgba, W, 40, 0, 4, 4, 2, PROBE_POD, PROBE_POD_HI, PROBE_POD_DK)
    rect(rgba, 42, 2, 46, 6, PROBE_RED_GLOW)        # glow ring
    rect(rgba, 43, 3, 45, 5, PROBE_RED)             # bright lens
    rect(rgba, 43, 3, 44, 4, PROBE_RED_HOT)         # hot-spot
    # Antenna @(52,0), fp 4x7: gray mast, dark tip band.
    shade_box(rgba, W, 52, 0, 1, 6, 1, PROBE_LEG, PROBE_POD_HI, PROBE_LEG_DK)
    rect(rgba, 52, 1, 56, 2, PROBE_POD_DK)
    # Legs @(0..16,18), fp 4x9 each: gray manipulator arms, dark joints.
    for u0 in (0, 4, 8, 12):
        shade_box(rgba, W, u0, 18, 1, 8, 1, PROBE_LEG, PROBE_POD_HI, PROBE_LEG_DK)
        rect(rgba, u0, 22, u0 + 4, 23, PROBE_POD_DK)   # knee joint
        rect(rgba, u0, 25, u0 + 4, 26, PROBE_POD_DK)   # claw joint

# Dragonsnake (64x64): murky green scales w/ darker diamond back pattern,
# pale belly, red eyes.
# UV: head 6x4x8 @(0,0); seg0 5x4x8 @(28,0); seg1 @(0,12); seg2 @(26,12);
# seg3 @(0,24).
DSNAKE_SCALE    = (0x4A, 0x6B, 0x3A, 0xFF)   # murky green
DSNAKE_SCALE_HI = (0x60, 0x84, 0x4C, 0xFF)
DSNAKE_SCALE_SH = (0x36, 0x50, 0x2A, 0xFF)
DSNAKE_DIAMOND  = (0x23, 0x3A, 0x1E, 0xFF)   # dark back pattern
DSNAKE_BELLY    = (0xB8, 0xB4, 0x8A, 0xFF)   # pale belly
DSNAKE_EYE      = (0xC0, 0x28, 0x28, 0xFF)   # red
DSNAKE_MOUTH    = (0x2A, 0x1E, 0x14, 0xFF)

def _dsnake_diamonds(rgba, x0, y0, x1, y1, salt):
    """Dark zigzag diamond stripe down the center of an up-face region."""
    cx = (x0 + x1) // 2
    for y in range(y0, y1):
        off = (0, 1, 0, -1)[(y + salt) % 4]
        rect(rgba, cx + off - 1, y, cx + off + 1, y + 1, DSNAKE_DIAMOND)

def paint_dragonsnake(rgba):
    fill(rgba, (0, 0, 0, 0))
    # (u, v, cube width) per box — head is 6 wide, segs are 5 wide.
    segs = [(0, 0, 6), (28, 0, 5), (0, 12, 5), (26, 12, 5), (0, 24, 5)]
    for i, (u0, v0, w0) in enumerate(segs):
        shade_box(rgba, W, u0, v0, w0, 4, 8, DSNAKE_SCALE, DSNAKE_SCALE_HI, DSNAKE_SCALE_SH)
        # Up face: diamond back pattern; down face: pale belly.
        _dsnake_diamonds(rgba, u0 + 8, v0, u0 + 8 + w0, v0 + 8, salt=i)
        rect(rgba, u0 + 8 + w0, v0, u0 + 8 + 2 * w0, v0 + 8, DSNAKE_BELLY)
        rect(rgba, u0 + 8 + w0, v0, u0 + 8 + 2 * w0, v0 + 1, (0x9E, 0x9A, 0x74, 0xFF))
        speckle(rgba, W, u0, v0 + 8, u0 + 2 * (8 + w0), v0 + 12, DSNAKE_SCALE_SH,
                mod=3, salt=13 + i)                  # scale noise on the strip
    # Head extras @(0,0): red eyes high on the front face (x8..14, rows
    # 8..12) + mouth line + snout shade.
    rect(rgba, 9, 8, 10, 10, DSNAKE_EYE)             # eye L
    rect(rgba, 12, 8, 13, 10, DSNAKE_EYE)            # eye R
    rect(rgba, 9, 8, 10, 9, (0xE8, 0x5A, 0x4A, 0xFF))   # eye glint
    rect(rgba, 10, 10, 12, 11, DSNAKE_SCALE_SH)      # snout shade
    rect(rgba, 8, 11, 14, 12, DSNAKE_MOUTH)          # mouth line

# Bogwing (32x32): teal-green membrane wings, brown furry body, tiny beak.
# UV: body 4x3x6 @(0,0); head 3x3x4 @(0,9); right_wing/left_wing 10x1x6
# @(0,16)/(0,23).
BOG_BODY    = (0x6B, 0x4A, 0x2B, 0xFF)   # brown fur
BOG_BODY_HI = (0x86, 0x62, 0x3C, 0xFF)
BOG_BODY_SH = (0x50, 0x36, 0x1E, 0xFF)
BOG_WING    = (0x4A, 0x8E, 0x8A, 0xFF)   # teal membrane
BOG_WING_HI = (0x62, 0xA8, 0xA2, 0xFF)
BOG_WING_SH = (0x35, 0x6B, 0x68, 0xFF)
BOG_VEIN    = (0x26, 0x4E, 0x4C, 0xFF)
BOG_BEAK    = (0xC8, 0x92, 0x3A, 0xFF)
BOG_EYE     = (0x14, 0x10, 0x0C, 0xFF)

def paint_bogwing(rgba):
    bw = 32
    fill_buf(rgba, (0, 0, 0, 0))
    # Body @(0,0), fp 20x9: brown fur + speckle shag.
    shade_box(rgba, bw, 0, 0, 4, 3, 6, BOG_BODY, BOG_BODY_HI, BOG_BODY_SH)
    speckle(rgba, bw, 0, 6, 20, 9, BOG_BODY_SH, mod=3, salt=14)
    # Head @(0,9), fp 14x7: fur; front face (x4..7, rows 13..16): eyes + beak.
    shade_box(rgba, bw, 0, 9, 3, 3, 4, BOG_BODY, BOG_BODY_HI, BOG_BODY_SH)
    rectb(rgba, bw, 4, 13, 5, 14, BOG_EYE)          # eye L
    rectb(rgba, bw, 6, 13, 7, 14, BOG_EYE)          # eye R
    rectb(rgba, bw, 4, 14, 7, 16, BOG_BEAK)         # tiny beak
    rectb(rgba, bw, 4, 15, 7, 16, (0x9A, 0x6E, 0x2A, 0xFF))  # beak underside
    # Wings @(0,16)/(0,23), fp 32x7 each: teal membrane on the up/down faces
    # (up x6..16, down x16..26), dark vein lines, brown leading-edge strip.
    for v0 in (16, 23):
        shade_box(rgba, bw, 0, v0, 10, 1, 6, BOG_BODY, BOG_WING_HI, BOG_WING_SH)
        rectb(rgba, bw, 6, v0, 16, v0 + 6, BOG_WING)         # up face membrane
        rectb(rgba, bw, 6, v0, 16, v0 + 1, BOG_WING_HI)
        rectb(rgba, bw, 16, v0, 26, v0 + 6, BOG_WING_SH)     # down face membrane
        for vx in (8, 11, 14):                               # vein lines (up)
            rectb(rgba, bw, vx, v0 + 1, vx + 1, v0 + 6, BOG_VEIN)
        for vx in (18, 21, 24):                              # vein lines (down)
            rectb(rgba, bw, vx, v0 + 1, vx + 1, v0 + 6, BOG_VEIN)
        rectb(rgba, bw, 26, v0, 32, v0 + 6, BOG_WING)        # wingtip block
        rectb(rgba, bw, 26, v0 + 5, 32, v0 + 6, BOG_WING_SH)
        rectb(rgba, bw, 0, v0 + 6, 32, v0 + 7, BOG_BODY_SH)  # brown leading edge

# Yoda (64x64): green skin (3-tone), wispy white hair strip, coarse beige robe.
# UV: head 9x8x8 @(0,0); right_ear/left_ear 3x2x1 @(34,0)/(42,0);
# body 6x7x4 @(0,16); right_arm/left_arm 2x6x2 @(20,16)/(28,16);
# right_leg/left_leg 2x4x2 @(36,16)/(44,16).
YODA_SKIN    = (0x7A, 0xA0, 0x5A, 0xFF)   # green
YODA_SKIN_HI = (0x94, 0xB8, 0x72, 0xFF)
YODA_SKIN_SH = (0x5C, 0x7E, 0x42, 0xFF)
YODA_HAIR    = (0xE8, 0xE6, 0xDE, 0xFF)   # wispy white
YODA_ROBE    = (0xB8, 0xA8, 0x88, 0xFF)   # coarse beige
YODA_ROBE_HI = (0xCE, 0xC0, 0xA2, 0xFF)
YODA_ROBE_SH = (0x94, 0x84, 0x66, 0xFF)
YODA_BELT    = (0x5A, 0x46, 0x2E, 0xFF)
YODA_EYE     = (0x2A, 0x30, 0x1E, 0xFF)

def paint_yoda(rgba):
    fill(rgba, (0, 0, 0, 0))
    # Head @(0,0), fp 34x16: green skin, wispy white hair on the crown's
    # back half + upper side rows, wrinkles + eyes + mouth on the front face
    # (x8..17, rows 8..16).
    shade_box(rgba, W, 0, 0, 9, 8, 8, YODA_SKIN, YODA_SKIN_HI, YODA_SKIN_SH)
    speckle(rgba, W, 8, 4, 17, 8, YODA_HAIR, mod=2, salt=15)   # crown wisps
    speckle(rgba, W, 0, 8, 8, 11, YODA_HAIR, mod=3, salt=16)   # side wisps (east)
    speckle(rgba, W, 17, 8, 25, 11, YODA_HAIR, mod=3, salt=17) # side wisps (west)
    rect(rgba, 9, 9, 16, 10, YODA_SKIN_SH)          # brow wrinkle
    rect(rgba, 10, 11, 11, 12, YODA_EYE)            # eye L
    rect(rgba, 14, 11, 15, 12, YODA_EYE)            # eye R
    rect(rgba, 11, 13, 14, 14, YODA_SKIN_SH)        # cheek wrinkle
    rect(rgba, 11, 14, 14, 15, (0x4A, 0x66, 0x36, 0xFF))  # mouth line
    # Ears @(34,0)/(42,0), fp 8x3: green, darker tips (east face = tip).
    for u0 in (34, 42):
        shade_box(rgba, W, u0, 0, 3, 2, 1, YODA_SKIN, YODA_SKIN_HI, YODA_SKIN_SH)
        rect(rgba, u0, 1, u0 + 1, 3, YODA_SKIN_SH)  # tip shade
    # Body @(0,16), fp 20x11: coarse beige robe, weave speckle, V-collar,
    # belt row on the strip (rows 20..27).
    shade_box(rgba, W, 0, 16, 6, 7, 4, YODA_ROBE, YODA_ROBE_HI, YODA_ROBE_SH)
    speckle(rgba, W, 0, 20, 20, 27, YODA_ROBE_SH, mod=4, salt=18)  # coarse weave
    rect(rgba, 5, 20, 9, 21, YODA_ROBE_SH)          # V-collar fold
    rect(rgba, 6, 21, 8, 22, YODA_ROBE_SH)          # V-collar point
    rect(rgba, 0, 24, 20, 25, YODA_BELT)            # belt
    rect(rgba, 6, 24, 8, 25, (0x8A, 0x7A, 0x5A, 0xFF))  # buckle
    # Arms @(20,16)/(28,16), fp 8x8: robe sleeves, green hands on the
    # bottom rows.
    for u0 in (20, 28):
        shade_box(rgba, W, u0, 16, 2, 6, 2, YODA_ROBE, YODA_ROBE_HI, YODA_ROBE_SH)
        rect(rgba, u0, 22, u0 + 8, 24, YODA_SKIN)   # hands
        rect(rgba, u0, 23, u0 + 8, 24, YODA_SKIN_SH)
    # Legs @(36,16)/(44,16), fp 8x6: robe hem, green feet row.
    for u0 in (36, 44):
        shade_box(rgba, W, u0, 16, 2, 4, 2, YODA_ROBE, YODA_ROBE_HI, YODA_ROBE_SH)
        rect(rgba, u0, 20, u0 + 8, 21, YODA_ROBE_SH)
        rect(rgba, u0, 21, u0 + 8, 22, YODA_SKIN)   # bare feet

# -----------------------------------------------------------------------------
# Wave-3 vehicles/creatures. Box-UV offsets match gen_bbmodels.py's *_CUBES
# tables exactly. Every region goes through shade_box (base + top highlight +
# bottom shade = 3 tones) before details, meeting the finished-art bar.
# -----------------------------------------------------------------------------

# speeder_bike (64x64): burnt-orange + brown swoop-bike chassis (74-Z vibe),
# silver steering vanes, black saddle, rust weathering.
# UV: chassis 6x4x18 @(0,0); seat 4x2x6 @(0,22); vane_left/right 2x2x6
# @(20,22)/(36,22); rail_left/right 1x1x8 @(0,30)/(18,30).
SB_ORANGE    = (0xC2, 0x6A, 0x2E, 0xFF)   # burnt orange
SB_ORANGE_HI = (0xE0, 0x8A, 0x44, 0xFF)
SB_ORANGE_SH = (0x8A, 0x46, 0x1E, 0xFF)
SB_BROWN     = (0x5A, 0x3A, 0x22, 0xFF)   # brown chassis accents
SB_BROWN_HI  = (0x74, 0x4E, 0x30, 0xFF)
SB_SILVER    = (0xB4, 0xB8, 0xC0, 0xFF)   # vanes
SB_SILVER_HI = (0xD2, 0xD6, 0xDE, 0xFF)
SB_SILVER_SH = (0x82, 0x86, 0x90, 0xFF)
SB_SEAT      = (0x1E, 0x1C, 0x1A, 0xFF)   # black saddle
SB_SEAT_HI   = (0x3A, 0x36, 0x32, 0xFF)
SB_SEAT_SH   = (0x10, 0x0E, 0x0C, 0xFF)
SB_RUST      = (0x6E, 0x40, 0x24, 0xFF)   # weathering streaks

def paint_speeder_bike(rgba):
    fill(rgba, (0, 0, 0, 0))
    # Chassis 6x4x18 @(0,0): burnt-orange hull, brown belly band, rust flecks,
    # brown deck panel seams.
    shade_box(rgba, W, 0, 0, 6, 4, 18, SB_ORANGE, SB_ORANGE_HI, SB_ORANGE_SH)
    rect(rgba, 0, 20, 48, 22, SB_BROWN)            # brown lower band (strip)
    for zx in range(18, 30, 4):                    # brown deck panel seams (up face)
        rect(rgba, zx, 0, zx + 1, 18, SB_BROWN_HI)
    speckle(rgba, W, 18, 0, 30, 18, SB_RUST, mod=7, salt=41)   # deck rust
    speckle(rgba, W, 0, 18, 48, 20, SB_RUST, mod=9, salt=42)   # side weathering
    # Seat 4x2x6 @(0,22): black saddle w/ faint sheen.
    shade_box(rgba, W, 0, 22, 4, 2, 6, SB_SEAT, SB_SEAT_HI, SB_SEAT_SH)
    # Vanes 2x2x6 @(20,22)/(36,22): silver steering fins, bright length rim.
    for u0 in (20, 36):
        shade_box(rgba, W, u0, 22, 2, 2, 6, SB_SILVER, SB_SILVER_HI, SB_SILVER_SH)
        rect(rgba, u0, 24, u0 + 16, 25, SB_SILVER_HI)
    # Rails 1x1x8 @(0,30)/(18,30): dark-brown footrails w/ a silver cap line.
    for u0 in (0, 18):
        shade_box(rgba, W, u0, 30, 1, 1, 8, SB_BROWN, SB_BROWN_HI, SB_SEAT_SH)
        rect(rgba, u0 + 2, 31, u0 + 16, 32, SB_SILVER_SH)

# band_droid (64x64): polished copper/bronze protocol-droid body, dark
# faceplate w/ two round cyan lens eyes, brass horn instrument.
# UV: head 6x6x6 @(0,0) + antenna 1x3x1 @(24,0); body 6x8x4 @(0,16);
# right_arm/left_arm 2x8x2 @(24,16)/(32,16); right_leg/left_leg 2x6x2
# @(0,32)/(12,32); horn 2x2x5 @(40,0).
BD_COPPER    = (0xB0, 0x6A, 0x38, 0xFF)   # polished copper/bronze
BD_COPPER_HI = (0xD8, 0x92, 0x54, 0xFF)
BD_COPPER_SH = (0x82, 0x48, 0x24, 0xFF)
BD_BRONZE    = (0x6E, 0x40, 0x22, 0xFF)   # dark bronze seams
BD_FACE      = (0x1E, 0x18, 0x14, 0xFF)   # dark faceplate
BD_FACE_HI   = (0x34, 0x2C, 0x24, 0xFF)
BD_LENS      = (0x4A, 0xC8, 0xD8, 0xFF)   # round lens eyes
BD_LENS_HOT  = (0xCE, 0xF4, 0xF8, 0xFF)
BD_BRASS     = (0xC8, 0xA0, 0x40, 0xFF)   # brass horn
BD_BRASS_HI  = (0xE6, 0xC4, 0x66, 0xFF)
BD_BRASS_SH  = (0x8E, 0x6E, 0x28, 0xFF)

def paint_band_droid(rgba):
    fill(rgba, (0, 0, 0, 0))
    # Head 6x6x6 @(0,0): copper dome; dark faceplate + two cyan lens eyes on
    # the front face (strip cols 6..12, rows 6..12).
    shade_box(rgba, W, 0, 0, 6, 6, 6, BD_COPPER, BD_COPPER_HI, BD_COPPER_SH)
    rect(rgba, 6, 8, 12, 12, BD_FACE)              # dark faceplate
    rect(rgba, 6, 8, 12, 9, BD_FACE_HI)            # brow highlight
    rect(rgba, 7, 9, 8, 11, BD_LENS)               # left lens
    rect(rgba, 10, 9, 11, 11, BD_LENS)             # right lens
    rect(rgba, 7, 9, 8, 10, BD_LENS_HOT)           # lens glints
    rect(rgba, 10, 9, 11, 10, BD_LENS_HOT)
    # Antenna 1x3x1 @(24,0): brass stub.
    shade_box(rgba, W, 24, 0, 1, 3, 1, BD_BRASS, BD_BRASS_HI, BD_BRASS_SH)
    # Body 6x8x4 @(0,16): copper torso w/ bronze chest panel + vent seams.
    shade_box(rgba, W, 0, 16, 6, 8, 4, BD_COPPER, BD_COPPER_HI, BD_COPPER_SH)
    rect(rgba, 4, 22, 16, 24, BD_BRONZE)           # chest panel (front strip)
    rect(rgba, 4, 25, 16, 26, BD_BRONZE)           # vent seam
    rect(rgba, 4, 27, 16, 28, BD_BRONZE)           # vent seam
    # Arms 2x8x2 @(24,16)/(32,16): copper w/ bronze shoulder + brass wrist ring.
    for u0 in (24, 32):
        shade_box(rgba, W, u0, 16, 2, 8, 2, BD_COPPER, BD_COPPER_HI, BD_COPPER_SH)
        rect(rgba, u0, 18, u0 + 8, 19, BD_BRONZE)
        rect(rgba, u0, 23, u0 + 8, 24, BD_BRASS)
    # Legs 2x6x2 @(0,32)/(12,32): copper w/ dark foot rows.
    for u0 in (0, 12):
        shade_box(rgba, W, u0, 32, 2, 6, 2, BD_COPPER, BD_COPPER_HI, BD_COPPER_SH)
        rect(rgba, u0, 38, u0 + 8, 40, BD_BRONZE)
    # Horn 2x2x5 @(40,0): brass instrument w/ a length highlight + bell shade.
    shade_box(rgba, W, 40, 0, 2, 2, 5, BD_BRASS, BD_BRASS_HI, BD_BRASS_SH)
    rect(rgba, 40, 2, 54, 3, BD_BRASS_HI)
    rect(rgba, 52, 2, 54, 4, BD_BRASS_SH)

# xwing (128x128): off-white hull w/ RED squadron stripes on the wings + nose,
# gray panel lines, dark canopy glass, orange engine glow at the rears.
# UV: fuselage 6x6x26 @(0,0); nose 4x4x10 @(64,0); cockpit 4x3x6 @(64,14);
# wing_tl/tr/bl/br 14x1x10 @(0,32)/(0,43)/(0,54)/(0,65); engine_tl/tr/bl/br
# 3x3x8 @(0,76)/(22,76)/(44,76)/(66,76).
XW_WHITE     = (0xE6, 0xE4, 0xDC, 0xFF)   # off-white hull
XW_WHITE_HI  = (0xF6, 0xF4, 0xEE, 0xFF)
XW_WHITE_SH  = (0xB8, 0xB6, 0xAE, 0xFF)
XW_GRAY      = (0x7A, 0x7C, 0x82, 0xFF)   # panel lines
XW_RED       = (0xC2, 0x30, 0x2E, 0xFF)   # squadron stripes
XW_RED_HI    = (0xE0, 0x4A, 0x44, 0xFF)
XW_GLASS     = (0x22, 0x2A, 0x36, 0xFF)   # dark canopy
XW_GLASS_HI  = (0x4C, 0x60, 0x78, 0xFF)
XW_GLASS_DK  = (0x16, 0x1C, 0x26, 0xFF)
XW_FRAME     = (0x9A, 0x9C, 0xA2, 0xFF)   # canopy frame
XW_ENGINE    = (0x3A, 0x3C, 0x42, 0xFF)   # engine housing
XW_ENGINE_HI = (0x54, 0x56, 0x5E, 0xFF)
XW_ENGINE_SH = (0x22, 0x24, 0x28, 0xFF)
XW_GLOW      = (0xE8, 0x7A, 0x2A, 0xFF)   # engine glow orange
XW_GLOW_HOT  = (0xFF, 0xCC, 0x66, 0xFF)

def paint_xwing(rgba):
    bw = 128
    fill_buf(rgba, (0, 0, 0, 0))
    # Fuselage 6x6x26 @(0,0): off-white hull w/ gray panel seams + greebles.
    shade_box(rgba, bw, 0, 0, 6, 6, 26, XW_WHITE, XW_WHITE_HI, XW_WHITE_SH)
    for zx in range(6, 32, 5):
        rectb(rgba, bw, 0, zx, 64, zx + 1, XW_GRAY)
    speckle(rgba, bw, 12, 0, 24, 26, XW_GRAY, mod=13, salt=43)
    # Nose 4x4x10 @(64,0): white w/ a red squadron stripe + gray panel line.
    shade_box(rgba, bw, 64, 0, 4, 4, 10, XW_WHITE, XW_WHITE_HI, XW_WHITE_SH)
    rectb(rgba, bw, 64, 8, 92, 10, XW_RED)
    rectb(rgba, bw, 64, 4, 92, 5, XW_GRAY)
    # Cockpit 4x3x6 @(64,14): dark canopy glass, light frame, reflection.
    shade_box(rgba, bw, 64, 14, 4, 3, 6, XW_GLASS, XW_FRAME, XW_GLASS_DK)
    rectb(rgba, bw, 70, 17, 78, 23, XW_GLASS)      # glass front face
    rectb(rgba, bw, 70, 17, 78, 18, XW_GLASS_HI)   # canopy reflection
    rectb(rgba, bw, 64, 14, 84, 15, XW_FRAME)      # frame top edge
    # Wings 14x1x10: white w/ a bold red stripe down the up face + gray panel
    # lines and a gray trailing edge on the strip.
    for v0 in (32, 43, 54, 65):
        shade_box(rgba, bw, 0, v0, 14, 1, 10, XW_WHITE, XW_WHITE_HI, XW_WHITE_SH)
        rectb(rgba, bw, 15, v0, 19, v0 + 10, XW_RED)       # red squadron stripe
        rectb(rgba, bw, 15, v0, 19, v0 + 1, XW_RED_HI)
        rectb(rgba, bw, 10, v0, 11, v0 + 10, XW_GRAY)      # inboard panel line
        rectb(rgba, bw, 23, v0, 24, v0 + 10, XW_GRAY)      # outboard panel line
        rectb(rgba, bw, 0, v0 + 10, 48, v0 + 11, XW_GRAY)  # trailing edge (strip)
    # Engines 3x3x8: dark housings w/ an orange glow on the rear (down) face.
    for u0 in (0, 22, 44, 66):
        shade_box(rgba, bw, u0, 76, 3, 3, 8, XW_ENGINE, XW_ENGINE_HI, XW_ENGINE_SH)
        rectb(rgba, bw, u0 + 11, 76, u0 + 14, 84, XW_GLOW)
        rectb(rgba, bw, u0 + 12, 78, u0 + 13, 82, XW_GLOW_HOT)

# tie_fighter (128x64): gunmetal-gray cockpit ball + pylons, BLACK solar
# panels w/ a dark-gray hex/rib lattice, red-tinted viewport.
# UV: ball 8x8x8 @(0,0); window 4x4x1 @(32,0); pylon_left/right 4x2x2
# @(32,5)/(44,5); panel_left/right 1x16x14 @(0,16)/(30,16).
TF_METAL      = (0x60, 0x64, 0x6C, 0xFF)  # gunmetal
TF_METAL_HI   = (0x82, 0x86, 0x90, 0xFF)
TF_METAL_SH   = (0x44, 0x47, 0x4E, 0xFF)
TF_PANEL      = (0x14, 0x15, 0x18, 0xFF)  # black solar panels
TF_PANEL_HI   = (0x2E, 0x30, 0x36, 0xFF)  # dark-gray ribs
TF_PANEL_EDGE = (0x0A, 0x0A, 0x0C, 0xFF)  # panel frame
TF_WINDOW     = (0x9A, 0x28, 0x22, 0xFF)  # red-tinted viewport
TF_WINDOW_HI  = (0xC6, 0x44, 0x3A, 0xFF)
TF_WINDOW_DK  = (0x5E, 0x18, 0x14, 0xFF)

def paint_tie_fighter(rgba):
    bw = 128
    fill_buf(rgba, (0, 0, 0, 0))
    # Ball 8x8x8 @(0,0): gunmetal cockpit; front face (cols 8..16, rows 8..16)
    # holds a dark window recess ring around the red viewport.
    shade_box(rgba, bw, 0, 0, 8, 8, 8, TF_METAL, TF_METAL_HI, TF_METAL_SH)
    rectb(rgba, bw, 9, 9, 15, 15, TF_PANEL)        # recess ring
    rectb(rgba, bw, 10, 10, 14, 14, TF_WINDOW)     # red viewport
    rectb(rgba, bw, 10, 10, 14, 11, TF_WINDOW_HI)  # top reflection
    rectb(rgba, bw, 11, 11, 13, 13, TF_WINDOW_DK)  # pupil recess
    # Window cube 4x4x1 @(32,0): standalone red viewport plate.
    shade_box(rgba, bw, 32, 0, 4, 4, 1, TF_WINDOW, TF_WINDOW_HI, TF_WINDOW_DK)
    rectb(rgba, bw, 33, 1, 41, 4, TF_WINDOW)
    # Pylons 4x2x2 @(32,5)/(44,5): short gunmetal struts.
    for u0 in (32, 44):
        shade_box(rgba, bw, u0, 5, 4, 2, 2, TF_METAL, TF_METAL_HI, TF_METAL_SH)
    # Panels 1x16x14 @(0,16)/(30,16): black solar wings w/ a dark-gray rib
    # lattice (vertical ribs + horizontal cross-bars) on the big strip faces.
    for u0 in (0, 30):
        shade_box(rgba, bw, u0, 16, 1, 16, 14, TF_PANEL, TF_PANEL_HI, TF_PANEL_EDGE)
        for rx in range(u0 + 2, u0 + 30, 4):
            rectb(rgba, bw, rx, 30, rx + 1, 46, TF_PANEL_HI)   # vertical ribs
        for ry in range(32, 46, 4):
            rectb(rgba, bw, u0, ry, u0 + 30, ry + 1, TF_PANEL_HI)  # cross-bars
        rectb(rgba, bw, u0, 30, u0 + 30, 31, TF_PANEL_EDGE)   # top frame
        rectb(rgba, bw, u0, 45, u0 + 30, 46, TF_PANEL_EDGE)   # bottom frame

# at_at (256x128, true scale): matte imperial-gray armor plating (3 grays,
# panel seams), darker joints/underbelly, black cockpit slit on the head,
# subtle rust streaks on the legs. All four legs share one UV block; likewise
# the four feet.
# UV: body 36x28x64 @(0,0); neck 8x6x20 @(64,92); head 14x10x18 @(0,92);
# legs 8x88x8 @(200,0) [shared]; feet 12x6x12 @(120,92) [shared].
AT_GRAY      = (0x8E, 0x92, 0x98, 0xFF)   # matte imperial gray (mid)
AT_GRAY_HI   = (0xAC, 0xB0, 0xB6, 0xFF)
AT_GRAY_SH   = (0x66, 0x6A, 0x70, 0xFF)
AT_JOINT     = (0x3E, 0x41, 0x47, 0xFF)   # dark joints / underbelly
AT_WINDOW    = (0x0C, 0x0C, 0x10, 0xFF)   # black cockpit slit
AT_WINDOW_HI = (0x30, 0x40, 0x52, 0xFF)
AT_RUST      = (0x7A, 0x56, 0x38, 0xFF)   # subtle rust streaks

def paint_at_at(rgba):
    bw = 256
    fill_buf(rgba, (0, 0, 0, 0))
    # Body 36x28x64 @(0,0): matte gray plating, panel seams on the strip, a
    # darkened underbelly on the down face, faint plating grain.
    shade_box(rgba, bw, 0, 0, 36, 28, 64, AT_GRAY, AT_GRAY_HI, AT_GRAY_SH)
    for sx in range(0, 200, 16):
        rectb(rgba, bw, sx, 64, sx + 1, 92, AT_GRAY_SH)      # vertical panel seams
    for sy in range(70, 92, 8):
        rectb(rgba, bw, 0, sy, 200, sy + 1, AT_GRAY_SH)      # horizontal seams
    rectb(rgba, bw, 100, 0, 136, 64, AT_JOINT)               # underbelly (down face)
    speckle(rgba, bw, 0, 64, 200, 92, AT_GRAY_HI, mod=17, salt=51)
    # Neck 8x6x20 @(64,92): gray w/ ribbed joint bands on the strip.
    shade_box(rgba, bw, 64, 92, 8, 6, 20, AT_GRAY, AT_GRAY_HI, AT_GRAY_SH)
    for rx in range(64, 120, 4):
        rectb(rgba, bw, rx, 112, rx + 1, 118, AT_JOINT)
    # Head 14x10x18 @(0,92): gray w/ a black cockpit window slit on the front
    # face (strip cols 18..32, rows 110..120) + a chin gun housing.
    shade_box(rgba, bw, 0, 92, 14, 10, 18, AT_GRAY, AT_GRAY_HI, AT_GRAY_SH)
    rectb(rgba, bw, 18, 113, 32, 116, AT_WINDOW)
    rectb(rgba, bw, 18, 113, 32, 114, AT_WINDOW_HI)
    rectb(rgba, bw, 20, 116, 30, 118, AT_JOINT)
    # Legs 8x88x8 @(200,0) [shared by all four]: gray armor, knee + ankle
    # joint bands, subtle rust streaks down the shins.
    shade_box(rgba, bw, 200, 0, 8, 88, 8, AT_GRAY, AT_GRAY_HI, AT_GRAY_SH)
    rectb(rgba, bw, 200, 40, 232, 44, AT_JOINT)              # knee joint band
    rectb(rgba, bw, 200, 84, 232, 88, AT_JOINT)              # ankle joint band
    speckle(rgba, bw, 200, 48, 232, 84, AT_RUST, mod=11, salt=52)
    # Feet 12x6x12 @(120,92) [shared by all four]: dark gray pads + sole shade.
    shade_box(rgba, bw, 120, 92, 12, 6, 12, AT_GRAY_SH, AT_GRAY, AT_JOINT)
    rectb(rgba, bw, 120, 104, 168, 110, AT_JOINT)

# -----------------------------------------------------------------------------
# Companion mobs: Chewbacca (64x64) + Grogu (32x32). Box-UV offsets below match
# gen_bbmodels.py's CHEWBACCA_CUBES / GROGU_CUBES exactly.
# -----------------------------------------------------------------------------

# Chewbacca (64x64): shaggy warm-brown Wookiee. Vertical fur banding all over,
# a slightly lighter face/snout, dark eyes + black nose, and a diagonal leather
# bandolier (silver ammo boxes) across the chest + back. Long fully-furred arms.
# UV: head 8x8x8 @(0,0) [face front x8..16,y8..16]; snout 4x3x4 @(32,0)
# [nose front x36..40,y4..7]; ear_left/ear_right 1x2x1 @(48,0)/(52,0);
# body 8x14x5 @(0,16) [chest front x5..13, back x18..26, rows 21..35];
# right_arm/left_arm 3x14x3 @(28,16)/(40,16); right_leg/left_leg 3x8x3
# @(0,36)/(14,36).
CHEW_FUR      = (0x6B, 0x4A, 0x2E, 0xFF)   # warm brown (egg primary)
CHEW_FUR_HI   = (0x8C, 0x64, 0x40, 0xFF)   # lit brown
CHEW_FUR_SH   = (0x4A, 0x31, 0x1E, 0xFF)   # shadow brown
CHEW_FUR_DK   = (0x35, 0x24, 0x16, 0xFF)   # darkest streak (~egg secondary)
CHEW_FACE     = (0x86, 0x60, 0x40, 0xFF)   # lighter face/muzzle
CHEW_FACE_HI  = (0xA2, 0x7A, 0x54, 0xFF)
CHEW_FACE_SH  = (0x64, 0x46, 0x2C, 0xFF)
CHEW_EYE      = (0x17, 0x0E, 0x09, 0xFF)   # dark eyes
CHEW_NOSE     = (0x0C, 0x09, 0x07, 0xFF)   # black nose
CHEW_STRAP    = (0x33, 0x24, 0x18, 0xFF)   # dark leather bandolier
CHEW_STRAP_HI = (0x4E, 0x39, 0x26, 0xFF)
CHEW_AMMO     = (0xBE, 0xC2, 0xCA, 0xFF)   # silver ammo box
CHEW_AMMO_SH  = (0x84, 0x88, 0x90, 0xFF)

def _fur_region(rgba, x0, y0, x1, y1, salt):
    """Shaggy vertical fur banding over a box-UV footprint already base-shaded
    by shade_box: broken dark + light vertical streaks (deterministic, no RNG,
    64x64 canvas)."""
    for x in range(x0, x1):
        col = (x + salt) % 3
        for y in range(y0, y1):
            broken = (x * 5 + y * 3 + salt) % 4
            if col == 0 and broken != 0:
                c = CHEW_FUR_DK
            elif col == 2 and broken == 0:
                c = CHEW_FUR_HI
            else:
                continue
            i = 4 * (y * W + x)
            rgba[i+0], rgba[i+1], rgba[i+2], rgba[i+3] = c

def _bandolier(rgba, x0, y0, w, h, flip):
    """Stepped 2px diagonal leather bandolier with silver ammo boxes across a
    body face at (x0,y0) spanning w x h (64x64 canvas)."""
    for i in range(h):
        fx = (w - 2) * i // (h - 1)
        if flip:
            fx = (w - 2) - fx
        sx = x0 + fx
        rect(rgba, sx, y0 + i, sx + 2, y0 + i + 1, CHEW_STRAP)
        rect(rgba, sx, y0 + i, sx + 1, y0 + i + 1, CHEW_STRAP_HI)  # lit edge
    for i in range(2, h - 2, 4):                     # silver ammo boxes
        fx = (w - 2) * i // (h - 1)
        if flip:
            fx = (w - 2) - fx
        sx = x0 + fx
        rect(rgba, sx, y0 + i, sx + 2, y0 + i + 2, CHEW_AMMO)
        rect(rgba, sx, y0 + i + 1, sx + 2, y0 + i + 2, CHEW_AMMO_SH)

def paint_chewbacca(rgba):
    fill(rgba, (0, 0, 0, 0))
    # Head 8x8x8 @(0,0): shaggy fur; lighter face on the front face (x8..16).
    shade_box(rgba, W, 0, 0, 8, 8, 8, CHEW_FUR, CHEW_FUR_HI, CHEW_FUR_SH)
    _fur_region(rgba, 0, 0, 32, 16, 1)
    rect(rgba, 9, 10, 15, 16, CHEW_FACE)             # lighter muzzle-frame
    rect(rgba, 9, 10, 15, 11, CHEW_FACE_HI)          # brow-ridge highlight
    rect(rgba, 9, 15, 15, 16, CHEW_FACE_SH)          # jaw shadow
    rect(rgba, 9, 11, 15, 12, CHEW_FUR_DK)           # brow fur line
    rect(rgba, 10, 12, 11, 13, CHEW_EYE)             # eye L
    rect(rgba, 13, 12, 14, 13, CHEW_EYE)             # eye R
    # Snout 4x3x4 @(32,0): black nose on the front face, muzzle below; furry top.
    shade_box(rgba, W, 32, 0, 4, 3, 4, CHEW_FACE, CHEW_FACE_HI, CHEW_FACE_SH)
    rect(rgba, 36, 0, 40, 1, CHEW_FUR_HI)            # up face (snout crown fur)
    rect(rgba, 36, 4, 40, 5, CHEW_NOSE)              # black nose
    rect(rgba, 36, 5, 40, 7, CHEW_FACE)              # muzzle
    rect(rgba, 36, 6, 40, 7, CHEW_FACE_SH)           # lip line
    # Ears 1x2x1 @(48,0)/(52,0): furred.
    shade_box(rgba, W, 48, 0, 1, 2, 1, CHEW_FUR, CHEW_FUR_HI, CHEW_FUR_SH)
    shade_box(rgba, W, 52, 0, 1, 2, 1, CHEW_FUR, CHEW_FUR_HI, CHEW_FUR_SH)
    # Body 8x14x5 @(0,16): shaggy fur + diagonal bandolier on the chest (front
    # face x5..13) and back (back face x18..26).
    shade_box(rgba, W, 0, 16, 8, 14, 5, CHEW_FUR, CHEW_FUR_HI, CHEW_FUR_SH)
    _fur_region(rgba, 0, 16, 26, 35, 2)
    _bandolier(rgba, 5, 21, 8, 14, False)            # chest strap
    _bandolier(rgba, 18, 21, 8, 14, True)            # back strap
    # Arms 3x14x3 @(28,16)/(40,16): long, fully furred.
    for u0 in (28, 40):
        shade_box(rgba, W, u0, 16, 3, 14, 3, CHEW_FUR, CHEW_FUR_HI, CHEW_FUR_SH)
        _fur_region(rgba, u0, 16, u0 + 12, 33, u0)
    # Legs 3x8x3 @(0,36)/(14,36): furred, darker paw rows.
    for u0 in (0, 14):
        shade_box(rgba, W, u0, 36, 3, 8, 3, CHEW_FUR, CHEW_FUR_HI, CHEW_FUR_SH)
        _fur_region(rgba, u0, 36, u0 + 12, 47, u0 + 3)
        rect(rgba, u0, 45, u0 + 12, 47, CHEW_FUR_DK)  # dark paws

# Grogu (32x32): pale sage-green foundling with an oversized head, big round
# ears (green outside / pink inside), huge dark friendly eyes, tiny nose+mouth,
# and a coarse tan robe. Adorable is the goal.
# UV: head 7x6x6 @(0,0) [face front x6..13,y6..12]; ear_left 5x1x3 @(0,14)
# [outer=up x3..8, inner/pink=down x8..13]; ear_right @(0,18);
# body 4x5x3 @(0,22) [robe front x3..7,y25..30]; right_arm/left_arm 1x4x1
# @(16,12)/(20,12); right_leg/left_leg 1x2x1 @(24,12)/(28,12);
# robe_skirt 5x3x4 @(14,22).
GROGU_SKIN     = (0x8F, 0xA8, 0x6E, 0xFF)  # pale sage green (egg primary)
GROGU_SKIN_HI  = (0xA8, 0xBE, 0x88, 0xFF)
GROGU_SKIN_SH  = (0x6E, 0x86, 0x52, 0xFF)
GROGU_EAR_IN   = (0xD6, 0xA6, 0xA0, 0xFF)  # pink inner ear
GROGU_EAR_INSH = (0xBC, 0x86, 0x82, 0xFF)
GROGU_EYE      = (0x17, 0x11, 0x0F, 0xFF)  # big dark eyes
GROGU_EYE_HI   = (0x4A, 0x3C, 0x36, 0xFF)  # catchlight
GROGU_NOSE     = (0x5C, 0x4E, 0x42, 0xFF)  # tiny nose
GROGU_ROBE     = (0xC8, 0xB2, 0x86, 0xFF)  # coarse tan robe (egg secondary)
GROGU_ROBE_HI  = (0xDC, 0xC8, 0xA0, 0xFF)
GROGU_ROBE_SH  = (0xA6, 0x90, 0x66, 0xFF)

def paint_grogu(rgba):
    bw = 32
    fill_buf(rgba, (0, 0, 0, 0))
    # Head 7x6x6 @(0,0): green skin, soft cheek shading; big eyes on the front
    # face (x6..13, rows 6..12).
    shade_box(rgba, bw, 0, 0, 7, 6, 6, GROGU_SKIN, GROGU_SKIN_HI, GROGU_SKIN_SH)
    rectb(rgba, bw, 6, 6, 13, 7, GROGU_SKIN_HI)      # forehead highlight
    rectb(rgba, bw, 6, 10, 13, 12, GROGU_SKIN_SH)    # cheek/chin shading
    rectb(rgba, bw, 7, 8, 9, 10, GROGU_EYE)          # eye L (2x2, large)
    rectb(rgba, bw, 10, 8, 12, 10, GROGU_EYE)        # eye R
    rectb(rgba, bw, 7, 8, 8, 9, GROGU_EYE_HI)        # catchlight L
    rectb(rgba, bw, 10, 8, 11, 9, GROGU_EYE_HI)      # catchlight R
    rectb(rgba, bw, 9, 10, 10, 11, GROGU_NOSE)       # tiny nose
    rectb(rgba, bw, 8, 11, 11, 12, GROGU_SKIN_SH)    # soft mouth line
    # Ears 5x1x3 @(0,14)/(0,18): green outer (up face), pink inner (down face).
    for v0 in (14, 18):
        shade_box(rgba, bw, 0, v0, 5, 1, 3, GROGU_SKIN, GROGU_SKIN_HI, GROGU_SKIN_SH)
        rectb(rgba, bw, 3, v0, 8, v0 + 3, GROGU_SKIN)          # outer = green
        rectb(rgba, bw, 3, v0, 8, v0 + 1, GROGU_SKIN_HI)
        rectb(rgba, bw, 8, v0, 13, v0 + 3, GROGU_EAR_IN)       # inner = pink
        rectb(rgba, bw, 8, v0 + 2, 13, v0 + 3, GROGU_EAR_INSH)
        rectb(rgba, bw, 9, v0 + 1, 12, v0 + 2, GROGU_EAR_INSH)  # inner fold
    # Body 4x5x3 @(0,22): coarse tan robe with weave speckle + fold shading.
    shade_box(rgba, bw, 0, 22, 4, 5, 3, GROGU_ROBE, GROGU_ROBE_HI, GROGU_ROBE_SH)
    speckle(rgba, bw, 0, 25, 14, 30, GROGU_ROBE_SH, mod=4, salt=61)
    rectb(rgba, bw, 3, 25, 7, 26, GROGU_ROBE_HI)     # collar highlight
    rectb(rgba, bw, 4, 26, 6, 30, GROGU_ROBE_SH)     # center robe fold
    # Arms 1x4x1 @(16,12)/(20,12): tan sleeve, green hand at the cuff.
    for u0 in (16, 20):
        shade_box(rgba, bw, u0, 12, 1, 4, 1, GROGU_ROBE, GROGU_ROBE_HI, GROGU_ROBE_SH)
        rectb(rgba, bw, u0, 15, u0 + 4, 17, GROGU_SKIN)        # hand
        rectb(rgba, bw, u0, 16, u0 + 4, 17, GROGU_SKIN_SH)
    # Legs 1x2x1 @(24,12)/(28,12): robe hem + green feet.
    for u0 in (24, 28):
        shade_box(rgba, bw, u0, 12, 1, 2, 1, GROGU_ROBE, GROGU_ROBE_HI, GROGU_ROBE_SH)
        rectb(rgba, bw, u0, 14, u0 + 4, 15, GROGU_SKIN)        # feet
    # Robe skirt 5x3x4 @(14,22): coarse tan wrap under the body, vertical folds.
    shade_box(rgba, bw, 14, 22, 5, 3, 4, GROGU_ROBE, GROGU_ROBE_HI, GROGU_ROBE_SH)
    speckle(rgba, bw, 14, 26, 32, 29, GROGU_ROBE_SH, mod=4, salt=62)
    for fx in (20, 24, 28):                          # cloth folds
        rectb(rgba, bw, fx, 26, fx + 1, 29, GROGU_ROBE_SH)

# Ewok (32x32): warm-brown furry forest native in a russet cowl. The 'head'
# bone carries ONE 8x7x7 cube that serves as both head and hood (a separate
# inner-head + outer-hood cube pair cannot be box-UV-packed onto 32x32
# alongside body+limbs — see tools/gen_bbmodels.py EWOK_CUBES note); its top/
# back/sides paint as the russet cowl, its FRONT face (x7..15, y7..14) as a
# tan face patch framed by the cowl, with big dark eyes + a tiny black nose.
# Body/arms/legs are warm brown fur (3 browns) with shaggy vertical banding;
# tiny dark paws/feet at the cuffs. Two 1x2x2 ear bumps flank the head.
EWOK_FUR     = (0x7A, 0x50, 0x2E, 0xFF)   # warm brown base
EWOK_FUR_HI  = (0x9A, 0x68, 0x3E, 0xFF)   # lighter brown
EWOK_FUR_SH  = (0x52, 0x34, 0x1C, 0xFF)   # darker brown
EWOK_HOOD    = (0x8A, 0x3E, 0x2A, 0xFF)   # russet cowl (contrasting earthy)
EWOK_HOOD_HI = (0xA4, 0x52, 0x38, 0xFF)
EWOK_HOOD_SH = (0x5E, 0x28, 0x1A, 0xFF)
EWOK_FACE    = (0xC8, 0x9E, 0x6E, 0xFF)   # lighter tan face patch
EWOK_FACE_HI = (0xDE, 0xB8, 0x8A, 0xFF)
EWOK_FACE_SH = (0xA0, 0x7A, 0x50, 0xFF)
EWOK_EYE     = (0x1E, 0x12, 0x0A, 0xFF)   # big dark friendly eye
EWOK_EYE_HI  = (0x6A, 0x4A, 0x30, 0xFF)   # warm catchlight
EWOK_NOSE    = (0x14, 0x0C, 0x08, 0xFF)   # tiny black nose
EWOK_PAW     = (0x46, 0x2C, 0x18, 0xFF)   # dark paws/feet

def paint_ewok(rgba):
    bw = 32
    fill_buf(rgba, (0, 0, 0, 0))
    # Head/hood 8x7x7 @(0,0): russet cowl on every face; a couple of fur
    # streaks on the wraparound so the cowl reads shaggy, not smooth cloth.
    shade_box(rgba, bw, 0, 0, 8, 7, 7, EWOK_HOOD, EWOK_HOOD_HI, EWOK_HOOD_SH)
    speckle(rgba, bw, 0, 8, 30, 13, EWOK_HOOD_SH, mod=5, salt=41)
    # Face on the FRONT face (x7..15, y7..14): tan patch inset one pixel so the
    # russet cowl frames it on every side.
    rectb(rgba, bw, 8, 8, 14, 13, EWOK_FACE)
    rectb(rgba, bw, 8, 8, 14, 9, EWOK_FACE_HI)      # brow highlight
    rectb(rgba, bw, 8, 9, 10, 11, EWOK_EYE)         # left eye (2x2, big)
    rectb(rgba, bw, 12, 9, 14, 11, EWOK_EYE)        # right eye
    rectb(rgba, bw, 8, 9, 9, 10, EWOK_EYE_HI)       # catchlight L
    rectb(rgba, bw, 12, 9, 13, 10, EWOK_EYE_HI)     # catchlight R
    rectb(rgba, bw, 10, 11, 12, 12, EWOK_NOSE)      # tiny black nose
    rectb(rgba, bw, 9, 12, 13, 13, EWOK_FACE_SH)    # muzzle/mouth shadow
    # Body 5x6x3 @(0,14): warm brown fur, shaggy vertical banding, lighter belly.
    shade_box(rgba, bw, 0, 14, 5, 6, 3, EWOK_FUR, EWOK_FUR_HI, EWOK_FUR_SH)
    speckle(rgba, bw, 0, 17, 16, 23, EWOK_FUR_SH, mod=4, salt=42)
    for bx in (1, 4, 7, 10, 13):                     # vertical fur bands
        rectb(rgba, bw, bx, 18, bx + 1, 22, EWOK_FUR_SH)
    rectb(rgba, bw, 4, 18, 7, 22, EWOK_FUR_HI)       # lighter chest/belly (front)
    rectb(rgba, bw, 5, 19, 6, 22, EWOK_FUR_SH)       # belly center fold
    # Arms 2x6x2 @(16,14)/(0,23): brown fur + dark paw at the cuff.
    for (u0, v0) in ((16, 14), (0, 23)):
        shade_box(rgba, bw, u0, v0, 2, 6, 2, EWOK_FUR, EWOK_FUR_HI, EWOK_FUR_SH)
        speckle(rgba, bw, u0, v0 + 2, u0 + 8, v0 + 6, EWOK_FUR_SH, mod=3, salt=43)
        rectb(rgba, bw, u0, v0 + 6, u0 + 8, v0 + 8, EWOK_PAW)   # tiny hands
    # Legs 2x5x2 @(8,23)/(16,23): brown fur + dark feet at the sole.
    for u0 in (8, 16):
        shade_box(rgba, bw, u0, 23, 2, 5, 2, EWOK_FUR, EWOK_FUR_HI, EWOK_FUR_SH)
        speckle(rgba, bw, u0, 25, u0 + 8, 30, EWOK_FUR_SH, mod=3, salt=44)
        rectb(rgba, bw, u0, 29, u0 + 8, 30, EWOK_PAW)           # feet
    # Ears 1x2x2 @(24,14)/(24,18): russet outer with a dark inner hollow.
    for v0 in (14, 18):
        shade_box(rgba, bw, 24, v0, 1, 2, 2, EWOK_HOOD, EWOK_HOOD_HI, EWOK_HOOD_SH)
        rectb(rgba, bw, 26, v0 + 2, 27, v0 + 4, EWOK_FUR_SH)    # inner ear hollow

# Darth Maul (64x64): the iconic Zabrak Sith — crimson-red skin carved with
# angular black tattoos radiating across the face + scalp, yellow-red Sith
# eyes, and a crown of ten bone-white Zabrak horns; a jet-black Sith robe on
# the body/arms/legs (3 black tones for fold shading) with dark gloves/boots
# and a dark-red waist sash. Deterministic (rect() only, no RNG/speckle).
#
# HORN CROWN — the horn UV cells must match client/model/MaulModel.java (and
# gen_bbmodels.py) EXACTLY, because this single darth_maul.png feeds both the
# runtime model and the bbmodel. MaulModel rings 10 Zabrak horns (each a
# 1x3x1 cube, a child bone of 'head') around the crown; each horn's box-UV
# footprint is a 4x4 cell (width 2*(1+1)=4, height 1+3=4) at texOffs
# u=(i%4)*4, v=32+(i//4)*4 — tiling the free right-leg-overlay band
# u[0..16] x v[32..44] (Maul's right leg has no overlay cube, so it is unused).
MAUL_HORN_UV = [((i % 4) * 4, 32 + (i // 4) * 4) for i in range(10)]

MAUL_SKIN     = (0xB0, 0x18, 0x10, 0xFF)   # crimson Sith skin (egg primary #B01810)
MAUL_SKIN_HI  = (0xD0, 0x30, 0x24, 0xFF)   # lit cheek/brow
MAUL_SKIN_SH  = (0x7C, 0x10, 0x0C, 0xFF)   # shadowed jaw/side/back
MAUL_TATTOO   = (0x10, 0x10, 0x10, 0xFF)   # black tattoo (egg secondary #101010)
MAUL_EYE      = (0xF4, 0xC8, 0x24, 0xFF)   # Sith yellow iris
MAUL_EYE_RED  = (0xD8, 0x48, 0x10, 0xFF)   # yellow-red lower lid
MAUL_HORN     = (0xE6, 0xDE, 0xC6, 0xFF)   # bone-white / ivory
MAUL_HORN_HI  = (0xF6, 0xF0, 0xDC, 0xFF)   # lit tip
MAUL_HORN_SH  = (0xA6, 0x9A, 0x7E, 0xFF)   # shadowed base
MAUL_ROBE     = (0x1A, 0x1A, 0x1E, 0xFF)   # Sith robe base (near-black)
MAUL_ROBE_HI  = (0x2E, 0x2E, 0x36, 0xFF)   # fold highlight
MAUL_ROBE_SH  = (0x0C, 0x0C, 0x0E, 0xFF)   # fold shadow
MAUL_LEATHER  = (0x08, 0x08, 0x0A, 0xFF)   # gloves / boots (darkest)
MAUL_SASH     = (0x5A, 0x14, 0x12, 0xFF)   # dark-red waist sash
MAUL_SASH_SH  = (0x3E, 0x0E, 0x0C, 0xFF)   # sash shade

def paint_darth_maul(rgba):
    fill(rgba, MAUL_ROBE)                          # black Sith robe everywhere
    # ================= HEAD: crimson Zabrak skin (u0..32, v0..16) =============
    rect(rgba, 0, 0, 32, 16, MAUL_SKIN)
    # Scalp (up face u8..16,v0..8): black tattoo spokes radiating from the crown.
    rect(rgba, 11, 0, 13, 8, MAUL_TATTOO)          # front-back spine
    rect(rgba, 8, 3, 16, 5, MAUL_TATTOO)           # left-right bar
    for (sx, sy) in ((9, 1), (14, 1), (9, 6), (14, 6)):
        rect(rgba, sx, sy, sx + 1, sy + 1, MAUL_TATTOO)   # diagonal spoke tips
    # Temples/sides (east u0..8, west u16..24) + back (south u24..32): vertical
    # black stripes over red, with a lower-edge form shade (3rd skin tone).
    for u0 in (0, 16, 24):
        rect(rgba, u0 + 2, 8, u0 + 3, 16, MAUL_TATTOO)
        rect(rgba, u0 + 5, 8, u0 + 6, 16, MAUL_TATTOO)
    rect(rgba, 0, 14, 8, 16, MAUL_SKIN_SH)
    rect(rgba, 16, 14, 32, 16, MAUL_SKIN_SH)
    # Face (north u8..16,v8..16): crimson base kept DOMINANT, with angular black
    # tattoos radiating from a central spine (thin stripes, not a solid mask) and
    # yellow-red Sith eyes.
    rect(rgba, 11, 8, 13, 15, MAUL_TATTOO)         # central spine (forehead -> nose)
    rect(rgba, 9, 8, 10, 11, MAUL_TATTOO)          # forehead brow spike L
    rect(rgba, 14, 8, 15, 11, MAUL_TATTOO)         # forehead brow spike R (mirror)
    rect(rgba, 9, 10, 11, 11, MAUL_TATTOO)         # over-eye bar L
    rect(rgba, 13, 10, 15, 11, MAUL_TATTOO)        # over-eye bar R
    rect(rgba, 8, 12, 9, 14, MAUL_TATTOO)          # cheek stripe L (upper, outer)
    rect(rgba, 9, 13, 10, 15, MAUL_TATTOO)         # cheek stripe L (lower, stepped in)
    rect(rgba, 15, 12, 16, 14, MAUL_TATTOO)        # cheek stripe R (upper, outer)
    rect(rgba, 14, 13, 15, 15, MAUL_TATTOO)        # cheek stripe R (lower, stepped in)
    rect(rgba, 11, 15, 13, 16, MAUL_TATTOO)        # chin point
    rect(rgba, 9, 12, 11, 13, MAUL_SKIN_HI)        # lit cheek L (3rd skin tone)
    rect(rgba, 13, 12, 15, 13, MAUL_SKIN_HI)       # lit cheek R
    rect(rgba, 10, 11, 11, 12, MAUL_EYE)           # Sith eye L (yellow)
    rect(rgba, 13, 11, 14, 12, MAUL_EYE)           # Sith eye R
    rect(rgba, 10, 12, 11, 13, MAUL_EYE_RED)       # yellow-red lower lid L
    rect(rgba, 13, 12, 14, 13, MAUL_EYE_RED)       # yellow-red lower lid R
    # ================= HORN CROWN (ivory, 3 tones each) ======================
    # 10 horns, each a 4x4 box-UV cell (1x3x1 -> 2*(1+1) wide x (1+3) tall) in
    # the free right-leg-overlay band; painted here (before body/arms/legs,
    # none of which touch u[0..16] x v[32..44]).
    for (u0, v0) in MAUL_HORN_UV:
        rect(rgba, u0, v0, u0 + 4, v0 + 4, MAUL_HORN)          # ivory body
        rect(rgba, u0, v0, u0 + 4, v0 + 1, MAUL_HORN_HI)       # lit tip (up face)
        rect(rgba, u0, v0 + 3, u0 + 4, v0 + 4, MAUL_HORN_SH)   # shadowed base
    # ================= BODY: black Sith robe + dark-red sash =================
    rect(rgba, 16, 16, 40, 32, MAUL_ROBE)
    rect(rgba, 16, 16, 40, 17, MAUL_ROBE_HI)       # collar/shoulder highlight
    for fx in range(19, 40, 5):                    # vertical fold-shadow stripes
        rect(rgba, fx, 17, fx + 1, 30, MAUL_ROBE_SH)
    rect(rgba, 16, 24, 40, 26, MAUL_SASH)          # dark-red waist sash
    rect(rgba, 16, 25, 40, 26, MAUL_SASH_SH)       # sash lower shade
    rect(rgba, 16, 30, 40, 32, MAUL_ROBE_SH)       # hem shadow
    # ================= ARMS: robe sleeves + dark gloves ======================
    for (u0, v0) in ((40, 16), (32, 48)):
        rect(rgba, u0, v0, u0 + 16, v0 + 16, MAUL_ROBE)
        rect(rgba, u0, v0, u0 + 16, v0 + 1, MAUL_ROBE_HI)          # shoulder highlight
        rect(rgba, u0 + 5, v0 + 1, u0 + 6, v0 + 13, MAUL_ROBE_SH)  # sleeve fold
        rect(rgba, u0 + 10, v0 + 1, u0 + 11, v0 + 13, MAUL_ROBE_SH)
        rect(rgba, u0, v0 + 13, u0 + 16, v0 + 16, MAUL_LEATHER)    # glove (cuff/hand)
        rect(rgba, u0, v0 + 13, u0 + 16, v0 + 14, MAUL_ROBE_SH)    # glove top seam
    # ================= LEGS: robe trousers + dark boots ======================
    for (u0, v0) in ((0, 16), (16, 48)):
        rect(rgba, u0, v0, u0 + 16, v0 + 16, MAUL_ROBE)
        rect(rgba, u0, v0, u0 + 16, v0 + 1, MAUL_ROBE_HI)          # hip highlight
        rect(rgba, u0 + 5, v0 + 1, u0 + 6, v0 + 12, MAUL_ROBE_SH)  # trouser fold
        rect(rgba, u0 + 10, v0 + 1, u0 + 11, v0 + 12, MAUL_ROBE_SH)
        rect(rgba, u0, v0 + 12, u0 + 16, v0 + 16, MAUL_LEATHER)    # boot
        rect(rgba, u0, v0 + 12, u0 + 16, v0 + 13, MAUL_ROBE_SH)    # boot top seam

# Rancor (128x128): brown/tan leathery hide, darker spine, pale underbelly,
# ivory tusks + knuckle claws, deep-set red eyes, fanged maw.
# UV (gen_bbmodels RANCOR_CUBES == RancorModel.java): body 22x30x14 @(0,0);
# head 18x15x14 @(0,44); brow 20x4x5 @(64,86); jaw 14x6x10 @(0,109);
# right_tusk/left_tusk 2x6x2 @(120,0)/(120,8); right_arm/left_arm 8x28x8
# @(0,73)/(32,73); right_claw/left_claw 9x5x9 @(84,58)/(84,72);
# right_leg/left_leg 10x20x9 @(72,0)/(72,29); tail 6x6x12 @(48,109).
RANCOR_HIDE    = (0x7A, 0x5C, 0x3E, 0xFF)   # brown leathery
RANCOR_HIDE_HI = (0x94, 0x72, 0x50, 0xFF)
RANCOR_HIDE_SH = (0x5A, 0x42, 0x2C, 0xFF)
RANCOR_HIDE_DK = (0x40, 0x2E, 0x1E, 0xFF)   # spine / deep streaks
RANCOR_BELLY   = (0xB0, 0x9A, 0x74, 0xFF)   # pale underbelly
RANCOR_BELLY_SH= (0x94, 0x7E, 0x5C, 0xFF)
RANCOR_IVORY   = (0xD8, 0xCC, 0xA8, 0xFF)   # tusks / claws
RANCOR_IVORY_HI= (0xEC, 0xE2, 0xC4, 0xFF)
RANCOR_IVORY_DK= (0xA8, 0x98, 0x70, 0xFF)
RANCOR_TOOTH   = (0xC8, 0xBC, 0x98, 0xFF)
RANCOR_MOUTH   = (0x3A, 0x18, 0x14, 0xFF)
RANCOR_EYE     = (0xD8, 0x30, 0x24, 0xFF)   # red eye
RANCOR_EYE_DK  = (0x8A, 0x1C, 0x14, 0xFF)   # red socket rim

def paint_rancor(rgba):
    bw = 128
    fill_buf(rgba, (0, 0, 0, 0))
    # Body @(0,0), fp 72x44: brown hide; front face x[14,36) y[14,44), back
    # face x[50,72). Pale belly on the lower front, dark spine down the back.
    shade_box(rgba, bw, 0, 0, 22, 30, 14, RANCOR_HIDE, RANCOR_HIDE_HI, RANCOR_HIDE_SH)
    speckle(rgba, bw, 0, 14, 72, 44, RANCOR_HIDE_SH, mod=5, salt=1)
    speckle(rgba, bw, 0, 14, 72, 44, RANCOR_HIDE_DK, mod=11, salt=2)
    rectb(rgba, bw, 14, 32, 36, 44, RANCOR_BELLY)        # pale belly (front lower)
    rectb(rgba, bw, 14, 42, 36, 44, RANCOR_BELLY_SH)
    rectb(rgba, bw, 50, 14, 72, 44, RANCOR_HIDE_SH)      # back a touch darker
    rectb(rgba, bw, 59, 14, 63, 44, RANCOR_HIDE_DK)      # spine ridge
    # Head @(0,44), fp 64x29: face on front x[14,32) y[58,73).
    shade_box(rgba, bw, 0, 44, 18, 15, 14, RANCOR_HIDE, RANCOR_HIDE_HI, RANCOR_HIDE_SH)
    speckle(rgba, bw, 0, 58, 72, 73, RANCOR_HIDE_SH, mod=6, salt=3)
    rectb(rgba, bw, 14, 59, 32, 61, RANCOR_HIDE_DK)      # heavy brow shadow
    rectb(rgba, bw, 17, 61, 20, 64, RANCOR_EYE_DK)       # left socket
    rectb(rgba, bw, 26, 61, 29, 64, RANCOR_EYE_DK)       # right socket
    rectb(rgba, bw, 18, 62, 19, 63, RANCOR_EYE)          # left eye
    rectb(rgba, bw, 27, 62, 28, 63, RANCOR_EYE)          # right eye
    rectb(rgba, bw, 22, 65, 24, 67, RANCOR_HIDE_DK)      # nostrils/snout shadow
    rectb(rgba, bw, 15, 70, 31, 73, RANCOR_MOUTH)        # upper maw
    for tx in (16, 19, 22, 25, 28):                      # upper fangs
        rectb(rgba, bw, tx, 70, tx + 1, 72, RANCOR_TOOTH)
    # Brow @(64,86), fp 50x9: heavy hide ridge.
    shade_box(rgba, bw, 64, 86, 20, 4, 5, RANCOR_HIDE, RANCOR_HIDE_HI, RANCOR_HIDE_SH)
    speckle(rgba, bw, 64, 91, 114, 95, RANCOR_HIDE_DK, mod=7, salt=4)
    # Jaw @(0,109), fp 48x16: underbite, lower fangs on the front x[10,24).
    shade_box(rgba, bw, 0, 109, 14, 6, 10, RANCOR_HIDE, RANCOR_HIDE_HI, RANCOR_HIDE_SH)
    rectb(rgba, bw, 10, 119, 24, 121, RANCOR_MOUTH)      # inner mouth line
    for tx in (11, 14, 17, 20, 23):                      # lower fangs (jut up)
        rectb(rgba, bw, tx, 119, tx + 1, 121, RANCOR_TOOTH)
    # Tusks @(120,0)/(120,8), fp 8x8: ivory.
    for v0 in (0, 8):
        shade_box(rgba, bw, 120, v0, 2, 6, 2, RANCOR_IVORY, RANCOR_IVORY_HI, RANCOR_IVORY_DK)
    # Arms @(0,73)/(32,73), fp 32x36: hide, darker forearm.
    for u0 in (0, 32):
        shade_box(rgba, bw, u0, 73, 8, 28, 8, RANCOR_HIDE, RANCOR_HIDE_HI, RANCOR_HIDE_SH)
        speckle(rgba, bw, u0, 81, u0 + 32, 109, RANCOR_HIDE_SH, mod=5, salt=u0 + 5)
        rectb(rgba, bw, u0, 101, u0 + 32, 109, RANCOR_HIDE_DK)   # dark forearm band
    # Claws @(84,58)/(84,72), fp 36x14: ivory, dirty tips (front x[93,102)).
    for v0 in (58, 72):
        shade_box(rgba, bw, 84, v0, 9, 5, 9, RANCOR_IVORY, RANCOR_IVORY_HI, RANCOR_IVORY_DK)
        rectb(rgba, bw, 93, v0 + 11, 102, v0 + 14, RANCOR_IVORY_DK)  # claw tips
    # Legs @(72,0)/(72,29), fp 38x29: hide, clawed toes (front x[81,91)).
    for v0 in (0, 29):
        shade_box(rgba, bw, 72, v0, 10, 20, 9, RANCOR_HIDE, RANCOR_HIDE_HI, RANCOR_HIDE_SH)
        speckle(rgba, bw, 72, v0 + 9, 110, v0 + 29, RANCOR_HIDE_SH, mod=5, salt=v0 + 7)
        rectb(rgba, bw, 81, v0 + 26, 91, v0 + 29, RANCOR_HIDE_DK)    # foot shadow
        for cx in (82, 85, 88):
            rectb(rgba, bw, cx, v0 + 27, cx + 1, v0 + 29, RANCOR_IVORY)  # toe claws
    # Tail @(48,109), fp 40x18: hide tapering to a dark tip.
    shade_box(rgba, bw, 48, 109, 6, 6, 12, RANCOR_HIDE, RANCOR_HIDE_HI, RANCOR_HIDE_SH)
    speckle(rgba, bw, 48, 121, 88, 127, RANCOR_HIDE_DK, mod=6, salt=8)

# Jabba (128x64): tan-to-green-brown slimy hide with a paler belly, mottled
# slime sheen, big dark lidded eyes, and a wide dark mouth.
# UV (gen_bbmodels JABBA_CUBES == JabbaModel.java): body 26x20x20 @(0,0);
# tail 16x8x16 @(0,40); head 18x12x12 @(64,40); right_arm/left_arm 4x8x4
# @(92,0)/(108,0).
JABBA_HIDE    = (0x8C, 0x8A, 0x54, 0xFF)   # tan-green slimy
JABBA_HIDE_HI = (0xA6, 0xA4, 0x6C, 0xFF)
JABBA_HIDE_SH = (0x6A, 0x68, 0x3E, 0xFF)
JABBA_HIDE_DK = (0x50, 0x50, 0x30, 0xFF)   # deep folds / nostrils
JABBA_BELLY   = (0xC2, 0xB6, 0x84, 0xFF)   # paler tan belly
JABBA_BELLY_SH= (0xA6, 0x9A, 0x6E, 0xFF)
JABBA_SLIME   = (0x9E, 0x9C, 0x60, 0xFF)   # slime sheen dots
JABBA_EYE     = (0x1A, 0x16, 0x12, 0xFF)   # big dark eyes
JABBA_EYE_HI  = (0x50, 0x46, 0x38, 0xFF)   # heavy lid
JABBA_MOUTH   = (0x4A, 0x24, 0x22, 0xFF)   # wide mouth
JABBA_MOUTH_DK= (0x30, 0x16, 0x16, 0xFF)

def paint_jabba(rgba):
    bw = 128
    fill_buf(rgba, (0, 0, 0, 0))
    # Body @(0,0), fp 92x40: front face x[20,46) y[20,40); down face (belly)
    # x[46,72) y[0,20). Pale belly on the underside + lower front, slime mottle.
    shade_box(rgba, bw, 0, 0, 26, 20, 20, JABBA_HIDE, JABBA_HIDE_HI, JABBA_HIDE_SH)
    speckle(rgba, bw, 0, 20, 92, 40, JABBA_HIDE_SH, mod=6, salt=1)
    speckle(rgba, bw, 0, 20, 92, 40, JABBA_SLIME, mod=13, salt=2)
    rectb(rgba, bw, 46, 0, 72, 20, JABBA_BELLY)          # underside belly (down face)
    rectb(rgba, bw, 20, 30, 46, 40, JABBA_BELLY)         # belly roll (front lower)
    rectb(rgba, bw, 20, 38, 46, 40, JABBA_BELLY_SH)
    rectb(rgba, bw, 20, 33, 46, 34, JABBA_BELLY_SH)      # belly fold
    rectb(rgba, bw, 20, 36, 46, 37, JABBA_BELLY_SH)      # belly fold
    # Tail @(0,40), fp 64x24: hide, pale underside (down face x[32,48) y[40,56)).
    shade_box(rgba, bw, 0, 40, 16, 8, 16, JABBA_HIDE, JABBA_HIDE_HI, JABBA_HIDE_SH)
    speckle(rgba, bw, 0, 56, 64, 64, JABBA_HIDE_SH, mod=6, salt=3)
    speckle(rgba, bw, 0, 56, 64, 64, JABBA_SLIME, mod=13, salt=4)
    rectb(rgba, bw, 32, 40, 48, 56, JABBA_BELLY)         # tail underside
    # Head @(64,40), fp 60x24: face on front x[76,94) y[52,64).
    shade_box(rgba, bw, 64, 40, 18, 12, 12, JABBA_HIDE, JABBA_HIDE_HI, JABBA_HIDE_SH)
    speckle(rgba, bw, 64, 52, 124, 64, JABBA_HIDE_SH, mod=7, salt=5)
    rectb(rgba, bw, 78, 53, 82, 56, JABBA_EYE_HI)        # left lid
    rectb(rgba, bw, 88, 53, 92, 56, JABBA_EYE_HI)        # right lid
    rectb(rgba, bw, 79, 54, 81, 56, JABBA_EYE)           # left eye
    rectb(rgba, bw, 89, 54, 91, 56, JABBA_EYE)           # right eye
    rectb(rgba, bw, 83, 57, 84, 58, JABBA_HIDE_DK)       # nostril
    rectb(rgba, bw, 86, 57, 87, 58, JABBA_HIDE_DK)       # nostril
    rectb(rgba, bw, 76, 60, 94, 63, JABBA_MOUTH)         # wide mouth
    rectb(rgba, bw, 76, 62, 94, 63, JABBA_MOUTH_DK)      # lower-lip shadow
    # Arms @(92,0)/(108,0), fp 16x12: stubby hide.
    for u0 in (92, 108):
        shade_box(rgba, bw, u0, 0, 4, 8, 4, JABBA_HIDE, JABBA_HIDE_HI, JABBA_HIDE_SH)

# Emperor Palpatine (64x64): a hunched hooded Sith drowned in near-black robes,
# a pale gnarled face sunk in the shadow of the cowl. Box-UV offsets match
# gen_bbmodels.py PALPATINE_CUBES / PalpatineModel.java exactly.
# UV: robe 9x11x5 @(36,0); robe_skirt 11x14x7 @(0,0); head 6x6x5 @(30,21);
# cowl 8x8x7 @(0,21); right_arm/left_arm 4x10x4 @(0,36)/(16,36); right_hand/
# left_hand 3x4x3 @(44,36)/(44,43); right_leg/left_leg 3x4x3 @(32,36)/(32,43).
PALP_ROBE      = (0x1A, 0x18, 0x1E, 0xFF)   # near-black robe, faintly cool
PALP_ROBE_HI   = (0x2C, 0x29, 0x33, 0xFF)   # dark-grey fold highlight
PALP_ROBE_SH   = (0x0C, 0x0B, 0x10, 0xFF)   # deep fold shadow
PALP_ROBE_TINT = (0x30, 0x18, 0x26, 0xFF)   # bruised-purple/sith tint in folds
PALP_COWL      = (0x12, 0x10, 0x16, 0xFF)   # even darker hood
PALP_COWL_HI   = (0x22, 0x1F, 0x28, 0xFF)
PALP_COWL_SH   = (0x07, 0x06, 0x0A, 0xFF)
PALP_VOID      = (0x04, 0x04, 0x06, 0xFF)   # deep shadow inside the cowl
PALP_SKIN      = (0xC2, 0xB6, 0xA6, 0xFF)   # pale sickly grey-tan
PALP_SKIN_HI   = (0xD6, 0xCC, 0xBE, 0xFF)
PALP_SKIN_SH   = (0x94, 0x86, 0x7A, 0xFF)   # wrinkle shadow
PALP_SKIN_DK   = (0x5E, 0x52, 0x4A, 0xFF)   # sunken sockets / deep wrinkle
PALP_EYE       = (0xC8, 0xA8, 0x38, 0xFF)   # faint sith-yellow glint
PALP_HAND      = (0xBC, 0xB0, 0xA0, 0xFF)   # bony pale hands
PALP_HAND_HI   = (0xD0, 0xC6, 0xB8, 0xFF)
PALP_HAND_SH   = (0x8A, 0x7E, 0x72, 0xFF)
PALP_BOOT      = (0x0E, 0x0D, 0x11, 0xFF)   # near-black boots/leg stubs
PALP_BOOT_HI   = (0x1C, 0x1A, 0x20, 0xFF)
PALP_BOOT_SH   = (0x06, 0x05, 0x08, 0xFF)

def paint_palpatine(rgba):
    fill(rgba, (0, 0, 0, 0))
    # Robe skirt @(0,0), fp 36x21: floor-length flare, front face x[7,18)
    # y[7,21). Near-black with deep vertical folds, a bruised-purple tint bled
    # into two fold shadows, and a shaded hem.
    shade_box(rgba, W, 0, 0, 11, 14, 7, PALP_ROBE, PALP_ROBE_HI, PALP_ROBE_SH)
    speckle(rgba, W, 0, 7, 36, 21, PALP_ROBE_SH, mod=6, salt=41)
    for fx in (8, 11, 14, 17):                       # vertical skirt folds
        rect(rgba, fx, 7, fx + 1, 21, PALP_ROBE_SH)
    rect(rgba, 10, 12, 11, 20, PALP_ROBE_TINT)       # sith tint deep in a fold
    rect(rgba, 15, 12, 16, 20, PALP_ROBE_TINT)
    rect(rgba, 7, 19, 18, 21, PALP_ROBE_SH)          # hem shadow

    # Robe torso @(36,0), fp 28x16: front face x[41,50) y[5,16). Crossed-over
    # collar V at the neck, a central seam, tinted chest folds.
    shade_box(rgba, W, 36, 0, 9, 11, 5, PALP_ROBE, PALP_ROBE_HI, PALP_ROBE_SH)
    rect(rgba, 41, 5, 50, 7, PALP_ROBE_SH)           # collar shadow band
    rect(rgba, 45, 5, 46, 9, PALP_COWL_SH)           # V-collar left fold
    rect(rgba, 44, 6, 45, 8, PALP_COWL_SH)
    rect(rgba, 46, 6, 47, 8, PALP_COWL_SH)           # V-collar right fold
    rect(rgba, 45, 8, 46, 16, PALP_ROBE_SH)          # central seam
    rect(rgba, 42, 9, 43, 16, PALP_ROBE_TINT)        # tint in a chest fold
    rect(rgba, 48, 9, 49, 16, PALP_ROBE_TINT)

    # Cowl @(0,21), fp 30x15: front face x[7,15) y[28,36). Very dark hood; the
    # heavy brow (top 2 rows) stays solid, the lower-front is carved
    # transparent (the hood opening) so the shadowed face behind peeks out,
    # ringed by deep void shadow on the inner edges.
    shade_box(rgba, W, 0, 21, 8, 8, 7, PALP_COWL, PALP_COWL_HI, PALP_COWL_SH)
    speckle(rgba, W, 0, 28, 30, 36, PALP_COWL_SH, mod=7, salt=42)
    rect(rgba, 7, 28, 15, 30, PALP_VOID)             # heavy overhanging brow
    rect(rgba, 7, 30, 8, 36, PALP_VOID)              # inner-left of the opening
    rect(rgba, 14, 30, 15, 36, PALP_VOID)            # inner-right of the opening
    rect(rgba, 8, 30, 14, 36, (0, 0, 0, 0))          # hood opening (transparent)

    # Face @(30,21), fp 22x11: front face x[35,41) y[26,32). Pale, gaunt,
    # sunken — brow shadow up top (under the hood), hollow eyes with a faint
    # sith-yellow glint, a hooked nose, cheek hollows, a down-turned mouth.
    shade_box(rgba, W, 30, 21, 6, 6, 5, PALP_SKIN, PALP_SKIN_HI, PALP_SKIN_SH)
    rect(rgba, 35, 26, 41, 27, PALP_SKIN_DK)         # deep under-hood brow shadow
    rect(rgba, 35, 27, 41, 28, PALP_SKIN_SH)         # furrowed forehead
    rect(rgba, 35, 28, 37, 30, PALP_SKIN_DK)         # left eye socket
    rect(rgba, 39, 28, 41, 30, PALP_SKIN_DK)         # right eye socket
    rect(rgba, 35, 29, 36, 30, PALP_EYE)             # left eye glint
    rect(rgba, 40, 29, 41, 30, PALP_EYE)             # right eye glint
    rect(rgba, 37, 29, 38, 31, PALP_SKIN_SH)         # hooked nose shadow
    rect(rgba, 35, 30, 36, 31, PALP_SKIN_SH)         # left cheek hollow
    rect(rgba, 40, 30, 41, 31, PALP_SKIN_SH)         # right cheek hollow
    rect(rgba, 36, 31, 40, 32, PALP_SKIN_SH)         # jaw shadow
    rect(rgba, 37, 31, 39, 32, PALP_SKIN_DK)         # down-turned mouth

    # Sleeves @(0,36)/(16,36), fp 16x14: front face x[u+4,u+8) y[40,50). Wide
    # draped robe with vertical folds + a shaded cuff at the wrist.
    for u0 in (0, 16):
        shade_box(rgba, W, u0, 36, 4, 10, 4, PALP_ROBE, PALP_ROBE_HI, PALP_ROBE_SH)
        rect(rgba, u0 + 5, 40, u0 + 6, 50, PALP_ROBE_SH)   # drape fold
        rect(rgba, u0 + 7, 41, u0 + 8, 50, PALP_ROBE_SH)   # drape fold
        rect(rgba, u0, 48, u0 + 16, 50, PALP_ROBE_SH)      # cuff shadow

    # Hands @(44,36)/(44,43), fp 12x7: front face x[47,50) y[v+3,v+7). Bony
    # pale claw with a knuckle groove, fingertip shadow + a dark cuff row.
    for v0 in (36, 43):
        shade_box(rgba, W, 44, v0, 3, 4, 3, PALP_HAND, PALP_HAND_HI, PALP_HAND_SH)
        rect(rgba, 44, v0 + 3, 56, v0 + 4, PALP_ROBE_SH)   # cuff over the wrist
        rect(rgba, 48, v0 + 4, 49, v0 + 7, PALP_HAND_SH)   # knuckle groove
        rect(rgba, 47, v0 + 6, 50, v0 + 7, PALP_HAND_SH)   # fingertip shadow

    # Leg stubs @(32,36)/(32,43), fp 12x7: near-black, all but hidden under the
    # hem — just a shaded hem row up top.
    for v0 in (36, 43):
        shade_box(rgba, W, 32, v0, 3, 4, 3, PALP_BOOT, PALP_BOOT_HI, PALP_BOOT_SH)
        rect(rgba, 32, v0 + 3, 44, v0 + 4, PALP_ROBE_SH)   # hem shadow at the top

MOBS = {
    'stormtrooper': paint_stormtrooper,
    'battle_droid': paint_battle_droid,
    'jedi_knight': paint_jedi_knight,
    'darth_vader': paint_darth_vader,
    'darth_maul': paint_darth_maul,
    'luke_skywalker': paint_luke_skywalker,
    'obi_wan': paint_obi_wan,
    'astromech': paint_astromech,
    'boba_fett': paint_boba_fett,
    'han_solo': paint_han_solo,
    'princess_leia': paint_princess_leia,
    'landspeeder': paint_landspeeder,
    'jawa': paint_jawa,
    'tusken_raider': paint_tusken_raider,
    'rebel_trooper': paint_rebel_trooper,
    'snowtrooper': paint_snowtrooper,
    'tauntaun': paint_tauntaun,
    'probe_droid': paint_probe_droid,
    'dragonsnake': paint_dragonsnake,
    'yoda': paint_yoda,
    # Wave-3: speeder_bike + band_droid use the default 64x64 canvas.
    'speeder_bike': paint_speeder_bike,
    'band_droid': paint_band_droid,
    # Companions: Chewbacca uses the default 64x64 canvas (Grogu is 32x32,
    # see SIZED_MOBS).
    'chewbacca': paint_chewbacca,
    # Emperor Palpatine: hooded-Sith humanoid on the default 64x64 canvas.
    'palpatine': paint_palpatine,
}

# Mobs whose skins are NOT on the default 64x64 canvas: name -> (w, h, fn).
SIZED_MOBS = {
    'bantha': (128, 64, paint_bantha),
    'wampa': (128, 64, paint_wampa),
    'bogwing': (32, 32, paint_bogwing),
    # Wave-3 vehicles.
    'xwing': (128, 128, paint_xwing),
    'tie_fighter': (128, 64, paint_tie_fighter),
    'at_at': (256, 128, paint_at_at),
    # Companion: Grogu on a compact 32x32 canvas.
    'grogu': (32, 32, paint_grogu),
    # Forest native: Ewok on a compact 32x32 canvas.
    'ewok': (32, 32, paint_ewok),
    # Jabba's Palace beasts.
    'rancor': (128, 128, paint_rancor),
    'jabba': (128, 64, paint_jabba),
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

# Han Solo worn-armor palette additions (reuses HAN_SHIRT/HAN_SHIRT_DIM/
# HAN_VEST*/HAN_TROUSER*/HAN_STRIPE/HAN_BELT/HAN_BUCKLE from paint_han_solo
# above) plus cap and boot tones.
HAN_CAP      = (0x4A, 0x35, 0x22, 0xFF)   # dark-brown cap (reuses HAN_HAIR tone)
HAN_CAP_HI   = (0x64, 0x4A, 0x32, 0xFF)
HAN_CAP_DK   = (0x33, 0x24, 0x16, 0xFF)
HAN_BOOT     = (0x54, 0x3A, 0x24, 0xFF)   # brown boots
HAN_BOOT_HI  = (0x6E, 0x50, 0x34, 0xFF)
HAN_BOOT_DK  = (0x3A, 0x28, 0x18, 0xFF)

def paint_han_solo_armor_layers(humanoid_rgba, leggings_rgba):
    """Worn-armor sheets for the Han Solo equipment asset. Same sampled
    UV extents as paint_stormtrooper_armor_layers (that function's region
    comments are the authoritative map — copy its rect() extents exactly,
    substituting palettes):
      humanoid sheet — head block: HAN_CAP 3-tone cap with HAN_CAP_DK
        band row; body block: HAN_VEST over HAN_SHIRT with the open-front
        column (shirt tone showing through center), HAN_BELT+HAN_BUCKLE
        waist rows; arm block: HAN_SHIRT sleeves with HAN_VEST shoulder
        strap rows and HAN_SHIRT_DIM elbow fold; leg block (BOOTS on this
        sheet): HAN_BOOT 3-tone on the lower rows only, transparent above.
      leggings sheet — leg block only: HAN_TROUSER 3-tone with the 1px
        HAN_STRIPE bloodstripe column and HAN_TROUSER_DK inner shadow.
    Every region: 3+ tones (art gate)."""
    fill_buf(humanoid_rgba, (0, 0, 0, 0))
    fill_buf(leggings_rgba, (0, 0, 0, 0))

    # ---- Head block (cap), UV 0..32,0..16 ----
    # Top/bottom faces: y0..7, inset to x8..24, 3-tone gradient.
    rect(humanoid_rgba, 8, 0, 13, 8, HAN_CAP_HI)
    rect(humanoid_rgba, 13, 0, 19, 8, HAN_CAP)
    rect(humanoid_rgba, 19, 0, 24, 8, HAN_CAP_DK)
    # Wraparound sides: y8..16, full x0..32.
    rect(humanoid_rgba, 0, 8, 32, 9, HAN_CAP_HI)    # cap crown highlight
    rect(humanoid_rgba, 0, 9, 32, 15, HAN_CAP)      # cap base band
    rect(humanoid_rgba, 0, 15, 32, 16, HAN_CAP_DK)  # brim band, distinct from base
    # Front face window (x9..15,y10..15): the cap only covers the crown and
    # brim, leaving the wearer's face visible — unlike the stormtrooper
    # helmet, which boxes the whole head. 1px cap brim retained at y8..10
    # above the window and the y15..16 brim band below stays cap-colored.
    rect(humanoid_rgba, 9, 10, 15, 15, (0, 0, 0, 0))

    # ---- Body block (vest), UV 16..40,16..32 ----
    # Top/bottom faces: y16..19, inset to x20..28.
    rect(humanoid_rgba, 20, 16, 24, 20, HAN_VEST_HI)
    rect(humanoid_rgba, 24, 16, 28, 20, HAN_VEST_DK)
    # Wraparound: y20..32, full x16..40.
    rect(humanoid_rgba, 16, 20, 40, 21, HAN_VEST_HI)  # shoulder/lapel highlight
    rect(humanoid_rgba, 16, 21, 40, 26, HAN_VEST)     # vest base
    rect(humanoid_rgba, 16, 26, 40, 27, HAN_VEST_DK)  # vest seam line
    rect(humanoid_rgba, 16, 27, 40, 29, HAN_VEST)     # lower vest
    rect(humanoid_rgba, 16, 29, 40, 30, HAN_VEST_DK)  # vest hem seam
    rect(humanoid_rgba, 16, 30, 40, 32, HAN_BELT)     # waist belt row
    rect(humanoid_rgba, 22, 30, 26, 32, HAN_BUCKLE)   # belt buckle
    # Open-front column: shirt tone shows through the vest opening,
    # painted last (over the vest fill) so it stays a continuous strip
    # from collar down to the belt. Centered on the front face (x20..28),
    # not the right/left side faces the wraparound strip also covers.
    rect(humanoid_rgba, 22, 21, 26, 30, HAN_SHIRT)

    # ---- Arm block (sleeve), UV 40..56,16..32 ----
    # Top/bottom faces: y16..19, inset to x44..52.
    rect(humanoid_rgba, 44, 16, 48, 20, HAN_SHIRT)
    rect(humanoid_rgba, 48, 16, 52, 20, HAN_SHIRT_DIM)
    # Wraparound: y20..32, full x40..56.
    rect(humanoid_rgba, 40, 20, 56, 22, HAN_VEST)       # vest shoulder strap peek
    rect(humanoid_rgba, 40, 22, 56, 26, HAN_SHIRT)      # upper sleeve
    rect(humanoid_rgba, 40, 26, 56, 27, HAN_SHIRT_DIM)  # elbow fold
    rect(humanoid_rgba, 40, 27, 56, 30, HAN_SHIRT)      # forearm
    rect(humanoid_rgba, 40, 30, 56, 32, HAN_SHIRT_DIM)  # wrist shade

    # ---- Leg block (boots, on the humanoid sheet), UV 0..16,16..32 ----
    # Top/bottom faces: y16..19, inset to x4..12 (soles visible from below).
    rect(humanoid_rgba, 4, 16, 8, 20, HAN_BOOT_HI)
    rect(humanoid_rgba, 8, 16, 12, 20, HAN_BOOT_DK)
    # Boots are ankle-height — transparent above y24 in the wraparound
    # strip, unlike the stormtrooper's full-leg armor plating.
    rect(humanoid_rgba, 0, 24, 16, 26, HAN_BOOT_HI)  # boot top highlight
    rect(humanoid_rgba, 0, 26, 16, 30, HAN_BOOT)     # boot body
    rect(humanoid_rgba, 0, 30, 16, 32, HAN_BOOT_DK)  # sole shade

    # ---- Leggings sheet: leg block only, UV 0..16,16..32 ----
    # Top/bottom faces: y16..19, inset to x4..12.
    rect(leggings_rgba, 4, 16, 8, 20, HAN_TROUSER)
    rect(leggings_rgba, 8, 16, 12, 20, HAN_TROUSER_DK)
    # Wraparound: y20..32, full x0..16 — horizontal bands first (thigh/shin
    # base, darker ankle cuff), then the full-height overlay columns (inner
    # shadow, bloodstripe) so the stripe runs the full leg height instead of
    # being clipped by the cuff rows.
    rect(leggings_rgba, 0, 20, 16, 21, HAN_TROUSER)     # thigh top highlight
    rect(leggings_rgba, 0, 21, 16, 30, HAN_TROUSER)     # thigh/shin base
    rect(leggings_rgba, 0, 30, 16, 32, HAN_TROUSER_DK)  # ankle cuff, darker
    rect(leggings_rgba, 1, 20, 3, 32, HAN_TROUSER_DK)   # inner shadow column
    rect(leggings_rgba, 14, 20, 15, 32, HAN_STRIPE)     # bloodstripe column

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

    for name, (mw, mh, paint_fn) in SIZED_MOBS.items():
        rgba = bytearray(mw * mh * 4)
        paint_fn(rgba)
        out_path = os.path.join(out_dir, f'{name}.png')
        write_png(out_path, rgba, width=mw, height=mh)
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

    humanoid_rgba = bytearray(ARMOR_W * ARMOR_H * 4)
    leggings_rgba = bytearray(ARMOR_W * ARMOR_H * 4)
    paint_han_solo_armor_layers(humanoid_rgba, leggings_rgba)
    write_png(os.path.join(humanoid_dir, 'han_solo.png'), humanoid_rgba,
              width=ARMOR_W, height=ARMOR_H)
    write_png(os.path.join(leggings_dir, 'han_solo.png'), leggings_rgba,
              width=ARMOR_W, height=ARMOR_H)
    print('han solo armor layers')
    print('OK')
