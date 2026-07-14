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
    'luke_skywalker_spawn_egg': ((0x1C, 0x1C, 0x20), (0xC8, 0xA8, 0x60)),
    'obi_wan_spawn_egg': ((0xE2, 0xD8, 0xC0), (0x74, 0x5C, 0x42)),
    'astromech_spawn_egg': ((0xE8, 0xE8, 0xEC), (0x28, 0x50, 0xA0)),
    'boba_fett_spawn_egg': ((0x5A, 0x6E, 0x50), (0xA0, 0x30, 0x28)),
    'han_solo_spawn_egg': ((0xE8, 0xE0, 0xD0), (0x2B, 0x2B, 0x2B)),
    'princess_leia_spawn_egg': ((0xF2, 0xEE, 0xE6), (0x5A, 0x40, 0x30)),
    'jawa_spawn_egg': ((0x6B, 0x4A, 0x2B), (0xF2, 0xC1, 0x4E)),
    'tusken_raider_spawn_egg': ((0xC8, 0xB2, 0x8A), (0x4A, 0x3B, 0x28)),
    'bantha_spawn_egg': ((0x6B, 0x4F, 0x35), (0xE8, 0xDC, 0xC0)),
    'rebel_trooper_spawn_egg': ((0x7A, 0x7A, 0x4A), (0xC8, 0xB2, 0x8A)),
    'probe_droid_spawn_egg': ((0x33, 0x36, 0x3B), (0xE3, 0x3B, 0x3B)),
    'wampa_spawn_egg': ((0xF0, 0xED, 0xE6), (0xB0, 0x30, 0x30)),
    'tauntaun_spawn_egg': ((0x9A, 0x8C, 0x7A), (0xE8, 0xDC, 0xC0)),
    'snowtrooper_spawn_egg': ((0xF2, 0xF2, 0xF2), (0x8A, 0x8F, 0x99)),
    'dragonsnake_spawn_egg': ((0x4A, 0x6B, 0x3A), (0x23, 0x3A, 0x1E)),
    'bogwing_spawn_egg': ((0x4A, 0x8E, 0x8A), (0x6B, 0x4A, 0x2B)),
    'yoda_spawn_egg': ((0x7A, 0xA0, 0x5A), (0x8A, 0x7A, 0x5A)),
}

if __name__ == '__main__':
    out_dir = sys.argv[1] if len(sys.argv) > 1 else '.'
    for name, (p, s) in EGGS.items():
        write_png(os.path.join(out_dir, f'{name}.png'), make(p, s))
        print(name)
