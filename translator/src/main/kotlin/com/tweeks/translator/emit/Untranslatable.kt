package com.tweeks.translator.emit

import java.nio.file.Path
import java.util.TreeMap
import java.util.TreeSet
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/**
 * Per-mod accumulator for translation losses.
 *
 * Phase 1a's transforms call into one of these as they encounter Bedrock-
 * incompatible inputs. After the pipeline runs for a mod, [writeFor]
 * regenerates `bedrock-out/<modid>/UNTRANSLATABLE.md` with the recorded
 * findings. Empty sections are omitted; if a mod has no losses at all, an
 * "everything translated cleanly" banner is emitted so reviewers know this
 * isn't a stale report.
 */
class Untranslatable {

    // Per-mod buckets — TreeMap/TreeSet for stable, sorted output.

    private val recipeCategoryDropped = TreeMap<String, TreeSet<String>>()
    private val recipeTypeSkipped = TreeMap<String, TreeMap<String, String>>()
    private val lootRandomSequenceDropped = TreeMap<String, TreeSet<String>>()
    private val soundSubtitleDropped = TreeMap<String, TreeSet<String>>()
    private val vanillaSoundPaths = TreeMap<String, TreeMap<String, String>>()
    private val textureCategoriesSkipped = TreeMap<String, TreeSet<String>>()
    private val bbmodelFaceUv = TreeMap<String, TreeSet<String>>()
    private val bbmodelLocatorSkipped = TreeMap<String, TreeSet<String>>()
    private val bbmodelElementTypeSkipped = TreeMap<String, TreeMap<String, String>>()
    private val bbmodelMultiDataPoint = TreeMap<String, TreeSet<String>>()
    private val bbmodelNonLinearInterp = TreeMap<String, TreeMap<String, String>>()
    private val bbmodelFlipYUnset = TreeMap<String, TreeSet<String>>()
    private val javaParseErrors = TreeMap<String, TreeMap<String, String>>()
    private val entityGoalsDeferred = TreeMap<String, TreeMap<String, TreeMap<String, GoalDeferral>>>()
    private val entityGoalsGatedOff = TreeMap<String, TreeMap<String, TreeSet<String>>>()
    // Phase 3: per-mod accumulators for the LLM-stage outcomes. The String
    // key on the inner map is the goal class simple name (`StunningMeleeGoal`).
    private val entityGoalsLlmTranslated = TreeMap<String, TreeMap<String, String>>()
    private val entityGoalsLlmStubbed = TreeMap<String, TreeMap<String, String>>()
    private val itemCustomBehavior = TreeMap<String, TreeMap<String, String>>()
    private val itemModelSelectorStatic = TreeMap<String, TreeMap<String, String>>()
    private val attachableTextureFallback = TreeMap<String, TreeMap<String, String>>()
    private val spawnEggColorsHardcoded = TreeMap<String, TreeMap<String, String>>()
    private val renderControllerAmbiguous = TreeMap<String, TreeMap<String, String>>()
    private val entityAttributesUnresolved = TreeMap<String, TreeMap<String, TreeSet<String>>>()
    private val datapackWorldgenStructures = TreeMap<String, TreeMap<String, String>>()
    private val phase2Failures = TreeMap<String, MutableList<String>>()
    private val duplicateBehaviorComponents = TreeMap<String, TreeMap<String, TreeSet<String>>>()

    fun recordRecipeCategoryDropped(modId: String, recipeName: String) {
        recipeCategoryDropped.getOrPut(modId) { TreeSet() }.add(recipeName)
    }

    fun recordRecipeTypeSkipped(modId: String, recipeName: String, type: String) {
        recipeTypeSkipped.getOrPut(modId) { TreeMap() }[recipeName] = type
    }

    fun recordLootRandomSequenceDropped(modId: String, relPath: String) {
        lootRandomSequenceDropped.getOrPut(modId) { TreeSet() }.add(relPath)
    }

    fun recordSoundSubtitleDropped(modId: String, eventName: String) {
        soundSubtitleDropped.getOrPut(modId) { TreeSet() }.add(eventName)
    }

    fun recordVanillaSoundPath(modId: String, javaPath: String, bedrockPath: String) {
        vanillaSoundPaths.getOrPut(modId) { TreeMap() }[javaPath] = bedrockPath
    }

    fun recordTextureCategorySkipped(modId: String, category: String) {
        textureCategoriesSkipped.getOrPut(modId) { TreeSet() }.add(category)
    }

    fun recordBbmodelFaceUv(modId: String, location: String) {
        bbmodelFaceUv.getOrPut(modId) { TreeSet() }.add(location)
    }

    fun recordBbmodelLocatorSkipped(modId: String, location: String) {
        bbmodelLocatorSkipped.getOrPut(modId) { TreeSet() }.add(location)
    }

    fun recordBbmodelElementTypeSkipped(modId: String, location: String, type: String) {
        bbmodelElementTypeSkipped.getOrPut(modId) { TreeMap() }[location] = type
    }

    fun recordBbmodelMultiDataPointKeyframe(modId: String, location: String) {
        bbmodelMultiDataPoint.getOrPut(modId) { TreeSet() }.add(location)
    }

    fun recordBbmodelNonLinearInterpolation(modId: String, location: String, interpolation: String) {
        bbmodelNonLinearInterp.getOrPut(modId) { TreeMap() }[location] = interpolation
    }

    /**
     * Record a `.bbmodel` whose `meta.modded_entity_flip_y` is explicitly
     * `false` — the Y axis is not flipped to match Java's modded-entity
     * convention, so visual alignment in Bedrock should be verified.
     */
    fun recordBbmodelFlipYUnset(modId: String, modelName: String) {
        bbmodelFlipYUnset.getOrPut(modId) { TreeSet() }.add(modelName)
    }

    /**
     * Record a `.java` source file that JavaParser failed to parse. The
     * file is identified by its repo-relative path so reviewers can find
     * it; [error] is a short human-readable summary of the failure.
     */
    fun recordJavaParseError(modId: String, file: String, error: String) {
        javaParseErrors.getOrPut(modId) { TreeMap() }[file] = error
    }

    /**
     * Whether to demote an AI goal to the Phase 3 LLM (Medium bucket) or
     * label it as Low — a hand-written-JS placeholder for novel logic.
     */
    enum class GoalBucket { MEDIUM, LOW }

    data class GoalDeferral(
        val bucket: GoalBucket,
        val reason: String,
        val sourceExcerpt: String?,
    )

    /**
     * Record an AI goal that the High-bucket pattern matcher could not
     * translate. [entityName] is the simple class name (e.g.
     * `SecurityGuardEntity`); [goalKey] is a stable key for the goal
     * call site (`<priority>:<goalFqn>`) so the report orders by the
     * priority position in `registerGoals()`.
     */
    fun recordEntityGoalDeferred(
        modId: String,
        entityName: String,
        goalKey: String,
        bucket: GoalBucket,
        reason: String,
        sourceExcerpt: String? = null,
    ) {
        entityGoalsDeferred
            .getOrPut(modId) { TreeMap() }
            .getOrPut(entityName) { TreeMap() }[goalKey] = GoalDeferral(bucket, reason, sourceExcerpt)
    }

    /**
     * Record an AI goal that was registered on a shared superclass but gated
     * by a per-entity condition (an anonymous `canUse` override, e.g. SwMob's
     * `MeleeAttackGoal` gated on `!usesBlaster()`). For [entityName] the gate
     * resolved statically closed (or could not be resolved), so the goal was
     * dropped from that entity's Bedrock JSON. [summary] explains which goal
     * and gate.
     */
    fun recordEntityGoalGatedOff(modId: String, entityName: String, summary: String) {
        entityGoalsGatedOff
            .getOrPut(modId) { TreeMap() }
            .getOrPut(entityName) { TreeSet() }
            .add(summary)
    }

    /**
     * Phase 3: record a goal whose JS came from an LLM cache hit.
     * [cacheKey] is the SHA-256 hex digest used as the cache filename.
     */
    fun recordEntityGoalLlmTranslated(modId: String, goalSimpleName: String, cacheKey: String) {
        entityGoalsLlmTranslated.getOrPut(modId) { TreeMap() }[goalSimpleName] = cacheKey
    }

    /**
     * Phase 3: record a goal that produced a `// TODO LLM:` stub. [reason]
     * is one of "cache miss; run :translate --with-llm", "cached refusal",
     * "model self-rated 0.65 < 0.8", etc. — see [com.tweeks.translator.java.llm.ConfidenceGate].
     */
    fun recordEntityGoalLlmStub(modId: String, goalSimpleName: String, reason: String) {
        entityGoalsLlmStubbed.getOrPut(modId) { TreeMap() }[goalSimpleName] = reason
    }

    /**
     * Record a custom item class whose behavior overrides (e.g.
     * `postHurtEnemy`, `useOn`) cannot be translated by Phase 2's
     * deterministic analyzer.
     */
    fun recordItemCustomBehavior(modId: String, itemId: String, summary: String) {
        itemCustomBehavior.getOrPut(modId) { TreeMap() }[itemId] = summary
    }

    /**
     * Record an item whose Java client model is a `minecraft:select`
     * (component-keyed model, e.g. a data-component-driven blade/dye
     * color) — Phase 2 doesn't translate the selector itself, so the item
     * was given a static icon instead (see [summary] for which one).
     */
    fun recordItemModelSelectorStatic(modId: String, itemId: String, summary: String) {
        itemModelSelectorStatic.getOrPut(modId) { TreeMap() }[itemId] = summary
    }

    /**
     * Record an item attachable whose conventional `textures/entity/<id>`
     * texture doesn't exist in the output tree, either because Phase 2
     * substituted the item's icon texture instead ([summary] says which),
     * or — if no fallback texture exists either — because the 3D held-item
     * view will render with a missing texture in Bedrock.
     */
    fun recordAttachableTextureFallback(modId: String, itemId: String, summary: String) {
        attachableTextureFallback.getOrPut(modId) { TreeMap() }[itemId] = summary
    }

    /**
     * Record a spawn-egg item that received hardcoded base/overlay
     * colors because the Java side computes them at runtime.
     */
    fun recordSpawnEggColorsHardcoded(modId: String, itemId: String, summary: String) {
        spawnEggColorsHardcoded.getOrPut(modId) { TreeMap() }[itemId] = summary
    }

    /**
     * Record an entity whose Java renderer's texture/geometry mapping
     * couldn't be determined statically — the reviewer should verify the
     * heuristic guess.
     */
    fun recordRenderControllerAmbiguous(modId: String, entityId: String, summary: String) {
        renderControllerAmbiguous.getOrPut(modId) { TreeMap() }[entityId] = summary
    }

    /**
     * Record an entity attribute whose value the analyzer could not fold to a
     * literal (neither a numeric literal nor a resolvable `static final`
     * constant — e.g. a runtime-computed value). [entityName] is the simple
     * class name; [summary] describes the attribute and its unresolved value.
     */
    fun recordEntityAttributeUnresolved(modId: String, entityName: String, summary: String) {
        entityAttributesUnresolved
            .getOrPut(modId) { TreeMap() }
            .getOrPut(entityName) { TreeSet() }
            .add(summary)
    }

    /**
     * Record a datapack worldgen structure (a JSON file under
     * `data/<mod>/worldgen/structure/`, driven by a Java `Structure`/
     * `StructurePiece` pair via datagen) that has
     * no Bedrock representation. Bedrock has no equivalent of Java's
     * procedural structure-placement API, so these structures — and whatever
     * they place (loot, mob spawns) beyond a plain chest loot table — simply
     * don't generate on the Bedrock side. [structureId] is the structure's
     * resource-path (namespace stripped), e.g. `escape_pod` or `sub/nested`.
     */
    fun recordDatapackWorldgenStructure(modId: String, structureId: String, summary: String) {
        datapackWorldgenStructures.getOrPut(modId) { TreeMap() }[structureId] = summary
    }

    /**
     * Record a Phase 2 analyzer that threw on this mod. The CLI catches
     * the exception so other mods continue to translate; this entry
     * tells the user what went wrong.
     */
    fun recordPhase2Failure(modId: String, summary: String) {
        phase2Failures.getOrPut(modId) { mutableListOf() }.add(summary)
    }

    /**
     * Record an AI goal whose Bedrock behavior component collided with another
     * goal mapping to the same component name. The first (higher-priority)
     * goal wins; the rest are dropped from the entity JSON. [entityName] is
     * the simple class name, [componentName] is the Bedrock behavior key,
     * and [droppedGoalDescription] identifies the dropped goal (e.g.
     * "priority 7: RandomStrollGoal").
     */
    fun recordDuplicateBehaviorComponent(
        modId: String,
        entityName: String,
        componentName: String,
        droppedGoalDescription: String,
    ) {
        duplicateBehaviorComponents
            .getOrPut(modId) { TreeMap() }
            .getOrPut(entityName) { TreeSet() }
            .add("$componentName ← $droppedGoalDescription")
    }

    /** Set of mod ids that have at least one recorded finding. */
    fun modsWithFindings(): Set<String> {
        val ids = TreeSet<String>()
        ids.addAll(recipeCategoryDropped.keys)
        ids.addAll(recipeTypeSkipped.keys)
        ids.addAll(lootRandomSequenceDropped.keys)
        ids.addAll(soundSubtitleDropped.keys)
        ids.addAll(vanillaSoundPaths.keys)
        ids.addAll(textureCategoriesSkipped.keys)
        ids.addAll(bbmodelFaceUv.keys)
        ids.addAll(bbmodelLocatorSkipped.keys)
        ids.addAll(bbmodelElementTypeSkipped.keys)
        ids.addAll(bbmodelMultiDataPoint.keys)
        ids.addAll(bbmodelNonLinearInterp.keys)
        ids.addAll(bbmodelFlipYUnset.keys)
        ids.addAll(javaParseErrors.keys)
        ids.addAll(entityGoalsDeferred.keys)
        ids.addAll(entityGoalsGatedOff.keys)
        ids.addAll(entityGoalsLlmTranslated.keys)
        ids.addAll(entityGoalsLlmStubbed.keys)
        ids.addAll(itemCustomBehavior.keys)
        ids.addAll(itemModelSelectorStatic.keys)
        ids.addAll(attachableTextureFallback.keys)
        ids.addAll(spawnEggColorsHardcoded.keys)
        ids.addAll(renderControllerAmbiguous.keys)
        ids.addAll(entityAttributesUnresolved.keys)
        ids.addAll(datapackWorldgenStructures.keys)
        ids.addAll(phase2Failures.keys)
        ids.addAll(duplicateBehaviorComponents.keys)
        return ids
    }

    /**
     * Write `<outputRoot>/<modId>/UNTRANSLATABLE.md` for this mod. Always
     * writes — even if there were no findings — so the report's existence is
     * a stable contract per the design spec.
     */
    fun writeFor(modId: String, outputRoot: Path) {
        val report = renderReport(modId)
        val out = outputRoot.resolve("$modId/UNTRANSLATABLE.md")
        out.parent?.createDirectories()
        out.writeText(report)
    }

    /** Public for tests: produce the rendered Markdown for one mod. */
    fun renderReport(modId: String): String {
        val sb = StringBuilder()
        sb.append("# UNTRANSLATABLE — ").append(modId).append('\n')
        sb.append('\n')
        sb.append("Auto-generated by the translator. Lists Java→Bedrock content losses for **")
        sb.append(modId).append("**.\n\n")

        var any = false
        recipeCategoryDropped[modId]?.takeIf { it.isNotEmpty() }?.let { recipes ->
            any = true
            sb.append("## Recipe `category` field dropped\n\n")
            sb.append("Bedrock recipes do not accept Java's datagen-only `category` hint; the field is dropped silently.\n\n")
            for (r in recipes) sb.append("- `").append(r).append("`\n")
            sb.append('\n')
        }
        recipeTypeSkipped[modId]?.takeIf { it.isNotEmpty() }?.let { skipped ->
            any = true
            sb.append("## Recipe types not yet supported\n\n")
            for ((name, type) in skipped) {
                sb.append("- `").append(name).append("`: type `").append(type).append("`\n")
            }
            sb.append('\n')
        }
        lootRandomSequenceDropped[modId]?.takeIf { it.isNotEmpty() }?.let { tables ->
            any = true
            sb.append("## Loot table `random_sequence` dropped\n\n")
            sb.append("Bedrock has no equivalent of Java's `random_sequence` field; loot rolls use the level RNG instead.\n\n")
            for (t in tables) sb.append("- `").append(t).append("`\n")
            sb.append('\n')
        }
        soundSubtitleDropped[modId]?.takeIf { it.isNotEmpty() }?.let { subs ->
            any = true
            sb.append("## Sound `subtitle` dropped\n\n")
            sb.append("Bedrock's subtitle system is structurally different (per-locale text vs. a translation key).\n")
            sb.append("Subtitles for these events were dropped:\n\n")
            for (s in subs) sb.append("- `").append(s).append("`\n")
            sb.append('\n')
        }
        vanillaSoundPaths[modId]?.takeIf { it.isNotEmpty() }?.let { mappings ->
            any = true
            sb.append("## Vanilla sound paths translated approximately\n\n")
            sb.append("Java→Bedrock vanilla-sound path mapping is best-effort for Phase 1a (the translator does not yet ship the full Bedrock sound table). Verify in-game and adjust if a sound is silent or wrong:\n\n")
            for ((java, bedrock) in mappings) {
                sb.append("- `").append(java).append("` → `").append(bedrock).append("`\n")
            }
            sb.append('\n')
        }
        textureCategoriesSkipped[modId]?.takeIf { it.isNotEmpty() }?.let { categories ->
            any = true
            sb.append("## Texture categories skipped\n\n")
            sb.append("These top-level categories under `assets/").append(modId)
                .append("/textures/` are not mapped to Bedrock locations by the translator. Files inside were not copied:\n\n")
            for (c in categories) sb.append("- `").append(c).append("`\n")
            sb.append('\n')
        }
        bbmodelFaceUv[modId]?.takeIf { it.isNotEmpty() }?.let { items ->
            any = true
            sb.append("## Bbmodel face-UV translated approximately\n\n")
            sb.append("These cubes use Blockbench's per-face UV mode. Bedrock's per-face UV is structurally similar, but the face uv-size is reconstructed as a best-effort `[x2 - x1, y2 - y1]` from Blockbench's `[x1, y1, x2, y2]` rectangle. Verify visually:\n\n")
            for (i in items) sb.append("- `").append(i).append("`\n")
            sb.append('\n')
        }
        bbmodelLocatorSkipped[modId]?.takeIf { it.isNotEmpty() }?.let { items ->
            any = true
            sb.append("## Bbmodel locator nodes skipped\n\n")
            sb.append("Locator nodes are not yet translated to Bedrock locators (Phase 1b out of scope):\n\n")
            for (i in items) sb.append("- `").append(i).append("`\n")
            sb.append('\n')
        }
        bbmodelElementTypeSkipped[modId]?.takeIf { it.isNotEmpty() }?.let { items ->
            any = true
            sb.append("## Bbmodel element types not supported\n\n")
            sb.append("Only cube elements are translated. These elements were skipped:\n\n")
            for ((loc, type) in items) sb.append("- `").append(loc).append("`: type `").append(type).append("`\n")
            sb.append('\n')
        }
        bbmodelMultiDataPoint[modId]?.takeIf { it.isNotEmpty() }?.let { items ->
            any = true
            sb.append("## Bbmodel multi-data-point keyframes\n\n")
            sb.append("These keyframes carry multiple `data_points`. Phase 1b takes the first one and ignores the rest:\n\n")
            for (i in items) sb.append("- `").append(i).append("`\n")
            sb.append('\n')
        }
        bbmodelNonLinearInterp[modId]?.takeIf { it.isNotEmpty() }?.let { items ->
            any = true
            sb.append("## Bbmodel non-linear interpolation downgraded\n\n")
            sb.append("Phase 1b emits keyframes without explicit interpolation hints (treated as linear). These keyframes used a non-linear curve in the source:\n\n")
            for ((loc, interp) in items) sb.append("- `").append(loc).append("`: `").append(interp).append("`\n")
            sb.append('\n')
        }
        bbmodelFlipYUnset[modId]?.takeIf { it.isNotEmpty() }?.let { items ->
            any = true
            sb.append("## Bbmodel authored Y-down — verify alignment in-game\n\n")
            sb.append("These bbmodels declare `meta.modded_entity_flip_y: false`. The Y axis was not flipped at authoring time, so vertical alignment in Bedrock may not match the Java mod. Spawn each entity in-game and confirm pivots line up:\n\n")
            for (i in items) sb.append("- `").append(i).append("`\n")
            sb.append('\n')
        }
        javaParseErrors[modId]?.takeIf { it.isNotEmpty() }?.let { items ->
            any = true
            sb.append("## Java source files JavaParser could not parse\n\n")
            sb.append("These files were skipped by the Java pipeline. Phase 2 analyses (entity attributes, AI goals, item logic) will not see them:\n\n")
            for ((f, err) in items) sb.append("- `").append(f).append("`: ").append(err).append('\n')
            sb.append('\n')
        }
        // Phase 3: split the old "deferred" section into three outcome buckets.
        // The deferred-records map is still populated by Phase 2b's GoalMatcher
        // (it's how we discover goals to feed the LLM gate), but the rendered
        // report reflects the Phase 3 outcome: cache-hit translation, TODO
        // stub from the LLM stage, or pure-Low manual stub.
        entityGoalsLlmTranslated[modId]?.takeIf { it.isNotEmpty() }?.let { goals ->
            any = true
            sb.append("## Entity goals translated by LLM (cache hit)\n\n")
            sb.append("These goals were translated from Java to Bedrock JS via the Phase 3 LLM stage. The committed JS lives at `behavior_pack/scripts/goals/<GoalClass>.ts`. Cache key is shown for debugging — see `translator/.cache/llm/<key>.json`:\n\n")
            for ((goalSimpleName, cacheKey) in goals) {
                sb.append("- `").append(goalSimpleName).append("` — cache key `").append(cacheKey).append("`\n")
            }
            sb.append('\n')
        }
        entityGoalsLlmStubbed[modId]?.takeIf { it.isNotEmpty() }?.let { goals ->
            any = true
            sb.append("## Entity goals stubbed for LLM (cache miss; run :translate --with-llm to translate)\n\n")
            sb.append("These goals produced a `// TODO LLM:` stub at `behavior_pack/scripts/goals/<GoalClass>.ts`. Either re-run with `--with-llm` (and `ANTHROPIC_API_KEY` set) to fill them in, or hand-translate them:\n\n")
            for ((goalSimpleName, reason) in goals) {
                sb.append("- `").append(goalSimpleName).append("` — ").append(reason).append('\n')
            }
            sb.append('\n')
        }
        // Render the raw Phase 2b deferred-records list only for goals NOT
        // already covered above (i.e. Low-bucket goals that Phase 2b classified
        // as manual-JS). Goals already in `entityGoalsLlmTranslated` or
        // `entityGoalsLlmStubbed` would otherwise double-list.
        entityGoalsDeferred[modId]?.takeIf { it.isNotEmpty() }?.let { entities ->
            val lowOnlyEntities = entities.mapValues { (_, goals) ->
                goals.filter { (_, deferral) -> deferral.bucket == GoalBucket.LOW }
            }.filterValues { it.isNotEmpty() }
            // Also include Medium-bucket goals that the Phase 3 stage didn't
            // process (e.g. when EntityAnalyzer hasn't been wired to the gate
            // yet, in older test fixtures).
            val handledNames = (entityGoalsLlmTranslated[modId]?.keys ?: emptySet()) +
                (entityGoalsLlmStubbed[modId]?.keys ?: emptySet())
            val unhandled = entities.mapValues { (_, goals) ->
                goals.filter { (key, deferral) ->
                    // The deferred key format is `<priority>:<fqn>` (priority
                    // optional). Split once on ':' and take the FQN portion;
                    // its simple name is the LLM-stage key. Known limitation:
                    // two goals in different packages with the same simple
                    // name (e.g. `a.b.Foo` and `c.d.Foo`) will be treated as
                    // a collision and one will be filtered out here. That's
                    // fine for the current corpus — Bedrock script paths
                    // would collide anyway.
                    val fqn = key.substringAfter(':', missingDelimiterValue = key)
                    val simpleName = fqn.substringAfterLast('.')
                    deferral.bucket == GoalBucket.MEDIUM && simpleName !in handledNames
                }
            }.filterValues { it.isNotEmpty() }

            if (lowOnlyEntities.isNotEmpty()) {
                any = true
                sb.append("## Entity goals stubbed for human (Low bucket)\n\n")
                sb.append("These goals were classified Low — novel logic that Phase 3's LLM stage doesn't attempt to translate. Hand-write the Bedrock equivalent:\n\n")
                for ((entityName, goals) in lowOnlyEntities) {
                    sb.append("### `").append(entityName).append("`\n\n")
                    for ((goalKey, deferral) in goals) {
                        sb.append("- `").append(goalKey).append("` — ").append(deferral.reason).append('\n')
                        if (!deferral.sourceExcerpt.isNullOrBlank()) {
                            sb.append("    ```java\n")
                            for (line in deferral.sourceExcerpt.lines()) {
                                sb.append("    ").append(line).append('\n')
                            }
                            sb.append("    ```\n")
                        }
                    }
                    sb.append('\n')
                }
            }
            if (unhandled.isNotEmpty()) {
                any = true
                sb.append("## Entity goals deferred (Phase 3 LLM stage not run)\n\n")
                sb.append("Phase 2b classified these as Medium bucket but the Phase 3 LLM stage didn't process them — typically the analyzer was invoked without a `ConfidenceGate`. Re-run `:translate` to pick them up:\n\n")
                for ((entityName, goals) in unhandled) {
                    sb.append("### `").append(entityName).append("`\n\n")
                    for ((goalKey, deferral) in goals) {
                        sb.append("- `").append(goalKey).append("` — ").append(deferral.reason).append('\n')
                        if (!deferral.sourceExcerpt.isNullOrBlank()) {
                            sb.append("    ```java\n")
                            for (line in deferral.sourceExcerpt.lines()) {
                                sb.append("    ").append(line).append('\n')
                            }
                            sb.append("    ```\n")
                        }
                    }
                    sb.append('\n')
                }
            }
        }
        entityGoalsGatedOff[modId]?.takeIf { it.isNotEmpty() }?.let { entities ->
            any = true
            sb.append("## Entity goals gated off per-entity\n\n")
            sb.append("These goals are registered on a shared superclass but gated by a per-entity ")
            sb.append("condition (an anonymous `canUse` override). For the entities below the gate ")
            sb.append("resolved statically closed (or could not be resolved), so the goal was dropped ")
            sb.append("from their Bedrock JSON — for a closed gate this matches the Java behavior, not a loss:\n\n")
            for ((entityName, summaries) in entities) {
                sb.append("### `").append(entityName).append("`\n\n")
                for (s in summaries) sb.append("- ").append(s).append('\n')
                sb.append('\n')
            }
        }
        itemCustomBehavior[modId]?.takeIf { it.isNotEmpty() }?.let { items ->
            any = true
            sb.append("## Item custom behavior\n\n")
            sb.append("These items override `Item` methods (e.g. `postHurtEnemy`, `useOn`, `hurtEnemy`) with custom logic. Phase 3 (LLM stage) translates these to `behavior_pack/scripts/items/*.ts` event handlers; Phase 2 only emits the static item JSON:\n\n")
            for ((itemId, summary) in items) sb.append("- `").append(itemId).append("`: ").append(summary).append('\n')
            sb.append('\n')
        }
        itemModelSelectorStatic[modId]?.takeIf { it.isNotEmpty() }?.let { items ->
            any = true
            sb.append("## Item model selector not translatable — static icon used\n\n")
            sb.append("These items use a `minecraft:select` component-keyed client model (e.g. a data-component-driven color). ")
            sb.append("Phase 2 does not translate the selector; the item was instead given a fixed icon so it isn't a broken/missing texture reference in Bedrock. ")
            sb.append("If the selected color/state matters in-game, hand-write the swap via Phase 3 scripting:\n\n")
            for ((itemId, summary) in items) sb.append("- `").append(itemId).append("`: ").append(summary).append('\n')
            sb.append('\n')
        }
        attachableTextureFallback[modId]?.takeIf { it.isNotEmpty() }?.let { items ->
            any = true
            sb.append("## Attachable held-item texture substituted or missing\n\n")
            sb.append("These items have a `resource_pack/attachables/<id>.json` (from a `.bbmodel`) whose conventional " +
                "`textures/entity/<id>` texture doesn't exist in the output. Verify the 3D held-item view in-game:\n\n")
            for ((itemId, summary) in items) sb.append("- `").append(itemId).append("`: ").append(summary).append('\n')
            sb.append('\n')
        }
        spawnEggColorsHardcoded[modId]?.takeIf { it.isNotEmpty() }?.let { items ->
            any = true
            sb.append("## Spawn egg colors hardcoded\n\n")
            sb.append("These spawn eggs received default base/overlay colors because the Java side computes them at runtime via `EntityType.Builder` defaults. Hand-tune per the source mod's mob palette if the colors look wrong in-game:\n\n")
            for ((itemId, summary) in items) sb.append("- `").append(itemId).append("`: ").append(summary).append('\n')
            sb.append('\n')
        }
        renderControllerAmbiguous[modId]?.takeIf { it.isNotEmpty() }?.let { items ->
            any = true
            sb.append("## Render-controller texture mapping ambiguous\n\n")
            sb.append("The Java entity renderer is too complex to parse statically. Phase 2 fell back to a heuristic geometry/texture name. Verify visually in-game and adjust the emitted `<entity_id>.entity.json` if wrong:\n\n")
            for ((entityId, summary) in items) sb.append("- `").append(entityId).append("`: ").append(summary).append('\n')
            sb.append('\n')
        }
        duplicateBehaviorComponents[modId]?.takeIf { it.isNotEmpty() }?.let { entities ->
            any = true
            sb.append("## Duplicate Bedrock behavior components dropped\n\n")
            sb.append("Multiple Java goals mapped to the same Bedrock behavior component name. ")
            sb.append("Bedrock entity JSON requires unique component keys, so only the highest-priority goal (lowest priority value) is emitted. ")
            sb.append("The dropped goals are listed here for review — hand-translate them into a script if their behavior is meaningful:\n\n")
            for ((entityName, drops) in entities) {
                sb.append("### `").append(entityName).append("`\n\n")
                for (d in drops) sb.append("- ").append(d).append('\n')
                sb.append('\n')
            }
        }
        entityAttributesUnresolved[modId]?.takeIf { it.isNotEmpty() }?.let { entities ->
            any = true
            sb.append("## Entity attributes not resolved to literals\n\n")
            sb.append("These `createAttributes()` values are neither numeric literals nor ")
            sb.append("resolvable `static final` constants (e.g. computed at runtime). ")
            sb.append("The attribute was dropped from the Bedrock entity JSON — set it by hand if it matters:\n\n")
            for ((entityName, attrs) in entities) {
                sb.append("### `").append(entityName).append("`\n\n")
                for (a in attrs) sb.append("- ").append(a).append('\n')
                sb.append('\n')
            }
        }
        datapackWorldgenStructures[modId]?.takeIf { it.isNotEmpty() }?.let { items ->
            any = true
            sb.append("## Datapack worldgen structures not translatable\n\n")
            sb.append("Bedrock has no equivalent of Java's procedural `Structure`/`StructurePiece` datapack API — ")
            sb.append("there is no `.nbt`/jigsaw template for these on disk, so there is nothing to translate. ")
            sb.append("These structures (and the biome placement / spacing that controls where they generate) do not ")
            sb.append("appear in Bedrock worldgen; any loot table they place is translated separately and listed elsewhere in this report:\n\n")
            for ((structureId, summary) in items) {
                sb.append("- `").append(structureId).append("`: ").append(summary).append('\n')
            }
            sb.append('\n')
        }
        phase2Failures[modId]?.takeIf { it.isNotEmpty() }?.let { summaries ->
            any = true
            sb.append("## Phase 2 analyzer failure\n\n")
            sb.append("A Phase 2 analyzer (entity / item) threw on this mod. Other mods still translated. Stack-trace summary:\n\n")
            for (summary in summaries) {
                sb.append("```\n").append(summary).append("\n```\n\n")
            }
        }

        if (!any) {
            sb.append("No translation losses recorded. Everything translated cleanly for this mod.\n")
        }
        return sb.toString()
    }
}
