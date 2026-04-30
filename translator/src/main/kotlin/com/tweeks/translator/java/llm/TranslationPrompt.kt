package com.tweeks.translator.java.llm

import com.tweeks.translator.manifest.BedrockTarget

/**
 * Phase 3: builds the system prompt + per-request user message for the LLM
 * augmentation layer.
 *
 * The system prompt is a list of three [ClaudeClient.SystemBlock]s, all marked
 * `cached = true`:
 *   1. The performance-rules preamble — the contract every translation must
 *      obey (event-driven for discrete events, runInterval for periodics, no
 *      `system.run()` for periodic logic).
 *   2. The vendored `@minecraft/server` type declarations — gives the LLM
 *      type fidelity for the API surface it's allowed to emit.
 *   3. The worked examples — Java→Bedrock idiom pairs the LLM pattern-matches
 *      against.
 *
 * The blocks are ordered so the slowest-changing content comes first, which
 * is what Anthropic's prompt cache wants. The cache lookup at the SDK layer
 * stops at the last [ClaudeClient.SystemBlock] marked `cached = true`.
 *
 * **Versioning:** [PROMPT_VERSION] is part of the cache key; bump it whenever
 * any of the three blocks changes in a way that should bust the cache.
 */
class TranslationPrompt(
    @Suppress("unused") private val target: BedrockTarget,
    /** Vendored `@minecraft/server` declarations (server-1.21.80.d.ts). */
    private val typeDeclarations: String,
    /** Worked Java→Bedrock idioms (worked-examples.md). */
    private val workedExamples: String,
    /**
     * Family-filter reference. Phase 4: instructs the LLM how to translate
     * `instanceof SecurityAlly`/`SecurityHostile` targeting to Bedrock family
     * filters instead of hard-coded entity ids. The marker→family mapping is
     * done deterministically by [com.tweeks.translator.java.EntityAnalyzer];
     * this prompt block tells the LLM how to *consume* those family tags.
     */
    private val familyFilters: String,
) {

    /**
     * Per-request goal context: the Java source the LLM is being asked to
     * translate, plus enough surrounding type info that the model can
     * reason about cross-file dependencies.
     */
    data class GoalContext(
        /** Simple class name of the goal — e.g. `StunningMeleeGoal`. */
        val goalClassSimpleName: String,
        /** Fully-qualified name of the goal — e.g. `com.tweeks.securitycore.ai.StunningMeleeGoal`. */
        val goalClassFqn: String,
        /** The full Java source of the goal class. */
        val goalSource: String,
        /** FQN of the parent class (`net.minecraft.world.entity.ai.goal.Goal` for most goals). */
        val parentClassFqn: String?,
        /** Method signatures the goal calls outside its own source. Empty for Phase 3. */
        val resolvedMethodSignatures: List<String>,
        /** Field references the goal makes outside its own source. Empty for Phase 3. */
        val resolvedFieldReferences: List<String>,
        /**
         * Summary of the entity that owns this goal. One line per attribute
         * plus the line that registered the goal in `registerGoals()`. Lets
         * the LLM understand the entity's identity (mod id, class name).
         */
        val entityClassSummary: String,
        /** Mod manifest excerpt — modId + display name. */
        val modManifestExcerpt: String,
    )

    /** Four [ClaudeClient.SystemBlock]s, all marked `cached = true`. */
    fun systemBlocks(): List<ClaudeClient.SystemBlock> = listOf(
        ClaudeClient.SystemBlock(
            text = performanceRulesText(),
            cached = true,
        ),
        ClaudeClient.SystemBlock(
            text = "## Bedrock @minecraft/server type declarations\n\n```typescript\n$typeDeclarations\n```\n",
            cached = true,
        ),
        ClaudeClient.SystemBlock(
            text = "## Worked examples\n\n$workedExamples\n",
            cached = true,
        ),
        ClaudeClient.SystemBlock(
            text = "## Family-filter reference\n\n$familyFilters\n",
            cached = true,
        ),
    )

    /** Per-request user message — concatenates the goal source, types, and surrounding context. */
    fun userMessageFor(goal: GoalContext): String = buildString {
        append("Translate this Java NeoForge AI goal to Bedrock JavaScript / TypeScript event handlers.\n\n")
        append("Goal class: `").append(goal.goalClassFqn).append("`\n")
        if (goal.parentClassFqn != null) {
            append("Extends: `").append(goal.parentClassFqn).append("`\n")
        }
        append("\n## Goal source\n\n```java\n").append(goal.goalSource).append("\n```\n\n")

        if (goal.resolvedMethodSignatures.isNotEmpty()) {
            append("## Resolved external method signatures\n\n")
            for (sig in goal.resolvedMethodSignatures) {
                append("- `").append(sig).append("`\n")
            }
            append('\n')
        }

        if (goal.resolvedFieldReferences.isNotEmpty()) {
            append("## Resolved external field references\n\n")
            for (f in goal.resolvedFieldReferences) {
                append("- `").append(f).append("`\n")
            }
            append('\n')
        }

        append("## Owner entity\n\n").append(goal.entityClassSummary).append("\n\n")
        append("## Mod manifest\n\n").append(goal.modManifestExcerpt).append("\n\n")

        append(
            """
            Output exactly one fenced JSON object — nothing before, nothing after — of the form:

            ```json
            { "confidence": <0.0..1.0>, "js": "<the full JavaScript / TypeScript module>" }
            ```

            The `js` value is a complete `@minecraft/server` module that, when packed under
            `behavior_pack/scripts/goals/${'$'}{goalClassSimpleName}.ts`, preserves the source Java's
            observable in-game behavior. Embed the original Java source as a leading comment
            block so reviewers can compare side-by-side. Apply the performance rules from the
            system prompt verbatim.
            """.trimIndent()
        )
    }

    companion object {
        /**
         * Bumped when any of the three system blocks (performance rules,
         * type declarations, worked examples) changes in a way that should
         * bust every cached translation. Cached results carry the version
         * they were generated against; mismatches are treated as misses.
         */
        const val PROMPT_VERSION: String = "phase4-1"

        /** Default LLM model. Spec section "LLM integration" pins this. */
        const val DEFAULT_MODEL_ID: String = "claude-opus-4-7"

        /**
         * Construct from the bundled resources under
         * `translator/src/main/resources/bedrock-api/`. Throws if a resource
         * is missing — the resource bundle is a build-time invariant.
         */
        fun load(target: BedrockTarget): TranslationPrompt {
            val typeDecls = readResource("/bedrock-api/server-${target.scripting_api_version}.d.ts")
                ?: readResource("/bedrock-api/server-1.21.80.d.ts")
                ?: error("missing /bedrock-api/server-*.d.ts resource bundle")
            val examples = readResource("/bedrock-api/worked-examples.md")
                ?: error("missing /bedrock-api/worked-examples.md resource bundle")
            val families = readResource("/bedrock-api/family-filters.md")
                ?: error("missing /bedrock-api/family-filters.md resource bundle")
            return TranslationPrompt(target, typeDecls, examples, families)
        }

        private fun readResource(path: String): String? {
            return TranslationPrompt::class.java.getResourceAsStream(path)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            }
        }

        private fun performanceRulesText(): String = """
            You are a translator from Java NeoForge mods to Bedrock JavaScript /
            TypeScript event handlers targeting `@minecraft/server` 1.21.x. Your
            output must obey these performance rules verbatim — they are the
            contract every translation is reviewed against.

            1. Translate **discrete-event** Java logic (entity hurt, entity hit,
               entity died, item used, block placed) to
               `world.afterEvents.*.subscribe(...)` or
               `world.beforeEvents.*.subscribe(...)`. **Never poll** for these.
            2. For **periodic state checks** (cooldown countdown, AI re-targeting,
               distance check), use `system.runInterval(fn, ticks)` with the
               **largest** interval that preserves correctness. Default to **20**
               (1 second) unless the source explicitly uses a tighter tick rate.
            3. **Pool intervals**: share a single `runInterval` for multiple
               pieces of logic that want the same cadence rather than registering
               one interval per piece.
            4. **Never use `system.run()` for periodic logic** — it is one-shot.
               Use `runInterval` instead.
            5. If the source's tick semantics genuinely cannot be loosened,
               output `runInterval(fn, 1)` with a `// PERF:` comment explaining
               why. The `// PERF:` markers are searchable audit points.

            ## Output contract

            Respond with exactly one fenced JSON object — nothing before, nothing
            after — of the form:

            ```json
            { "confidence": <0.0..1.0>, "js": "<full TypeScript module source>" }
            ```

            Self-rate `confidence` in [0.0, 1.0]: your confidence that the emitted
            JS preserves the source Java's observable in-game behavior. The
            translator demotes outputs with confidence < 0.8 to a TODO stub for
            manual review, so be calibrated.

            Embed the original Java as a leading `/* */` comment block so
            reviewers can diff side-by-side. Use only `@minecraft/server` APIs
            shown in the type declarations below — anything outside that surface
            will fail to load in Bedrock.
        """.trimIndent()
    }
}
