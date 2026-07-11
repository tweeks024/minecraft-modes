package com.tweeks.translator.java.llm

import com.tweeks.translator.emit.Untranslatable
import kotlinx.serialization.json.JsonObject
import java.security.MessageDigest

/**
 * Phase 3: routes a deferred-from-Phase-2b goal through cache → LLM →
 * confidence threshold → on-disk JS or TODO stub.
 *
 * Routing logic per [route]:
 *   - **Cache hit**: return [RouteOutcome.MediumJs] (or a TODO stub if the
 *     cached entry was a Refused/Error — those are treated as "we already
 *     tried, don't waste tokens").
 *   - **Cache miss + `liveCallsEnabled = true`**: call the [client]. If the
 *     model returns Ok with confidence ≥ [CONFIDENCE_THRESHOLD], cache and
 *     return [RouteOutcome.MediumJs]. Below threshold, demote to a TODO stub
 *     and **do not** cache the demoted result (so a future
 *     `--clear-cache` + re-run with a tweaked prompt is a clean slate).
 *   - **Cache miss + `liveCallsEnabled = false`**: TODO stub. The user can
 *     pre-populate the cache out-of-band or re-run with `--with-llm`.
 *
 * The cache key per the design spec section "LLM integration" is:
 * ```
 *   sha256(java_source + prompt_version + model_id + bedrock_api_version)
 * ```
 * Phase 4 hardening adds `resolved_symbol_closure` (catches changes to a
 * dependency's signature) and `classpath_fingerprint` (catches NeoForge /
 * Minecraft version bumps). For Phase 3 the simpler key is correct for the
 * cases we have today: every goal's behavior is fully expressed in its own
 * Java source.
 */
class ConfidenceGate(
    private val client: ClaudeClient,
    private val cache: TranslationCache,
    private val prompt: TranslationPrompt,
    private val unt: Untranslatable,
    private val liveCallsEnabled: Boolean,
    private val modelId: String = TranslationPrompt.DEFAULT_MODEL_ID,
    private val bedrockApiVersion: String = "1.21.80",
) {

    /**
     * Decide what to emit for [goal]. The caller (the entity analyzer) is
     * responsible for actually writing the result to disk — this method is a
     * pure routing decision.
     *
     * [modId] is forwarded to [Untranslatable] so the cache-hit / cache-miss
     * sections of `UNTRANSLATABLE.md` are populated as a side effect.
     */
    fun route(goal: TranslationPrompt.GoalContext, modId: String): RouteOutcome {
        val key = computeKey(goal)

        // Cache lookup first.
        val hit = cache.get(key)
        if (hit != null) {
            when (hit) {
                is ClaudeClient.TranslationResult.Ok -> {
                    unt.recordEntityGoalLlmTranslated(modId, goal.goalClassSimpleName, key)
                    return RouteOutcome.MediumJs(
                        script = hit.js,
                        confidence = hit.confidence,
                    )
                }
                is ClaudeClient.TranslationResult.Refused -> {
                    // The model previously declined — don't waste tokens
                    // calling again until the user runs `--clear-cache`.
                    return todoStub(goal, modId, "cached refusal: ${hit.reason}")
                }
                is ClaudeClient.TranslationResult.Error -> {
                    // Stale entry from a prior version that cached errors.
                    // Treat as a miss so a re-run can retry the API call.
                    // Deletion would be cleaner but the cache file will be
                    // overwritten by the next successful translation.
                    // Fall through to the cache-miss path below.
                }
            }
        }

        // Cache miss. With live calls disabled, emit a stub.
        if (!liveCallsEnabled) {
            return todoStub(goal, modId, "cache miss; run :translate --with-llm to translate")
        }

        // Cache miss + live calls enabled — call the model.
        val req = ClaudeClient.TranslationRequest(
            systemPrompt = prompt.systemBlocks(),
            userMessage = prompt.userMessageFor(goal),
            modelId = modelId,
        )
        val result = client.translate(req)

        when (result) {
            is ClaudeClient.TranslationResult.Ok -> {
                if (result.confidence >= CONFIDENCE_THRESHOLD) {
                    cache.put(key, result)
                    unt.recordEntityGoalLlmTranslated(modId, goal.goalClassSimpleName, key)
                    return RouteOutcome.MediumJs(
                        script = result.js,
                        confidence = result.confidence,
                    )
                }
                // Demoted to Low — do NOT cache; the user may re-run with a
                // tweaked prompt or fixture and want a fresh attempt.
                return todoStub(
                    goal,
                    modId,
                    "model self-rated ${"%.2f".format(result.confidence)} < $CONFIDENCE_THRESHOLD; demoted to manual",
                )
            }
            is ClaudeClient.TranslationResult.Refused -> {
                cache.put(key, result)
                return todoStub(goal, modId, "model refused: ${result.reason}")
            }
            is ClaudeClient.TranslationResult.Error -> {
                // Do NOT cache transient API errors (429s, 5xx, network
                // timeouts). A first-run rate-limit spike on a fresh mod
                // would otherwise permanently stub every affected goal until
                // someone runs --clear-cache. Re-running on a clean network
                // condition should be a fresh attempt.
                return todoStub(goal, modId, "model error: ${result.message}")
            }
        }
    }

    private fun todoStub(
        goal: TranslationPrompt.GoalContext,
        modId: String,
        reason: String,
    ): RouteOutcome.TodoStub {
        unt.recordEntityGoalLlmStub(modId, goal.goalClassSimpleName, reason)
        return RouteOutcome.TodoStub(
            script = renderTodoStub(goal, reason),
            reason = reason,
        )
    }

    /**
     * Compute the cache key for this goal. Spec form (Phase 3 simplification):
     *
     *     sha256(java_source + prompt_version + system_prompt_digest +
     *            model_id + bedrock_api_version)
     *
     * The `system_prompt_digest` is the first 8 hex chars of a SHA-256 over
     * the concatenated text of every [ClaudeClient.SystemBlock] in the
     * current [TranslationPrompt]. This way editing a resource file
     * (`worked-examples.md`, `family-filters.md`, the bundled type
     * declarations, or the performance-rules preamble) automatically
     * invalidates every cached translation, even if the author forgets to
     * bump [TranslationPrompt.PROMPT_VERSION].
     *
     * TODO(phase 4): add `resolved_symbol_closure` (FQN + signature hash for
     * each external method/field the goal references) and
     * `classpath_fingerprint` (sha256 of sorted (jarFileName, jarSha256)
     * pairs) per the design spec. Phase 3 ships the simpler key; the four
     * goals we currently translate are self-contained.
     */
    internal fun computeKey(goal: TranslationPrompt.GoalContext): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(goal.goalSource.toByteArray(Charsets.UTF_8))
        md.update(0)
        md.update(TranslationPrompt.PROMPT_VERSION.toByteArray(Charsets.UTF_8))
        md.update(0)
        md.update(systemPromptDigest.toByteArray(Charsets.UTF_8))
        md.update(0)
        md.update(modelId.toByteArray(Charsets.UTF_8))
        md.update(0)
        md.update(bedrockApiVersion.toByteArray(Charsets.UTF_8))
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * 8-char SHA-256 prefix of the resolved system-prompt text. Computed
     * once per gate instance — the prompt is immutable for the lifetime
     * of the [ConfidenceGate].
     */
    private val systemPromptDigest: String by lazy {
        val concatenated = prompt.systemBlocks().joinToString(" ") { it.text }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(concatenated.toByteArray(Charsets.UTF_8))
        digest.joinToString("") { "%02x".format(it) }.take(8)
    }

    companion object {
        /** Self-rated confidence ≥ this is committed; below demotes to TODO. */
        const val CONFIDENCE_THRESHOLD: Double = 0.8

        /**
         * Render a TODO stub the user can replace with the real translation.
         * Includes the original Java in a comment block per the spec.
         */
        fun renderTodoStub(goal: TranslationPrompt.GoalContext, reason: String): String = buildString {
            append("// TODO LLM: ").append(reason).append('\n')
            append("// Goal: ").append(goal.goalClassFqn).append('\n')
            append("//\n")
            append("// This file is a placeholder. Either:\n")
            append("//   1. Run `./gradlew :translator:translate --with-llm` with ANTHROPIC_API_KEY set, or\n")
            append("//   2. Hand-translate the Java below to @minecraft/server event handlers.\n")
            append('\n')
            append("/*\n")
            append("Original Java source — translate this:\n\n")
            for (line in goal.goalSource.lines()) {
                append(line).append('\n')
            }
            append("*/\n")
            append('\n')
            append("// Empty handler so Bedrock's script engine accepts the file.\n")
            append("export {};\n")
        }
    }
}

/**
 * The three possible outcomes of routing a Phase-2b-deferred goal.
 *
 * `HighEmit` is included for symmetry with the design spec's three-bucket
 * model, but Phase 3 doesn't synthesize High outcomes — those are emitted
 * directly by Phase 2b's `GoalMatcher`. It's here so the type matches the
 * spec's `RouteOutcome` and Phase 4 can reuse it.
 */
sealed class RouteOutcome {
    /** Phase 2b path — kept for symmetry with the design spec; not used in Phase 3. */
    data class HighEmit(
        val componentName: String,
        val componentJson: JsonObject,
    ) : RouteOutcome()

    /** Cache hit or live LLM call ≥ threshold — write [script] to disk. */
    data class MediumJs(
        val script: String,
        val confidence: Double,
    ) : RouteOutcome()

    /** Cache miss without live calls, or live call below threshold — write a stub. */
    data class TodoStub(
        val script: String,
        val reason: String,
    ) : RouteOutcome()
}
