package com.tweeks.translator.java

import com.tweeks.translator.discover.ModDiscovery
import com.tweeks.translator.emit.Untranslatable
import com.tweeks.translator.manifest.BedrockTarget
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
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

    private fun read(outDir: Path, rel: String): JsonObject {
        val path = outDir.resolve(rel)
        assertTrue(path.toFile().exists()) { "expected $path to exist" }
        return Json.parseToJsonElement(path.readText()) as JsonObject
    }

    private fun itemComponents(json: JsonObject): JsonObject =
        json["minecraft:item"]!!.jsonObject["components"]!!.jsonObject
}
