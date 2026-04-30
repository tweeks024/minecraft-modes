package com.tweeks.translator.diff

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/**
 * Phase 5.4 unit tests for [TreeDiff]. Verifies all three drift cases —
 * modified, added, deleted — and the no-drift fast path.
 */
class TreeDiffTest {

    @Test
    fun `empty trees produce empty diff`(@TempDir dir: Path) {
        val expected = (dir.resolve("expected")).also { it.createDirectories() }
        val actual = (dir.resolve("actual")).also { it.createDirectories() }
        assertEquals(emptyList<TreeDiff.Entry>(), TreeDiff.diff(expected, actual))
    }

    @Test
    fun `byte-equal files produce empty diff`(@TempDir dir: Path) {
        val expected = dir.resolve("expected").also { it.createDirectories() }
        val actual = dir.resolve("actual").also { it.createDirectories() }
        expected.resolve("a.json").writeText("""{"hello": 1}""")
        actual.resolve("a.json").writeText("""{"hello": 1}""")
        assertEquals(emptyList<TreeDiff.Entry>(), TreeDiff.diff(expected, actual))
    }

    @Test
    fun `modified file is reported with both sizes`(@TempDir dir: Path) {
        val expected = dir.resolve("expected").also { it.createDirectories() }
        val actual = dir.resolve("actual").also { it.createDirectories() }
        expected.resolve("a.json").writeText("""{"hello": 1}""")
        actual.resolve("a.json").writeText("""{"hello": 1, "extra": "field"}""")

        val diff = TreeDiff.diff(expected, actual)
        assertEquals(1, diff.size)
        val mod = diff[0] as TreeDiff.Entry.Modified
        assertEquals("a.json", mod.relPath)
        assertTrue(mod.expectedSize < mod.actualSize) {
            "expected to grow on edit; got ${mod.expectedSize} → ${mod.actualSize}"
        }
    }

    @Test
    fun `added file is reported`(@TempDir dir: Path) {
        val expected = dir.resolve("expected").also { it.createDirectories() }
        val actual = dir.resolve("actual").also { it.createDirectories() }
        actual.resolve("new.json").writeText("""{"new": true}""")

        val diff = TreeDiff.diff(expected, actual)
        assertEquals(1, diff.size)
        val added = diff[0] as TreeDiff.Entry.Added
        assertEquals("new.json", added.relPath)
        assertTrue(added.sizeBytes > 0)
    }

    @Test
    fun `deleted file is reported`(@TempDir dir: Path) {
        val expected = dir.resolve("expected").also { it.createDirectories() }
        val actual = dir.resolve("actual").also { it.createDirectories() }
        expected.resolve("gone.json").writeText("""{"gone": true}""")

        val diff = TreeDiff.diff(expected, actual)
        assertEquals(1, diff.size)
        val deleted = diff[0] as TreeDiff.Entry.Deleted
        assertEquals("gone.json", deleted.relPath)
        assertTrue(deleted.sizeBytes > 0)
    }

    @Test
    fun `mixed changes produce stable sorted output`(@TempDir dir: Path) {
        val expected = dir.resolve("expected").also { it.createDirectories() }
        val actual = dir.resolve("actual").also { it.createDirectories() }

        // Three changes: modified `a.json`, added `b.json`, deleted `c.json`.
        // Output should be sorted by relPath: a, b, c.
        expected.resolve("a.json").writeText("v1")
        actual.resolve("a.json").writeText("v2")
        actual.resolve("b.json").writeText("new")
        expected.resolve("c.json").writeText("gone")

        val diff = TreeDiff.diff(expected, actual)
        assertEquals(3, diff.size)
        assertEquals("a.json", diff[0].relPath)
        assertEquals("b.json", diff[1].relPath)
        assertEquals("c.json", diff[2].relPath)
        assertTrue(diff[0] is TreeDiff.Entry.Modified)
        assertTrue(diff[1] is TreeDiff.Entry.Added)
        assertTrue(diff[2] is TreeDiff.Entry.Deleted)
    }

    @Test
    fun `mod filter scopes diff to one mod`(@TempDir dir: Path) {
        val expected = dir.resolve("expected").also { it.createDirectories() }
        val actual = dir.resolve("actual").also { it.createDirectories() }

        // Two mods, drift in both. With a filter on `mod1`, only mod1's
        // entry should appear.
        expected.resolve("mod1").createDirectories()
        actual.resolve("mod1").createDirectories()
        expected.resolve("mod2").createDirectories()
        actual.resolve("mod2").createDirectories()
        expected.resolve("mod1/x.json").writeText("v1")
        actual.resolve("mod1/x.json").writeText("v2")
        expected.resolve("mod2/y.json").writeText("v1")
        actual.resolve("mod2/y.json").writeText("v2")

        val filteredToMod1 = TreeDiff.diff(expected, actual, modFilter = "mod1")
        assertEquals(1, filteredToMod1.size)
        assertEquals("mod1/x.json", filteredToMod1[0].relPath)
    }

    @Test
    fun `summary names every modified file with sizes`(@TempDir dir: Path) {
        val expected = dir.resolve("expected").also { it.createDirectories() }
        val actual = dir.resolve("actual").also { it.createDirectories() }
        expected.resolve("only-here.json").writeText("v1")
        actual.resolve("only-there.json").writeText("v2")

        val diff = TreeDiff.diff(expected, actual)
        val summary = TreeDiff.summary(diff)
        assertTrue(summary.contains("only-here.json")) { summary }
        assertTrue(summary.contains("only-there.json")) { summary }
        assertTrue(summary.contains("Drift detected")) { summary }
        assertTrue(summary.contains(":translator:translate")) {
            "summary must guide the user to fix: $summary"
        }
    }

    @Test
    fun `summary of empty diff is empty string`() {
        assertEquals("", TreeDiff.summary(emptyList()))
    }

    @Test
    fun `missing root tree is treated as empty`(@TempDir dir: Path) {
        val expected = dir.resolve("expected") // does NOT exist
        val actual = dir.resolve("actual").also { it.createDirectories() }
        actual.resolve("only-actual.json").writeText("v1")

        val diff = TreeDiff.diff(expected, actual)
        assertEquals(1, diff.size)
        assertNotNull(diff[0] as? TreeDiff.Entry.Added)
    }

    @Test
    fun `nested directories are walked`(@TempDir dir: Path) {
        val expected = dir.resolve("expected").also { it.createDirectories() }
        val actual = dir.resolve("actual").also { it.createDirectories() }
        expected.resolve("a/b/c").createDirectories()
        actual.resolve("a/b/c").createDirectories()
        expected.resolve("a/b/c/file.json").writeText("v1")
        actual.resolve("a/b/c/file.json").writeText("v2")

        val diff = TreeDiff.diff(expected, actual)
        assertEquals(1, diff.size)
        assertEquals("a/b/c/file.json", diff[0].relPath)
    }
}
