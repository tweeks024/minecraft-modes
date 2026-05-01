# wildwest texture & model tools

Phase 1 ships hand-written model JSONs at
`wildwest/src/main/resources/assets/wildwest/models/item/{pistol,rifle,
rifle_bolt_open,rifle_bolt_closing}.json` and a placeholder texture per
item. To iterate visually:

1. Install Blockbench from https://www.blockbench.net/
2. **File -> Import -> Generic Model** on a model JSON above.
3. Edit cubes / paint texture in Blockbench.
4. Save as `.bbmodel` in this folder for source-of-truth (committed alongside
   the exported JSON).
5. Re-export to the same `models/item/<name>.json` path.

The bullet entity model is rendered programmatically via `BulletRenderer`;
its texture lives at `assets/wildwest/textures/entity/bullet.png`.

The rifle's bolt-cycle animation is driven by the cooldown-driven model
selector in `assets/wildwest/items/rifle.json` -- modifying that JSON
switches between rifle, rifle_bolt_open, rifle_bolt_closing variants.

## Phase 2 mob bbmodels

The four mobs in phase 2 (deputy, sherrif, bandit, bandit_leader) reuse
the vanilla `HumanoidModel` (player skeleton). Texture authoring goes
through the standard 64×64 player UV layout:

- Head: top-left 32×16
- Body: 16×16 starting at (16, 16)
- Right arm / left arm / right leg / left leg: their standard regions

To author in Blockbench:

1. **File → New → Modded Entity** (or open a copy of `securityguard/tools/security_guard.bbmodel` as a template if you want a pre-built humanoid skeleton).
2. Import the placeholder texture, paint over it.
3. Export as PNG, save to `assets/wildwest/textures/entity/<mob_id>.png` overwriting the placeholder.
4. Save the .bbmodel source in `wildwest/tools/<mob_id>.bbmodel` for future iteration.
