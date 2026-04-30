package com.tweeks.translator.java.llm

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.CacheControlEphemeral
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.TextBlockParam

/**
 * Phase 3: real Anthropic client backed by the `anthropic-java` SDK.
 *
 * **This class is wired but is not exercised by Phase 3's default run or by
 * any test.** Live calls require:
 *   - `ANTHROPIC_API_KEY` set in the environment, AND
 *   - the user opts in via the `--with-llm` CLI flag.
 *
 * The implementation lives here so that:
 *   1. Removing the `anthropic-java` Gradle dependency is detected at compile
 *      time (the import below breaks).
 *   2. The shape of [translate] is fixed during Phase 3 review even though the
 *      first live invocation happens in Phase 4 when the user explicitly
 *      flips the switch.
 *
 * No retries, no streaming, no tool use — those are Phase 4+ concerns. Phase 3
 * just needs a single Messages API call returning a JSON-shaped JS string.
 */
internal class RealClaudeClient(
    apiKey: String? = null,
    /**
     * Override hook for tests that want to plug in a fake [AnthropicClient].
     * Production code passes null and the SDK is constructed below.
     */
    private val clientOverride: AnthropicClient? = null,
) : ClaudeClient {

    private val client: AnthropicClient by lazy {
        clientOverride ?: if (apiKey != null) {
            AnthropicOkHttpClient.builder().apiKey(apiKey).build()
        } else {
            AnthropicOkHttpClient.fromEnv()
        }
    }

    override fun translate(req: ClaudeClient.TranslationRequest): ClaudeClient.TranslationResult {
        return try {
            val systemBlocks = req.systemPrompt.map { block ->
                val builder = TextBlockParam.builder().text(block.text)
                if (block.cached) {
                    builder.cacheControl(CacheControlEphemeral.builder().build())
                }
                builder.build()
            }

            val params = MessageCreateParams.builder()
                .model(req.modelId)
                .maxTokens(req.maxTokens.toLong())
                .systemOfTextBlockParams(systemBlocks)
                .addUserMessage(req.userMessage)
                .build()

            val message = client.messages().create(params)

            // Concatenate every text block in the response. Phase 3 expects
            // the model to emit a JSON wrapper of `{ "js": "...", "confidence": 0.x }`
            // per the system-prompt contract; we parse it on the way out.
            val raw = message.content().asSequence()
                .mapNotNull { it.text().orElse(null) }
                .joinToString(separator = "\n") { it.text() }

            parseModelOutput(raw)
        } catch (e: Throwable) {
            ClaudeClient.TranslationResult.Error("RealClaudeClient: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /**
     * Parse the model's text response. The system prompt instructs the model
     * to wrap its output in a fenced JSON object:
     *
     *     ```json
     *     { "confidence": 0.85, "js": "...escaped..." }
     *     ```
     *
     * Tolerates surrounding prose; finds the first `{` … `}` JSON object that
     * has both keys.
     */
    private fun parseModelOutput(raw: String): ClaudeClient.TranslationResult {
        // Strip the most common fence shapes; anything in between is body.
        val candidate = stripFences(raw).trim()
        // Fall back to a regex-free brace-balance walk.
        val jsonStart = candidate.indexOf('{')
        if (jsonStart < 0) {
            return ClaudeClient.TranslationResult.Refused("model returned no JSON object: $raw")
        }
        val jsonEnd = findMatchingBrace(candidate, jsonStart)
        if (jsonEnd < 0) {
            return ClaudeClient.TranslationResult.Refused("model returned unterminated JSON: $raw")
        }
        val body = candidate.substring(jsonStart, jsonEnd + 1)
        return try {
            val parsed = kotlinx.serialization.json.Json.parseToJsonElement(body) as kotlinx.serialization.json.JsonObject
            val confidence = (parsed["confidence"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull()
                ?: return ClaudeClient.TranslationResult.Refused("model output missing 'confidence': $body")
            val js = (parsed["js"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                ?: return ClaudeClient.TranslationResult.Refused("model output missing 'js': $body")
            ClaudeClient.TranslationResult.Ok(js = js, confidence = confidence)
        } catch (e: Throwable) {
            ClaudeClient.TranslationResult.Refused("could not parse model output as JSON: ${e.message}")
        }
    }

    private fun stripFences(s: String): String {
        val fenceStart = s.indexOf("```")
        if (fenceStart < 0) return s
        val afterFirst = s.indexOf('\n', fenceStart).takeIf { it >= 0 } ?: return s
        val fenceEnd = s.indexOf("```", afterFirst + 1)
        if (fenceEnd < 0) return s.substring(afterFirst + 1)
        return s.substring(afterFirst + 1, fenceEnd)
    }

    private fun findMatchingBrace(s: String, openAt: Int): Int {
        var depth = 0
        var inString = false
        var escape = false
        for (i in openAt until s.length) {
            val c = s[i]
            if (escape) { escape = false; continue }
            if (c == '\\') { escape = true; continue }
            if (c == '"') { inString = !inString; continue }
            if (inString) continue
            when (c) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }
}
