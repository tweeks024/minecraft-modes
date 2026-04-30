package com.tweeks.translator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the flag-handling helpers in [Cli].
 *
 * Phase 3 status:
 *   - `--no-llm` and `--clear-cache` are now implemented; they no longer
 *     trigger an unimplemented-flag exit. `--no-llm` is a no-op alias for
 *     "default behavior" (cache-or-stub). `--clear-cache` wipes the on-disk
 *     LLM cache before running. `--with-llm` opts into live API calls.
 *   - `--diff` is still unimplemented (planned Phase 4+).
 *
 * `main()` itself isn't tested here (it calls `exitProcess`); these tests
 * exercise the pure helpers `parseArgs`, `collectUnimplementedFlags`, and
 * `unimplementedFlagMessage`.
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
    fun `diff alone is reported`() {
        val opts = CliOptions(
            modId = null,
            diff = true,
            noLlm = false,
            withLlm = false,
            clearCache = false,
        )
        assertEquals(listOf(UnimplementedFlag.DIFF), collectUnimplementedFlags(opts))
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
    fun `single flag message names the flag and its phase`() {
        val msg = unimplementedFlagMessage(listOf(UnimplementedFlag.DIFF))
        assertTrue(msg.startsWith("[translator] Flag not yet implemented:")) {
            "Expected singular header, got: $msg"
        }
        assertTrue(msg.contains("--diff")) { "Message must name --diff: $msg" }
        assertTrue(msg.contains("Phase 4+")) { "Message must name --diff's planned phase: $msg" }
    }

    @Test
    fun `parseArgs accepts with-llm and clear-cache together`() {
        val opts = parseArgs(arrayOf("--with-llm", "--clear-cache", "securityguard"))
        assertTrue(opts.withLlm)
        assertTrue(opts.clearCache)
        assertEquals("securityguard", opts.modId)
    }
}
