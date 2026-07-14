package com.tweeks.translator.java

import com.tweeks.translator.discover.ModDiscovery
import com.tweeks.translator.emit.Untranslatable
import com.tweeks.translator.json.AssetCopier
import com.tweeks.translator.json.ItemAtlasBuilder
import com.tweeks.translator.manifest.BedrockTarget
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * End-to-end test for [ItemAnalyzer] over the real mods.
 */
class ItemAnalyzerTest {

    private val repoRoot: Path = ModDiscovery.findRepoRoot(Path.of("").toAbsolutePath())
    private val target = BedrockTarget.load(repoRoot.resolve("translator/bedrock-target.json"))

    @Test
    fun `securityguard items emit expected components`(@TempDir outDir: Path) {
        val resolver = ClasspathResolver.fromSystemProperties()
        assertTrue(resolver.isAvailable())
        val all = ModDiscovery(repoRoot).discover()
        val mod = all.first { it.modId == "securityguard" }
        val unt = Untranslatable()
        val sources = JavaSourceLoader(resolver, unt).load(mod, all)
        ItemAnalyzer(target, unt).analyze(mod, sources, outDir)

        // baton: durability 250, attack damage 6, hand_equipped, custom-behavior logged.
        val batonJson = read(outDir, "securityguard/behavior_pack/items/baton.json")
        val batonComponents = itemComponents(batonJson)
        assertEquals(250, batonComponents["minecraft:durability"]!!.jsonObject["max_durability"]!!.jsonPrimitive.intOrNull)
        assertEquals(6, batonComponents["minecraft:damage"]!!.jsonPrimitive.intOrNull)
        assertEquals(true, batonComponents["minecraft:hand_equipped"]!!.jsonPrimitive.content.toBoolean())

        // guard_helmet: stack 64, no damage/durability.
        val helmetJson = read(outDir, "securityguard/behavior_pack/items/guard_helmet.json")
        val helmetComponents = itemComponents(helmetJson)
        assertEquals(64, helmetComponents["minecraft:max_stack_size"]!!.jsonPrimitive.intOrNull)

        // spawn egg: spawn_egg component points at securityguard:guard.
        val eggJson = read(outDir, "securityguard/behavior_pack/items/guard_spawn_egg.json")
        val eggComponents = itemComponents(eggJson)
        val spawnEgg = eggComponents["minecraft:spawn_egg"]!!.jsonObject
        assertEquals("securityguard:guard", spawnEgg["type_id"]!!.jsonPrimitive.content)
        assertNotNull(spawnEgg["base_color"])
        assertNotNull(spawnEgg["overlay_color"])

        // Attachable for the baton (item bbmodel exists).
        val attachable = read(outDir, "securityguard/resource_pack/attachables/baton.json")
        val attachableDesc = attachable["minecraft:attachable"]!!.jsonObject["description"]!!.jsonObject
        assertEquals("securityguard:baton", attachableDesc["identifier"]!!.jsonPrimitive.content)
        assertEquals(
            "geometry.securityguard.baton",
            attachableDesc["geometry"]!!.jsonObject["default"]!!.jsonPrimitive.content,
        )

        // Untranslatable: BatonItem.postHurtEnemy + GuardHelmetItem.useOn.
        val report = unt.renderReport("securityguard")
        assertTrue(report.contains("Item custom behavior"))
        assertTrue(report.contains("postHurtEnemy"))
        assertTrue(report.contains("useOn"))
        assertTrue(report.contains("Spawn egg colors hardcoded"))
    }

    @Test
    fun `creeperskin armor items map to wearable slots`(@TempDir outDir: Path) {
        val resolver = ClasspathResolver.fromSystemProperties()
        assertTrue(resolver.isAvailable())
        val all = ModDiscovery(repoRoot).discover()
        val mod = all.first { it.modId == "creeperskin" }
        val unt = Untranslatable()
        val sources = JavaSourceLoader(resolver, unt).load(mod, all)
        ItemAnalyzer(target, unt).analyze(mod, sources, outDir)

        val helmet = itemComponents(read(outDir, "creeperskin/behavior_pack/items/creeper_helmet.json"))
        assertEquals(
            "slot.armor.head",
            helmet["minecraft:wearable"]!!.jsonObject["slot"]!!.jsonPrimitive.content,
        )
        assertEquals(1, helmet["minecraft:max_stack_size"]!!.jsonPrimitive.intOrNull)
        assertNotNull(helmet["minecraft:fire_resistant"])

        val chest = itemComponents(read(outDir, "creeperskin/behavior_pack/items/creeper_chestplate.json"))
        assertEquals(
            "slot.armor.chest",
            chest["minecraft:wearable"]!!.jsonObject["slot"]!!.jsonPrimitive.content,
        )

        val legs = itemComponents(read(outDir, "creeperskin/behavior_pack/items/creeper_leggings.json"))
        assertEquals(
            "slot.armor.legs",
            legs["minecraft:wearable"]!!.jsonObject["slot"]!!.jsonPrimitive.content,
        )

        val boots = itemComponents(read(outDir, "creeperskin/behavior_pack/items/creeper_boots.json"))
        assertEquals(
            "slot.armor.feet",
            boots["minecraft:wearable"]!!.jsonObject["slot"]!!.jsonPrimitive.content,
        )

        // CreeperArmorMaterials.DEFENSE is netherite-grade (3/8/6/3) — the
        // folded values, not the old hardcoded iron defaults (2/6/5/2).
        assertEquals(3, helmet["minecraft:wearable"]!!.jsonObject["protection"]!!.jsonPrimitive.intOrNull)
        assertEquals(8, chest["minecraft:wearable"]!!.jsonObject["protection"]!!.jsonPrimitive.intOrNull)
        assertEquals(6, legs["minecraft:wearable"]!!.jsonObject["protection"]!!.jsonPrimitive.intOrNull)
        assertEquals(3, boots["minecraft:wearable"]!!.jsonObject["protection"]!!.jsonPrimitive.intOrNull)

        val report = unt.renderReport("creeperskin")
        assertFalse(report.contains("iron-armor defaults")) { report }
        assertTrue(report.contains("worn-armor visuals are absent on Bedrock")) { report }
        assertTrue(report.contains("the armor geometry/textures are emitted but no attachable consumes them")) { report }
    }

    @Test
    fun `thief blackjack picks up class-constructor stack size and damage`(@TempDir outDir: Path) {
        val resolver = ClasspathResolver.fromSystemProperties()
        assertTrue(resolver.isAvailable())
        val all = ModDiscovery(repoRoot).discover()
        val mod = all.first { it.modId == "thief" }
        val unt = Untranslatable()
        val sources = JavaSourceLoader(resolver, unt).load(mod, all)
        ItemAnalyzer(target, unt).analyze(mod, sources, outDir)

        val components = itemComponents(read(outDir, "thief/behavior_pack/items/blackjack.json"))
        assertEquals(1, components["minecraft:max_stack_size"]!!.jsonPrimitive.intOrNull)
        assertEquals(2, components["minecraft:damage"]!!.jsonPrimitive.intOrNull)

        val report = unt.renderReport("thief")
        assertTrue(report.contains("hurtEnemy"))
    }

    @Test
    fun `durability falls back to item class constructor when registration omits it`(@TempDir outDir: Path) {
        // Wildwest pattern: `ITEMS.registerItem("pistol", PistolItem::new, p -> p)`
        // with `super(properties.stacksTo(1).durability(300))` inside PistolItem.
        // Previously the registration-site builder lambda was the only source
        // for durability; class-constructor `.durability(N)` calls were ignored.
        val registrationSrc = """
            package com.example.themod;
            class Registration {
                public static final Object PISTOL = ITEMS.registerItem(
                    "pistol", PistolItem::new, p -> p);
            }
        """.trimIndent()
        val pistolItemSrc = """
            package com.example.themod;
            public class PistolItem {
                public PistolItem(Properties properties) {
                    super(properties.stacksTo(1).durability(300));
                }
            }
        """.trimIndent()
        val mod = ModDiscovery.DiscoveredMod(modId = "themod", rootDir = outDir.resolve("themodroot"))
        java.nio.file.Files.createDirectories(mod.rootDir)
        val parser = com.github.javaparser.JavaParser()
        val units = listOf(registrationSrc, pistolItemSrc).map { parser.parse(it).result.orElseThrow() }
        val sources = JavaSourceLoader.ResolvedModSources(
            mod = mod,
            units = units,
            typeSolver = com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver(),
        )

        val unt = Untranslatable()
        ItemAnalyzer(target, unt).analyze(mod, sources, outDir)

        val pistol = itemComponents(read(outDir, "themod/behavior_pack/items/pistol.json"))
        assertEquals(
            300,
            pistol["minecraft:durability"]!!.jsonObject["max_durability"]!!.jsonPrimitive.intOrNull,
        )
        // Items with durability are conventionally hand-equipped tools.
        assertEquals(true, pistol["minecraft:hand_equipped"]!!.jsonPrimitive.content.toBoolean())
    }

    @Test
    fun `spawn egg resolves entity id when registration lives in a separate file`(@TempDir outDir: Path) {
        // Reproduces the wildwest layout: ModEntities.java registers entities,
        // Registration.java registers items including spawn eggs that reference
        // ModEntities.<CONSTANT>.get(). The two are in different
        // CompilationUnits, so a per-unit entity-id map would fail to resolve
        // the spawn egg's type_id.
        val modEntitiesSrc = """
            package com.example.themod;
            class ModEntities {
                public static final Object DEPUTY =
                    ENTITY_TYPES.register("deputy", () -> null);
                public static final Object PIRATE =
                    ENTITY_TYPES.register("pirate", () -> null);
            }
        """.trimIndent()
        val registrationSrc = """
            package com.example.themod;
            class Registration {
                public static final Object DEPUTY_SPAWN_EGG = ITEMS.registerItem(
                    "deputy_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.DEPUTY.get()));
                public static final Object PIRATE_SPAWN_EGG = ITEMS.registerItem(
                    "pirate_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.PIRATE.get()));
            }
        """.trimIndent()

        val mod = ModDiscovery.DiscoveredMod(modId = "themod", rootDir = outDir.resolve("themodroot"))
        java.nio.file.Files.createDirectories(mod.rootDir)
        val parser = com.github.javaparser.JavaParser()
        val units = listOf(modEntitiesSrc, registrationSrc).map {
            parser.parse(it).result.orElseThrow()
        }
        val sources = JavaSourceLoader.ResolvedModSources(
            mod = mod,
            units = units,
            typeSolver = com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver(),
        )

        val unt = Untranslatable()
        ItemAnalyzer(target, unt).analyze(mod, sources, outDir)

        val deputy = itemComponents(read(outDir, "themod/behavior_pack/items/deputy_spawn_egg.json"))
        assertEquals(
            "themod:deputy",
            deputy["minecraft:spawn_egg"]!!.jsonObject["type_id"]!!.jsonPrimitive.content,
        )
        val pirate = itemComponents(read(outDir, "themod/behavior_pack/items/pirate_spawn_egg.json"))
        assertEquals(
            "themod:pirate",
            pirate["minecraft:spawn_egg"]!!.jsonObject["type_id"]!!.jsonPrimitive.content,
        )
    }

    @Test
    fun `starwars lightsaber select-type item model resolves to a static existing-texture icon`(@TempDir outDir: Path) {
        // lightsaber.json's client model is a `minecraft:select` on the
        // starwars:blade_color data component with 4 per-color cases (blue,
        // green, red, purple) plus a blue fallback. The old behavior
        // hardcoded the icon to `starwars:lightsaber`, a key item_texture.json
        // never defines (only the per-color keys exist) — a broken icon.
        val resolver = ClasspathResolver.fromSystemProperties()
        assertTrue(resolver.isAvailable())
        val all = ModDiscovery(repoRoot).discover()
        val mod = all.first { it.modId == "starwars" }
        val unt = Untranslatable()
        // Mirror the real Cli.kt pipeline order: assets are copied before
        // the Java pipeline runs, so item_texture.json short names exist by
        // the time ItemAnalyzer resolves an icon/attachable texture.
        val copyResult = AssetCopier(unt).copy(mod.rootDir, mod.modId, outDir)
        ItemAtlasBuilder().build(mod.modId, copyResult.itemTextureShortNames, outDir)
        val sources = JavaSourceLoader(resolver, unt).load(mod, all)
        ItemAnalyzer(target, unt).analyze(mod, sources, outDir)

        val lightsaber = itemComponents(read(outDir, "starwars/behavior_pack/items/lightsaber.json"))
        val icon = lightsaber["minecraft:icon"]!!.jsonPrimitive.content
        assertEquals("starwars:lightsaber_blue", icon)

        // The chosen icon key must actually exist in item_texture.json —
        // that's the whole point of the fix.
        val atlas = read(outDir, "starwars/resource_pack/textures/item_texture.json")
        assertNotNull(atlas["texture_data"]!!.jsonObject[icon]) { "expected $icon in item_texture.json" }

        val report = unt.renderReport("starwars")
        assertTrue(report.contains("Item model selector not translatable")) { report }
        assertTrue(report.contains("`lightsaber`")) { report }
        assertTrue(report.contains("item model selector not translatable — using starwars:lightsaber_blue as static icon")) { report }
    }

    @Test
    fun `starwars weapon attachables reference textures that actually exist in the output`(@TempDir outDir: Path) {
        // blaster_pistol/blaster_rifle/lightsaber attachables all reference
        // `textures/entity/<itemId>` (no extension), but starwars never
        // authored entity-view textures for these weapons — only flat item
        // icons. The fix substitutes the resolved item-icon texture (which
        // AssetCopier does place in the output) instead of a dangling path.
        val resolver = ClasspathResolver.fromSystemProperties()
        assertTrue(resolver.isAvailable())
        val all = ModDiscovery(repoRoot).discover()
        val mod = all.first { it.modId == "starwars" }
        val unt = Untranslatable()
        val copyResult = AssetCopier(unt).copy(mod.rootDir, mod.modId, outDir)
        ItemAtlasBuilder().build(mod.modId, copyResult.itemTextureShortNames, outDir)
        val sources = JavaSourceLoader(resolver, unt).load(mod, all)
        ItemAnalyzer(target, unt).analyze(mod, sources, outDir)

        fun attachableTexture(itemId: String): String {
            val attachable = read(outDir, "starwars/resource_pack/attachables/$itemId.json")
            return attachable["minecraft:attachable"]!!.jsonObject["description"]!!.jsonObject["textures"]!!
                .jsonObject["default"]!!.jsonPrimitive.content
        }

        assertEquals("textures/items/blaster_pistol", attachableTexture("blaster_pistol"))
        assertEquals("textures/items/blaster_rifle", attachableTexture("blaster_rifle"))
        assertEquals("textures/items/lightsaber_blue", attachableTexture("lightsaber"))

        // Every emitted attachable texture path must resolve to a real PNG
        // in the output tree — no silent dangling references.
        for (itemId in listOf("blaster_pistol", "blaster_rifle", "lightsaber")) {
            val texRelPath = attachableTexture(itemId)
            val pngPath = outDir.resolve("starwars/resource_pack/$texRelPath.png")
            assertTrue(pngPath.toFile().exists()) { "expected $pngPath to exist for $itemId's attachable" }
        }

        val report = unt.renderReport("starwars")
        assertTrue(report.contains("Attachable held-item texture substituted or missing")) { report }
        assertTrue(report.contains("`blaster_pistol`")) { report }
        assertTrue(report.contains("`blaster_rifle`")) { report }
        assertFalse(report.contains("no item-icon")) { "expected a fallback texture to be found, not a hard miss: $report" }
    }

    @Test
    fun `landspeeder item gets entity_placer, non-vehicle items do not`(@TempDir outDir: Path) {
        // LandspeederItem's `use()` spawns a LandspeederEntity (a
        // VehicleEntity subclass), but nothing about the item registration
        // itself says so — without minecraft:entity_placer, the crafted item
        // does nothing when used on Bedrock. Registration.java registers the
        // item as "landspeeder" and ModEntities.java registers the entity as
        // "landspeeder" too, so id-matching against EntityAnalyzer's vehicle
        // classes is how ItemAnalyzer wires this up.
        val resolver = ClasspathResolver.fromSystemProperties()
        assertTrue(resolver.isAvailable())
        val all = ModDiscovery(repoRoot).discover()
        val mod = all.first { it.modId == "starwars" }
        val unt = Untranslatable()
        val sources = JavaSourceLoader(resolver, unt).load(mod, all)
        ItemAnalyzer(target, unt).analyze(mod, sources, outDir)

        val speeder = itemComponents(read(outDir, "starwars/behavior_pack/items/landspeeder.json"))
        assertEquals(
            "starwars:landspeeder",
            speeder["minecraft:entity_placer"]!!.jsonObject["entity"]!!.jsonPrimitive.content,
        )

        // A non-vehicle item (e.g. the lightsaber) must not get one.
        val lightsaber = itemComponents(read(outDir, "starwars/behavior_pack/items/lightsaber.json"))
        assertFalse(lightsaber.containsKey("minecraft:entity_placer"))
    }

    @Test
    fun `vehicle item gets no attachable, a normal item still does`(@TempDir outDir: Path) {
        // landspeeder.bbmodel exists under starwars/tools/ — before the fix
        // this made ItemAnalyzer emit an `attachables/landspeeder.json` that
        // renders the entire 3-block vehicle geometry as the held-item
        // model. Vehicle items must be suppressed; a normal item with its
        // own bbmodel (e.g. the lightsaber) must be unaffected.
        val resolver = ClasspathResolver.fromSystemProperties()
        assertTrue(resolver.isAvailable())
        val all = ModDiscovery(repoRoot).discover()
        val mod = all.first { it.modId == "starwars" }
        val unt = Untranslatable()
        val sources = JavaSourceLoader(resolver, unt).load(mod, all)
        ItemAnalyzer(target, unt).analyze(mod, sources, outDir)

        val speederAttachable = outDir.resolve("starwars/resource_pack/attachables/landspeeder.json")
        assertFalse(speederAttachable.toFile().exists()) {
            "vehicle item must not get an attachable: $speederAttachable"
        }

        val lightsaberAttachable = read(outDir, "starwars/resource_pack/attachables/lightsaber.json")
        assertEquals(
            "starwars:lightsaber",
            lightsaberAttachable["minecraft:attachable"]!!.jsonObject["description"]!!.jsonObject["identifier"]!!.jsonPrimitive.content,
        )
    }

    @Test
    fun `armor protection folds the ArmorMaterial DEFENSE map per piece`(@TempDir outDir: Path) {
        // Mirrors starwars' HanSoloArmorMaterials: a netherite-grade DEFENSE
        // map (helmet 3 / chest 8 / legs 6 / boots 3). Before the fix, every
        // modded armor piece was emitted with hardcoded iron-armor defaults
        // (2/6/5/2), silently under-powering any non-iron-tier set on Bedrock.
        val materialsSrc = """
            package com.example.themod;
            public final class MyArmorMaterials {
                private static final Map<ArmorType, Integer> DEFENSE = Map.of(
                    ArmorType.BOOTS,      3,
                    ArmorType.LEGGINGS,   6,
                    ArmorType.CHESTPLATE, 8,
                    ArmorType.HELMET,     3,
                    ArmorType.BODY,       19);
                public static final ArmorMaterial MY = new ArmorMaterial(
                    37, DEFENSE, 15, null, 3.0F, 0.1F, null, null);
            }
        """.trimIndent()
        val registrationSrc = """
            package com.example.themod;
            class Registration {
                public static final Object HELMET = ITEMS.registerItem("my_helmet",
                    Item::new, p -> p.humanoidArmor(MyArmorMaterials.MY, ArmorType.HELMET));
                public static final Object CHESTPLATE = ITEMS.registerItem("my_chestplate",
                    Item::new, p -> p.humanoidArmor(com.example.themod.MyArmorMaterials.MY,
                        net.minecraft.world.item.equipment.ArmorType.CHESTPLATE));
                public static final Object LEGGINGS = ITEMS.registerItem("my_leggings",
                    Item::new, p -> p.humanoidArmor(MyArmorMaterials.MY, ArmorType.LEGGINGS));
                public static final Object BOOTS = ITEMS.registerItem("my_boots",
                    Item::new, p -> p.humanoidArmor(MyArmorMaterials.MY, ArmorType.BOOTS));
            }
        """.trimIndent()
        val mod = ModDiscovery.DiscoveredMod(modId = "themod", rootDir = outDir.resolve("themodroot"))
        java.nio.file.Files.createDirectories(mod.rootDir)
        val parser = com.github.javaparser.JavaParser()
        val units = listOf(materialsSrc, registrationSrc).map { parser.parse(it).result.orElseThrow() }
        val sources = JavaSourceLoader.ResolvedModSources(
            mod = mod,
            units = units,
            typeSolver = com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver(),
        )

        val unt = Untranslatable()
        ItemAnalyzer(target, unt).analyze(mod, sources, outDir)

        fun protection(itemId: String): Int? =
            itemComponents(read(outDir, "themod/behavior_pack/items/$itemId.json"))["minecraft:wearable"]!!
                .jsonObject["protection"]!!.jsonPrimitive.intOrNull

        assertEquals(3, protection("my_helmet"))
        assertEquals(8, protection("my_chestplate"))
        assertEquals(6, protection("my_leggings"))
        assertEquals(3, protection("my_boots"))

        val report = unt.renderReport("themod")
        // Resolved statically — the stale "verify against the source" hedge
        // must NOT appear for these items.
        assertFalse(report.contains("iron-armor defaults")) { report }
        // Honesty: functional wearable, but no worn-body visuals on Bedrock.
        assertTrue(report.contains("worn-armor visuals are absent on Bedrock")) { report }
        assertTrue(report.contains("the armor geometry/textures are emitted but no attachable consumes them")) { report }
        for (id in listOf("my_helmet", "my_chestplate", "my_leggings", "my_boots")) {
            assertTrue(report.contains("`$id`")) { report }
        }
    }

    @Test
    fun `armor protection falls back to iron defaults when the material cannot be resolved`(@TempDir outDir: Path) {
        // The material class lives outside the mod's sources (or has no
        // readable DEFENSE map) — keep the old defaults and the old honest
        // UNTRANSLATABLE entry for exactly this case.
        val registrationSrc = """
            package com.example.themod;
            class Registration {
                public static final Object HELMET = ITEMS.registerItem("my_helmet",
                    Item::new, p -> p.humanoidArmor(VanillaMaterials.IRON, ArmorType.HELMET));
            }
        """.trimIndent()
        val mod = ModDiscovery.DiscoveredMod(modId = "themod", rootDir = outDir.resolve("themodroot"))
        java.nio.file.Files.createDirectories(mod.rootDir)
        val parser = com.github.javaparser.JavaParser()
        val units = listOf(registrationSrc).map { parser.parse(it).result.orElseThrow() }
        val sources = JavaSourceLoader.ResolvedModSources(
            mod = mod,
            units = units,
            typeSolver = com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver(),
        )

        val unt = Untranslatable()
        ItemAnalyzer(target, unt).analyze(mod, sources, outDir)

        val helmet = itemComponents(read(outDir, "themod/behavior_pack/items/my_helmet.json"))
        assertEquals(2, helmet["minecraft:wearable"]!!.jsonObject["protection"]!!.jsonPrimitive.intOrNull)

        val report = unt.renderReport("themod")
        assertTrue(report.contains("iron-armor defaults")) { report }
        // The worn-visuals honesty line applies to every wearable item,
        // resolved or not.
        assertTrue(report.contains("worn-armor visuals are absent on Bedrock")) { report }
        assertTrue(report.contains("the armor geometry/textures are emitted but no attachable consumes them")) { report }
    }

    @Test
    fun `item referencing ScoundrelLuck in use() records the set-bonus honesty entry`(@TempDir outDir: Path) {
        // Mirrors starwars' BlasterPistolItem: a full Han Solo set doubles the
        // first blaster shot against each new target via ScoundrelLuck, a
        // server-side Java mechanic with no Bedrock equivalent. This must be
        // recorded so it surfaces in UNTRANSLATABLE.md, not silently dropped.
        val registrationSrc = """
            package com.example.themod;
            class Registration {
                public static final Object BLASTER = ITEMS.registerItem(
                    "blaster", BlasterItem::new, p -> p);
            }
        """.trimIndent()
        val blasterItemSrc = """
            package com.example.themod;
            public class BlasterItem extends Item {
                public BlasterItem(Properties properties) {
                    super(properties);
                }
                @Override
                public InteractionResult use(Level level, Player player, InteractionHand hand) {
                    if (ScoundrelLuck.isWearingFullHanSoloSet(player)) {
                        QuickdrawState state = ScoundrelLuck.stateFor(player.getUUID());
                    }
                    return InteractionResult.CONSUME;
                }
            }
        """.trimIndent()
        val mod = ModDiscovery.DiscoveredMod(modId = "themod", rootDir = outDir.resolve("themodroot"))
        java.nio.file.Files.createDirectories(mod.rootDir)
        val parser = com.github.javaparser.JavaParser()
        val units = listOf(registrationSrc, blasterItemSrc).map { parser.parse(it).result.orElseThrow() }
        val sources = JavaSourceLoader.ResolvedModSources(
            mod = mod,
            units = units,
            typeSolver = com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver(),
        )

        val unt = Untranslatable()
        ItemAnalyzer(target, unt).analyze(mod, sources, outDir)

        val report = unt.renderReport("themod")
        assertTrue(report.contains("`blaster`")) { report }
        assertTrue(
            report.contains(
                "Scoundrel's Luck set bonus (full Han Solo set doubles the first blaster " +
                    "shot against each new target) is server-side Java logic — absent on Bedrock.",
            ),
        ) { report }
    }

    @Test
    fun `item without ScoundrelLuck reference does not record the set-bonus entry`(@TempDir outDir: Path) {
        val registrationSrc = """
            package com.example.themod;
            class Registration {
                public static final Object BLASTER = ITEMS.registerItem(
                    "blaster", BlasterItem::new, p -> p);
            }
        """.trimIndent()
        val blasterItemSrc = """
            package com.example.themod;
            public class BlasterItem extends Item {
                public BlasterItem(Properties properties) {
                    super(properties);
                }
                @Override
                public InteractionResult use(Level level, Player player, InteractionHand hand) {
                    return InteractionResult.CONSUME;
                }
            }
        """.trimIndent()
        val mod = ModDiscovery.DiscoveredMod(modId = "themod", rootDir = outDir.resolve("themodroot"))
        java.nio.file.Files.createDirectories(mod.rootDir)
        val parser = com.github.javaparser.JavaParser()
        val units = listOf(registrationSrc, blasterItemSrc).map { parser.parse(it).result.orElseThrow() }
        val sources = JavaSourceLoader.ResolvedModSources(
            mod = mod,
            units = units,
            typeSolver = com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver(),
        )

        val unt = Untranslatable()
        ItemAnalyzer(target, unt).analyze(mod, sources, outDir)

        val report = unt.renderReport("themod")
        // The plain `use()` override is still recorded (existing custom-
        // behavior detection), just without the set-bonus honesty sentence.
        assertTrue(report.contains("overrides: use")) { report }
        assertFalse(report.contains("Scoundrel's Luck set bonus")) { report }
    }

    @Test
    fun `starwars star_compass records the useOn override and the gate-ignition honesty note`(@TempDir outDir: Path) {
        // StarCompassItem.useOn validates an iron-block gate frame and opens
        // the planet-picker UI — the whole hyperspace-gate chain (portal
        // block, custom planet dimensions) has no Bedrock counterpart, so
        // the compass must not silently become an inert trinket.
        val resolver = ClasspathResolver.fromSystemProperties()
        assertTrue(resolver.isAvailable())
        val all = ModDiscovery(repoRoot).discover()
        val mod = all.first { it.modId == "starwars" }
        val unt = Untranslatable()
        val sources = JavaSourceLoader(resolver, unt).load(mod, all)
        ItemAnalyzer(target, unt).analyze(mod, sources, outDir)

        // The static item JSON is still emitted; stacksTo(1) lives in the
        // item class's constructor, not the registration lambda.
        val compass = itemComponents(read(outDir, "starwars/behavior_pack/items/star_compass.json"))
        assertEquals(1, compass["minecraft:max_stack_size"]!!.jsonPrimitive.intOrNull)

        val report = unt.renderReport("starwars")
        assertTrue(report.contains("`star_compass`: StarCompassItem overrides: useOn")) { report }
        assertTrue(
            report.contains(
                "Gate ignition (iron-block frame validation + planet-picker UI that fills the " +
                    "frame with hyperspace_portal film) is server-side Java logic — absent on " +
                    "Bedrock; the hyperspace_portal block and the planet dimensions it leads to " +
                    "are not translated.",
            ),
        ) { report }
    }

    @Test
    fun `gate-ignition note fires only for items referencing GateShape`(@TempDir outDir: Path) {
        val registrationSrc = """
            package com.example.themod;
            class Registration {
                public static final Object COMPASS = ITEMS.registerItem(
                    "gate_compass", GateCompassItem::new, p -> p);
                public static final Object WRENCH = ITEMS.registerItem(
                    "wrench", WrenchItem::new, p -> p);
            }
        """.trimIndent()
        val compassItemSrc = """
            package com.example.themod;
            public class GateCompassItem extends Item {
                public GateCompassItem(Properties properties) {
                    super(properties);
                }
                @Override
                public InteractionResult useOn(UseOnContext context) {
                    Optional<GateShape.Result> shape = GateShape.find(context.getClickedPos());
                    return InteractionResult.CONSUME;
                }
            }
        """.trimIndent()
        val wrenchItemSrc = """
            package com.example.themod;
            public class WrenchItem extends Item {
                public WrenchItem(Properties properties) {
                    super(properties);
                }
                @Override
                public InteractionResult useOn(UseOnContext context) {
                    return InteractionResult.CONSUME;
                }
            }
        """.trimIndent()
        val mod = ModDiscovery.DiscoveredMod(modId = "themod", rootDir = outDir.resolve("themodroot"))
        java.nio.file.Files.createDirectories(mod.rootDir)
        val parser = com.github.javaparser.JavaParser()
        val units = listOf(registrationSrc, compassItemSrc, wrenchItemSrc).map { parser.parse(it).result.orElseThrow() }
        val sources = JavaSourceLoader.ResolvedModSources(
            mod = mod,
            units = units,
            typeSolver = com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver(),
        )

        val unt = Untranslatable()
        ItemAnalyzer(target, unt).analyze(mod, sources, outDir)

        val report = unt.renderReport("themod")
        assertTrue(report.contains("`gate_compass`: GateCompassItem overrides: useOn. Gate ignition")) { report }
        // The plain useOn override is still recorded for the wrench, but
        // without the gate-ignition sentence.
        assertTrue(report.contains("`wrench`: WrenchItem overrides: useOn")) { report }
        assertFalse(report.contains("`wrench`: WrenchItem overrides: useOn. Gate ignition")) { report }
    }

    private fun read(outDir: Path, rel: String): JsonObject {
        val path = outDir.resolve(rel)
        assertTrue(path.toFile().exists()) { "expected $path to exist" }
        return Json.parseToJsonElement(path.readText()) as JsonObject
    }

    private fun itemComponents(json: JsonObject): JsonObject =
        json["minecraft:item"]!!.jsonObject["components"]!!.jsonObject
}
