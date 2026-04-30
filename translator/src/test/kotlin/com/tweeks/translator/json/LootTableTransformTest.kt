package com.tweeks.translator.json

import com.tweeks.translator.discover.ModDiscovery
import com.tweeks.translator.emit.Untranslatable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.readText

class LootTableTransformTest {

    private val repoRoot: Path = ModDiscovery.findRepoRoot(Path.of("").toAbsolutePath())

    @Test
    fun `securityguard empty loot table emits empty pools array`() {
        val unt = Untranslatable()
        val transform = LootTableTransform(unt)
        val javaJson = repoRoot
            .resolve("securityguard/src/generated/serverData/data/securityguard/loot_table/entities/guard.json")
            .readText()
        val actual = transform.translateJson(javaJson, "securityguard", "entities/guard.json")
        assertJsonEquals(readGolden("loot_table/securityguard_guard.json"), actual)
    }

    @Test
    fun `thief loot table strips minecraft prefixes from type and function names`() {
        val unt = Untranslatable()
        val transform = LootTableTransform(unt)
        val javaJson = repoRoot
            .resolve("thief/src/generated/serverData/data/thief/loot_table/entities/thief.json")
            .readText()
        val actual = transform.translateJson(javaJson, "thief", "entities/thief.json")
        assertJsonEquals(readGolden("loot_table/thief_thief.json"), actual)
    }

    @Test
    fun `random_sequence is recorded in untranslatable report`() {
        val unt = Untranslatable()
        val transform = LootTableTransform(unt)
        val javaJson = repoRoot
            .resolve("securityguard/src/generated/serverData/data/securityguard/loot_table/entities/guard.json")
            .readText()
        transform.translateJson(javaJson, "securityguard", "entities/guard.json")
        val report = unt.renderReport("securityguard")
        assert(report.contains("random_sequence")) { report }
    }

    private fun readGolden(name: String): String =
        repoRoot.resolve("translator/src/test/resources/goldens/$name").readText()

    private fun assertJsonEquals(expected: String, actual: String) {
        val e = Json.parseToJsonElement(expected)
        val a = Json.parseToJsonElement(actual)
        assertEquals(e, a)
    }
}
