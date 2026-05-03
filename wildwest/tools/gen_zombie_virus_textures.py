#!/usr/bin/env python3
"""Generate placeholder PNGs for the zombie-virus feature.

Outputs:
- 16x16 tainted_vial item icon (vial silhouette w/ green liquid)
- 16x16 walker_spawn_egg flat icon
- 64x64 walker entity texture (humanoid skin, dark cowboy palette)
- 18x18 effect icons (festering_wound, zombified, curing_shake)
"""
import struct, zlib, os, sys

ROOT = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.abspath(os.path.join(ROOT, '..', 'src', 'main', 'resources', 'assets', 'wildwest'))

def write_png(path, w, h, rgba):
    sig = b'\x89PNG\r\n\x1a\n'
    def chunk(tag, data):
        crc = zlib.crc32(tag + data) & 0xffffffff
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', crc)
    ihdr = struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        raw.extend(rgba[4*w*y : 4*w*(y+1)])
    idat = zlib.compress(bytes(raw))
    out = sig + chunk(b'IHDR', ihdr) + chunk(b'IDAT', idat) + chunk(b'IEND', b'')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb') as f:
        f.write(out)

def solid_rect(rgba, w, x0, y0, x1, y1, color):
    r, g, b, a = color
    for y in range(y0, y1):
        for x in range(x0, x1):
            i = 4 * (y * w + x)
            rgba[i+0] = r; rgba[i+1] = g; rgba[i+2] = b; rgba[i+3] = a

def make_vial():
    w = h = 16
    rgba = bytearray(w * h * 4)
    GLASS = (200, 220, 220, 0xFF)
    LIQUID = (90, 140, 60, 0xFF)
    DARK = (40, 30, 30, 0xFF)
    solid_rect(rgba, w, 6, 2, 10, 4, DARK)
    solid_rect(rgba, w, 5, 4, 11, 14, GLASS)
    solid_rect(rgba, w, 4, 13, 12, 14, DARK)
    solid_rect(rgba, w, 6, 8, 10, 13, LIQUID)
    return w, h, rgba

def make_spawn_egg():
    w = h = 16
    rgba = bytearray(w * h * 4)
    PRIMARY = (0x1A, 0x3A, 0x1A)
    SECONDARY = (0x6B, 0x8E, 0x23)
    EGG = [
        "................","................","......####......",".....######.....",
        ".....######.....","....########....","....########....","...##########...",
        "...##########...","..############..","..############..","..############..",
        "...##########...","....########....",".....######.....","................",
    ]
    SPECKLES = [
        "................","................","................","......#.........",
        "....#.....#.....",".......#........","..#..........#..",".....#.....#....",
        "...#............","........#.......",".#...........#..","....#....#......",
        ".....#..........","................","................","................",
    ]
    for y in range(h):
        for x in range(w):
            if EGG[y][x] != '#': continue
            i = 4 * (y * w + x)
            r, g, b = SECONDARY if SPECKLES[y][x] == '#' else PRIMARY
            rgba[i+0] = r; rgba[i+1] = g; rgba[i+2] = b; rgba[i+3] = 0xFF
    return w, h, rgba

def make_walker_entity():
    """64x64 humanoid skin, vanilla 1.8+ layout. Walker = decayed cowboy.

    UV regions painted (front faces of each cube, plus sides where reachable):
      head:      (0..32, 0..16)  — face front at (8..16, 8..16)
      body:      (16..40, 16..32) — front at (20..28, 20..32)
      right arm: (40..56, 16..32) — front at (44..48, 20..32)
      right leg: (0..16, 16..32)  — front at (4..8, 20..32)
      left leg:  (16..32, 48..64) — front at (20..24, 52..64)
      left arm:  (32..48, 48..64) — front at (36..40, 52..64)
    """
    w = h = 64
    rgba = bytearray(w * h * 4)
    SKIN  = (0x7A, 0xA0, 0x2E, 0xFF)   # sickly green
    SKIN_D = (0x4E, 0x6A, 0x18, 0xFF)  # shadowed skin (jaw, decay patches)
    HAIR   = (0x1A, 0x12, 0x0A, 0xFF)  # near-black matted hair
    SHIRT  = (0x5C, 0x42, 0x28, 0xFF)  # mid-brown shirt
    SLEEVE = (0x42, 0x2C, 0x18, 0xFF)  # darker arm color
    PANTS  = (0x22, 0x18, 0x10, 0xFF)  # dark pants
    BOOT   = (0x0E, 0x08, 0x06, 0xFF)  # near-black boots
    EYE    = (0xC8, 0x10, 0x10, 0xFF)  # red eyes

    # ---- Head box (full UV strip 0..32 x 0..16 around the cube) ----
    # Top of head (up face, 8..16, 0..8) — hair
    solid_rect(rgba, w, 8, 0, 16, 8, HAIR)
    # Bottom of head (down face, 16..24, 0..8) — neck shadow
    solid_rect(rgba, w, 16, 0, 24, 8, SKIN_D)
    # Right side of head (0..8, 8..16) — skin
    solid_rect(rgba, w, 0, 8, 8, 16, SKIN)
    # Front face (8..16, 8..16) — skin
    solid_rect(rgba, w, 8, 8, 16, 16, SKIN)
    # Left side (16..24, 8..16) — skin
    solid_rect(rgba, w, 16, 8, 24, 16, SKIN)
    # Back (24..32, 8..16) — hair
    solid_rect(rgba, w, 24, 8, 32, 16, HAIR)
    # Hairline strip across forehead
    solid_rect(rgba, w, 8, 8, 16, 9, HAIR)
    # Eyes — two red dots
    solid_rect(rgba, w, 10, 11, 12, 12, EYE)
    solid_rect(rgba, w, 13, 11, 15, 12, EYE)
    # Mouth — short dark line
    solid_rect(rgba, w, 11, 14, 14, 15, SKIN_D)
    # Cheek decay smudges
    solid_rect(rgba, w, 9, 13, 10, 14, SKIN_D)

    # ---- Body box (16..40 x 16..32) ----
    # Top (20..28, 16..20) — shirt
    solid_rect(rgba, w, 20, 16, 28, 20, SHIRT)
    # Bottom (28..36, 16..20) — pants band
    solid_rect(rgba, w, 28, 16, 36, 20, PANTS)
    # Right side (16..20, 20..32) — shirt
    solid_rect(rgba, w, 16, 20, 20, 32, SHIRT)
    # Front (20..28, 20..32) — shirt
    solid_rect(rgba, w, 20, 20, 28, 32, SHIRT)
    # Left (28..32, 20..32) — shirt
    solid_rect(rgba, w, 28, 20, 32, 32, SHIRT)
    # Back (32..40, 20..32) — shirt
    solid_rect(rgba, w, 32, 20, 40, 32, SHIRT)
    # Vest seam down center-front (visual interest)
    solid_rect(rgba, w, 23, 21, 25, 31, SLEEVE)

    # ---- Right arm (40..56 x 16..32, 4 wide front) ----
    # Top cap (44..48, 16..20) — sleeve
    solid_rect(rgba, w, 44, 16, 48, 20, SLEEVE)
    # Bottom cap — bare hand (skin)
    solid_rect(rgba, w, 48, 16, 52, 20, SKIN)
    # Right side (40..44, 20..32) — sleeve
    solid_rect(rgba, w, 40, 20, 44, 32, SLEEVE)
    # Front (44..48, 20..32) — sleeve top, skin hand
    solid_rect(rgba, w, 44, 20, 48, 28, SLEEVE)
    solid_rect(rgba, w, 44, 28, 48, 32, SKIN)
    # Left (48..52, 20..32) — sleeve top, skin hand
    solid_rect(rgba, w, 48, 20, 52, 28, SLEEVE)
    solid_rect(rgba, w, 48, 28, 52, 32, SKIN)
    # Back (52..56, 20..32)
    solid_rect(rgba, w, 52, 20, 56, 28, SLEEVE)
    solid_rect(rgba, w, 52, 28, 56, 32, SKIN)

    # ---- Right leg (0..16 x 16..32, 4 wide front) ----
    # Top cap (4..8, 16..20)
    solid_rect(rgba, w, 4, 16, 8, 20, PANTS)
    # Bottom cap — boot
    solid_rect(rgba, w, 8, 16, 12, 20, BOOT)
    # Right side (0..4, 20..32) — pants then boot
    solid_rect(rgba, w, 0, 20, 4, 30, PANTS)
    solid_rect(rgba, w, 0, 30, 4, 32, BOOT)
    # Front (4..8, 20..32)
    solid_rect(rgba, w, 4, 20, 8, 30, PANTS)
    solid_rect(rgba, w, 4, 30, 8, 32, BOOT)
    # Left (8..12, 20..32)
    solid_rect(rgba, w, 8, 20, 12, 30, PANTS)
    solid_rect(rgba, w, 8, 30, 12, 32, BOOT)
    # Back (12..16, 20..32)
    solid_rect(rgba, w, 12, 20, 16, 30, PANTS)
    solid_rect(rgba, w, 12, 30, 16, 32, BOOT)

    # ---- Left leg (16..32 x 48..64) — mirror of right leg ----
    solid_rect(rgba, w, 20, 48, 24, 52, PANTS)
    solid_rect(rgba, w, 24, 48, 28, 52, BOOT)
    solid_rect(rgba, w, 16, 52, 20, 62, PANTS)
    solid_rect(rgba, w, 16, 62, 20, 64, BOOT)
    solid_rect(rgba, w, 20, 52, 24, 62, PANTS)
    solid_rect(rgba, w, 20, 62, 24, 64, BOOT)
    solid_rect(rgba, w, 24, 52, 28, 62, PANTS)
    solid_rect(rgba, w, 24, 62, 28, 64, BOOT)
    solid_rect(rgba, w, 28, 52, 32, 62, PANTS)
    solid_rect(rgba, w, 28, 62, 32, 64, BOOT)

    # ---- Left arm (32..48 x 48..64) — mirror of right arm ----
    solid_rect(rgba, w, 36, 48, 40, 52, SLEEVE)
    solid_rect(rgba, w, 40, 48, 44, 52, SKIN)
    solid_rect(rgba, w, 32, 52, 36, 64, SLEEVE)
    solid_rect(rgba, w, 36, 52, 40, 60, SLEEVE)
    solid_rect(rgba, w, 36, 60, 40, 64, SKIN)
    solid_rect(rgba, w, 40, 52, 44, 60, SLEEVE)
    solid_rect(rgba, w, 40, 60, 44, 64, SKIN)
    solid_rect(rgba, w, 44, 52, 48, 60, SLEEVE)
    solid_rect(rgba, w, 44, 60, 48, 64, SKIN)

    return w, h, rgba

def make_effect_icon(primary, accent):
    """18x18 effect icon: filled circle of primary, accent dot in center."""
    w = h = 18
    rgba = bytearray(w * h * 4)
    cx = cy = 8
    for y in range(h):
        for x in range(w):
            d2 = (x-cx)*(x-cx) + (y-cy)*(y-cy)
            if d2 <= 64:
                i = 4 * (y * w + x)
                if d2 <= 4:
                    rgba[i+0], rgba[i+1], rgba[i+2] = accent
                else:
                    rgba[i+0], rgba[i+1], rgba[i+2] = primary
                rgba[i+3] = 0xFF
    return w, h, rgba

if __name__ == '__main__':
    w, h, rgba = make_vial()
    write_png(os.path.join(ASSETS, 'textures', 'item', 'tainted_vial.png'), w, h, rgba)
    print('tainted_vial.png')

    w, h, rgba = make_spawn_egg()
    write_png(os.path.join(ASSETS, 'textures', 'item', 'walker_spawn_egg.png'), w, h, rgba)
    print('walker_spawn_egg.png')

    w, h, rgba = make_walker_entity()
    write_png(os.path.join(ASSETS, 'textures', 'entity', 'walker.png'), w, h, rgba)
    print('walker.png')

    for name, primary, accent in [
        ('festering_wound', (0x6B, 0x8E, 0x23), (0x4A, 0x3A, 0x10)),
        ('zombified',       (0x4A, 0x7C, 0x2E), (0x10, 0x30, 0x10)),
        ('curing_shake',    (0xFF, 0xD7, 0x00), (0xFF, 0xFF, 0xFF)),
    ]:
        w, h, rgba = make_effect_icon(primary, accent)
        write_png(os.path.join(ASSETS, 'textures', 'mob_effect', f'{name}.png'), w, h, rgba)
        print(f'{name}.png')
