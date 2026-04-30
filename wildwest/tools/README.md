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
