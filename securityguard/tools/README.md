# securityguard texture tools

This directory contains a deterministic generator for the Security Guard
entity texture.

## Regenerating `security_guard.png`

```bash
python3 generate_guard_texture.py
```

Re-running with the script unchanged produces a byte-identical PNG. To
change the look, edit the palette constants or the `paint_*` functions
inside the script and re-run, then commit the script and the regenerated
PNG together.

## Requirements

Python 3.10+ and Pillow. Install Pillow with:

```bash
python3 -m pip install --user Pillow
```
