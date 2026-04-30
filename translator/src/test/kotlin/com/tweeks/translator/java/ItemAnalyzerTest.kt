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

    private fun read(outDir: Path, rel: String): JsonObject {
        val path = outDir.resolve(rel)
        assertTrue(path.toFile().exists()) { "expected $path to exist" }
        return Json.parseToJsonElement(path.readText()) as JsonObject
    }

    private fun itemComponents(json: JsonObject): JsonObject =
        json["minecraft:item"]!!.jsonObject["components"]!!.jsonObject
}
