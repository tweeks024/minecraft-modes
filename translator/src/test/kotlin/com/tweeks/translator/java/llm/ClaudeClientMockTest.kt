package com.tweeks.translator.java.llm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Phase 3 sanity check on [MockClaudeClient].
 *
 * Critical property: the mock NEVER reaches the real Anthropic SDK. Confirmed
 * implicitly by import boundaries — this test file does not import any
 * `com.anthropic.*` symbol. If a future refactor accidentally instantiates
 * [RealClaudeClient] from a test, the build will fail because tests are
 * supposed to use [MockClaudeClient] exclusively.
 */
class ClaudeClientMockTest {

    private fun sampleRequest(user: String = "default") = ClaudeClient.TranslationRequest(
        systemPrompt = listOf(
            ClaudeClient.SystemBlock("rules", cached = true),
            ClaudeClient.SystemBlock("examples", cached = true),
        ),
        userMessage = user,
        modelId = "claude-opus-4-7",
    )

    @Test
    fun `mock returns canned Ok when key matches`() {
        val req = sampleRequest("translate me")
        val key = MockClaudeClient.keyOf(req)
        val canned = ClaudeClient.TranslationResult.Ok(
            js = "// translated\n",
            confidence = 0.92,
        )
        val client = MockClaudeClient(mapOf(key to canned))

        val result = client.translate(req)
        assertTrue(result is ClaudeClient.TranslationResult.Ok)
        result as ClaudeClient.TranslationResult.Ok
        assertEquals("// translated\n", result.js)
        assertEquals(0.92, result.confidence)
    }

    @Test
    fun `mock returns Error when no canned response`() {
        val client = MockClaudeClient(canned = emptyMap())
        val result = client.translate(sampleRequest("anything"))
        assertTrue(result is ClaudeClient.TranslationResult.Error) {
            "expected Error fallback when key is absent, got $result"
        }
    }

    @Test
    fun `keyOf is deterministic and depends on userMessage`() {
        val a = sampleRequest("hello")
        val b = sampleRequest("hello")
        val c = sampleRequest("world")

        assertEquals(MockClaudeClient.keyOf(a), MockClaudeClient.keyOf(b))
        assertTrue(MockClaudeClient.keyOf(a) != MockClaudeClient.keyOf(c)) {
            "different userMessage must produce a different key"
        }
    }

    @Test
    fun `keyOf depends on modelId`() {
        val a = sampleRequest("hi").copy(modelId = "claude-opus-4-7")
        val b = sampleRequest("hi").copy(modelId = "claude-sonnet-4-6")
        assertTrue(MockClaudeClient.keyOf(a) != MockClaudeClient.keyOf(b))
    }

    @Test
    fun `mock records lastRequest`() {
        val client = MockClaudeClient(emptyMap())
        client.translate(sampleRequest("watched"))
        assertNotNull(client.lastRequest)
        assertEquals("watched", client.lastRequest!!.userMessage)
    }
}
