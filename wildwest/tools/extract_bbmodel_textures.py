#!/usr/bin/env python3
"""Extract embedded base64 textures from .bbmodel files into PNG files.

Blockbench saves textures inside the .bbmodel as data URIs. Minecraft loads
PNGs from the entity-texture path. Run this after editing in Blockbench to
sync changes to the runtime path.

Usage: python3 extract_bbmodel_textures.py [out_dir]
"""
import json, base64, os, sys

NAMES = ['deputy', 'sherrif', 'bandit', 'bandit_leader', 'walker']
HERE = os.path.dirname(os.path.abspath(__file__))

def extract(bbmodel_path, out_path):
    with open(bbmodel_path) as f:
        m = json.load(f)
    texs = m.get('textures', [])
    if not texs:
        print(f'{bbmodel_path}: no textures')
        return False
    src = texs[0].get('source', '')
    if not src.startswith('data:image/png;base64,'):
        print(f'{bbmodel_path}: texture not embedded as base64')
        return False
    png = base64.b64decode(src.split(',', 1)[1])
    with open(out_path, 'wb') as f:
        f.write(png)
    print(f'{out_path}: {len(png)} bytes')
    return True

if __name__ == '__main__':
    out_dir = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
        HERE, '..', 'src', 'main', 'resources', 'assets', 'wildwest',
        'textures', 'entity')
    out_dir = os.path.abspath(out_dir)
    os.makedirs(out_dir, exist_ok=True)
    for n in NAMES:
        extract(os.path.join(HERE, f'{n}.bbmodel'),
                os.path.join(out_dir, f'{n}.png'))
