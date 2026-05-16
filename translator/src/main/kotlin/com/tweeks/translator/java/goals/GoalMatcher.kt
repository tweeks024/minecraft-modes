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

    fun match(modId: String, entity: ClassOrInterfaceDeclaration): Result {
        val components = mutableListOf<MatchedComponent>()
        val deferred = mutableListOf<DeferredGoal>()
        val registerGoals = entity.methods.firstOrNull { it.nameAsString == "registerGoals" }
            ?: return Result(emptyList(), emptyList())

        for (call in registerGoals.findAll(MethodCallExpr::class.java)) {
            val outcome = processAddGoalCall(modId, entity, call) ?: continue
            when (outcome) {
                is Outcome.Component -> components.add(outcome.component)
                is Outcome.Deferred -> deferred.add(outcome.goal)
            }
        }
        return Result(components, deferred)
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
            is LongLiteralExpr -> VanillaGoalCatalog.LiteralArg.IntArg(expr.asNumber().toInt())
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
