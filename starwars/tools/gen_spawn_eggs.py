#!/usr/bin/env python3
"""Generate 16x16 spawn-egg icon PNGs for the starwars mobs.

Uses a faction-colored egg silhouette with darker speckles, matching the
vanilla spawn-egg vibe but rendered as a flat icon (no live tint).
"""
import struct, zlib, os, sys

W = H = 16

EGG = [
    "................",
    "................",
    "......####......",
    ".....######.....",
    ".....######.....",
    "....########....",
    "....########....",
    "...##########...",
    "...##########...",
    "..############..",
    "..############..",
    "..############..",
    "...##########...",
    "....########....",
    ".....######.....",
    "................",
]

SPECKLES = [
    "................",
    "................",
    "................",
    "......#.........",
    "....#.....#.....",
    ".......#........",
    "..#..........#..",
    ".....#.....#....",
    "...#............",
    "........#.......",
    ".#...........#..",
    "....#....#......",
    ".....#..........",
    "................",
    "................",
    "................",
]

def write_png(path, rgba):
    sig = b'\x89PNG\r\n\x1a\n'
    def chunk(tag, data):
        crc = zlib.crc32(tag + data) & 0xffffffff
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', crc)
    ihdr = struct.pack('>IIBBBBB', W, H, 8, 6, 0, 0, 0)
    raw = bytearray()
    for y in range(H):
        raw.append(0)
        raw.extend(rgba[4*W*y : 4*W*(y+1)])
    idat = zlib.compress(bytes(raw))
    out = sig + chunk(b'IHDR', ihdr) + chunk(b'IDAT', idat) + chunk(b'IEND', b'')
    with open(path, 'wb') as f:
        f.write(out)

def make(primary, secondary):
    rgba = bytearray(W * H * 4)
    for y in range(H):
        for x in range(W):
            i = 4 * (y * W + x)
            if EGG[y][x] == '#':
                if SPECKLES[y][x] == '#':
                    r, g, b = secondary
                else:
                    r, g, b = primary
                rgba[i+0] = r
                rgba[i+1] = g
                rgba[i+2] = b
                rgba[i+3] = 0xFF
    return rgba

EGGS = {
    'stormtrooper_spawn_egg': ((0xEE, 0xEE, 0xF2), (0x16, 0x16, 0x1A)),
    'battle_droid_spawn_egg': ((0xB8, 0xA0, 0x78), (0x50, 0x44, 0x32)),
    'jedi_knight_spawn_egg': ((0xD8, 0xCC, 0xB0), (0x6A, 0x50, 0x34)),
    'darth_vader_spawn_egg': ((0x14, 0x14, 0x18), (0xC0, 0x30, 0x30)),
}

if __name__ == '__main__':
    out_dir = sys.argv[1] if len(sys.argv) > 1 else '.'
    for name, (p, s) in EGGS.items():
        write_png(os.path.join(out_dir, f'{name}.png'), make(p, s))
        print(name)
