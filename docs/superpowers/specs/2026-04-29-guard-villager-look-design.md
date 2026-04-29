# Security Guard — Villager-Like Visual Redesign

**Date:** 2026-04-29
**Status:** Approved (pending user review of this doc)
**Parent:** [2026-04-29-security-guard-mod-design.md](2026-04-29-security-guard-mod-design.md)

## Goal

Make the Security Guard read more "realistic" by giving him villager-style face features (protruding nose, dot eyes, unibrow, villager skin tone) while keeping the existing navy uniform, peaked cap, and adding a painted gold police shield on the chest.

## Non-Goals

- Switching to vanilla `VillagerModel` (would cascade through `HumanoidMobRenderer`, `HeldItemLayer`, walk-cycle hooks).
- Multiple guard variants / randomized skin tones.
- Profession-style robe replacing legs.
- Animated badge, glow, or particle effects.
- Touching the baton model, the guard's AI, or any server-side code.

## Approach

**Decorate the existing `HumanoidModel`-based guard** rather than replacing it. Three changes only:

1. Add a single nose `ModelPart` as a child of `head` in `SecurityGuardModel`.
2. Repaint `security_guard.png` (64×64) with villager-style face, villager skin tone on exposed skin, shaded uniform, and a flat gold shield on the chest.
3. Commit a deterministic texture-generation script so future tweaks regenerate the PNG from a small declarative spec.

No new files in Java, no renderer changes, no AI changes.

## Scope

### Files modified

- `securityguard/src/main/java/com/tweeks/securityguard/client/model/SecurityGuardModel.java` — add nose part
- `securityguard/src/main/resources/assets/securityguard/textures/entity/security_guard.png` — repainted

### Files added

- `securityguard/tools/generate_guard_texture.py` — deterministic PIL-based generator that produces `security_guard.png` from a hand-authored color/region spec embedded in the script. Run once per visual change; output is committed.
- `securityguard/tools/README.md` — one-paragraph note: "Run `python3 generate_guard_texture.py` to regenerate the texture; commit the resulting PNG."

### Files unchanged

- `SecurityGuardRenderer.java`
- `BatonModel.java`
- `HeldItemLayer` (in securitycore)
- `SecurityGuardEntity.java`
- All AI goals
- All mod metadata (`neoforge.mods.toml`, `gradle.properties`)

## Component Design

### Nose `ModelPart`

Added inside `SecurityGuardModel.createBodyLayer()` as a child of the existing `head` part.

| Property | Value | Reasoning |
|---|---|---|
| Size | 2×2×2 voxels | Slightly under vanilla villager's 2×4×2 — guard is humanoid-leaning, not full villager |
| Anchor offset | `(-1.0f, -2.0f, -5.0f)` | Centers on face X, sits at eye line Y, origin one voxel forward of the head's front face (z=-4) so the 2-voxel-deep nose protrudes z=-5..-3 |
| Parent | `head` | Inherits head pitch/yaw rotation |
| `texOffs` | TBD during implementation | Verify the chosen slot does not overlap existing cap or humanoid UV regions before painting. A 2×2×2 cube needs an 8-wide × 4-tall UV strip; pick the first empty slot found by visual inspection of the current 64×64 sheet |

### Texture repaint

64×64 PNG. Each region authored as a constant in the generator script, rendered to a fresh image, and saved.

| Region | Pixel size | Color spec | Notes |
|---|---|---|---|
| Head — face front | 8×8 | base `#A37D5B` (villager skin tone); brow `#3A2618` 8×1 strip; eyes 1×1 black dots flanking nose | No mouth (matches vanilla villager) |
| Head — sides/back/top/bottom | as before | villager skin tone | Replaces previous player-tan |
| Nose part UVs | 2×2 face × 6 = small UV cluster at chosen `texOffs` | villager skin tone | Solid; tiny shading diff on side faces if it fits |
| Cap brim + crown | as before | navy `#1F2A55` base; thin gold band `#D4A82A` 1px row where brim meets crown | Cap geometry unchanged; only paint changes |
| Torso — front | 8×12 | navy base; 1px lighter-blue `#2D3D70` highlight along shoulder line; 1px darker-blue `#152040` shadow along bottom hem; gold shield silhouette | Shield: 3 wide × 4 tall, painted center-left of chest, gold `#D4A82A` core, 1px highlight `#F1C84A` top-left, 1px shadow `#9C7A1F` bottom-right |
| Torso — sides/back | as before | navy with shoulder highlight wrapping | No badge on back |
| Arms | as before | navy uniform with villager-skin-tone hands | Wrists transition with 1px darker line |
| Legs | as before | navy trousers, slightly darker than torso | Subtle shading |

### Texture generator script

Python 3, single file, dependency: Pillow (PIL).

Top of the script declares named constants for every color and every region's pixel rect. The `main()` function:

1. Creates a 64×64 RGBA image, fills transparent.
2. Fills each region rect with its base color in declaration order.
3. Paints fine details (brow, eyes, badge, highlights, shadows) as a final overlay pass.
4. Saves to `../src/main/resources/assets/securityguard/textures/entity/security_guard.png`.

Idempotent — running it twice produces byte-identical output.

## Risks & Mitigations

1. **UV collision on nose `texOffs`** — if the chosen offset overlaps an existing UV region (cap, head, or humanoid body), one part renders garbled. Mitigation: pre-implementation, map all existing `texOffs` rectangles on the 64×64 sheet, then pick a slot known to be empty. The vanilla humanoid sheet has free space below the standard head/body unwrap; the cap occupies a region near `(32, 0)`. A side-by-side check before the first paint pass will catch overlap.
2. **Cap brim clipping the nose** — brim sits at head-relative y=-9..-8, nose top at y=-2 (its origin is y=-2 with size 2 going down to y=0). Vertical clearance is 6 voxels — safe. If clipping is observed in `runClient`, drop nose anchor to `(-1.0f, -1.0f, -5.0f)`.
3. **Villager skin tone clashes with navy** — if the contrast reads as muddy, lighten the skin to `#B89070` or shift uniform from navy to royal blue `#243C8C`. Decision deferred to first visual smoke test.
4. **Shield reads as a smudge at distance** — at 3-pixel width, the shield silhouette may not be legible past ~10 blocks. Mitigation: if smoke test confirms, bump to 4×5 and re-center; skip if it reads fine.

## Testing

- **Build:** `./gradlew :securityguard:build` — must remain green.
- **Existing tests:** `SpawnPatternTest` is server-side and unaffected.
- **Manual smoke test (`runClient`):**
  1. Spawn a guard (spawn egg or iron-block + helmet pattern).
  2. Front-on view: nose protrudes, eyes + brow visible, gold shield legible on chest.
  3. Profile view: nose silhouette breaks the head outline; cap unchanged.
  4. Spawn a zombie nearby; confirm baton swing animation plays correctly (no UV regression on hand/arm).
  5. Watch from ~10 blocks back: shield still readable.
- **Texture regeneration:** `cd securityguard/tools && python3 generate_guard_texture.py` — confirm the resulting PNG matches the committed one byte-for-byte (`git diff` is empty).

## Open Decisions Resolved During Brainstorm

- **Approach A vs B vs C** → A (decorate `HumanoidModel`).
- **Badge style** → Police shield, painted (flat) into texture, not a 3D `ModelPart`.
- **Texture script vs hand-edit** → Committed Python+PIL script.

## Out of Scope / Future Work

- Multiple skin tone variants (vanilla villagers have several biome tints).
- 3D pinned badge if the painted version reads as too flat.
- Sleeve cuff stripes for officer rank.
- Switching to a true villager body (robe legs).
