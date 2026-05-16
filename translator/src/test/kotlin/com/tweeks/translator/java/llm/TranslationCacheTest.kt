package com.tweeks.translator.java.llm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.exists

class TranslationCacheTest {

    private val sampleKey = "deadbeefcafef00d"

    @Test
    fun `round-trip Ok result through the cache`(@TempDir dir: Path) {
        val cache = TranslationCache(dir)
        val value = ClaudeClient.TranslationResult.Ok(
            js = "import { world } from \"@minecraft/server\";\n",
            confidence = 0.87,
        )
        cache.put(sampleKey, value)

        val read = cache.get(sampleKey)
        assertTrue(read is ClaudeClient.TranslationResult.Ok)
        read as ClaudeClient.TranslationResult.Ok
        assertEquals(value.js, read.js)
        assertEquals(value.confidence, read.confidence)
    }

    @Test
    fun `round-trip Refused result through the cache`(@TempDir dir: Path) {
        val cache = TranslationCache(dir)
        cache.put(sampleKey, ClaudeClient.TranslationResult.Refused("policy"))

        val read = cache.get(sampleKey)
        assertTrue(read is ClaudeClient.TranslationResult.Refused)
        read as ClaudeClient.TranslationResult.Refused
        assertEquals("policy", read.reason)
    }

    @Test
    fun `round-trip Error result through the cache`(@TempDir dir: Path) {
        val cache = TranslationCache(dir)
        cache.put(sampleKey, ClaudeClient.TranslationResult.Error("rate limit"))

        val read = cache.get(sampleKey)
        assertTrue(read is ClaudeClient.TranslationResult.Error)
        read as ClaudeClient.TranslationResult.Error
        assertEquals("rate limit", read.message)
    }

    @Test
    fun `get returns null on miss`(@TempDir dir: Path) {
        val cache = TranslationCache(dir)
        assertNull(cache.get(sampleKey))
    }

    @Test
    fun `clear removes the entire cache directory`(@TempDir dir: Path) {
        val cacheRoot = dir.resolve("llm")
        val cache = TranslationCache(cacheRoot)
        cache.put(sampleKey, ClaudeClient.TranslationResult.Ok("// js\n", 0.9))
        assertTrue(cacheRoot.exists())

        cache.clear()
        assertFalse(cacheRoot.exists())
    }

    @Test
    fun `key must be a hex digest`(@TempDir dir: Path) {
        val cache = TranslationCache(dir)
        assertThrows(IllegalArgumentException::class.java) {
            cache.put("../etc/passwd", ClaudeClient.TranslationResult.Ok("", 1.0))
        }
        assertThrows(IllegalArgumentException::class.java) {
            cache.get("not-hex!!")
        }
    }

    @Test
    fun `corrupted cache file is treated as a miss`(@TempDir dir: Path) {
        val cache = TranslationCache(dir)
        cache.put(sampleKey, ClaudeClient.TranslationResult.Ok("// good\n", 0.9))
        // Corrupt the file directly.
        val file = dir.resolve("$sampleKey.json").toFile()
        file.writeText("{ this isn't valid JSON")
        assertNull(cache.get(sampleKey))
    }

    @Test
    fun `put writes atomically (no leftover temp files)`(@TempDir dir: Path) {
        val cache = TranslationCache(dir)
        cache.put(sampleKey, ClaudeClient.TranslationResult.Ok("// x\n", 0.9))
        // The final file should exist and there should be no `.tmp` siblings
        // — if a write was interrupted halfway, that's what we'd see.
        val files = dir.toFile().listFiles()?.map { it.name } ?: emptyList()
        assertTrue(files.contains("$sampleKey.json")) { "missing committed cache file: $files" }
        assertTrue(files.none { it.endsWith(".tmp") }) {
            "unexpected leftover temp file: $files"
        }
    }

    @Test
    fun `entry from a future schema version is treated as a miss`(@TempDir dir: Path) {
        // Write a cache file with a higher schemaVersion than the current
        // code knows. The cache must reject it (return null) rather than
        // returning a stale result built from fields the writer hadn't yet
        // populated, otherwise schema additions silently desync.
        val file = dir.resolve("$sampleKey.json").toFile()
        file.parentFile.mkdirs()
        file.writeText("""{"schemaVersion":9999,"kind":"ok","js":"x","confidence":0.9}""")
        assertNull(TranslationCache(dir).get(sampleKey))
    }
}
