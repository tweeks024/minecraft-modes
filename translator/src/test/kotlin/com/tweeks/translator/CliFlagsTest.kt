package com.tweeks.translator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the unimplemented-flag handling in [Cli].
 *
 * In Phase 0 the three feature flags `--diff`, `--no-llm`, and `--clear-cache`
 * are all recognized but not yet implemented. We treat them uniformly: any
 * combination causes the CLI to exit 2 with a message naming each flag and
 * the phase that will implement it.
 *
 * `main()` itself isn't directly tested here (it calls `exitProcess`); these
 * tests exercise the pure helpers `collectUnimplementedFlags` and
 * `unimplementedFlagMessage` that drive that exit path.
 */
class CliFlagsTest {

    @Test
    fun `no unimplemented flags returns empty list`() {
        val opts = CliOptions(modId = "securityguard", diff = false, noLlm = false, clearCache = false)
        assertEquals(emptyList<UnimplementedFlag>(), collectUnimplementedFlags(opts))
    }

    @Test
    fun `diff alone is reported`() {
        val opts = CliOptions(modId = null, diff = true, noLlm = false, clearCache = false)
        assertEquals(listOf(UnimplementedFlag.DIFF), collectUnimplementedFlags(opts))
    }

    @Test
    fun `multiple flags are reported in declaration order`() {
        val opts = CliOptions(modId = "securityguard", diff = false, noLlm = true, clearCache = true)
        assertEquals(
            listOf(UnimplementedFlag.NO_LLM, UnimplementedFlag.CLEAR_CACHE),
            collectUnimplementedFlags(opts),
        )
    }

    @Test
    fun `single flag message names the flag and its phase`() {
        val msg = unimplementedFlagMessage(listOf(UnimplementedFlag.DIFF))
        // Header uses singular wording so the user sees one consistent format.
        assertTrue(msg.startsWith("[translator] Flag not yet implemented:")) {
            "Expected singular header, got: $msg"
        }
        assertTrue(msg.contains("--diff")) { "Message must name --diff: $msg" }
        assertTrue(msg.contains("Phase 1+")) { "Message must name --diff's planned phase: $msg" }
    }

    @Test
    fun `multi-flag message lists every flag and its phase`() {
        val msg = unimplementedFlagMessage(
            listOf(UnimplementedFlag.NO_LLM, UnimplementedFlag.CLEAR_CACHE)
        )
        assertTrue(msg.startsWith("[translator] Flags not yet implemented:")) {
            "Expected plural header, got: $msg"
        }
        assertTrue(msg.contains("--no-llm")) { "Message must name --no-llm: $msg" }
        assertTrue(msg.contains("--clear-cache")) { "Message must name --clear-cache: $msg" }
        // Both planned phases must appear (Phase 3 for both, but it must
        // appear at least once per flag — the format is "<flag>: planned for <phase>").
        assertTrue(msg.contains("--no-llm: planned for Phase 3")) { msg }
        assertTrue(msg.contains("--clear-cache: planned for Phase 3")) { msg }
    }
}
