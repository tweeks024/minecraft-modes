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
    """64x64 placeholder humanoid skin. Dark cowboy palette."""
    w = h = 64
    rgba = bytearray(w * h * 4)
    SKIN = (0x6B, 0x8E, 0x23, 0xFF)
    SHIRT = (0x3A, 0x2A, 0x1A, 0xFF)
    PANTS = (0x2A, 0x1A, 0x10, 0xFF)
    HAT = (0x10, 0x10, 0x10, 0xFF)
    solid_rect(rgba, w, 8, 8, 16, 16, SKIN)
    solid_rect(rgba, w, 8, 8, 16, 10, HAT)
    solid_rect(rgba, w, 16, 16, 24, 32, SHIRT)
    solid_rect(rgba, w, 40, 16, 48, 32, SHIRT)
    solid_rect(rgba, w, 0, 16, 8, 32, PANTS)
    solid_rect(rgba, w, 16, 48, 24, 64, PANTS)
    solid_rect(rgba, w, 32, 48, 40, 64, SHIRT)
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
