#!/usr/bin/env python3
"""Generate 64x64 RGBA PNG placeholder textures for the four wildwest mobs.

The output is a layered palette over the standard humanoid UV layout:
  v  0..16: head + hat overlay (head 0..32u, hat 32..64u)
  v 16..32: arms (40..56u right, ...) + body (16..40u) + legs (0..16u right leg)
  v 32..48: body_2 / legs_2 overlay (used for cape, jacket trim)
  v 48..64: leg_2 / arm_2 overlay

Per-mob choices are at the bottom of the script. The base color fills the
whole 64x64; specific regions are painted on top for distinguishing color.
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

# Palette
SKIN     = (0xFF, 0xCF, 0xA8, 0xFF)
PANTS    = (0x40, 0x40, 0x70, 0xFF)  # denim
RED      = (0xA0, 0x30, 0x30, 0xFF)  # bandanna
GOLD     = (0xE0, 0xC0, 0x40, 0xFF)  # sheriff star
BROWN    = (0x5A, 0x38, 0x20, 0xFF)  # leather hat
DARK_BR  = (0x3A, 0x28, 0x18, 0xFF)  # darker hat (sheriff/bandit-leader hat)
DARK     = (0x14, 0x14, 0x14, 0xFF)  # cape

# Faction body colors
DEPUTY_SHIRT  = (0xA8, 0x80, 0x60, 0xFF)
SHERRIF_SHIRT = (0x7A, 0x52, 0x40, 0xFF)
BANDIT_SHIRT  = (0x3A, 0x3A, 0x3A, 0xFF)
LEADER_SHIRT  = (0x1A, 0x1A, 0x1A, 0xFF)

def paint_humanoid_base(rgba, shirt):
    """Lay down the standard humanoid skin: face + arms + body shirt + pants."""
    # Head: rows 0..16. Face is at u 8..16, v 8..16 (front of head cube).
    # Top of head (u 8..16, v 0..8) is the skullcap.
    rect(rgba, 0, 0, 32, 8, shirt)       # head top/back/sides — base
    rect(rgba, 8, 8, 16, 16, SKIN)       # face front
    rect(rgba, 16, 8, 24, 16, shirt)     # head right side (already covered by base, but explicit)
    rect(rgba, 24, 8, 32, 16, SKIN)      # head left side — keep skin so the side of the face shows

    # Body row (v 16..32):
    # u  0..16: right leg
    # u 16..40: body
    # u 40..56: right arm
    rect(rgba, 0, 16, 16, 32, PANTS)     # right leg
    rect(rgba, 16, 16, 40, 32, shirt)    # body — torso shirt
    rect(rgba, 40, 16, 56, 32, shirt)    # right arm — sleeve
    # left leg / left arm in v 48..64
    rect(rgba, 0, 48, 16, 64, PANTS)     # leg_2 region (covers left leg)
    rect(rgba, 16, 48, 32, 64, PANTS)    # left leg
    rect(rgba, 32, 48, 48, 64, shirt)    # left arm
    rect(rgba, 48, 48, 64, 64, shirt)    # arm_2 region

def gen_deputy(out_path):
    rgba = bytearray(W * H * 4)
    fill(rgba, DEPUTY_SHIRT)
    paint_humanoid_base(rgba, DEPUTY_SHIRT)
    # Hat — uses 32..64, 0..16 region (we disabled the vanilla hat overlay
    # in DeputyModel, so this region is free for the cap_brim/cap_crown cubes).
    # cap_brim:  texOffs(32, 0), 9x1x9 — UV roughly 32..50, 0..18 (wraps around)
    # cap_crown: texOffs(32, 10), 6x2x6 — UV roughly 32..44, 10..28
    rect(rgba, 32, 0, 64, 16, BROWN)
    write_png(out_path, rgba)

def gen_sherrif(out_path):
    rgba = bytearray(W * H * 4)
    fill(rgba, SHERRIF_SHIRT)
    paint_humanoid_base(rgba, SHERRIF_SHIRT)
    # Hat — darker brown, fills the entire hat-overlay region.
    rect(rgba, 32, 0, 64, 16, DARK_BR)
    # Sheriff star — texOffs(56, 0), 3x3x1. UV ~56..62, 0..4.
    rect(rgba, 56, 0, 62, 4, GOLD)
    write_png(out_path, rgba)

def gen_bandit(out_path):
    rgba = bytearray(W * H * 4)
    fill(rgba, BANDIT_SHIRT)
    paint_humanoid_base(rgba, BANDIT_SHIRT)
    # Bandanna — texOffs(32, 0), 9x4x1. UV ~32..50, 0..6.
    rect(rgba, 32, 0, 50, 6, RED)
    write_png(out_path, rgba)

def gen_bandit_leader(out_path):
    rgba = bytearray(W * H * 4)
    fill(rgba, LEADER_SHIRT)
    paint_humanoid_base(rgba, LEADER_SHIRT)
    # Hat — darker brown, hat-overlay region.
    rect(rgba, 32, 0, 64, 16, DARK_BR)
    # Bandanna — texOffs(48, 4), 9x4x1. UV ~48..58, 4..10. Overrides part of the hat.
    rect(rgba, 48, 4, 58, 10, RED)
    # Cape — texOffs(40, 32), 9x12x1. UV ~40..52, 32..46.
    rect(rgba, 40, 32, 52, 46, DARK)
    write_png(out_path, rgba)

if __name__ == '__main__':
    out_dir = sys.argv[1] if len(sys.argv) > 1 else '.'
    gen_deputy(os.path.join(out_dir, 'deputy.png'))
    gen_sherrif(os.path.join(out_dir, 'sherrif.png'))
    gen_bandit(os.path.join(out_dir, 'bandit.png'))
    gen_bandit_leader(os.path.join(out_dir, 'bandit_leader.png'))
    print('OK')
