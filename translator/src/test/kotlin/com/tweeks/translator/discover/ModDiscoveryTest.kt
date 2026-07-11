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
            listOf("securitycore", "securityguard", "thief", "creeperskin", "wildwest", "craftee", "starwars"),
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

    @Test
    fun `parses multi-arg include lines`(@org.junit.jupiter.api.io.TempDir tmp: Path) {
        // Groovy supports `include 'foo', 'bar'`; the original regex required
        // a line to end after the first quoted token and would silently drop
        // 'bar'. New shape: scan all quoted tokens on each include line.
        val sg = tmp.resolve("settings.gradle")
        sg.toFile().writeText(
            """
            rootProject.name = 'test'
            include 'foo', 'bar', 'baz'
            include 'solo'
            """.trimIndent()
        )
        // Create stub mod dirs so isModModule passes for each.
        for (m in listOf("foo", "bar", "baz", "solo")) {
            tmp.resolve("$m/src/main/java").toFile().mkdirs()
        }
        val ids = ModDiscovery(tmp).discover().map { it.modId }
        assertEquals(listOf("foo", "bar", "baz", "solo"), ids)
    }
}
