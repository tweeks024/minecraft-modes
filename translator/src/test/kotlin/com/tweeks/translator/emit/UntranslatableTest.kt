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

    @Test
    fun `multiple phase 2 failures for one mod are all rendered`() {
        val unt = Untranslatable()
        unt.recordPhase2Failure("foo", "first analyzer threw: NPE on EntityFoo")
        unt.recordPhase2Failure("foo", "second analyzer threw: ClassCastException on ItemBar")

        val report = unt.renderReport("foo")
        assertTrue(report.contains("Phase 2 analyzer failure")) { report }
        assertTrue(report.contains("first analyzer threw: NPE on EntityFoo")) { report }
        assertTrue(report.contains("second analyzer threw: ClassCastException on ItemBar")) { report }
    }

    @Test
    fun `vehicle approximated finding appears in its own section`() {
        val unt = Untranslatable()
        unt.recordVehicleApproximated(
            "starwars", "landspeeder",
            "Java hover physics has no Bedrock equivalent — emitted as ground-driven rideable.",
        )

        val report = unt.renderReport("starwars")
        assertTrue(report.contains("Vehicles (approximated)")) { report }
        assertTrue(report.contains("landspeeder")) { report }
        assertTrue(report.contains("hover physics")) { report }
    }

    @Test
    fun `named-character singleton finding appears in its own section`() {
        val unt = Untranslatable()
        unt.recordNamedCharacterSingleton(
            "starwars", "vader",
            "Java enforces one living instance per server via SavedData; Bedrock has no equivalent.",
        )

        val report = unt.renderReport("starwars")
        assertTrue(report.contains("Named-character singletons")) { report }
        assertTrue(report.contains("vader")) { report }
        assertTrue(report.contains("SavedData")) { report }
    }

    @Test
    fun `duplicate behavior component drop appears in renderReport`() {
        val unt = Untranslatable()
        unt.recordDuplicateBehaviorComponent(
            modId = "foo",
            entityName = "Guard",
            componentName = "minecraft:behavior.random_stroll",
            droppedGoalDescription = "priority 7: RandomStrollGoal",
        )

        val report = unt.renderReport("foo")
        assertTrue(report.contains("Duplicate Bedrock behavior components dropped")) { report }
        assertTrue(report.contains("Guard")) { report }
        assertTrue(report.contains("minecraft:behavior.random_stroll")) { report }
        assertTrue(report.contains("priority 7: RandomStrollGoal")) { report }
    }

    @Test
    fun `datapack dimension biome and noise findings appear in their sections`() {
        val unt = Untranslatable()
        unt.recordDatapackDimension(
            "foo", "dimension/moon",
            "custom dimension not expressible in a Bedrock add-on — data/foo/dimension/moon.json",
        )
        unt.recordDatapackDimension(
            "foo", "dimension_type/moon",
            "custom dimension type not expressible in a Bedrock add-on — data/foo/dimension_type/moon.json",
        )
        unt.recordDatapackBiome(
            "foo", "crater",
            "custom biome not translated (no Bedrock biome emitter) — data/foo/worldgen/biome/crater.json",
        )
        unt.recordDatapackNoiseSettings(
            "foo", "moon",
            "custom noise settings / chunk generation not expressible — data/foo/worldgen/noise_settings/moon.json",
        )

        val report = unt.renderReport("foo")
        assertTrue(report.contains("## Custom dimensions not translatable")) { report }
        assertTrue(report.contains("- `dimension/moon`")) { report }
        assertTrue(report.contains("- `dimension_type/moon`")) { report }
        assertTrue(report.contains("## Custom biomes not translated")) { report }
        assertTrue(report.contains("- `crater`")) { report }
        assertTrue(report.contains("## Custom noise settings / chunk generation not expressible")) { report }
        assertTrue(report.contains("- `moon`")) { report }
    }

    @Test
    fun `custom block finding appears in its own section`() {
        val unt = Untranslatable()
        unt.recordBlockNotTranslated(
            "foo", "hyperspace_portal",
            "custom block 'hyperspace_portal' not translated — translator has no Bedrock block emitter",
        )

        val report = unt.renderReport("foo")
        assertTrue(report.contains("## Custom blocks not translated")) { report }
        assertTrue(report.contains("- `hyperspace_portal`")) { report }
        assertTrue(report.contains("no Bedrock block emitter")) { report }
    }
}
