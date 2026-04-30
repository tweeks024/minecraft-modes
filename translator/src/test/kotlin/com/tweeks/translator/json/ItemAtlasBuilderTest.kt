package com.tweeks.translator.json

import com.tweeks.translator.discover.ModDiscovery
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText

class ItemAtlasBuilderTest {

    private val repoRoot: Path = ModDiscovery.findRepoRoot(Path.of("").toAbsolutePath())

    @Test
    fun `securityguard item atlas matches golden`(@TempDir tempDir: Path) {
        val builder = ItemAtlasBuilder()
        // Pass deliberately out-of-order to verify the builder sorts.
        val itemNames = listOf("guard_spawn_egg", "baton", "guard_helmet")
        builder.build("securityguard", itemNames, tempDir)

        val actual = tempDir
            .resolve("securityguard/resource_pack/textures/item_texture.json")
            .readText()
        assertJsonEquals(readGolden("atlas/securityguard_item_texture.json"), actual)
    }

    @Test
    fun `empty item list emits no atlas file`(@TempDir tempDir: Path) {
        val builder = ItemAtlasBuilder()
        builder.build("emptymod", emptyList(), tempDir)
        val out = tempDir.resolve("emptymod/resource_pack/textures/item_texture.json")
        assert(!out.toFile().exists()) {
            "Empty atlas must not produce a file (Bedrock is happy without one)."
        }
    }

    private fun readGolden(name: String): String =
        repoRoot.resolve("translator/src/test/resources/goldens/$name").readText()

    private fun assertJsonEquals(expected: String, actual: String) {
        val e = Json.parseToJsonElement(expected)
        val a = Json.parseToJsonElement(actual)
        assertEquals(e, a)
    }
}
