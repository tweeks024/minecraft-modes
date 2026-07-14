package com.tweeks.translator.json

import com.tweeks.translator.discover.ModDiscovery
import com.tweeks.translator.emit.Untranslatable
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class DimensionScannerTest {

    private val repoRoot: Path = ModDiscovery.findRepoRoot(Path.of("").toAbsolutePath())

    @Test
    fun `starwars planet datapack is recorded as untranslatable`() {
        val unt = Untranslatable()
        DimensionScanner(unt).scan(repoRoot.resolve("starwars"), "starwars")

        val report = unt.renderReport("starwars")
        assertTrue(report.contains("Custom dimensions not translatable")) { report }
        for (planet in listOf("andor", "coruscant", "tatooine")) {
            assertTrue(report.contains("- `dimension/$planet`")) { report }
            assertTrue(report.contains("- `dimension_type/$planet`")) { report }
        }
        assertTrue(report.contains("Custom biomes not translated")) { report }
        for (biome in listOf("aldhani_highlands", "coruscant_city", "dune_sea", "jundland_wastes")) {
            assertTrue(report.contains("- `$biome`")) { report }
        }
        assertTrue(report.contains("Custom noise settings / chunk generation not expressible")) { report }
        for (noise in listOf("andor", "tatooine")) {
            assertTrue(
                report.contains("data/starwars/worldgen/noise_settings/$noise.json"),
            ) { report }
        }

        // Exact per-family message shapes — the Java-side path must be named.
        assertTrue(
            report.contains(
                "custom dimension not expressible in a Bedrock add-on — " +
                    "data/starwars/dimension/andor.json; the planet world and travel into it are Java-only",
            ),
        ) { report }
        assertTrue(
            report.contains(
                "custom dimension type not expressible in a Bedrock add-on — " +
                    "data/starwars/dimension_type/andor.json; sky/light/height rules for the planet are Java-only",
            ),
        ) { report }
        assertTrue(
            report.contains(
                "custom biome not translated (no Bedrock biome emitter) — " +
                    "data/starwars/worldgen/biome/dune_sea.json; surface rules, ambience, and spawn lists stay Java-only",
            ),
        ) { report }
        assertTrue(
            report.contains(
                "custom noise settings / chunk generation not expressible — " +
                    "data/starwars/worldgen/noise_settings/tatooine.json; terrain shaping lives in the datapack " +
                    "noise router and the mod's Java chunk generator",
            ),
        ) { report }
    }

    @Test
    fun `mod with no dimension datapack records nothing`() {
        val unt = Untranslatable()
        DimensionScanner(unt).scan(repoRoot.resolve("wildwest"), "wildwest")

        val report = unt.renderReport("wildwest")
        assertTrue(!report.contains("Custom dimensions not translatable")) { report }
        assertTrue(!report.contains("Custom biomes not translated")) { report }
        assertTrue(!report.contains("Custom noise settings / chunk generation not expressible")) { report }
    }

    @Test
    fun `reads dimension json from src main resources when generated serverData absent`(@TempDir tmp: Path) {
        val modRoot = tmp.resolve("themod")
        val dimDir = modRoot.resolve("src/main/resources/data/themod/dimension")
        dimDir.createDirectories()
        dimDir.resolve("moon.json").writeText("""{"type": "themod:moon"}""")

        val unt = Untranslatable()
        DimensionScanner(unt).scan(modRoot, "themod")

        val report = unt.renderReport("themod")
        assertTrue(report.contains("- `dimension/moon`")) { report }
        assertTrue(report.contains("data/themod/dimension/moon.json")) { report }
    }

    @Test
    fun `merges generated and src main resources deduping by relative path`(@TempDir tmp: Path) {
        val modRoot = tmp.resolve("themod")
        val genDir = modRoot.resolve("src/generated/serverData/data/themod/worldgen/biome")
        val mainDir = modRoot.resolve("src/main/resources/data/themod/worldgen/biome")
        genDir.createDirectories()
        mainDir.createDirectories()
        genDir.resolve("crater.json").writeText("""{"temperature": 2.0}""")
        mainDir.resolve("crater.json").writeText("""{"temperature": 2.0}""")
        mainDir.resolve("mare.json").writeText("""{"temperature": 0.5}""")

        val unt = Untranslatable()
        DimensionScanner(unt).scan(modRoot, "themod")

        val report = unt.renderReport("themod")
        // Both biomes recorded exactly once — no duplicate bullet lines.
        val craterOccurrences = Regex("- `crater`").findAll(report).count()
        assertTrue(craterOccurrences == 1) { "Expected crater recorded once, report:\n$report" }
        assertTrue(report.contains("- `mare`")) { report }
    }

    @Test
    fun `each datapack family lands in its own section`(@TempDir tmp: Path) {
        val modRoot = tmp.resolve("themod")
        for (family in listOf("dimension", "dimension_type", "worldgen/biome", "worldgen/noise_settings")) {
            val dir = modRoot.resolve("src/generated/serverData/data/themod/$family")
            dir.createDirectories()
            dir.resolve("moon.json").writeText("{}")
        }

        val unt = Untranslatable()
        DimensionScanner(unt).scan(modRoot, "themod")

        val report = unt.renderReport("themod")
        // dimension + dimension_type share the dimensions section, keyed by family.
        assertTrue(report.contains("## Custom dimensions not translatable")) { report }
        assertTrue(report.contains("- `dimension/moon`")) { report }
        assertTrue(report.contains("- `dimension_type/moon`")) { report }
        assertTrue(report.contains("## Custom biomes not translated")) { report }
        assertTrue(report.contains("## Custom noise settings / chunk generation not expressible")) { report }
    }
}
