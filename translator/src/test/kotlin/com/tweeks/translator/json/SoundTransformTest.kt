package com.tweeks.translator.json

import com.tweeks.translator.discover.ModDiscovery
import com.tweeks.translator.emit.Untranslatable
import com.tweeks.translator.manifest.BedrockTarget
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
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

    private fun readGolden(name: String): String =
        repoRoot.resolve("translator/src/test/resources/goldens/$name").readText()

    private fun assertJsonEquals(expected: String, actual: String) {
        val e = Json.parseToJsonElement(expected)
        val a = Json.parseToJsonElement(actual)
        assertEquals(e, a)
    }
}
