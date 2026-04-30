package com.tweeks.translator.emit

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText

class UntranslatableTest {

    @Test
    fun `clean mod renders no-findings banner`() {
        val unt = Untranslatable()
        val report = unt.renderReport("creeperskin")
        assertTrue(report.contains("No translation losses recorded.")) { report }
    }

    @Test
    fun `recipe and sound findings appear in their sections`() {
        val unt = Untranslatable()
        unt.recordRecipeCategoryDropped("foo", "alpha")
        unt.recordRecipeCategoryDropped("foo", "beta")
        unt.recordSoundSubtitleDropped("foo", "fizz")
        unt.recordVanillaSoundPath("foo", "minecraft:entity/villager/idle1", "sounds/mob/villager/idle1")

        val report = unt.renderReport("foo")
        assertTrue(report.contains("Recipe `category` field dropped")) { report }
        assertTrue(report.contains("- `alpha`")) { report }
        assertTrue(report.contains("- `beta`")) { report }
        assertTrue(report.contains("Sound `subtitle` dropped")) { report }
        assertTrue(report.contains("Vanilla sound paths translated approximately")) { report }
        assertTrue(report.contains("minecraft:entity/villager/idle1")) { report }
        assertTrue(report.contains("sounds/mob/villager/idle1")) { report }
    }

    @Test
    fun `writeFor produces a file at the conventional path`(@TempDir tempDir: Path) {
        val unt = Untranslatable()
        unt.recordRecipeCategoryDropped("foo", "alpha")
        unt.writeFor("foo", tempDir)
        val report = tempDir.resolve("foo/UNTRANSLATABLE.md").readText()
        assertTrue(report.contains("UNTRANSLATABLE — foo")) { report }
        assertTrue(report.contains("- `alpha`")) { report }
    }
}
