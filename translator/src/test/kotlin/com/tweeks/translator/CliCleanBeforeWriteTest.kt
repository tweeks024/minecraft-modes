package com.tweeks.translator

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText

/**
 * Tests for [cleanModOutputDir] — the per-mod scrub step run by the
 * translator CLI before each pipeline run, so stale files (renamed or
 * removed recipes, deleted textures, etc.) do not leak across runs.
 */
class CliCleanBeforeWriteTest {

    @Test
    fun `clean removes stale file inside the mod directory`(@TempDir tempDir: Path) {
        val outputRoot = tempDir.resolve("bedrock-out")
        val modDir = outputRoot.resolve("securityguard")
        modDir.createDirectories()
        val staleFile = modDir.resolve("STALE_TEST.json")
        staleFile.writeText("{\"stale\":true}\n")
        assertTrue(staleFile.exists()) { "precondition: stale file should exist" }

        cleanModOutputDir(outputRoot, "securityguard")

        assertFalse(staleFile.exists()) { "stale file should have been removed" }
        assertFalse(modDir.exists()) { "the per-mod dir itself should be gone after clean" }
    }

    @Test
    fun `clean removes nested stale subtree`(@TempDir tempDir: Path) {
        val outputRoot = tempDir.resolve("bedrock-out")
        val nested = outputRoot.resolve("thief/behavior_pack/recipes")
        nested.createDirectories()
        val staleNested = nested.resolve("removed_recipe.json")
        staleNested.writeText("{}")
        assertTrue(staleNested.exists())

        cleanModOutputDir(outputRoot, "thief")

        assertFalse(staleNested.exists())
        assertFalse(outputRoot.resolve("thief").exists())
    }

    @Test
    fun `clean leaves sibling mod dirs untouched`(@TempDir tempDir: Path) {
        val outputRoot = tempDir.resolve("bedrock-out")
        val targetMod = outputRoot.resolve("securityguard")
        val siblingMod = outputRoot.resolve("thief")
        targetMod.createDirectories()
        siblingMod.createDirectories()
        targetMod.resolve("a.json").writeText("{}")
        val siblingFile = siblingMod.resolve("b.json")
        siblingFile.writeText("{}")

        cleanModOutputDir(outputRoot, "securityguard")

        assertFalse(targetMod.exists()) { "target mod should be wiped" }
        assertTrue(siblingMod.exists()) { "sibling mod dir must survive" }
        assertTrue(siblingFile.exists()) { "sibling mod files must survive" }
    }

    @Test
    fun `clean is a no-op when the mod directory does not exist`(@TempDir tempDir: Path) {
        val outputRoot = tempDir.resolve("bedrock-out")
        outputRoot.createDirectories()
        // Should not throw even though `creeperskin/` doesn't exist yet.
        cleanModOutputDir(outputRoot, "creeperskin")
        assertTrue(outputRoot.exists())
    }

    @Test
    fun `clean refuses suspicious modIds`(@TempDir tempDir: Path) {
        val outputRoot = tempDir.resolve("bedrock-out")
        outputRoot.createDirectories()
        // Path traversal attempts must be refused before they touch the FS.
        assertThrows(IllegalArgumentException::class.java) {
            cleanModOutputDir(outputRoot, "../etc")
        }
        assertThrows(IllegalArgumentException::class.java) {
            cleanModOutputDir(outputRoot, "")
        }
        assertThrows(IllegalArgumentException::class.java) {
            cleanModOutputDir(outputRoot, "foo/bar")
        }
        assertThrows(IllegalArgumentException::class.java) {
            cleanModOutputDir(outputRoot, ".")
        }
        assertTrue(outputRoot.exists())
    }
}
