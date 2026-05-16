package com.tweeks.translator.json

import com.tweeks.translator.discover.ModDiscovery
import com.tweeks.translator.emit.Untranslatable
import com.tweeks.translator.manifest.BedrockTarget
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Golden-file tests for [RecipeTransform].
 *
 * Comparison is structural (parse-and-compare) rather than byte-exact so
 * tests aren't fragile against pretty-printer settings. Byte-stability of
 * the actual output is verified separately by the CLI's idempotency at
 * runtime — re-running the translator must produce identical files.
 */
class RecipeTransformTest {

    private val repoRoot: Path = ModDiscovery.findRepoRoot(Path.of("").toAbsolutePath())
    private val target = BedrockTarget.load(repoRoot.resolve("translator/bedrock-target.json"))

    @Test
    fun `securityguard guard_helmet recipe matches golden`() {
        val unt = Untranslatable()
        val transform = RecipeTransform(target, unt)
        val javaJson = repoRoot
            .resolve("securityguard/src/generated/serverData/data/securityguard/recipe/guard_helmet.json")
            .readText()
        val actual = transform.translateJson(javaJson, "securityguard", "guard_helmet")!!
        assertJsonEquals(readGolden("recipe/securityguard_guard_helmet.json"), actual)
    }

    @Test
    fun `thief blackjack recipe matches golden`() {
        val unt = Untranslatable()
        val transform = RecipeTransform(target, unt)
        val javaJson = repoRoot
            .resolve("thief/src/generated/serverData/data/thief/recipe/blackjack.json")
            .readText()
        val actual = transform.translateJson(javaJson, "thief", "blackjack")!!
        assertJsonEquals(readGolden("recipe/thief_blackjack.json"), actual)
    }

    @Test
    fun `recipe category is dropped and recorded as untranslatable`() {
        val unt = Untranslatable()
        val transform = RecipeTransform(target, unt)
        val javaJson = repoRoot
            .resolve("securityguard/src/generated/serverData/data/securityguard/recipe/guard_helmet.json")
            .readText()
        transform.translateJson(javaJson, "securityguard", "guard_helmet")
        // The Java recipe declares `"category": "misc"`; verify it lands in the report.
        val report = unt.renderReport("securityguard")
        assert(report.contains("Recipe `category` field dropped")) {
            "Expected category dropped section in report: $report"
        }
        assert(report.contains("`guard_helmet`")) {
            "Expected guard_helmet listed in report: $report"
        }
    }

    @Test
    fun `merges generated and src main resources when both exist`(@TempDir tmp: Path) {
        // Wildwest reality: some recipes datagen'd, others authored in src/main/resources.
        val modRoot = tmp.resolve("themod")
        val genDir = modRoot.resolve("src/generated/serverData/data/themod/recipe")
        val mainDir = modRoot.resolve("src/main/resources/data/themod/recipe")
        genDir.createDirectories()
        mainDir.createDirectories()
        val shapeless = """
          {"type":"minecraft:crafting_shapeless","ingredients":[{"item":"minecraft:stone"}],
           "result":{"id":"minecraft:cobblestone"}}
        """.trimIndent()
        genDir.resolve("gen_recipe.json").writeText(shapeless)
        mainDir.resolve("main_recipe.json").writeText(shapeless)
        val out = tmp.resolve("out")
        RecipeTransform(target, Untranslatable()).translate(modRoot, "themod", out)
        assertTrue(out.resolve("themod/behavior_pack/recipes/gen_recipe.json").isRegularFile())
        assertTrue(out.resolve("themod/behavior_pack/recipes/main_recipe.json").isRegularFile())
    }

    @Test
    fun `falls back to src main resources when generated absent`(@TempDir tmp: Path) {
        val modRoot = tmp.resolve("themod")
        val mainDir = modRoot.resolve("src/main/resources/data/themod/recipe")
        mainDir.createDirectories()
        mainDir.resolve("only.json").writeText(
            """{"type":"minecraft:crafting_shapeless","ingredients":[{"item":"minecraft:stone"}],
                "result":{"id":"minecraft:cobblestone"}}""".trimIndent(),
        )
        val out = tmp.resolve("out")
        RecipeTransform(target, Untranslatable()).translate(modRoot, "themod", out)
        assertTrue(out.resolve("themod/behavior_pack/recipes/only.json").isRegularFile())
    }

    private fun readGolden(name: String): String =
        repoRoot.resolve("translator/src/test/resources/goldens/$name").readText()

    private fun assertJsonEquals(expected: String, actual: String) {
        val e = Json.parseToJsonElement(expected)
        val a = Json.parseToJsonElement(actual)
        assertEquals(e, a)
    }
}
