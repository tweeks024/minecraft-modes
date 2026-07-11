package com.tweeks.translator.json

import com.tweeks.translator.discover.ModDiscovery
import com.tweeks.translator.emit.Untranslatable
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class WorldgenStructureScannerTest {

    private val repoRoot: Path = ModDiscovery.findRepoRoot(Path.of("").toAbsolutePath())

    @Test
    fun `starwars datapack structures are recorded as untranslatable`() {
        val unt = Untranslatable()
        WorldgenStructureScanner(unt).scan(repoRoot.resolve("starwars"), "starwars")

        val report = unt.renderReport("starwars")
        assertTrue(report.contains("Datapack worldgen structures not translatable")) { report }
        assertTrue(report.contains("escape_pod")) { report }
        assertTrue(report.contains("imperial_outpost")) { report }
        assertTrue(report.contains("jedi_ruin")) { report }
        assertTrue(
            report.contains(
                "datapack worldgen structure not translatable to Bedrock — escape_pod; " +
                    "garrison/loot behavior lives in the Java piece",
            ),
        ) { report }
    }

    @Test
    fun `mod with no worldgen structure directory records nothing`() {
        val unt = Untranslatable()
        WorldgenStructureScanner(unt).scan(repoRoot.resolve("wildwest"), "wildwest")

        val report = unt.renderReport("wildwest")
        assertTrue(!report.contains("Datapack worldgen structures not translatable")) { report }
    }

    @Test
    fun `reads structures from src main resources when generated serverData absent`(@TempDir tmp: Path) {
        val modRoot = tmp.resolve("themod")
        val dir = modRoot.resolve("src/main/resources/data/themod/worldgen/structure")
        dir.createDirectories()
        dir.resolve("outpost.json").writeText("""{"type": "themod:outpost"}""")

        val unt = Untranslatable()
        WorldgenStructureScanner(unt).scan(modRoot, "themod")

        val report = unt.renderReport("themod")
        assertTrue(report.contains("outpost")) { report }
    }

    @Test
    fun `merges generated and src main resources deduping by relative path`(@TempDir tmp: Path) {
        val modRoot = tmp.resolve("themod")
        val genDir = modRoot.resolve("src/generated/serverData/data/themod/worldgen/structure")
        val mainDir = modRoot.resolve("src/main/resources/data/themod/worldgen/structure")
        genDir.createDirectories()
        mainDir.createDirectories()
        genDir.resolve("crash_site.json").writeText("""{"type": "themod:crash_site"}""")
        mainDir.resolve("crash_site.json").writeText("""{"type": "themod:crash_site"}""")
        mainDir.resolve("ruin.json").writeText("""{"type": "themod:ruin"}""")

        val unt = Untranslatable()
        WorldgenStructureScanner(unt).scan(modRoot, "themod")

        val report = unt.renderReport("themod")
        // Both structures recorded exactly once — no duplicate bullet lines.
        val crashSiteOccurrences = Regex("- `crash_site`").findAll(report).count()
        assertTrue(crashSiteOccurrences == 1) { "Expected crash_site recorded once, report:\n$report" }
        assertTrue(report.contains("- `ruin`")) { report }
    }

    @Test
    fun `nested structure subdirectory yields a namespaced id`(@TempDir tmp: Path) {
        val modRoot = tmp.resolve("themod")
        val dir = modRoot.resolve("src/generated/serverData/data/themod/worldgen/structure/sub")
        dir.createDirectories()
        dir.resolve("nested.json").writeText("""{"type": "themod:nested"}""")

        val unt = Untranslatable()
        WorldgenStructureScanner(unt).scan(modRoot, "themod")

        val report = unt.renderReport("themod")
        assertTrue(report.contains("- `sub/nested`")) { report }
    }
}
