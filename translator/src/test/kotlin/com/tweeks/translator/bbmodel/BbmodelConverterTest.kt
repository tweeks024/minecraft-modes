package com.tweeks.translator.bbmodel

import com.tweeks.translator.discover.ModDiscovery
import com.tweeks.translator.emit.Untranslatable
import com.tweeks.translator.manifest.BedrockTarget
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Golden-file tests for [BbmodelConverter].
 *
 * Each of the six real `.bbmodel` files in the repo is converted and
 * compared against a checked-in golden under
 * `translator/src/test/resources/goldens/geometry/`. Comparison is
 * structural (parse-and-compare) so tests aren't fragile against
 * pretty-printer settings — byte-stability of the actual output is
 * verified at runtime by the CLI's idempotency.
 */
class BbmodelConverterTest {

    private val repoRoot: Path = ModDiscovery.findRepoRoot(Path.of("").toAbsolutePath())
    private val target = BedrockTarget.load(repoRoot.resolve("translator/bedrock-target.json"))

    @Test
    fun `securityguard security_guard geometry matches golden`() {
        assertGeometryGolden(
            modId = "securityguard",
            bbmodel = "securityguard/tools/security_guard.bbmodel",
            modelName = "security_guard",
            golden = "geometry/securityguard_security_guard.geo.json",
        )
    }

    @Test
    fun `securityguard baton geometry matches golden`() {
        assertGeometryGolden(
            modId = "securityguard",
            bbmodel = "securityguard/tools/baton.bbmodel",
            modelName = "baton",
            golden = "geometry/securityguard_baton.geo.json",
        )
    }

    @Test
    fun `creeperskin armor pieces geometries match goldens`() {
        listOf("helmet", "chestplate", "leggings", "boots").forEach { piece ->
            val name = "creeper_armor_$piece"
            assertGeometryGolden(
                modId = "creeperskin",
                bbmodel = "creeperskin/tools/$name.bbmodel",
                modelName = name,
                golden = "geometry/creeperskin_$name.geo.json",
            )
        }
    }

    @Test
    fun `security_guard bone tree is humanoid`() {
        // Sanity-check that the bbmodel parsed into the bones we expect.
        val (bb, geoText) = renderGeometry("securityguard", "securityguard/tools/security_guard.bbmodel", "security_guard")
        val parsed = Json.parseToJsonElement(geoText)
        val geo = parsed
            .let { it as kotlinx.serialization.json.JsonObject }["minecraft:geometry"]
            .let { it as kotlinx.serialization.json.JsonArray }[0]
            .let { it as kotlinx.serialization.json.JsonObject }
        val bones = geo["bones"] as kotlinx.serialization.json.JsonArray
        val names = bones.map { (it as kotlinx.serialization.json.JsonObject)["name"]!!.let { n -> (n as kotlinx.serialization.json.JsonPrimitive).content } }
        assertEquals(listOf("head", "body", "right_arm", "left_arm", "right_leg", "left_leg"), names)
        // No synthetic root bone — security_guard has explicit groups for every cube.
        assertFalse(names.contains("root")) { "Did not expect synthetic root bone in security_guard" }
        // bb sanity
        assertEquals(9, bb.elements.size)
    }

    @Test
    fun `creeper_armor_helmet uses synthetic root bone for orphan cubes`() {
        // The helmet bbmodel's outliner is a flat list of UUIDs (no groups),
        // so the converter must emit a synthetic `root` bone holding the
        // cubes.
        val (_, geoText) = renderGeometry(
            "creeperskin",
            "creeperskin/tools/creeper_armor_helmet.bbmodel",
            "creeper_armor_helmet",
        )
        assertTrue(geoText.contains("\"name\": \"root\"")) {
            "Expected synthetic root bone in:\n$geoText"
        }
    }

    @Test
    fun `convert writes geo file but no animation file when bbmodel has no animations`(@TempDir out: Path) {
        val unt = Untranslatable()
        val converter = BbmodelConverter(target, unt)
        converter.convertOne(
            input = repoRoot.resolve("securityguard/tools/security_guard.bbmodel"),
            modId = "securityguard",
            outputRoot = out,
        )
        assertTrue(
            out.resolve("securityguard/resource_pack/models/entity/security_guard.geo.json").exists(),
            "expected geometry file",
        )
        assertFalse(
            out.resolve("securityguard/resource_pack/animations/security_guard.animation.json").exists(),
            "expected NO animation file (bbmodel has no animations)",
        )
    }

    @Test
    fun `convert is a no-op for missing tools dir`(@TempDir out: Path) {
        val unt = Untranslatable()
        val converter = BbmodelConverter(target, unt)
        converter.convert("nosuch", out.resolve("does_not_exist"), out)
        // Should not have created anything.
        assertFalse(out.resolve("nosuch").exists())
    }

    // ---- helpers ----

    private fun renderGeometry(modId: String, bbmodelPath: String, modelName: String): Pair<Bbmodel, String> {
        val unt = Untranslatable()
        val converter = BbmodelConverter(target, unt)
        val raw = repoRoot.resolve(bbmodelPath).readText()
        val bb = BbmodelConverter.JSON.decodeFromString(Bbmodel.serializer(), raw)
        return bb to converter.buildGeometryJson(bb, modId, modelName)
    }

    private fun assertGeometryGolden(modId: String, bbmodel: String, modelName: String, golden: String) {
        val (_, actual) = renderGeometry(modId, bbmodel, modelName)
        val expected = repoRoot.resolve("translator/src/test/resources/goldens/$golden").readText()
        assertJsonEquals(expected, actual)
    }

    private fun assertJsonEquals(expected: String, actual: String) {
        val e = Json.parseToJsonElement(expected)
        val a = Json.parseToJsonElement(actual)
        assertEquals(e, a)
    }
}
