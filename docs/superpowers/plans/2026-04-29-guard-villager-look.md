# Security Guard Villager-Like Visual Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the existing `HumanoidModel`-based Security Guard villager-style face features (protruding nose, dot eyes, unibrow), villager skin tone on exposed skin, a shaded navy uniform, a gold cap band, and a flat gold police shield on the chest — without replacing the model class, the renderer, or any AI.

**Architecture:** Two changes to runtime code: add a single `nose` `ModelPart` as a child of `head` in `SecurityGuardModel`, and replace `security_guard.png` with a redrawn 64×64 texture. The texture is produced by a committed Python+PIL generator script (`securityguard/tools/generate_guard_texture.py`) so future visual changes are deterministic and reproducible.

**Tech Stack:** Java 25, NeoForge 26.1.2.30-beta, Minecraft 26.1.2, gradle multi-module, Python 3 + Pillow (PIL) for the texture generator.

**Spec:** [docs/superpowers/specs/2026-04-29-guard-villager-look-design.md](../specs/2026-04-29-guard-villager-look-design.md).

**Working directory for all commands:** `/Users/tweeks/code/minecraft-mods` (repo root).

---

## File Structure

### Files created

```
securityguard/tools/generate_guard_texture.py     # NEW: deterministic Python+PIL texture generator
securityguard/tools/README.md                     # NEW: one-paragraph usage note
```

### Files modified

```
securityguard/src/main/java/com/tweeks/securityguard/client/model/SecurityGuardModel.java   # ADD: nose ModelPart child of head, texOffs(56,16)
securityguard/src/main/resources/assets/securityguard/textures/entity/security_guard.png    # REPAINT: villager features + shield + gold cap band
```

### Files unchanged

- `SecurityGuardRenderer.java`
- `BatonModel.java`
- `securitycore/.../HeldItemLayer.java`
- `SecurityGuardEntity.java` and all AI goals
- `neoforge.mods.toml`, `gradle.properties`, `build.gradle`

---

## Texture region map (referenced throughout)

The 64×64 texture follows the standard Minecraft humanoid skin layout. The plan paints into these rectangles (all coordinates are pixel indices, top-left origin, half-open intervals `[x0,x1) × [y0,y1)`):

| Region name | Rect | Purpose |
|---|---|---|
| `HEAD_FRONT` | `(8, 8)..(16, 16)` | Face panel — receives villager features |
| `HEAD_RIGHT` | `(0, 8)..(8, 16)` | Right side of head |
| `HEAD_BACK` | `(24, 8)..(32, 16)` | Back of head |
| `HEAD_LEFT` | `(16, 8)..(24, 16)` | Left side of head |
| `HEAD_TOP` | `(8, 0)..(16, 8)` | Top of head |
| `HEAD_BOTTOM` | `(16, 0)..(24, 8)` | Bottom of head (under cap) |
| `BODY_FRONT` | `(20, 20)..(28, 32)` | Torso front — receives shield |
| `BODY_BACK` | `(32, 20)..(40, 32)` | Torso back |
| `BODY_RIGHT` | `(16, 20)..(20, 32)` | Torso right side |
| `BODY_LEFT` | `(28, 20)..(32, 32)` | Torso left side |
| `RIGHT_ARM_FRONT` | `(44, 20)..(48, 32)` | Right arm front (holds baton) |
| `RIGHT_ARM_BACK` | `(52, 20)..(56, 32)` | Right arm back |
| `RIGHT_ARM_OUTER` | `(40, 20)..(44, 32)` | Right arm outer side |
| `RIGHT_ARM_INNER` | `(48, 20)..(52, 32)` | Right arm inner side |
| `RIGHT_ARM_TOP` | `(44, 16)..(48, 20)` | Right arm top (where it meets shoulder) |
| `RIGHT_ARM_BOTTOM` | `(48, 16)..(52, 20)` | Right arm bottom (hand area) |
| `LEFT_ARM_*` | mirrored at `(32, 48)..(48, 64)` | Left arm faces |
| `RIGHT_LEG_*` | `(0, 20)..(16, 32)` | Right leg |
| `LEFT_LEG_*` | mirrored at `(16, 48)..(32, 64)` | Left leg |
| `CAP_BRIM` | `(32, 0)..(64, 10)` (approx) | Cap brim UV unwrap (existing) |
| `CAP_CROWN` | `(32, 10)..(60, 19)` (approx) | Cap crown UV unwrap (existing) |
| `NOSE_UVS` | `(56, 16)..(64, 20)` | NEW — nose ModelPart unwrap (8 wide × 4 tall, fits a 2×2×2 cube) |

The "hand area" of `RIGHT_ARM_BOTTOM` and `LEFT_ARM_BOTTOM` is where exposed-skin villager tone goes; everywhere else is uniform navy.

---

## Color palette (referenced throughout)

| Constant | Hex | Use |
|---|---|---|
| `SKIN` | `#A37D5B` | Villager skin tone — face, hands, nose |
| `BROW` | `#3A2618` | Unibrow stripe |
| `EYE` | `#000000` | Dot eyes |
| `NAVY` | `#1F2A55` | Uniform base |
| `NAVY_HIGHLIGHT` | `#2D3D70` | Shoulder/highlight pixels |
| `NAVY_SHADOW` | `#152040` | Hem/shadow pixels |
| `GOLD` | `#D4A82A` | Cap band, shield body |
| `GOLD_HIGHLIGHT` | `#F1C84A` | Shield top-left highlight |
| `GOLD_SHADOW` | `#9C7A1F` | Shield bottom-right shadow |
| `TROUSERS` | `#15203F` | Slightly darker than navy for legs |

---

## Task 1: Establish baseline — confirm current build is green

**Files:** none modified.

- [ ] **Step 1: Build the securityguard module**

Run:
```bash
./gradlew :securityguard:build
```
Expected: `BUILD SUCCESSFUL`. Existing tests pass.

- [ ] **Step 2: Confirm Pillow is available for the generator script we'll add later**

Run:
```bash
python3 -c "from PIL import Image; print(Image.__name__, 'OK')"
```
Expected: `PIL.Image OK`.

If Pillow is missing, install it:
```bash
python3 -m pip install --user Pillow
```
Re-run the import check; it must print `OK` before continuing.

- [ ] **Step 3: No commit. Verification only.**

If the build or Pillow check fails, **stop and fix before proceeding** — every later task assumes a green baseline.

---

## Task 2: Add the `nose` `ModelPart` to `SecurityGuardModel`

**Files:**
- Modify: `securityguard/src/main/java/com/tweeks/securityguard/client/model/SecurityGuardModel.java`

- [ ] **Step 1: Add the nose part as a child of `head`**

Open `securityguard/src/main/java/com/tweeks/securityguard/client/model/SecurityGuardModel.java`. Find the existing `cap_crown` block (the second `addOrReplaceChild` call inside `createBodyLayer()`):

```java
        head.addOrReplaceChild("cap_crown",
            CubeListBuilder.create()
                .texOffs(32, 10)
                .addBox(-3.5f, -11.5f, -3.5f, 7, 2, 7),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
```

Insert a new `addOrReplaceChild` call for `nose` between `cap_crown` and the `return` statement:

```java
        head.addOrReplaceChild("cap_crown",
            CubeListBuilder.create()
                .texOffs(32, 10)
                .addBox(-3.5f, -11.5f, -3.5f, 7, 2, 7),
            PartPose.ZERO);
        head.addOrReplaceChild("nose",
            CubeListBuilder.create()
                .texOffs(56, 16)
                .addBox(-1.0f, -2.0f, -5.0f, 2, 2, 2),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
```

The box parameters break down as:
- `-1.0f, -2.0f, -5.0f` — origin: 1 voxel left of center, 2 voxels above head center (eye line), 1 voxel forward of the head's front face (head front sits at z=-4, nose extends to z=-5)
- `2, 2, 2` — 2×2×2 voxels
- `texOffs(56, 16)` — UV slot in the free band immediately right of where the right-arm UV unwrap ends (right arm 4×12×4 at `texOffs(40,16)` unwraps to x=40..56, y=16..32; the strip from x=56..64, y=16..20 is unused and exactly fits an 8×4 cube unwrap)

- [ ] **Step 2: Build and confirm the model compiles**

Run:
```bash
./gradlew :securityguard:compileJava
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Build the full module to ensure no other code broke**

Run:
```bash
./gradlew :securityguard:build
```
Expected: `BUILD SUCCESSFUL`. Existing tests pass.

- [ ] **Step 4: Commit**

```bash
git add securityguard/src/main/java/com/tweeks/securityguard/client/model/SecurityGuardModel.java
git commit -m "$(cat <<'EOF'
feat(securityguard): add nose ModelPart to SecurityGuardModel

Adds a 2x2x2 nose cube as a child of the head bone, anchored at
(-1, -2, -5) so it protrudes one voxel forward of the head front face
at eye level. UV slot texOffs(56, 16) sits in the free 8x4 band
immediately right of the right-arm UV unwrap. The texture still shows
the old solid-blue placeholder for this UV slot at this commit; the
villager-style repaint follows in a subsequent commit.
EOF
)"
```

---

## Task 3: Scaffold the texture generator script

**Files:**
- Create: `securityguard/tools/generate_guard_texture.py`

- [ ] **Step 1: Create the tools directory**

Run:
```bash
mkdir -p securityguard/tools
```

- [ ] **Step 2: Write the generator script with palette, region map, and a placeholder `main()`**

Create file `securityguard/tools/generate_guard_texture.py`:

```python
#!/usr/bin/env python3
"""
Deterministic generator for the Security Guard entity texture.

Run:
    python3 generate_guard_texture.py

Writes ../src/main/resources/assets/securityguard/textures/entity/security_guard.png.
Re-running produces byte-identical output as long as this script is unchanged.
"""

from __future__ import annotations

from pathlib import Path
from PIL import Image, ImageDraw

# ---------- Palette (RGBA) ----------

SKIN           = (0xA3, 0x7D, 0x5B, 0xFF)
BROW           = (0x3A, 0x26, 0x18, 0xFF)
EYE            = (0x00, 0x00, 0x00, 0xFF)
NAVY           = (0x1F, 0x2A, 0x55, 0xFF)
NAVY_HIGHLIGHT = (0x2D, 0x3D, 0x70, 0xFF)
NAVY_SHADOW    = (0x15, 0x20, 0x40, 0xFF)
TROUSERS       = (0x15, 0x20, 0x3F, 0xFF)
GOLD           = (0xD4, 0xA8, 0x2A, 0xFF)
GOLD_HIGHLIGHT = (0xF1, 0xC8, 0x4A, 0xFF)
GOLD_SHADOW    = (0x9C, 0x7A, 0x1F, 0xFF)
TRANSPARENT    = (0x00, 0x00, 0x00, 0x00)

# ---------- Region rectangles (x0, y0, x1, y1), half-open ----------

# Head (outer, not the hat overlay)
HEAD_FRONT  = (8,  8, 16, 16)
HEAD_RIGHT  = (0,  8,  8, 16)
HEAD_BACK   = (24, 8, 32, 16)
HEAD_LEFT   = (16, 8, 24, 16)
HEAD_TOP    = (8,  0, 16,  8)
HEAD_BOTTOM = (16, 0, 24,  8)

# Body (torso outer)
BODY_FRONT = (20, 20, 28, 32)
BODY_BACK  = (32, 20, 40, 32)
BODY_RIGHT = (16, 20, 20, 32)
BODY_LEFT  = (28, 20, 32, 32)
BODY_TOP   = (20, 16, 28, 20)
BODY_BOT   = (28, 16, 36, 20)

# Right arm (outer)
RIGHT_ARM_OUTER  = (40, 20, 44, 32)
RIGHT_ARM_FRONT  = (44, 20, 48, 32)
RIGHT_ARM_INNER  = (48, 20, 52, 32)
RIGHT_ARM_BACK   = (52, 20, 56, 32)
RIGHT_ARM_TOP    = (44, 16, 48, 20)
RIGHT_ARM_BOTTOM = (48, 16, 52, 20)

# Left arm (outer, lower half of sheet)
LEFT_ARM_OUTER  = (32, 52, 36, 64)
LEFT_ARM_FRONT  = (36, 52, 40, 64)
LEFT_ARM_INNER  = (40, 52, 44, 64)
LEFT_ARM_BACK   = (44, 52, 48, 64)
LEFT_ARM_TOP    = (36, 48, 40, 52)
LEFT_ARM_BOTTOM = (40, 48, 44, 52)

# Right leg (outer)
RIGHT_LEG_OUTER  = (0,  20,  4, 32)
RIGHT_LEG_FRONT  = (4,  20,  8, 32)
RIGHT_LEG_INNER  = (8,  20, 12, 32)
RIGHT_LEG_BACK   = (12, 20, 16, 32)
RIGHT_LEG_TOP    = (4,  16,  8, 20)
RIGHT_LEG_BOTTOM = (8,  16, 12, 20)

# Left leg (outer, lower half of sheet)
LEFT_LEG_OUTER  = (16, 52, 20, 64)
LEFT_LEG_FRONT  = (20, 52, 24, 64)
LEFT_LEG_INNER  = (24, 52, 28, 64)
LEFT_LEG_BACK   = (28, 52, 32, 64)
LEFT_LEG_TOP    = (20, 48, 24, 52)
LEFT_LEG_BOTTOM = (24, 48, 28, 52)

# Cap (existing UVs from the model)
CAP_BRIM_UNWRAP  = (32,  0, 64, 10)
CAP_CROWN_UNWRAP = (32, 10, 60, 19)

# Nose (NEW — matches texOffs(56, 16) for a 2x2x2 cube)
NOSE_UNWRAP = (56, 16, 64, 20)

OUTPUT = Path(__file__).resolve().parent.parent / \
    "src/main/resources/assets/securityguard/textures/entity/security_guard.png"

# ---------- Painting helpers ----------

def fill(img: Image.Image, rect: tuple[int, int, int, int], color):
    """Fill a half-open rect (x0, y0, x1, y1) with a solid color."""
    x0, y0, x1, y1 = rect
    ImageDraw.Draw(img).rectangle((x0, y0, x1 - 1, y1 - 1), fill=color)


def pixel(img: Image.Image, x: int, y: int, color):
    img.putpixel((x, y), color)


# ---------- Main paint pipeline ----------

def paint_skin(img: Image.Image) -> None:
    """Fill all exposed-skin regions with villager skin tone."""
    for rect in (HEAD_FRONT, HEAD_RIGHT, HEAD_BACK, HEAD_LEFT, HEAD_TOP, HEAD_BOTTOM):
        fill(img, rect, SKIN)
    fill(img, NOSE_UNWRAP, SKIN)
    for rect in (RIGHT_ARM_BOTTOM, LEFT_ARM_BOTTOM):
        fill(img, rect, SKIN)


def paint_uniform_base(img: Image.Image) -> None:
    """Solid navy fill across torso, arms (except hands), and the cap."""
    for rect in (BODY_FRONT, BODY_BACK, BODY_RIGHT, BODY_LEFT, BODY_TOP, BODY_BOT):
        fill(img, rect, NAVY)
    for rect in (RIGHT_ARM_OUTER, RIGHT_ARM_FRONT, RIGHT_ARM_INNER, RIGHT_ARM_BACK, RIGHT_ARM_TOP):
        fill(img, rect, NAVY)
    for rect in (LEFT_ARM_OUTER, LEFT_ARM_FRONT, LEFT_ARM_INNER, LEFT_ARM_BACK, LEFT_ARM_TOP):
        fill(img, rect, NAVY)
    for rect in (RIGHT_LEG_OUTER, RIGHT_LEG_FRONT, RIGHT_LEG_INNER, RIGHT_LEG_BACK,
                 RIGHT_LEG_TOP, RIGHT_LEG_BOTTOM):
        fill(img, rect, TROUSERS)
    for rect in (LEFT_LEG_OUTER, LEFT_LEG_FRONT, LEFT_LEG_INNER, LEFT_LEG_BACK,
                 LEFT_LEG_TOP, LEFT_LEG_BOTTOM):
        fill(img, rect, TROUSERS)
    fill(img, CAP_BRIM_UNWRAP, NAVY)
    fill(img, CAP_CROWN_UNWRAP, NAVY)


def paint_face(img: Image.Image) -> None:
    """Paint villager-style face features into HEAD_FRONT (8x8 panel at (8, 8))."""
    # Will be implemented in Task 4.
    pass


def paint_uniform_shading(img: Image.Image) -> None:
    """Add 1px shoulder highlights, 1px hem shadows, and the gold cap band."""
    # Will be implemented in Task 5.
    pass


def paint_shield(img: Image.Image) -> None:
    """Paint the gold police shield on BODY_FRONT."""
    # Will be implemented in Task 6.
    pass


def main() -> None:
    img = Image.new("RGBA", (64, 64), TRANSPARENT)
    paint_skin(img)
    paint_uniform_base(img)
    paint_face(img)
    paint_uniform_shading(img)
    paint_shield(img)
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUTPUT, format="PNG", optimize=True)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    main()
```

- [ ] **Step 3: Run the script to confirm it executes cleanly (it produces a base-fill texture; we paint details in later tasks)**

Run:
```bash
python3 securityguard/tools/generate_guard_texture.py
```
Expected output:
```
Wrote /Users/tweeks/code/minecraft-mods/securityguard/src/main/resources/assets/securityguard/textures/entity/security_guard.png
```

The texture is now overwritten with the new base layout (villager skin on head, navy uniform). Visually it differs from the previous placeholder; that's expected.

- [ ] **Step 4: Confirm the build still works with the regenerated texture**

Run:
```bash
./gradlew :securityguard:build
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit (script + regenerated PNG together)**

```bash
git add securityguard/tools/generate_guard_texture.py \
    securityguard/src/main/resources/assets/securityguard/textures/entity/security_guard.png
git commit -m "$(cat <<'EOF'
feat(securityguard): add deterministic texture generator + base repaint

Adds securityguard/tools/generate_guard_texture.py — a Python+PIL
script that produces security_guard.png from named color and region
constants. This commit regenerates the PNG with villager skin tone on
the head and hands and navy on the uniform; villager-style face
features, uniform shading, gold cap band, and gold shield arrive in
subsequent commits.

The script is committed so future visual tweaks regenerate the PNG
deterministically rather than being hand-edited.
EOF
)"
```

---

## Task 4: Paint villager-style face features

**Files:**
- Modify: `securityguard/tools/generate_guard_texture.py` (replace `paint_face`)
- Modify: `securityguard/src/main/resources/assets/securityguard/textures/entity/security_guard.png` (regenerated)

- [ ] **Step 1: Implement `paint_face`**

Open `securityguard/tools/generate_guard_texture.py`. Find:

```python
def paint_face(img: Image.Image) -> None:
    """Paint villager-style face features into HEAD_FRONT (8x8 panel at (8, 8))."""
    # Will be implemented in Task 4.
    pass
```

Replace with:

```python
def paint_face(img: Image.Image) -> None:
    """Paint villager-style face features into HEAD_FRONT (8x8 panel at (8, 8)).

    Layout inside the 8x8 panel (x relative to panel origin):
      row 2-3: blank (forehead)
      row 3:   unibrow stripe spanning x=2..6
      row 4:   eye dots at x=2 and x=5 (one column gap between for the nose center)
      row 5-7: blank (cheeks/chin — no mouth, matching vanilla villagers)
    """
    panel_x, panel_y = HEAD_FRONT[0], HEAD_FRONT[1]
    # Unibrow: 5 pixels wide
    for dx in range(2, 7):
        pixel(img, panel_x + dx, panel_y + 3, BROW)
    # Eye dots, flanking where the nose protrudes
    pixel(img, panel_x + 2, panel_y + 4, EYE)
    pixel(img, panel_x + 5, panel_y + 4, EYE)
```

(The eye-dot columns 2 and 5 leave columns 3 and 4 between them, which is where the 2-wide nose protrudes from the head. The brow stripe sits one row above the eyes.)

- [ ] **Step 2: Run the script**

Run:
```bash
python3 securityguard/tools/generate_guard_texture.py
```
Expected: `Wrote ...security_guard.png`. The PNG now has a face.

- [ ] **Step 3: Build to confirm nothing broke**

Run:
```bash
./gradlew :securityguard:build
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add securityguard/tools/generate_guard_texture.py \
    securityguard/src/main/resources/assets/securityguard/textures/entity/security_guard.png
git commit -m "$(cat <<'EOF'
feat(securityguard): paint villager-style brow and dot eyes into face panel

Adds the unibrow stripe (5px wide at row 3 of the 8x8 face panel) and
two black dot eyes flanking the nose center column. No mouth, matching
vanilla villager faces. Regenerates the texture.
EOF
)"
```

---

## Task 5: Paint uniform shading + gold cap band

**Files:**
- Modify: `securityguard/tools/generate_guard_texture.py` (replace `paint_uniform_shading`)
- Modify: `securityguard/src/main/resources/assets/securityguard/textures/entity/security_guard.png` (regenerated)

- [ ] **Step 1: Implement `paint_uniform_shading`**

Open `securityguard/tools/generate_guard_texture.py`. Find:

```python
def paint_uniform_shading(img: Image.Image) -> None:
    """Add 1px shoulder highlights, 1px hem shadows, and the gold cap band."""
    # Will be implemented in Task 5.
    pass
```

Replace with:

```python
def paint_uniform_shading(img: Image.Image) -> None:
    """Add 1px shoulder highlights, 1px hem shadows, and the gold cap band.

    Highlights run along the top row of BODY_FRONT/BODY_BACK and the
    inner-shoulder edge. Shadows run along the bottom row of the torso
    where the uniform meets the trousers. The cap gets a single-row gold
    band along the top of the brim unwrap.
    """
    # Body shoulder highlight: top row of front and back
    x0, y0, x1, _ = BODY_FRONT
    for x in range(x0, x1):
        pixel(img, x, y0, NAVY_HIGHLIGHT)
    x0, y0, x1, _ = BODY_BACK
    for x in range(x0, x1):
        pixel(img, x, y0, NAVY_HIGHLIGHT)

    # Body hem shadow: bottom row of front and back
    x0, _, x1, y1 = BODY_FRONT
    for x in range(x0, x1):
        pixel(img, x, y1 - 1, NAVY_SHADOW)
    x0, _, x1, y1 = BODY_BACK
    for x in range(x0, x1):
        pixel(img, x, y1 - 1, NAVY_SHADOW)

    # Right arm shoulder highlight: top row of arm front
    x0, y0, x1, _ = RIGHT_ARM_FRONT
    for x in range(x0, x1):
        pixel(img, x, y0, NAVY_HIGHLIGHT)

    # Left arm shoulder highlight: top row of arm front
    x0, y0, x1, _ = LEFT_ARM_FRONT
    for x in range(x0, x1):
        pixel(img, x, y0, NAVY_HIGHLIGHT)

    # Cap brim — gold band along the row where brim meets crown.
    # CAP_BRIM_UNWRAP is (32, 0, 64, 10); the bottom row at y=9 reads as
    # the visible band where the brim front face transitions to the crown.
    x0, _, x1, y1 = CAP_BRIM_UNWRAP
    for x in range(x0, x1):
        pixel(img, x, y1 - 1, GOLD)
```

- [ ] **Step 2: Run the script**

Run:
```bash
python3 securityguard/tools/generate_guard_texture.py
```
Expected: `Wrote ...security_guard.png`.

- [ ] **Step 3: Build**

Run:
```bash
./gradlew :securityguard:build
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add securityguard/tools/generate_guard_texture.py \
    securityguard/src/main/resources/assets/securityguard/textures/entity/security_guard.png
git commit -m "$(cat <<'EOF'
feat(securityguard): add uniform shading + gold cap band to texture

Paints 1px shoulder highlights along the top of torso/arm fronts, 1px
hem shadows along the bottom of the torso, and a gold band along the
brim-crown junction of the cap. Pure cosmetic detailing — no model or
UV changes. Regenerates the texture.
EOF
)"
```

---

## Task 6: Paint the gold police shield on the chest

**Files:**
- Modify: `securityguard/tools/generate_guard_texture.py` (replace `paint_shield`)
- Modify: `securityguard/src/main/resources/assets/securityguard/textures/entity/security_guard.png` (regenerated)

- [ ] **Step 1: Implement `paint_shield`**

Open `securityguard/tools/generate_guard_texture.py`. Find:

```python
def paint_shield(img: Image.Image) -> None:
    """Paint the gold police shield on BODY_FRONT."""
    # Will be implemented in Task 6.
    pass
```

Replace with:

```python
def paint_shield(img: Image.Image) -> None:
    """Paint a 3-wide x 4-tall gold shield on the left chest of BODY_FRONT.

    BODY_FRONT is (20, 20)..(28, 32) — an 8x12 panel. The shield sits
    centered vertically and slightly left of center horizontally so it
    reads as "left chest" the way real police badges do.

    Shield silhouette (rows top to bottom):
      row 0: . X .
      row 1: X X X
      row 2: X X X
      row 3: . X .

    Top-left highlight pixel and bottom-right shadow pixel give it a
    1-pixel pop without losing the silhouette at distance.
    """
    panel_x, panel_y = BODY_FRONT[0], BODY_FRONT[1]
    # Shield top-left corner relative to BODY_FRONT origin
    sx, sy = 2, 4

    shield_pixels = [
        (1, 0),                 # row 0: . X .
        (0, 1), (1, 1), (2, 1),  # row 1: X X X
        (0, 2), (1, 2), (2, 2),  # row 2: X X X
        (1, 3),                 # row 3: . X .
    ]
    for dx, dy in shield_pixels:
        pixel(img, panel_x + sx + dx, panel_y + sy + dy, GOLD)

    # Highlight: top-left of the central body
    pixel(img, panel_x + sx + 0, panel_y + sy + 1, GOLD_HIGHLIGHT)
    # Shadow: bottom-right of the central body
    pixel(img, panel_x + sx + 2, panel_y + sy + 2, GOLD_SHADOW)
```

- [ ] **Step 2: Run the script**

Run:
```bash
python3 securityguard/tools/generate_guard_texture.py
```
Expected: `Wrote ...security_guard.png`.

- [ ] **Step 3: Build**

Run:
```bash
./gradlew :securityguard:build
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add securityguard/tools/generate_guard_texture.py \
    securityguard/src/main/resources/assets/securityguard/textures/entity/security_guard.png
git commit -m "$(cat <<'EOF'
feat(securityguard): paint gold police shield on left chest

Adds a 3x4 shield silhouette with 1px top-left highlight and 1px
bottom-right shadow, painted into the left chest of the torso front
panel. Regenerates the texture.
EOF
)"
```

---

## Task 7: Add the tools README

**Files:**
- Create: `securityguard/tools/README.md`

- [ ] **Step 1: Create the README**

Create file `securityguard/tools/README.md` with:

```markdown
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
```

- [ ] **Step 2: Commit**

```bash
git add securityguard/tools/README.md
git commit -m "$(cat <<'EOF'
docs(securityguard): add README for the texture generator tool

One-paragraph note on how to regenerate the entity texture and the
single dependency (Pillow).
EOF
)"
```

---

## Task 8: Idempotency check

**Files:** none modified.

- [ ] **Step 1: Confirm re-running the generator produces no diff**

Run:
```bash
python3 securityguard/tools/generate_guard_texture.py
git status securityguard/src/main/resources/assets/securityguard/textures/entity/security_guard.png
```
Expected: `git status` reports a clean working tree (or at most "nothing to commit, working tree clean"). If the PNG changed, the generator is non-deterministic — investigate before continuing.

- [ ] **Step 2: No commit. Verification only.**

If the PNG drifts, common causes:
- Pillow version differences in PNG metadata (the `optimize=True` flag should be stable across versions but not guaranteed).
- Floating-point ordering — none in this script, but worth checking.

If a drift appears, normalize by stripping ancillary PNG chunks or pinning a Pillow version in the README. **Do not proceed until the script is bit-stable.**

---

## Task 9: Manual smoke test in dev client

**Files:** none modified.

- [ ] **Step 1: Launch the dev client**

Run:
```bash
./gradlew :securityguard:runClient
```
Wait for Minecraft to load to the title screen.

- [ ] **Step 2: Spawn a guard and inspect the head**

Create a Creative-mode flat world. Spawn a Security Guard (use the spawn egg or build the iron-block I-shape and place a Guard Helmet on top).

Verify, looking at the guard from the front:
- Nose protrudes ~2 voxels forward of the head front
- Two black eye dots visible, flanking the nose
- Single dark unibrow stripe above the eyes
- No mouth
- Skin tone is villager-tan (subdued earthy brown, not bright pink)
- Cap unchanged in shape; gold band visible where the brim meets the crown

If the nose is **not visible**: most likely the `texOffs(56, 16)` slot maps to a transparent region of the PNG. Check that `paint_skin` covers `NOSE_UNWRAP` (Task 3) — it should.

If the nose **is visible but textured wrong** (e.g. shows random head pixels): the `texOffs` value collides with another part. Run the inspection sub-step below.

UV inspection sub-step (only if the nose looks wrong):
```bash
python3 - <<'PY'
from PIL import Image
img = Image.open('securityguard/src/main/resources/assets/securityguard/textures/entity/security_guard.png')
# Print the colors at the nose unwrap rect
for y in range(16, 20):
    row = []
    for x in range(56, 64):
        row.append(img.getpixel((x, y)))
    print(y, row)
PY
```
All pixels in this rect must be the SKIN color `(163, 125, 91, 255)`. If any are different, the nose UV slot was overwritten by a later paint pass — re-order or move the slot.

- [ ] **Step 3: Inspect the chest for the shield**

Front view, eye-level: a small gold shield silhouette is visible on the guard's left chest (i.e. the right side of the chest from the viewer's perspective, if looking at the guard face-on). It should be readable from ~5 blocks away and still recognizable from ~10 blocks away.

If the shield is missing or in the wrong place, re-check the `sx, sy` offset constants in `paint_shield` against `BODY_FRONT`'s origin.

- [ ] **Step 4: Smoke-test combat — confirm baton still attaches and AI still works**

Spawn a zombie nearby (creative inventory or `/summon zombie ~ ~ ~`). Verify:
- The guard runs to the zombie
- The guard's right arm holds the baton in the same position as before the refactor (the baton is unchanged; we only added the nose, no arm UV changes)
- The baton swings on attack
- The zombie gets Slowness II + Weakness I after each hit

If the baton renders in the wrong place: this plan should not have affected it, but if it broke, check that `RIGHT_ARM_*` UVs were untouched in the texture script.

- [ ] **Step 5: Profile view check**

Walk around the guard. The nose should break the head silhouette when viewed from the side, confirming the 3D part rendered correctly.

- [ ] **Step 6: Close the client; no commit needed**

Verification only. If everything reads right, the redesign is complete.

If the guard looks "muddy" (skin tone clashes with navy): consider the spec's deferred adjustment — lighten `SKIN` to `#B89070` or shift `NAVY` toward royal blue `#243C8C`. Make the change in the script's palette constants, regenerate, re-run smoke test. Only then commit, with a separate "tune palette after smoke test" commit.

---

## Task 10: Update the plan checkboxes and finish

**Files:** none modified.

- [ ] **Step 1: Mark every step in this plan complete**

Open `docs/superpowers/plans/2026-04-29-guard-villager-look.md`. Confirm every `- [ ]` is now `- [x]`. If executing via subagent-driven-development, the orchestrator handles this automatically.

- [ ] **Step 2: Done — no further commit needed.**

The redesign is done. The next visual iteration (e.g. a 3D pinned badge, sleeve cuff stripes, or skin-tone variants) can fork from this base by editing the palette/regions in the generator script and re-running.

---

## Self-review notes (for the implementer)

- Every code-touching task ends in a `git commit`. Verification-only tasks (Tasks 1, 8, 9, 10) do not commit; that's intentional so the bisect log only has substantive commits.
- The order matters: **add the nose part (Task 2) BEFORE writing the texture script (Task 3)**. If the texture were redrawn first, the missing `texOffs(56, 16)` paint would either show as an incorrect (transparent) nose or as the previous PNG's stale pixels in that slot, depending on how the existing PNG happens to look at those coordinates. Adding the part first lets each subsequent texture task refresh the whole image deterministically.
- The `texOffs(56, 16)` slot was chosen because the right-arm UV unwrap (4×12×4 at `texOffs(40, 16)`) terminates at x=56, leaving x=56..64 by y=16..32 free — more than enough for an 8×4 nose unwrap.
- The shield is **3 pixels wide and 4 pixels tall**. If the spec's "could read as a smudge at distance" risk materializes, bump to 4×5 by editing `paint_shield`'s `shield_pixels` list and re-centering `(sx, sy)`.
- The nose anchor `(-1.0f, -2.0f, -5.0f)` puts the nose front face at z=-5, exactly one voxel forward of the head front (z=-4). If the nose visually clips into the cap brim during smoke test, drop the anchor to `(-1.0f, -1.0f, -5.0f)` (lower by one voxel — that's a clean 4-voxel gap to the brim).
- Pillow's `optimize=True` flag is generally bit-stable across point releases. If Task 8's idempotency check fails, the most likely cause is a Pillow version that writes a different PNG metadata chunk; pin to the version on the dev's machine in `tools/README.md` if that happens.
