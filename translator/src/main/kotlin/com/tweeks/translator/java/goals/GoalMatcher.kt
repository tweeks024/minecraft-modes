package com.tweeks.translator.java.goals

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.CharLiteralExpr
import com.github.javaparser.ast.expr.ClassExpr
import com.github.javaparser.ast.expr.DoubleLiteralExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LongLiteralExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.expr.SuperExpr
import com.github.javaparser.ast.expr.UnaryExpr
import com.github.javaparser.ast.expr.ThisExpr
import com.tweeks.translator.emit.Untranslatable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Inspects an entity class's `registerGoals()` method and produces:
 *   - a list of Bedrock `minecraft:behavior.*` components (one per High-bucket
 *     goal), each tagged with the goal's `addGoal(priority, ...)` value;
 *   - an [Untranslatable] log entry for every Medium / Low goal (catalog miss
 *     or non-literal args).
 *
 * Per the Phase 2b spec, this only emits the High bucket. Phase 3's LLM
 * stage walks `entityGoalsDeferred` to pick up the rest.
 */
internal class GoalMatcher(private val unt: Untranslatable) {

    /** One emitted Bedrock behavior. */
    data class MatchedComponent(
        val priority: Int,
        val componentName: String,
        val body: JsonObject,
    )

    /**
     * One Medium-bucket goal that Phase 3's LLM stage should pick up. Carries
     * the goal class name + FQN so EntityAnalyzer can find the matching
     * source file and build a [com.tweeks.translator.java.llm.TranslationPrompt.GoalContext].
     */
    data class DeferredGoal(
        val priority: Int,
        val goalFqn: String,
        val goalSimpleName: String,
        /** The full `addGoal(...)` call site, for context. */
        val callSiteSource: String,
    )

    data class Result(
        val components: List<MatchedComponent>,
        /**
         * Phase 2b populated this only as a side-effect on Untranslatable.
         * Phase 3 needs the typed list so EntityAnalyzer can route each one
         * through the LLM confidence gate.
         */
        val deferred: List<DeferredGoal> = emptyList(),
    )

    /**
     * Analyze [entity]'s AI goals, walking same-module superclasses.
     *
     * [superclassLookup] maps a simple class name to its declaration if it
     * lives in the same module (EntityAnalyzer passes the module's parsed
     * classes). It returns null for classes outside the module (vanilla
     * `PathfinderMob`, `Monster`, …), which is where the walk stops.
     *
     * Merge semantics mirror Java's override rules:
     *   - A class with no `registerGoals()` inherits its superclass's goals
     *     wholesale (walk into the superclass).
     *   - A class that overrides `registerGoals()` uses its own goals; it
     *     additionally merges the superclass's goals **only if** the override
     *     calls `super.registerGoals()` (the wildwest HerobrineEntity /
     *     starwars DarthVader pattern). Without a super-call the override
     *     fully replaces the inherited goals.
     */
    fun match(
        modId: String,
        entity: ClassOrInterfaceDeclaration,
        superclassLookup: (String) -> ClassOrInterfaceDeclaration? = { null },
    ): Result {
        val components = mutableListOf<MatchedComponent>()
        val deferred = mutableListOf<DeferredGoal>()
        collectGoals(modId, entity, superclassLookup, components, deferred, mutableSetOf())
        return Result(components, deferred)
    }

    /** Recursively gather goals from [entity] and (per merge rules) its superclasses. */
    private fun collectGoals(
        modId: String,
        entity: ClassOrInterfaceDeclaration,
        superclassLookup: (String) -> ClassOrInterfaceDeclaration?,
        components: MutableList<MatchedComponent>,
        deferred: MutableList<DeferredGoal>,
        visited: MutableSet<String>,
    ) {
        // Guard against a cyclic extends chain (shouldn't happen in valid
        // Java, but a malformed source shouldn't loop forever).
        if (!visited.add(entity.nameAsString)) return

        val registerGoals = entity.methods.firstOrNull { it.nameAsString == "registerGoals" }
        if (registerGoals == null) {
            // No override: goals are inherited wholesale from the superclass.
            walkSuperclasses(modId, entity, superclassLookup, components, deferred, visited)
            return
        }

        for (call in registerGoals.findAll(MethodCallExpr::class.java)) {
            val outcome = processAddGoalCall(modId, entity, call) ?: continue
            when (outcome) {
                is Outcome.Component -> components.add(outcome.component)
                is Outcome.Deferred -> deferred.add(outcome.goal)
            }
        }
        // Only merge the superclass's goals when the override chains up.
        if (callsSuperRegisterGoals(registerGoals)) {
            walkSuperclasses(modId, entity, superclassLookup, components, deferred, visited)
        }
    }

    private fun walkSuperclasses(
        modId: String,
        entity: ClassOrInterfaceDeclaration,
        superclassLookup: (String) -> ClassOrInterfaceDeclaration?,
        components: MutableList<MatchedComponent>,
        deferred: MutableList<DeferredGoal>,
        visited: MutableSet<String>,
    ) {
        for (extended in entity.extendedTypes) {
            val superDecl = superclassLookup(extended.nameAsString) ?: continue
            collectGoals(modId, superDecl, superclassLookup, components, deferred, visited)
        }
    }

    /** True if [method] contains a `super.registerGoals()` call. */
    private fun callsSuperRegisterGoals(method: MethodDeclaration): Boolean {
        return method.findAll(MethodCallExpr::class.java).any {
            it.nameAsString == "registerGoals" && it.scope.orElse(null) is SuperExpr
        }
    }

    /** Distinguish "matched a vanilla goal" from "logged a Medium-bucket deferral". */
    private sealed class Outcome {
        data class Component(val component: MatchedComponent) : Outcome()
        data class Deferred(val goal: DeferredGoal) : Outcome()
    }

    /**
     * Process one `addGoal(priority, ...)` call. Returns the matched
     * Bedrock component on success, or null on any of:
     *   - the call isn't an `addGoal` on `goalSelector` / `targetSelector`;
     *   - the goal class is non-vanilla (Medium/Low) — logged.
     *   - the goal class is vanilla but its argMapper returned null — logged.
     */
    private fun processAddGoalCall(
        modId: String,
        entity: ClassOrInterfaceDeclaration,
        call: MethodCallExpr,
    ): Outcome? {
        if (call.nameAsString != "addGoal") return null
        // We only care about `this.goalSelector.addGoal(...)` and
        // `this.targetSelector.addGoal(...)`.
        val scope = call.scope.orElse(null) ?: return null
        val scopeName = when (scope) {
            is FieldAccessExpr -> scope.nameAsString
            is NameExpr -> scope.nameAsString
            else -> return null
        }
        if (scopeName != "goalSelector" && scopeName != "targetSelector") return null

        // Args: addGoal(priority, goalCtor)
        if (call.arguments.size != 2) return null
        val priority = readIntLiteral(call.arguments[0])
        if (priority == null) {
            logDeferral(modId, entity, call, "non-literal priority", Untranslatable.GoalBucket.MEDIUM)
            return null
        }
        val goalExpr = call.arguments[1]
        val ctor = goalExpr as? ObjectCreationExpr
        if (ctor == null) {
            logDeferral(modId, entity, call, "non-constructor goal expression", Untranslatable.GoalBucket.LOW)
            return null
        }

        val fqn = resolveFqn(ctor)
        if (fqn == null) {
            logDeferral(modId, entity, call, "could not resolve goal class FQN", Untranslatable.GoalBucket.MEDIUM)
            return null
        }

        val mapping = VanillaGoalCatalog.lookup(fqn)
        if (mapping == null) {
            val bucket = bucketFor(modId, fqn, entity)
            logDeferral(
                modId, entity, call,
                reason = "catalog miss for $fqn",
                bucket = bucket,
                fqnOverride = fqn,
                priority = priority,
            )
            return if (bucket == Untranslatable.GoalBucket.MEDIUM) {
                Outcome.Deferred(DeferredGoal(priority, fqn, fqn.substringAfterLast('.'), call.toString()))
            } else null
        }

        val literals = ctor.arguments.map { argToLiteral(it) }
        if (literals.any { it == null }) {
            logDeferral(
                modId, entity, call,
                reason = "non-literal argument to $fqn",
                bucket = Untranslatable.GoalBucket.MEDIUM,
                fqnOverride = fqn,
                priority = priority,
            )
            return Outcome.Deferred(DeferredGoal(priority, fqn, fqn.substringAfterLast('.'), call.toString()))
        }

        @Suppress("UNCHECKED_CAST")
        val body = mapping.argMapper(literals as List<VanillaGoalCatalog.LiteralArg>)
        if (body == null) {
            logDeferral(
                modId, entity, call,
                reason = "$fqn has no clean Bedrock 1.21.0 equivalent",
                bucket = Untranslatable.GoalBucket.MEDIUM,
                fqnOverride = fqn,
                priority = priority,
            )
            return Outcome.Deferred(DeferredGoal(priority, fqn, fqn.substringAfterLast('.'), call.toString()))
        }

        return Outcome.Component(
            MatchedComponent(
                priority = priority,
                componentName = mapping.bedrockComponent,
                body = body,
            )
        )
    }

    /**
     * Heuristic Medium-vs-Low classification for a *non-vanilla* goal class.
     * Medium = looks like a vanilla goal extension or composition of
     * recognizable primitives. Low = touches NBT serialization, IO, maps/sets,
     * or other novel state machinery.
     */
    private fun bucketFor(
        modId: String,
        goalFqn: String,
        @Suppress("UNUSED_PARAMETER") entity: ClassOrInterfaceDeclaration,
    ): Untranslatable.GoalBucket {
        // Vanilla goal classes — but unmapped — are Medium by definition.
        if (goalFqn.startsWith("net.minecraft.world.entity.ai.goal.")) {
            return Untranslatable.GoalBucket.MEDIUM
        }
        // Custom classes: try to find the source. If we can find it and it
        // looks novel (HashMap/HashSet/IO), Low; otherwise Medium.
        // Without cross-mod source resolution at this layer, default Medium —
        // Phase 3's LLM will read the source anyway and re-bucket.
        //
        // TODO(phase 4): once the source-resolution layer (cf. design spec
        // section "Cross-mod source resolution") is wired through to this
        // function, inspect the goal class body for novel-state markers
        // (NBT serialization, IO, persistent maps, scheduler/calendar
        // primitives) and return LOW for those. Adding heuristics here
        // without source resolution would be speculative and could mis-
        // classify well-known patterns; we prefer to keep MEDIUM as the
        // safe default and let the LLM stage handle bucket refinement.
        return Untranslatable.GoalBucket.MEDIUM
    }

    private fun logDeferral(
        modId: String,
        entity: ClassOrInterfaceDeclaration,
        call: MethodCallExpr,
        reason: String,
        bucket: Untranslatable.GoalBucket,
        fqnOverride: String? = null,
        priority: Int? = null,
    ) {
        val key = buildString {
            if (priority != null) {
                append(priority).append(":")
            }
            append(fqnOverride ?: extractCtorName(call) ?: "<unknown>")
        }
        unt.recordEntityGoalDeferred(
            modId = modId,
            entityName = entity.nameAsString,
            goalKey = key,
            bucket = bucket,
            reason = reason,
            sourceExcerpt = call.toString(),
        )
    }

    private fun extractCtorName(call: MethodCallExpr): String? {
        val ctor = call.arguments.lastOrNull() as? ObjectCreationExpr ?: return null
        return ctor.type.nameAsString
    }

    private fun readIntLiteral(expr: Expression): Int? {
        return when (expr) {
            is IntegerLiteralExpr -> expr.asNumber().toInt()
            is UnaryExpr -> {
                val inner = readIntLiteral(expr.expression) ?: return null
                when (expr.operator) {
                    UnaryExpr.Operator.MINUS -> -inner
                    UnaryExpr.Operator.PLUS -> inner
                    else -> null
                }
            }
            else -> null
        }
    }

    /**
     * Resolve the FQN of a constructor expression. Tries (in order):
     *   1. JavaParser's symbol-resolver (gives a fully-qualified name).
     *   2. The source-text package-qualifier on the `new` expression itself
     *      (`new com.tweeks.foo.MyGoal(...)`).
     *   3. The compilation unit's imports — if `MyGoal` is imported, return
     *      that import's FQN. Wildwest uses `import …NearestAttackableTargetGoal;`
     *      and constructs `new NearestAttackableTargetGoal<>(...)` with the
     *      diamond; without this fallback resolveFqn returns null and the
     *      goal is logged as `<unknown>`.
     */
    private fun resolveFqn(ctor: ObjectCreationExpr): String? {
        val resolved = try {
            ctor.resolve().declaringType().qualifiedName
        } catch (e: Throwable) {
            null
        }
        if (resolved != null) return resolved

        val typeName = ctor.type
        val qualifier = typeName.scope.orElse(null)
        if (qualifier != null) return "${qualifier}.${typeName.nameAsString}"

        // Fallback: walk the compilation unit's imports for one whose simple
        // name matches. Java forbids two type imports with the same simple
        // name, so the first match (if any) is unambiguous.
        val simple = typeName.nameAsString
        val cu = ctor.findCompilationUnit().orElse(null) ?: return null
        for (imp in cu.imports) {
            if (imp.isAsterisk) continue
            val importName = imp.nameAsString
            if (importName.substringAfterLast('.') == simple) return importName
        }
        return null
    }

    private fun argToLiteral(expr: Expression): VanillaGoalCatalog.LiteralArg? {
        return when (expr) {
            is IntegerLiteralExpr -> VanillaGoalCatalog.LiteralArg.IntArg(expr.asNumber().toInt())
            is LongLiteralExpr -> {
                // Bedrock's behavior-component args are 32-bit ints. If a
                // Java goal passed a `long` literal that fits in Int range
                // we narrow it; otherwise return null so the goal is
                // deferred rather than silently truncated to a wrong value
                // (e.g. 5_000_000_000L would become 705032704).
                val v = expr.asNumber().toLong()
                if (v in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
                    VanillaGoalCatalog.LiteralArg.IntArg(v.toInt())
                } else {
                    null
                }
            }
            is DoubleLiteralExpr -> VanillaGoalCatalog.LiteralArg.DoubleArg(expr.asDouble())
            is BooleanLiteralExpr -> VanillaGoalCatalog.LiteralArg.BoolArg(expr.value)
            is StringLiteralExpr -> VanillaGoalCatalog.LiteralArg.StringArg(expr.asString())
            is CharLiteralExpr -> VanillaGoalCatalog.LiteralArg.StringArg(expr.asChar().toString())
            is ClassExpr -> VanillaGoalCatalog.LiteralArg.ClassArg(expr.type.toString())
            is ThisExpr -> VanillaGoalCatalog.LiteralArg.StringArg("this")
            is FieldAccessExpr -> {
                // Treat `Foo.BAR.BAZ` as a class arg name — it's the only
                // shape we care about for vanilla constructors that take a
                // `Class<? extends LivingEntity>`.
                VanillaGoalCatalog.LiteralArg.ClassArg(expr.toString())
            }
            is NameExpr -> {
                // A bare identifier — could be a constant. Pretty rare in
                // vanilla goal calls; treat as Medium-bucket trigger.
                null
            }
            is UnaryExpr -> {
                // `-2.4` is parsed as UnaryExpr(MINUS, DoubleLiteralExpr).
                val inner = argToLiteral(expr.expression) ?: return null
                when (expr.operator) {
                    UnaryExpr.Operator.MINUS -> when (inner) {
                        is VanillaGoalCatalog.LiteralArg.IntArg -> VanillaGoalCatalog.LiteralArg.IntArg(-inner.value)
                        is VanillaGoalCatalog.LiteralArg.DoubleArg -> VanillaGoalCatalog.LiteralArg.DoubleArg(-inner.value)
                        else -> null
                    }
                    UnaryExpr.Operator.PLUS -> inner
                    else -> null
                }
            }
            else -> null
        }
    }
}
