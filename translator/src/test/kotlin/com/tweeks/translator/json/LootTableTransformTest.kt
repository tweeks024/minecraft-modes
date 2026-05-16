package com.tweeks.translator.json

import com.tweeks.translator.discover.ModDiscovery
import com.tweeks.translator.emit.Untranslatable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

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
    fun `function object keys are emitted alphabetized regardless of source order`() {
        // Deliberately shuffled: `count` (c), `add` (a), `function` (f) — i.e.
        // not source-order, not alphabetical. We expect alphabetical output.
        val javaJson = """
            {
              "pools": [
                {
                  "rolls": 1,
                  "entries": [
                    {
                      "type": "minecraft:item",
                      "name": "minecraft:stone",
                      "functions": [
                        {
                          "count": { "min": 1, "max": 3 },
                          "add": false,
                          "function": "minecraft:set_count"
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        val unt = Untranslatable()
        val transform = LootTableTransform(unt)
        val out = transform.translateJson(javaJson, "testmod", "entities/x.json")

        // Parse the emitted JSON, dive to the function object, assert key order.
        val root = Json.parseToJsonElement(out).jsonObject
        val function = root["pools"]!!.jsonArray[0].jsonObject["entries"]!!
            .jsonArray[0].jsonObject["functions"]!!.jsonArray[0].jsonObject

        // JsonObject is backed by a LinkedHashMap, so .keys preserves insertion order.
        val keysInOrder = function.keys.toList()
        assertEquals(listOf("add", "count", "function"), keysInOrder) {
            "Expected function keys alphabetized, got: $keysInOrder"
        }

        // The pretty-printed text should also show keys in that order — assert
        // on the raw string so we catch a future regression that builds the
        // map alphabetized but somehow re-orders during serialize.
        val addIdx = out.indexOf("\"add\"")
        val countIdx = out.indexOf("\"count\"")
        val functionIdx = out.indexOf("\"function\"")
        assert(addIdx in 0 until countIdx && countIdx < functionIdx) {
            "Expected emitted text in order add, count, function. Got:\n$out"
        }
    }

    @Test
    fun `nested condition object keys are alphabetized`() {
        // stripMinecraftNamespaces walks recursively; verify it sorts there too.
        val javaJson = """
            {
              "pools": [
                {
                  "rolls": 1,
                  "entries": [
                    {
                      "type": "minecraft:item",
                      "name": "minecraft:stone",
                      "conditions": [
                        {
                          "predicate": { "z": 1, "a": 2, "m": 3 },
                          "condition": "minecraft:entity_properties",
                          "entity": "this"
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        val unt = Untranslatable()
        val transform = LootTableTransform(unt)
        val out = transform.translateJson(javaJson, "testmod", "entities/x.json")

        val root = Json.parseToJsonElement(out).jsonObject
        val condition = root["pools"]!!.jsonArray[0].jsonObject["entries"]!!
            .jsonArray[0].jsonObject["conditions"]!!.jsonArray[0].jsonObject
        assertEquals(listOf("condition", "entity", "predicate"), condition.keys.toList())

        val predicate = (condition["predicate"] as JsonObject)
        assertEquals(listOf("a", "m", "z"), predicate.keys.toList())
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

    @Test
    fun `falls back to src main resources when generated serverData absent`(@TempDir tmp: Path) {
        val modRoot = tmp.resolve("themod")
        val lootDir = modRoot.resolve("src/main/resources/data/themod/loot_table/entities")
        lootDir.createDirectories()
        lootDir.resolve("foo.json").writeText(
            """{"pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"minecraft:stone"}]}]}""",
        )
        val out = tmp.resolve("out")
        LootTableTransform(Untranslatable()).translate(modRoot, "themod", out)
        val written = out.resolve("themod/behavior_pack/loot_tables/entities/foo.json")
        assertTrue(written.isRegularFile(), "Expected loot table written at $written")
    }

    @Test
    fun `merges generated and src main resources when both exist`(@TempDir tmp: Path) {
        // Wildwest reality: some loot tables in generated/, others in main/resources/.
        val modRoot = tmp.resolve("themod")
        val genDir = modRoot.resolve("src/generated/serverData/data/themod/loot_table/entities")
        val mainDir = modRoot.resolve("src/main/resources/data/themod/loot_table/chests")
        genDir.createDirectories()
        mainDir.createDirectories()
        genDir.resolve("walker.json").writeText("""{"pools":[]}""")
        mainDir.resolve("treasure.json").writeText("""{"pools":[]}""")
        val out = tmp.resolve("out")
        LootTableTransform(Untranslatable()).translate(modRoot, "themod", out)
        assertTrue(out.resolve("themod/behavior_pack/loot_tables/entities/walker.json").isRegularFile())
        assertTrue(out.resolve("themod/behavior_pack/loot_tables/chests/treasure.json").isRegularFile())
    }

    private fun readGolden(name: String): String =
        repoRoot.resolve("translator/src/test/resources/goldens/$name").readText()

    private fun assertJsonEquals(expected: String, actual: String) {
        val e = Json.parseToJsonElement(expected)
        val a = Json.parseToJsonElement(actual)
        assertEquals(e, a)
    }
}
