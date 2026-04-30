package com.tweeks.translator.manifest

import com.tweeks.translator.discover.ModDiscovery
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.readText

class ManifestWriterTest {

    private val repoRoot: Path = ModDiscovery.findRepoRoot(Path.of("").toAbsolutePath())
    private val target = BedrockTarget.load(repoRoot.resolve("translator/bedrock-target.json"))
    private val writer = ManifestWriter(target)

    @Test
    fun `securityguard manifests match golden files`() {
        val out = writer.build(
            ManifestWriter.ModManifestInputs(
                modId = "securityguard",
                displayName = "Security Guard",
                description = "Adds a friendly humanoid security guard mob who defends players and villagers with a stun baton.",
                requiresSecurityCore = true,
            )
        )

        val expectedBp = readGolden("securityguard_behavior.json")
        val expectedRp = readGolden("securityguard_resource.json")

        assertEquals(expectedBp, out.behaviorPackManifest)
        assertEquals(expectedRp, out.resourcePackManifest)
    }

    @Test
    fun `creeperskin behavior pack has no securitycore dependency`() {
        val out = writer.build(
            ManifestWriter.ModManifestInputs(
                modId = "creeperskin",
                displayName = "Creeperskin",
                description = "Wearable creeper-skin armor.",
                requiresSecurityCore = false,
            )
        )
        // Single dependency: BP→RP only.
        val deps = parseDependencyUuids(out.behaviorPackManifest)
        assertEquals(1, deps.size, "creeperskin BP should only declare its sibling RP, not securitycore")
    }

    @Test
    fun `securityguard behavior pack lists securitycore as a dependency`() {
        val out = writer.build(
            ManifestWriter.ModManifestInputs(
                modId = "securityguard",
                displayName = "Security Guard",
                description = "...",
                requiresSecurityCore = true,
            )
        )
        val deps = parseDependencyUuids(out.behaviorPackManifest)
        assertEquals(2, deps.size, "securityguard BP should depend on its sibling RP plus securitycore")
        val coreUuid = UuidGen.coreDependencyUuid().toString()
        assertTrue(deps.contains(coreUuid)) { "Expected core uuid $coreUuid in deps $deps" }
    }

    @Test
    fun `securitycore does not list itself as a dependency even if flag is true`() {
        // Defensive: if someone sets requiresSecurityCore=true on the core mod
        // by mistake, the writer must not produce a self-dependency loop.
        val out = writer.build(
            ManifestWriter.ModManifestInputs(
                modId = "securitycore",
                displayName = "Security Core",
                description = "Core",
                requiresSecurityCore = true,
            )
        )
        val deps = parseDependencyUuids(out.behaviorPackManifest)
        val selfHeader = ManifestWriter.behaviorPackHeaderUuid("securitycore").toString()
        assertTrue(deps.none { it == selfHeader }) { "securitycore must not depend on its own header" }
    }

    /**
     * Extract dependency UUIDs by structurally parsing the JSON. Avoids regex
     * pitfalls with nested arrays (the version[3] inside each dep entry).
     */
    private fun parseDependencyUuids(json: String): List<String> {
        val parsed = kotlinx.serialization.json.Json.parseToJsonElement(json)
        val deps = parsed.jsonObject["dependencies"]?.jsonArray ?: return emptyList()
        return deps.map { it.jsonObject["uuid"]!!.jsonPrimitive.content }
    }

    private fun readGolden(name: String): String {
        val path = repoRoot.resolve("translator/src/test/resources/goldens/manifest/$name")
        return path.readText()
    }
}
