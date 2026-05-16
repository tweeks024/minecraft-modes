package com.tweeks.translator.java.llm

import com.tweeks.translator.discover.ModDiscovery
import com.tweeks.translator.emit.Untranslatable
import com.tweeks.translator.manifest.BedrockTarget
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * Phase 3 routing tests for [ConfidenceGate]. All tests use [MockClaudeClient]
 * — no live API calls.
 */
class ConfidenceGateTest {

    private val repoRoot: Path = ModDiscovery.findRepoRoot(Path.of("").toAbsolutePath())
    private val target = BedrockTarget.load(repoRoot.resolve("translator/bedrock-target.json"))
    private val prompt = TranslationPrompt.load(target)

    private fun sampleGoal() = TranslationPrompt.GoalContext(
        goalClassSimpleName = "StunningMeleeGoal",
        goalClassFqn = "com.tweeks.securitycore.ai.StunningMeleeGoal",
        goalSource = "public class StunningMeleeGoal {}\n",
        parentClassFqn = "net.minecraft.world.entity.ai.goal.Goal",
        resolvedMethodSignatures = emptyList(),
        resolvedFieldReferences = emptyList(),
        entityClassSummary = "entity: guard",
        modManifestExcerpt = "modId: securityguard",
    )

    @Test
    fun `cache hit Ok returns MediumJs without calling client`(@TempDir dir: Path) {
        val unt = Untranslatable()
        val cache = TranslationCache(dir)
        val client = MockClaudeClient(emptyMap()) // would Error if called

        val gate = ConfidenceGate(client, cache, prompt, unt, liveCallsEnabled = false)
        val goal = sampleGoal()
        val key = gate.computeKey(goal)
        cache.put(key, ClaudeClient.TranslationResult.Ok("// from cache\n", 0.93))

        val outcome = gate.route(goal, modId = "securityguard")
        assertTrue(outcome is RouteOutcome.MediumJs)
        outcome as RouteOutcome.MediumJs
        assertEquals("// from cache\n", outcome.script)
        assertEquals(0.93, outcome.confidence)
        // Client was never called: lastRequest stays null.
        assertNull(client.lastRequest)

        val report = unt.renderReport("securityguard")
        assertTrue(report.contains("translated by LLM (cache hit)")) { report }
    }

    @Test
    fun `cache miss with live calls disabled returns TodoStub`(@TempDir dir: Path) {
        val unt = Untranslatable()
        val cache = TranslationCache(dir)
        val client = MockClaudeClient(emptyMap())

        val gate = ConfidenceGate(client, cache, prompt, unt, liveCallsEnabled = false)
        val outcome = gate.route(sampleGoal(), modId = "securityguard")

        assertTrue(outcome is RouteOutcome.TodoStub)
        outcome as RouteOutcome.TodoStub
        assertTrue(outcome.script.contains("// TODO LLM:"))
        assertTrue(outcome.script.contains("StunningMeleeGoal"))
        // Original Java is embedded in a comment block.
        assertTrue(outcome.script.contains("public class StunningMeleeGoal"))
        // Client was not called.
        assertNull(client.lastRequest)

        val report = unt.renderReport("securityguard")
        assertTrue(report.contains("stubbed for LLM (cache miss")) { report }
    }

    @Test
    fun `cache miss with live calls and high-confidence Ok writes to cache and returns MediumJs`(@TempDir dir: Path) {
        val unt = Untranslatable()
        val cache = TranslationCache(dir)

        // Pre-compute the canned key against the request the gate will build.
        val gate0 = ConfidenceGate(
            MockClaudeClient(emptyMap()), cache, prompt, unt,
            liveCallsEnabled = true,
        )
        val goal = sampleGoal()
        val req = ClaudeClient.TranslationRequest(
            systemPrompt = prompt.systemBlocks(),
            userMessage = prompt.userMessageFor(goal),
            modelId = TranslationPrompt.DEFAULT_MODEL_ID,
        )
        val mockKey = MockClaudeClient.keyOf(req)
        val client = MockClaudeClient(
            mapOf(mockKey to ClaudeClient.TranslationResult.Ok("// translated\n", 0.85)),
        )
        val gate = ConfidenceGate(client, cache, prompt, unt, liveCallsEnabled = true)

        val outcome = gate.route(goal, modId = "securityguard")
        assertTrue(outcome is RouteOutcome.MediumJs)
        outcome as RouteOutcome.MediumJs
        assertEquals("// translated\n", outcome.script)
        assertEquals(0.85, outcome.confidence)

        // Cache was written.
        val cached = cache.get(gate.computeKey(goal))
        assertNotNull(cached)
        assertTrue(cached is ClaudeClient.TranslationResult.Ok)
    }

    @Test
    fun `cache miss with live calls and low-confidence Ok demotes to TodoStub and does not cache`(@TempDir dir: Path) {
        val unt = Untranslatable()
        val cache = TranslationCache(dir)

        val goal = sampleGoal()
        val req = ClaudeClient.TranslationRequest(
            systemPrompt = prompt.systemBlocks(),
            userMessage = prompt.userMessageFor(goal),
            modelId = TranslationPrompt.DEFAULT_MODEL_ID,
        )
        val mockKey = MockClaudeClient.keyOf(req)
        val client = MockClaudeClient(
            mapOf(mockKey to ClaudeClient.TranslationResult.Ok("// shaky\n", 0.6)),
        )
        val gate = ConfidenceGate(client, cache, prompt, unt, liveCallsEnabled = true)

        val outcome = gate.route(goal, modId = "securityguard")
        assertTrue(outcome is RouteOutcome.TodoStub) {
            "expected demotion to TodoStub, got $outcome"
        }
        // Demoted results MUST NOT be cached.
        assertNull(cache.get(gate.computeKey(goal)))
    }

    @Test
    fun `cache miss with live calls and Refused returns TodoStub and caches the refusal`(@TempDir dir: Path) {
        val unt = Untranslatable()
        val cache = TranslationCache(dir)

        val goal = sampleGoal()
        val req = ClaudeClient.TranslationRequest(
            systemPrompt = prompt.systemBlocks(),
            userMessage = prompt.userMessageFor(goal),
            modelId = TranslationPrompt.DEFAULT_MODEL_ID,
        )
        val mockKey = MockClaudeClient.keyOf(req)
        val client = MockClaudeClient(
            mapOf(mockKey to ClaudeClient.TranslationResult.Refused("policy refusal")),
        )
        val gate = ConfidenceGate(client, cache, prompt, unt, liveCallsEnabled = true)

        val outcome = gate.route(goal, modId = "securityguard")
        assertTrue(outcome is RouteOutcome.TodoStub)
        outcome as RouteOutcome.TodoStub
        assertTrue(outcome.script.contains("// TODO LLM:"))

        // Refusals ARE cached so we don't repeatedly hit the API.
        val cached = cache.get(gate.computeKey(goal))
        assertTrue(cached is ClaudeClient.TranslationResult.Refused)
    }

    @Test
    fun `transient API error is NOT cached (so a retry can succeed)`(@TempDir dir: Path) {
        // First-run scenario: 10+ wildwest goals all miss cache, hit a 429
        // rate-limit. If we cached the error, every subsequent run would hit
        // the cached stub permanently until --clear-cache. Errors must stay
        // out of the cache so the next attempt is a fresh API call.
        val unt = Untranslatable()
        val cache = TranslationCache(dir)

        val goal = sampleGoal()
        val req = ClaudeClient.TranslationRequest(
            systemPrompt = prompt.systemBlocks(),
            userMessage = prompt.userMessageFor(goal),
            modelId = TranslationPrompt.DEFAULT_MODEL_ID,
        )
        val mockKey = MockClaudeClient.keyOf(req)
        val client = MockClaudeClient(
            mapOf(mockKey to ClaudeClient.TranslationResult.Error("429 rate limit")),
        )
        val gate = ConfidenceGate(client, cache, prompt, unt, liveCallsEnabled = true)

        val outcome = gate.route(goal, modId = "securityguard")
        assertTrue(outcome is RouteOutcome.TodoStub)

        // The transient error MUST NOT have been cached.
        assertNull(cache.get(gate.computeKey(goal))) {
            "transient errors must not be cached — a re-run with the same key " +
                "should retry the API call, not return a stale stub"
        }
    }

    @Test
    fun `cache key changes with model id`(@TempDir dir: Path) {
        val unt = Untranslatable()
        val cache = TranslationCache(dir)
        val client = MockClaudeClient(emptyMap())

        val gateA = ConfidenceGate(client, cache, prompt, unt, false, modelId = "claude-opus-4-7")
        val gateB = ConfidenceGate(client, cache, prompt, unt, false, modelId = "claude-sonnet-4-6")

        val goal = sampleGoal()
        assertNotEquals(gateA.computeKey(goal), gateB.computeKey(goal)) {
            "model_id must be part of the cache key — bumping the model should bust the cache"
        }
    }

    @Test
    fun `cache key changes with bedrock api version`(@TempDir dir: Path) {
        val unt = Untranslatable()
        val cache = TranslationCache(dir)
        val client = MockClaudeClient(emptyMap())

        val gateA = ConfidenceGate(client, cache, prompt, unt, false, bedrockApiVersion = "1.21.80")
        val gateB = ConfidenceGate(client, cache, prompt, unt, false, bedrockApiVersion = "1.22.0")

        val goal = sampleGoal()
        assertNotEquals(gateA.computeKey(goal), gateB.computeKey(goal)) {
            "bedrock_api_version must be part of the cache key"
        }
    }
}
