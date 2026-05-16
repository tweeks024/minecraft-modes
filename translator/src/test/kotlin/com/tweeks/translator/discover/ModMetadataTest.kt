package com.tweeks.translator.discover

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class ModMetadataTest {

    @Test
    fun `parseSecurityCoreDep detects literal modId line`() {
        val toml = """
            [[mods]]
            modId="${'$'}{mod_id}"
            [[dependencies.${'$'}{mod_id}]]
                modId="securitycore"
                type="required"
        """.trimIndent()
        assertTrue(ModMetadata.parseSecurityCoreDep(toml, "wildwest"))
    }

    @Test
    fun `parseSecurityCoreDep returns false when toml has no securitycore dep`() {
        val toml = """
            [[mods]]
            modId="${'$'}{mod_id}"
            [[dependencies.${'$'}{mod_id}]]
                modId="neoforge"
                type="required"
        """.trimIndent()
        assertFalse(ModMetadata.parseSecurityCoreDep(toml, "creeperskin"))
    }

    @Test
    fun `parseSecurityCoreDep returns false for securitycore itself`() {
        // securitycore's own toml contains its modId in a few places — it
        // must not declare itself as a dependency.
        val toml = """modId="securitycore""""
        assertFalse(ModMetadata.parseSecurityCoreDep(toml, "securitycore"))
    }

    @Test
    fun `read picks up securitycore dep from a real-shape toml`(@TempDir tmp: Path) {
        val modRoot = tmp.resolve("wildwest")
        val tomlDir = modRoot.resolve("src/main/templates/META-INF")
        tomlDir.createDirectories()
        tomlDir.resolve("neoforge.mods.toml").writeText(
            """
            license="MIT"
            [[mods]]
            modId="${'$'}{mod_id}"
            displayName="Wild West"
            description='''
            Wild west themed mod.
            '''
            [[dependencies.${'$'}{mod_id}]]
                modId="neoforge"
                type="required"
            [[dependencies.${'$'}{mod_id}]]
                modId="securitycore"
                type="required"
            """.trimIndent()
        )
        val md = ModMetadata.read(modRoot, "wildwest")
        assertEquals("Wild West", md.displayName)
        assertTrue(md.requiresSecurityCore, "wildwest's toml lists securitycore as a dep")
    }
}
