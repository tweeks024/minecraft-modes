#!/usr/bin/env python3
"""Generate 32x32 planet globe icons for the hyperspace picker + galaxy map.

One shaded sphere per destination — lit from the top-left, terminator falling
to the bottom-right — with per-planet surface features (dunes, city lights,
ice caps, the Death Star trench + dish, ...). Transparent outside the disc.

Deterministic: stdlib-only, no random; all surface noise is seeded from the
planet name via zlib.crc32 (never Python's salted hash()), so re-runs are
byte-identical.

Output: starwars/src/main/resources/assets/starwars/textures/gui/planet/<id>.png
"""
import math
import os
import struct
import sys
import zlib

SIZE = 32
CX = CY = 15.5
RADIUS = 14.5
# Light direction (top-left, slightly toward viewer).
LX, LY, LZ = -0.55, -0.55, 0.63


def _crc(name):
    return zlib.crc32(('planet_icon_' + name).encode('utf-8')) & 0xffffffff


def _hash(seed, x, y):
    h = (seed ^ (x * 0x9E3779B1) ^ (y * 0x85EBCA77)) & 0xffffffff
    h = ((h ^ (h >> 13)) * 0xC2B2AE3D) & 0xffffffff
    return (h ^ (h >> 16)) & 0xffffffff


def _noise(seed, x, y):
    return _hash(seed, x, y) / 4294967296.0


def _mix(c0, c1, f):
    f = max(0.0, min(1.0, f))
    return tuple(int(a + (b - a) * f + 0.5) for a, b in zip(c0, c1))


def _clampc(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def write_png(path, rgba):
    sig = b'\x89PNG\r\n\x1a\n'

    def chunk(tag, data):
        crc = zlib.crc32(tag + data) & 0xffffffff
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', crc)

    ihdr = struct.pack('>IIBBBBB', SIZE, SIZE, 8, 6, 0, 0, 0)
    raw = bytearray()
    for y in range(SIZE):
        raw.append(0)
        raw.extend(rgba[4 * SIZE * y: 4 * SIZE * (y + 1)])
    idat = zlib.compress(bytes(raw), 9)
    with open(path, 'wb') as f:
        f.write(sig + chunk(b'IHDR', ihdr) + chunk(b'IDAT', idat) + chunk(b'IEND', b''))


def _surface(name, seed, sx, sy, sz, base):
    """Per-planet surface colour at unit-sphere point (sx, sy, sz).

    sx,sy in screen-ish space (−1..1), sz is the sphere bulge toward viewer.
    Returns an (r,g,b) before lighting."""
    # Latitude/longitude for banding + speckle sampling.
    lat = sy
    lon = math.atan2(sx, max(1e-3, sz))
    nx = int((lon + math.pi) * 6)
    ny = int((lat + 1) * 10)
    n = _noise(seed, nx, ny)

    if name == 'tatooine':
        band = 0.5 + 0.5 * math.sin(lat * 9.0 + n)
        return _mix(base, (0xC8, 0x92, 0x40), band * 0.5)
    if name == 'andor':
        # Green land mottled with blue lochs + white cloud wisps.
        if n > 0.72:
            return (0xE8, 0xF0, 0xF4)  # cloud
        if n < 0.28:
            return (0x2E, 0x5A, 0x86)  # loch
        return _mix(base, (0x3A, 0x6E, 0x3A), n)
    if name == 'coruscant':
        # Grey cityscape; night side (lower-right) glitters gold.
        lit = n > 0.80
        night = (sx + sy) > 0.35
        if lit:
            return (0xF0, 0xC8, 0x54) if night else (0xB8, 0xBE, 0xC8)
        return _mix(base, (0x55, 0x50, 0x62), n * 0.6)
    if name == 'dagobah':
        return _mix(base, (0x38, 0x4A, 0x2A), n)
    if name == 'hoth':
        cap = abs(lat) > 0.6
        if cap:
            return (0xF4, 0xFA, 0xFF)
        return _mix(base, (0x9A, 0xC4, 0xE4), n * 0.5)
    if name == 'death_star':
        # Grey plating, a dark equatorial trench, and the superlaser dish
        # up and to the right.
        dish_dx, dish_dy = sx - 0.45, sy + 0.42
        if dish_dx * dish_dx + dish_dy * dish_dy < 0.05:
            return (0x2A, 0x2C, 0x30)
        if abs(lat) < 0.06:
            return (0x30, 0x32, 0x36)  # trench
        return _mix(base, (0x60, 0x64, 0x6C), n * 0.5)
    if name == 'home':
        # Earth-like: blue seas, green land, white clouds.
        if n > 0.74:
            return (0xE8, 0xF0, 0xF6)
        if n < 0.42:
            return (0x2E, 0x64, 0xA6)
        return _mix(base, (0x3C, 0x8A, 0x44), n)
    return base


PLANETS = {
    'tatooine':   (0xE3, 0xC0, 0x76),
    'andor':      (0x4E, 0x86, 0x54),
    'coruscant':  (0x6A, 0x66, 0x7A),
    'dagobah':    (0x4A, 0x58, 0x36),
    'hoth':       (0xC6, 0xE2, 0xF2),
    'death_star': (0x7C, 0x80, 0x88),
    'home':       (0x4D, 0x9B, 0xE8),
}


def paint(name, base):
    seed = _crc(name)
    rgba = bytearray(SIZE * SIZE * 4)
    for py in range(SIZE):
        for px in range(SIZE):
            dx = (px + 0.5 - CX) / RADIUS
            dy = (py + 0.5 - CY) / RADIUS
            d2 = dx * dx + dy * dy
            if d2 > 1.0:
                continue  # outside the globe → transparent
            sz = math.sqrt(max(0.0, 1.0 - d2))
            surf = _surface(name, seed, dx, dy, sz, base)
            # Lambert-ish shading + a specular pop near the light point.
            lambert = max(0.0, dx * LX + dy * LY + sz * LZ)
            shade = 0.35 + 0.75 * lambert
            col = [c * shade for c in surf]
            spec = max(0.0, dx * LX + dy * LY + sz * LZ)
            if spec > 0.985 and d2 < 0.5:
                col = [c + 90 for c in col]
            # Dark limb near the edge for roundness.
            if d2 > 0.86:
                col = [c * 0.7 for c in col]
            r, g, b = _clampc(col)
            i = 4 * (py * SIZE + px)
            rgba[i:i + 4] = bytes((r, g, b, 255))
    return rgba


if __name__ == '__main__':
    out_dir = sys.argv[1] if len(sys.argv) > 1 else \
        'starwars/src/main/resources/assets/starwars/textures/gui/planet'
    os.makedirs(out_dir, exist_ok=True)
    for name, base in PLANETS.items():
        write_png(os.path.join(out_dir, name + '.png'), paint(name, base))
        print(name)
