#!/usr/bin/env python3
"""Generate the animated hyperspace-portal block textures for starwars.

One 16x128 RGBA PNG per destination planet — a vertical strip of 8 stacked
16x16 animation frames — plus a sibling .png.mcmeta ({"frametime": 3,
"interpolate": true}) so vanilla plays the strip as a smooth 8-frame loop.

Finished-art bar (repo rule): every frame layers 3 tonal bands of the portal
hue (dark edge vignette / mid base / bright swirl streaks) plus 4-6 near-white
sparkle motes — no flat single-color regions. Body tones are semi-transparent
(alpha 200-235) so the portal reads as an energy film; sparkles are opaque.

Seamless loop: every animated term advances by an exact whole period across
the 8 frames — sinusoid phases move by integer multiples of TAU*t/8, the two
hash-noise layers scroll 2px/frame (16px = one full wrap per loop), and the
sparkle motes orbit the frame center by TAU/8 per frame (one revolution per
loop) — so a hypothetical frame 8 is pixel-identical to frame 0 and the
interpolated frame-7 -> frame-0 blend has no visible seam.

Deterministic: stdlib-only, no random; all per-pixel hashing is seeded from
the planet name string via zlib.crc32 (Python's salted str hash() is never
used), so re-runs are byte-identical.
"""
import struct, zlib, sys, os, math

FRAME = 16    # frame width == frame height
FRAMES = 8    # frames per animation strip
TAU = math.tau

# Destination planet -> base energy hue (spec hues).
PORTALS = {
    'tatooine':  (0xE8, 0xA3, 0x3D),   # amber/orange
    'andor':     (0x57, 0xB8, 0x6B),   # green
    'coruscant': (0x8E, 0x6B, 0xE8),   # violet
    'dagobah':   (0x6B, 0x8E, 0x4E),   # murky swamp green
    'hoth':      (0xA8, 0xD8, 0xF0),   # glacier blue-white
    'death_star': (0x9A, 0x9E, 0xA6),  # cold imperial gray
    'home':      (0x4D, 0x9B, 0xE8),   # blue
}

MCMETA = '{"animation": {"frametime": 3, "interpolate": true}}\n'

def write_png(path, rgba, w, h):
    """Write an RGBA PNG from a flat byte buffer (same chunk-based writer as
    gen_textures.py, parametrized for the 16x128 animation strips)."""
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
    out = sig + chunk(b'IHDR', ihdr) + chunk(b'IDAT', idat) + chunk(b'IEND', b'')
    with open(path, 'wb') as f:
        f.write(out)

def _mix(c0, c1, f):
    """Blend RGB c0 toward c1 by fraction f, per channel, rounded."""
    return tuple(int(a + (b - a) * f + 0.5) for a, b in zip(c0, c1))

def _palette(base):
    """The 4 portal tones: dark edge vignette, mid base, bright streaks,
    near-white sparkle. Body alphas 200/218/235 (semi-transparent energy
    film), sparkles fully opaque."""
    dark    = _mix(base, (0, 0, 0), 0.60) + (200,)
    mid     = base + (218,)
    bright  = _mix(base, (255, 255, 255), 0.45) + (235,)
    sparkle = _mix(base, (255, 255, 255), 0.85) + (255,)
    return dark, mid, bright, sparkle

def _seed(name):
    """All per-pixel hashing for a portal derives from its planet name."""
    return zlib.crc32(('hyperspace_portal_' + name).encode('utf-8')) & 0xffffffff

def _hash2(seed, x, y):
    """Deterministic 32-bit integer hash of a lattice point."""
    h = (seed ^ (x * 0x9E3779B1) ^ (y * 0x85EBCA77)) & 0xffffffff
    h = ((h ^ (h >> 13)) * 0xC2B2AE3D) & 0xffffffff
    return (h ^ (h >> 16)) & 0xffffffff

def _noise(seed, x, y):
    """Hash noise in [0, 1)."""
    return _hash2(seed, x, y) / 4294967296.0

def paint_frame(seed, palette, t):
    """Return one 16x16 RGBA frame of the swirl at integer frame index t.

    Every t-dependent term below is periodic with period FRAMES — sinusoid
    phases are integer multiples of TAU*t/FRAMES, the noise layers scroll an
    integer 2px/frame (wrapping the 16px tile exactly once per loop), and
    the sparkles orbit TAU/FRAMES per frame — so paint_frame(seed, pal,
    FRAMES) == paint_frame(seed, pal, 0): the loop is seamless by
    construction.
    """
    dark, mid, bright, sparkle = palette
    a = TAU * t / FRAMES
    buf = bytearray(FRAME * FRAME * 4)
    for y in range(FRAME):
        for x in range(FRAME):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            phi = math.atan2(dy, dx)
            # Layered swirl field: a 2-arm spiral rotating once per loop, a
            # counter-rotating 3-arm spiral at double speed, a breathing
            # radial ripple, two scrolling hash-noise shimmer layers, and a
            # square-edge vignette pulling the border into the dark band.
            s = (math.sin(2 * phi + 0.9 * r - a)
                 + 0.6 * math.sin(3 * phi - 0.7 * r + 2 * a)
                 + 0.35 * math.sin(1.3 * r - a)
                 + 0.55 * (_noise(seed, (x + 2 * t) % FRAME, y) - 0.5)
                 + 0.35 * (_noise(seed ^ 0xA5A5A5A5, x, (y - 2 * t) % FRAME) - 0.5)
                 - 0.50 * max(0.0, max(abs(dx), abs(dy)) - 4.25))
            color = bright if s >= 0.72 else (mid if s >= -0.55 else dark)
            i = 4 * (y * FRAME + x)
            buf[i:i+4] = bytes(color)
    # Sparkle motes: 4-6 near-white opaque pixels drifting with the swirl —
    # each orbits the center once per 8-frame loop, with a bright-band glint
    # trailing 0.45 rad behind it. Glints first so sparkles win overlaps.
    n = 4 + _hash2(seed, 101, 7) % 3
    for k in range(n):
        ang0 = TAU * (k + 0.55 * _noise(seed, 11, k)) / n
        rad = 2.8 + 3.2 * _noise(seed, 23, k)
        for da, col in ((-0.45, bright), (0.0, sparkle)):
            ang = ang0 + a + da
            px = int(7.5 + rad * math.cos(ang) + 0.5)  # rad <= 6.0 keeps
            py = int(7.5 + rad * math.sin(ang) + 0.5)  # px/py inside 0..15
            i = 4 * (py * FRAME + px)
            buf[i:i+4] = bytes(col)
    return buf

def paint_portal_sheet(name):
    """Build the full 16x128 strip: frames 0..7 stacked top to bottom."""
    seed = _seed(name)
    palette = _palette(PORTALS[name])
    sheet = bytearray()
    for t in range(FRAMES):
        sheet += paint_frame(seed, palette, t)
    return sheet

if __name__ == '__main__':
    out_dir = sys.argv[1] if len(sys.argv) > 1 else \
        'starwars/src/main/resources/assets/starwars/textures/block'
    os.makedirs(out_dir, exist_ok=True)
    for name in PORTALS:
        png_path = os.path.join(out_dir, f'hyperspace_portal_{name}.png')
        write_png(png_path, paint_portal_sheet(name), FRAME, FRAME * FRAMES)
        with open(png_path + '.mcmeta', 'w') as f:
            f.write(MCMETA)
        print(f'hyperspace_portal_{name}')
    print('OK')
