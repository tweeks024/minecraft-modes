package com.tweeks.translator.java.llm

import java.security.MessageDigest

/**
 * Phase 3: thin abstraction over the Anthropic Messages API used by the
 * translator's LLM augmentation layer.
 *
 * Two implementations:
 *   - [RealClaudeClient]: calls the real API via the `anthropic-java` SDK.
 *     Wired and compiled, but **not** invoked by the default translate run
 *     and **not** invoked by any test. Live calls require an explicit
 *     `--with-llm` CLI flag and `ANTHROPIC_API_KEY` set in the environment.
 *   - [MockClaudeClient]: returns canned responses keyed by a digest of the
 *     request. Used by every test in this package.
 *
 * The hard separation guarantees that no test path can accidentally spend
 * Anthropic tokens — the user has explicit control over when live calls
 * happen.
 */
sealed interface ClaudeClient {
    fun translate(req: TranslationRequest): TranslationResult

    /**
     * One full Anthropic request. The system prompt is split into [SystemBlock]s
     * so that the slow-changing fragments (type declarations, worked examples,
     * performance rules) can be flagged for prompt caching at the SDK layer.
     */
    data class TranslationRequest(
        val systemPrompt: List<SystemBlock>,
        val userMessage: String,
        val modelId: String,
        val maxTokens: Int = 4096,
    )

    /**
     * One fragment of the system prompt.
     *
     * @property text raw text shown to the model.
     * @property cached if true, [RealClaudeClient] sets `cache_control:
     * { type: "ephemeral" }` on this block so subsequent requests in the same
     * session re-use the prefill at a steep discount.
     */
    data class SystemBlock(
        val text: String,
        val cached: Boolean,
    )

    sealed class TranslationResult {
        /**
         * @property js the emitted JavaScript / TypeScript source.
         * @property confidence model self-rated confidence in [0.0, 1.0].
         * The [ConfidenceGate] gates writes on this value.
         */
        data class Ok(val js: String, val confidence: Double) : TranslationResult()

        /**
         * The model declined to translate (e.g. policy refusal, out-of-domain
         * input). Cached so we don't repeatedly call for the same input.
         */
        data class Refused(val reason: String) : TranslationResult()

        /**
         * Network / SDK / unexpected error. Cached for the same reason as
         * [Refused] — Phase 3 cache-forever; Phase 4+ may add TTLs.
         */
        data class Error(val message: String) : TranslationResult()
    }
}

/**
 * Test-only client that returns canned [ClaudeClient.TranslationResult]s.
 *
 * The map is keyed by [keyOf] — a SHA-256 digest of the request's userMessage
 * + modelId + every system block's text, in order. Test fixtures pre-compute
 * the key and put a canned response in the map.
 *
 * If the request has no canned response, [translate] returns
 * [ClaudeClient.TranslationResult.Error] rather than throwing. This mirrors
 * the real-world behavior of "the API call failed" without giving tests a
 * way to accidentally exercise the live path.
 */
class MockClaudeClient(
    private val canned: Map<String, ClaudeClient.TranslationResult>,
) : ClaudeClient {

    /** True if at least one [translate] call was served from [canned]. */
    var lastRequest: ClaudeClient.TranslationRequest? = null
        private set

    override fun translate(req: ClaudeClient.TranslationRequest): ClaudeClient.TranslationResult {
        lastRequest = req
        val k = keyOf(req)
        return canned[k]
            ?: ClaudeClient.TranslationResult.Error("MockClaudeClient: no canned response for key $k")
    }

    companion object {
        /**
         * Stable digest over the parts of a request a test fixture cares
         * about. Excludes maxTokens (rarely interesting) and the cached flag
         * on each block (also irrelevant for keying).
         */
        fun keyOf(req: ClaudeClient.TranslationRequest): String {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(req.modelId.toByteArray(Charsets.UTF_8))
            md.update(0)
            for (block in req.systemPrompt) {
                md.update(block.text.toByteArray(Charsets.UTF_8))
                md.update(0)
            }
            md.update(req.userMessage.toByteArray(Charsets.UTF_8))
            return md.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
