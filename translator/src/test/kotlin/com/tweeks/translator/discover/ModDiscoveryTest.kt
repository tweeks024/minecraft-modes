package com.tweeks.translator.discover

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Path

class ModDiscoveryTest {

    /**
     * The repo root is two levels up from the translator module's working
     * directory when tests run via Gradle. We resolve it via the same helper
     * the CLI uses so the test exercises the real file walk.
     */
    private val repoRoot: Path = ModDiscovery.findRepoRoot(Path.of("").toAbsolutePath())

    @Test
    fun `discovers all sibling NeoForge mods in settings_gradle order`() {
        val ids = ModDiscovery(repoRoot).discover().map { it.modId }
        assertEquals(
            listOf("securitycore", "securityguard", "thief", "creeperskin", "wildwest", "craftee"),
            ids,
            "Expected all NeoForge mods in settings.gradle order; the translator module itself must be filtered out",
        )
    }

    @Test
    fun `findById returns the correct mod and resolves a real directory`() {
        val mod = ModDiscovery(repoRoot).findById("securityguard")
        assertEquals("securityguard", mod?.modId)
        assertEquals(repoRoot.resolve("securityguard"), mod?.rootDir)
    }

    @Test
    fun `findById returns null for unknown ids and for the translator module`() {
        val discovery = ModDiscovery(repoRoot)
        assertEquals(null, discovery.findById("nope"))
        // 'translator' is in settings.gradle but is not a NeoForge mod, so
        // discovery must filter it out.
        assertEquals(null, discovery.findById("translator"))
    }
}
