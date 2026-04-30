# Realm deployment guide (Switch via Bedrock Realms)

End-to-end guide for taking the translator's `.mcaddon` files and standing
them up on a Bedrock Realm so a Switch player can join. The four mods
(`securitycore`, `securityguard`, `creeperskin`, `thief`) are the entire
scope; this guide is intentionally Switch-focused.

## Prerequisites

- A Bedrock-edition Minecraft account on a desktop / mobile platform you can
  drag-drop files on (Windows, macOS, iPad, or Android). The Switch itself
  cannot import `.mcaddon` files directly — you import them from a host
  device, then the Switch downloads them automatically when joining the
  Realm.
- An active **Realms Plus** or **Realms** subscription on that account.
- The four `.mcaddon` files produced by the translator. Build them locally:
  ```sh
  ./gradlew :translator:packAddon
  ```
  Output lands at:
  - `bedrock-out/securitycore.mcaddon`
  - `bedrock-out/securityguard.mcaddon`
  - `bedrock-out/creeperskin.mcaddon`
  - `bedrock-out/thief.mcaddon`

## Step 1 — Install the packs on your host machine

`securityguard.mcaddon` and `thief.mcaddon` already bundle securitycore
inside, so importing either of them imports `securitycore_BP` and
`securitycore_RP` along the way. You still need to import each `.mcaddon`
once so all four mods land in *My Packs*.

### Windows / macOS

1. Open Bedrock Minecraft on your host machine.
2. Make sure the game is at the main menu (not in a world). Background
   it (don't quit).
3. Double-click each `.mcaddon` file in turn. Bedrock auto-imports.
4. After all four are imported, return to the main menu and check
   *Settings → Global Resources* and *Settings → Behavior Packs*. You
   should see all eight pack entries:
   - Security Core (Behavior Pack)
   - Security Core (Resource Pack)
   - Security Guard (Behavior Pack)
   - Security Guard (Resource Pack)
   - Creeperskin (Behavior Pack)
   - Creeperskin (Resource Pack)
   - Thief (Behavior Pack)
   - Thief (Resource Pack)

### iPad / Android

1. Save the `.mcaddon` files to *Files* (iPad) or *Downloads* (Android).
2. Tap each one; choose *Open in Minecraft*.
3. Bedrock launches and imports automatically.

## Step 2 — Create or open a Realm world

1. Main menu → *Play* → *Realms* tab → *Create New Realm* (or pick an
   existing Realm).
2. When configuring the world, **either**:
   - Create a new world for the security mods to drop into, **or**
   - *Edit World* on the Realm's existing world.
3. Open *Settings → Behavior Packs*. Activate, **in this order**:
   1. *Security Core (Behavior Pack)* — must come first, the others
      depend on it.
   2. *Security Guard (Behavior Pack)*
   3. *Thief (Behavior Pack)*
   4. *Creeperskin (Behavior Pack)*
4. Open *Settings → Resource Packs*. Activate, in any order:
   - *Security Core (Resource Pack)*
   - *Security Guard (Resource Pack)*
   - *Thief (Resource Pack)*
   - *Creeperskin (Resource Pack)*
5. Save world settings. The Realm uploads the world (and packs) to
   Mojang's servers.

> Bedrock enforces the dependency order via the manifest UUIDs. Activating
> Security Guard before Security Core surfaces a "missing dependency" error
> in the activation list — fix by activating the core first.

## Step 3 — Switch player joins

1. The Switch player needs to be added as a friend on the Realm owner's
   Microsoft account, **or** invited explicitly to the Realm.
2. On Switch: *Play* → *Friends* → the Realm shows up under the owner's
   name. Tap it.
3. Bedrock auto-downloads every required pack to the Switch on first
   join. **This can take several minutes** for the initial download — the
   Switch's network stack is slower than a desktop's.
4. Once download completes, the Switch player spawns into the world with
   all packs active.

> The Switch never sees the `.mcaddon` files. It only sees them as Realm
> resource/behavior packs streamed from Mojang's servers, which is why
> `.mcaddon` Marketplace signing is unnecessary for this deployment path.

## Step 4 — Verify in-game

A quick sanity check the security mods loaded:
- `/summon securityguard:guard` should spawn a guard mob (no error).
- `/give @s securityguard:guard_helmet` should give the helmet item
  (icon visible in inventory).
- `/summon thief:thief` should spawn a thief mob.
- `/give @s creeperskin:creeper_armor_helmet` should give a creeper-skin
  helmet.

Plain vanilla mobs and recipes continue to work — the packs are purely
additive.

## Troubleshooting

**Pack does not appear in *My Packs* after importing.**
Re-import the `.mcaddon`. If it still fails, the pack's
`min_engine_version` may be newer than your Bedrock client. The translator
pins `[1, 21, 80]`. Update Bedrock and try again.

**World load shows a "missing dependency" red banner.**
A behavior pack was activated before its dependency. Most often: Security
Guard or Thief was enabled before Security Core. Fix by reordering
activations (Security Core BP first).

**Spawn-egg colors look wrong (uniform grey).**
Phase 3's LLM stage hasn't run on this checkout, so spawn-egg base/overlay
colors are placeholder defaults. The eggs still work; they just don't have
the original mod's palette. Run `:translator:translate --args="--with-llm"`
locally with `ANTHROPIC_API_KEY` set, commit the result, and rebuild
`.mcaddon`.

**Custom AI behavior missing on a security guard.**
Phase 3 LLM stubs are TODOs unless the live LLM stage was run. The mob
will use vanilla goals (move toward target, defend village, look at
player) but not the mod's bespoke goals (`StunningMeleeGoal`,
`GuardTargetHostilesGoal`, etc.) until you run with `--with-llm`.

**Switch shows "Pack ID conflict" when joining the Realm.**
Two packs are claiming the same UUID, usually because an older translator
output and a newer one were both installed on the host. Remove the older
copies from *My Packs*, then re-import the latest `.mcaddon`s.

**General "I don't know what's wrong" debug.**
Each mod ships its own `bedrock-out/<modId>/UNTRANSLATABLE.md` file
listing every translation loss for that mod. Most "behavior X is missing
on Bedrock" bugs are explained there.

## Rolling out updates

The translator's UUIDs are deterministic, so a rerun produces the same
header UUIDs as the previous build. Bedrock recognizes a re-imported
`.mcaddon` with the same UUID as an *update* to the existing pack rather
than a duplicate. The Realm world keeps working through pack upgrades:

1. On the host, build new `.mcaddon`s: `./gradlew :translator:packAddon`.
2. Re-import each `.mcaddon`. *My Packs* stays at four entries; Bedrock
   silently bumps the pack version.
3. Open the Realm; Bedrock prompts to upgrade the world to the new pack
   versions. Accept.
4. On Switch, the next join auto-downloads the updated packs.

If a future translator change bumps a pack's `version` field, this
upgrade path is the only one — Bedrock has no concept of "downgrade", so
keep an off-realm copy of the working world if you need rollback.
