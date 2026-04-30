package com.tweeks.translator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the flag-handling helpers in [Cli].
 *
 * Phase 5: every CLI flag is now implemented, so [collectUnimplementedFlags]
 * always returns the empty list. The flag wiring (parseArgs) is still the
 * thing under test — Phase 6+ flags will go back through the unimplemented
 * helpers when they land.
 *
 * `main()` itself isn't tested here (it calls `exitProcess`); these tests
 * exercise the pure helpers `parseArgs` and `collectUnimplementedFlags`.
 */
class CliFlagsTest {

    @Test
    fun `no unimplemented flags returns empty list`() {
        val opts = CliOptions(
            modId = "securityguard",
            diff = false,
            noLlm = false,
            withLlm = false,
            clearCache = false,
        )
        assertEquals(emptyList<UnimplementedFlag>(), collectUnimplementedFlags(opts))
    }

    @Test
    fun `diff is now implemented`() {
        // Phase 5.4: --diff translates to a temp dir and compares against
        // bedrock-out/. No longer an unimplemented flag.
        val opts = CliOptions(
            modId = null,
            diff = true,
            noLlm = false,
            withLlm = false,
            clearCache = false,
        )
        assertEquals(emptyList<UnimplementedFlag>(), collectUnimplementedFlags(opts))
    }

    @Test
    fun `no-llm alone is no longer unimplemented`() {
        // Phase 3: --no-llm is the default behavior. Recognized as a no-op.
        val opts = CliOptions(
            modId = null,
            diff = false,
            noLlm = true,
            withLlm = false,
            clearCache = false,
        )
        assertEquals(emptyList<UnimplementedFlag>(), collectUnimplementedFlags(opts))
    }

    @Test
    fun `clear-cache alone is no longer unimplemented`() {
        val opts = CliOptions(
            modId = null,
            diff = false,
            noLlm = false,
            withLlm = false,
            clearCache = true,
        )
        assertEquals(emptyList<UnimplementedFlag>(), collectUnimplementedFlags(opts))
    }

    @Test
    fun `with-llm alone is recognized`() {
        val opts = CliOptions(
            modId = null,
            diff = false,
            noLlm = false,
            withLlm = true,
            clearCache = false,
        )
        assertEquals(emptyList<UnimplementedFlag>(), collectUnimplementedFlags(opts))
    }

    @Test
    fun `parseArgs accepts with-llm and clear-cache together`() {
        val opts = parseArgs(arrayOf("--with-llm", "--clear-cache", "securityguard"))
        assertTrue(opts.withLlm)
        assertTrue(opts.clearCache)
        assertEquals("securityguard", opts.modId)
    }

    @Test
    fun `parseArgs accepts diff and no-llm together`() {
        val opts = parseArgs(arrayOf("--diff", "--no-llm"))
        assertTrue(opts.diff)
        assertTrue(opts.noLlm)
    }
}
