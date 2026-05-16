package com.tweeks.translator.json

import com.tweeks.translator.discover.ModDiscovery
import com.tweeks.translator.emit.Untranslatable
import com.tweeks.translator.manifest.BedrockTarget
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.readText

class SoundTransformTest {

    private val repoRoot: Path = ModDiscovery.findRepoRoot(Path.of("").toAbsolutePath())
    private val target = BedrockTarget.load(repoRoot.resolve("translator/bedrock-target.json"))

    @Test
    fun `securityguard sound_definitions matches golden`() {
        val unt = Untranslatable()
        val transform = SoundTransform(target, unt)
        val javaJson = repoRoot
            .resolve("securityguard/src/main/resources/assets/securityguard/sounds.json")
            .readText()
        val actual = transform.translateJson(javaJson, "securityguard")
        assertJsonEquals(readGolden("sounds/securityguard_sound_definitions.json"), actual)
    }

    @Test
    fun `subtitles and approximate vanilla paths are recorded as untranslatable`() {
        val unt = Untranslatable()
        val transform = SoundTransform(target, unt)
        val javaJson = repoRoot
            .resolve("securityguard/src/main/resources/assets/securityguard/sounds.json")
            .readText()
        transform.translateJson(javaJson, "securityguard")
        val report = unt.renderReport("securityguard")
        assert(report.contains("subtitle")) { report }
        assert(report.contains("Vanilla sound paths translated approximately")) { report }
        assert(report.contains("minecraft:entity/villager/idle1")) { report }
    }

    @Test
    fun `type event entries are emitted as Bedrock event refs, not file paths`() {
        // Wildwest sounds.json uses this shape for every entry: redirect
        // each custom event to a vanilla Minecraft sound event id. The
        // previous code blindly prepended `sounds/` (yielding the broken
        // path `sounds/item.crossbow.shoot`); the right thing is to emit
        // the event id as the Bedrock entry's `name` so Bedrock's audio
        // engine plays the vanilla event.
        val javaJson = """
            {
              "pistol_fire": {
                "category": "player",
                "sounds": [ { "name": "minecraft:item.crossbow.shoot", "type": "event" } ]
              }
            }
        """.trimIndent()
        val unt = Untranslatable()
        val out = SoundTransform(target, unt).translateJson(javaJson, "wildwest")
        val parsed = Json.parseToJsonElement(out).jsonObject
        val pistolFire = parsed["sound_definitions"]!!.jsonObject["wildwest:pistol_fire"]!!.jsonObject
        val firstSound = pistolFire["sounds"]!!.jsonArray[0].jsonObject
        val emittedName = firstSound["name"]!!.jsonPrimitive.content
        assertFalse(emittedName.startsWith("sounds/")) {
            "type:event entries must not be prepended with `sounds/`, got: $emittedName"
        }
        assertEquals("item.crossbow.shoot", emittedName)
    }

    private fun readGolden(name: String): String =
        repoRoot.resolve("translator/src/test/resources/goldens/$name").readText()

    private fun assertJsonEquals(expected: String, actual: String) {
        val e = Json.parseToJsonElement(expected)
        val a = Json.parseToJsonElement(actual)
        assertEquals(e, a)
    }
}
