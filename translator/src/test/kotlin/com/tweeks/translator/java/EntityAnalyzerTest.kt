package com.tweeks.translator.java

import com.tweeks.translator.discover.ModDiscovery
import com.tweeks.translator.emit.Untranslatable
import com.tweeks.translator.manifest.BedrockTarget
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * End-to-end test for [EntityAnalyzer] over the real `securityguard` mod.
 *
 * Verifies that:
 *   - the entity JSON is emitted with the expected identifier;
 *   - vanilla `MAX_HEALTH` / `ATTACK_DAMAGE` etc. attributes round-trip;
 *   - High-bucket goals appear as `minecraft:behavior.*` components with
 *     priority pulled from the source `addGoal(priority, ...)` call site;
 *   - custom goals (`StunningMeleeGoal`, `GuardTargetHostilesGoal`) do
 *     **not** appear as components but are logged on Untranslatable.
 *   - the resource_pack `<entity_id>.entity.json` carries the expected
 *     securityguard-specific `security_guard` geometry/texture mapping.
 */
class EntityAnalyzerTest {

    private val repoRoot: Path = ModDiscovery.findRepoRoot(Path.of("").toAbsolutePath())
    private val target = BedrockTarget.load(repoRoot.resolve("translator/bedrock-target.json"))

    @Test
    fun `securityguard guard entity emits expected components`(@TempDir outDir: Path) {
        val resolver = ClasspathResolver.fromSystemProperties()
        assertTrue(resolver.isAvailable())
        val all = ModDiscovery(repoRoot).discover()
        val securityguard = all.first { it.modId == "securityguard" }
        val unt = Untranslatable()
        val sources = JavaSourceLoader(resolver, unt).load(securityguard, all)

        EntityAnalyzer(target, unt).analyze(securityguard, sources, outDir)

        val behaviorPath = outDir.resolve("securityguard/behavior_pack/entities/guard.json")
        assertTrue(behaviorPath.toFile().exists(), "entity behavior pack JSON must be written")

        val parsed = Json.parseToJsonElement(behaviorPath.readText()) as JsonObject
        val entity = parsed["minecraft:entity"]!!.jsonObject
        val desc = entity["description"]!!.jsonObject
        assertEquals("securityguard:guard", desc["identifier"]!!.jsonPrimitive.content)
        assertEquals(true, desc["is_spawnable"]!!.jsonPrimitive.content.toBoolean())

        val components = entity["components"]!!.jsonObject

        // Attributes pulled from createAttributes().
        assertEquals(50, components["minecraft:health"]!!.jsonObject["value"]!!.jsonPrimitive.intOrNull)
        assertEquals(5, components["minecraft:attack"]!!.jsonObject["damage"]!!.jsonPrimitive.intOrNull)

        // High-bucket vanilla goals should be present, with priority injected.
        val moveTowards = components["minecraft:behavior.move_towards_target"]!!.jsonObject
        assertEquals(2, moveTowards["priority"]!!.jsonPrimitive.intOrNull)

        val randomStroll = components["minecraft:behavior.random_stroll"]!!.jsonObject
        assertEquals(7, randomStroll["priority"]!!.jsonPrimitive.intOrNull)

        val defendVillage = components["minecraft:behavior.defend_village_target"]!!.jsonObject
        assertEquals(1, defendVillage["priority"]!!.jsonPrimitive.intOrNull)

        // Custom goals must NOT appear as components.
        assertNull(components["minecraft:behavior.stunning_melee"])
        assertNull(components["minecraft:behavior.guard_target_hostiles"])

        // The collision_box should be a clean 0.6 × 1.95 (no float-noise tail).
        val box = components["minecraft:collision_box"]!!.jsonObject
        assertEquals(0.6, box["width"]!!.jsonPrimitive.content.toDouble())
        assertEquals(1.95, box["height"]!!.jsonPrimitive.content.toDouble())

        // The resource_pack client_entity wires geometry to security_guard.
        val clientPath = outDir.resolve("securityguard/resource_pack/entity/guard.entity.json")
        assertTrue(clientPath.toFile().exists())
        val clientJson = Json.parseToJsonElement(clientPath.readText()) as JsonObject
        val clientDesc = clientJson["minecraft:client_entity"]!!.jsonObject["description"]!!.jsonObject
        val geometry = clientDesc["geometry"]!!.jsonObject["default"]!!.jsonPrimitive.content
        assertEquals("geometry.securityguard.security_guard", geometry)

        val tex = clientDesc["textures"]!!.jsonObject["default"]!!.jsonPrimitive.content
        assertEquals("textures/entity/security_guard.png", tex)

        // Untranslatable should record the deferred custom goals. With no
        // Phase 3 gate wired in this test, the goals land in the
        // "deferred (Phase 3 LLM stage not run)" section.
        val report = unt.renderReport("securityguard")
        assertTrue(report.contains("Entity goals deferred")) { report }
        assertTrue(report.contains("StunningMeleeGoal"))
        assertTrue(report.contains("GuardTargetHostilesGoal"))
    }

    @Test
    fun `thief entity emits vanilla goals only and defers custom ones`(@TempDir outDir: Path) {
        val resolver = ClasspathResolver.fromSystemProperties()
        assertTrue(resolver.isAvailable())
        val all = ModDiscovery(repoRoot).discover()
        val thief = all.first { it.modId == "thief" }
        val unt = Untranslatable()
        val sources = JavaSourceLoader(resolver, unt).load(thief, all)

        EntityAnalyzer(target, unt).analyze(thief, sources, outDir)

        val behaviorPath = outDir.resolve("thief/behavior_pack/entities/thief.json")
        assertTrue(behaviorPath.toFile().exists())

        val parsed = Json.parseToJsonElement(behaviorPath.readText()) as JsonObject
        val components = parsed["minecraft:entity"]!!.jsonObject["components"]!!.jsonObject

        // Vanilla High-bucket goals.
        assertNotNull(components["minecraft:behavior.float"])
        assertNotNull(components["minecraft:behavior.random_stroll"])
        assertNotNull(components["minecraft:behavior.look_at_player"])
        assertNotNull(components["minecraft:behavior.hurt_by_target"])

        // Thief mob_category is MONSTER → family contains "monster".
        val family = components["minecraft:type_family"]!!.jsonObject["family"]!!
        assertTrue(family.toString().contains("monster"))

        val report = unt.renderReport("thief")
        assertTrue(report.contains("BlackjackStrikeGoal") || report.contains("FleeAndFireCrossbowGoal"))
    }
}
