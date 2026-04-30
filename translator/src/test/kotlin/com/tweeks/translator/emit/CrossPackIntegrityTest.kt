package com.tweeks.translator.emit

import com.tweeks.translator.discover.ModDiscovery
import com.tweeks.translator.manifest.BedrockTarget
import com.tweeks.translator.manifest.ManifestWriter
import com.tweeks.translator.manifest.UuidGen
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

/**
 * Phase 4.2 / 4.3: integrity checks over the committed `bedrock-out/` tree.
 *
 * These tests treat the on-disk output as ground truth and verify:
 *   - Every BP manifest's `dependencies[]` UUIDs resolve to either its sibling
 *     RP header or another mod's BP header — no dangling references.
 *   - Every emitted `<entity>.entity.json` references a real geometry and a
 *     real texture file under the same mod's resource pack (or a vanilla
 *     `geometry.*` fallback).
 *   - All mods declare the same `min_engine_version` so they're install-
 *     compatible on a single Realm.
 *
 * The tests rely on `bedrock-out/` being present and committed. If a
 * developer hand-edits the tree or commits drift, these tests catch it the
 * same way the Phase 5 CI drift gate would.
 */
class CrossPackIntegrityTest {

    private val repoRoot: Path = ModDiscovery.findRepoRoot(Path.of("").toAbsolutePath())
    private val bedrockOut: Path = repoRoot.resolve("bedrock-out")
    private val target = BedrockTarget.load(repoRoot.resolve("translator/bedrock-target.json"))

    /** Mods we expect to find committed. Any missing → fail loud. */
    private val expectedMods = listOf("creeperskin", "securitycore", "securityguard", "thief")

    @Test
    fun `bedrock-out has all four mods`() {
        for (modId in expectedMods) {
            assertTrue(bedrockOut.resolve(modId).isDirectory()) {
                "Expected committed bedrock-out/$modId/ — run :translator:translate"
            }
        }
    }

    @Test
    fun `every BP dependency UUID resolves to a known pack header`() {
        // Build the universe of known header UUIDs across every mod.
        val knownHeaders = mutableMapOf<String, String>() // uuid → "modId BP"/"modId RP"
        for (modId in expectedMods) {
            knownHeaders[ManifestWriter.behaviorPackHeaderUuid(modId).toString()] = "$modId BP"
            knownHeaders[ManifestWriter.resourcePackHeaderUuid(modId).toString()] = "$modId RP"
        }
        // Sanity: securitycore's BP header is what other mods depend on.
        assertEquals(
            ManifestWriter.behaviorPackHeaderUuid("securitycore").toString(),
            UuidGen.coreDependencyUuid().toString(),
        ) { "spec invariant: coreDependencyUuid must equal securitycore's BP header" }

        for (modId in expectedMods) {
            val bpManifest = bedrockOut.resolve("$modId/behavior_pack/manifest.json")
            assertTrue(bpManifest.isRegularFile()) { "missing $bpManifest" }
            val deps = parseDependencyUuids(bpManifest.readText())
            for (uuid in deps) {
                assertTrue(uuid in knownHeaders) {
                    "Dangling BP dependency UUID $uuid in $bpManifest — known UUIDs: ${knownHeaders.keys}"
                }
            }
        }
    }

    @Test
    fun `securityguard and thief BP depend on securitycore`() {
        for (modId in listOf("securityguard", "thief")) {
            val bp = bedrockOut.resolve("$modId/behavior_pack/manifest.json")
            val deps = parseDependencyUuids(bp.readText())
            val coreUuid = UuidGen.coreDependencyUuid().toString()
            assertTrue(deps.contains(coreUuid)) {
                "$modId BP must depend on securitycore (uuid $coreUuid). Found deps: $deps"
            }
        }
    }

    @Test
    fun `creeperskin BP does not depend on securitycore`() {
        val bp = bedrockOut.resolve("creeperskin/behavior_pack/manifest.json")
        val deps = parseDependencyUuids(bp.readText())
        val coreUuid = UuidGen.coreDependencyUuid().toString()
        assertEquals(false, deps.contains(coreUuid)) {
            "creeperskin must NOT depend on securitycore. Found deps: $deps"
        }
        // Single dep: BP→RP only.
        assertEquals(1, deps.size, "creeperskin BP should declare only its sibling RP")
    }

    @Test
    fun `securitycore does not self-depend`() {
        val bp = bedrockOut.resolve("securitycore/behavior_pack/manifest.json")
        val deps = parseDependencyUuids(bp.readText())
        val selfHeader = ManifestWriter.behaviorPackHeaderUuid("securitycore").toString()
        assertTrue(deps.none { it == selfHeader }) {
            "securitycore must not depend on its own BP header. Deps: $deps"
        }
    }

    @Test
    fun `every pack uses the same min_engine_version`() {
        val expected = target.min_engine_version
        for (modId in expectedMods) {
            for (kind in listOf("behavior_pack", "resource_pack")) {
                val manifest = bedrockOut.resolve("$modId/$kind/manifest.json")
                if (!manifest.isRegularFile()) continue
                val parsed = Json.parseToJsonElement(manifest.readText()).jsonObject
                val mev = parsed["header"]!!.jsonObject["min_engine_version"]!!.jsonArray
                    .map { it.jsonPrimitive.content.toInt() }
                assertEquals(expected, mev) {
                    "$modId/$kind/manifest.json min_engine_version drift: expected $expected, got $mev"
                }
            }
        }
    }

    @Test
    fun `every entity_json references a real geometry and texture`() {
        for (modId in expectedMods) {
            val entityDir = bedrockOut.resolve("$modId/resource_pack/entity")
            if (!entityDir.isDirectory()) continue
            val files = entityDir.toFile().listFiles { f -> f.name.endsWith(".entity.json") }
                ?: continue
            for (file in files) {
                val parsed = Json.parseToJsonElement(file.readText()).jsonObject
                val desc = parsed["minecraft:client_entity"]!!.jsonObject["description"]!!.jsonObject

                // Geometry: must be either a vanilla `geometry.*` (Bedrock
                // built-in) or `geometry.<modid>.<name>` matching a real
                // `<name>.geo.json` under this mod's resource pack.
                val geomRef = desc["geometry"]?.jsonObject?.get("default")?.jsonPrimitive?.content
                assertNotNull(geomRef) { "${file.name} has no geometry.default" }
                geomRef!!
                if (geomRef.startsWith("geometry.$modId.")) {
                    val baseName = geomRef.removePrefix("geometry.$modId.")
                    val geoFile = bedrockOut.resolve("$modId/resource_pack/models/entity/$baseName.geo.json")
                    assertTrue(geoFile.exists()) {
                        "${file.name} references $geomRef but $geoFile is missing"
                    }
                } else {
                    // Vanilla fallback (e.g. `geometry.humanoid`). Bedrock
                    // ships these as built-ins; we don't need a local file.
                    assertTrue(geomRef.startsWith("geometry.")) {
                        "${file.name} geometry must start with `geometry.`: $geomRef"
                    }
                }

                // Texture: every entry under `textures` (typically just
                // `default`) must resolve to a real PNG under this mod's
                // resource_pack/textures/. Bedrock convention is the texture
                // path WITHOUT a file extension, but the on-disk file is
                // `<path>.png`. Strip an explicit `.png` if the analyzer
                // happened to include one (it currently does — we keep that
                // for now), then add `.png` to find the real file.
                val textures = desc["textures"]?.jsonObject ?: continue
                for ((slot, ref) in textures) {
                    val texPath = ref.jsonPrimitive.content.removeSuffix(".png")
                    val texFile = bedrockOut.resolve("$modId/resource_pack/$texPath.png")
                    assertTrue(texFile.exists()) {
                        "${file.name}#textures.$slot references `$texPath` but $texFile is missing"
                    }
                }
            }
        }
    }

    private fun parseDependencyUuids(json: String): List<String> {
        val parsed = Json.parseToJsonElement(json) as JsonObject
        val deps = parsed["dependencies"]?.jsonArray ?: return emptyList()
        return deps.map { it.jsonObject["uuid"]!!.jsonPrimitive.content }
    }
}
