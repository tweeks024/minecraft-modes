package com.tweeks.translator.java.goals

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Catalog of vanilla `net.minecraft.world.entity.ai.goal.*` classes mapped
 * to Bedrock `minecraft:behavior.*` JSON components.
 *
 * Phase 2b only handles the **High** bucket: vanilla classes (or their
 * `target.` counterparts) whose constructor arguments are simple literals.
 * Anything else — extending classes, lambdas, runtime-computed values,
 * goals not in this table — is demoted to Medium/Low for Phase 3.
 *
 * The Bedrock `priority` field is filled in by [GoalMatcher] from the
 * `addGoal(priority, ...)` call site. The mappers leave it absent.
 *
 * Mappings target Bedrock `entity` format version `1.21.0` (pinned in
 * `bedrock-target.json`). When the Bedrock target bumps, spot-check that
 * the `minecraft:behavior.*` schemas haven't changed.
 */
internal object VanillaGoalCatalog {

    /**
     * A single literal constructor argument. The matcher only resolves
     * arg shapes the High bucket cares about — numbers, booleans,
     * strings, class literals; anything else demotes the goal.
     */
    sealed interface LiteralArg {
        data class IntArg(val value: Int) : LiteralArg
        data class DoubleArg(val value: Double) : LiteralArg
        data class BoolArg(val value: Boolean) : LiteralArg
        data class StringArg(val value: String) : LiteralArg
        /** A `Mob.class`-style class literal — captured but rarely used. */
        data class ClassArg(val name: String) : LiteralArg
    }

    /**
     * A catalog entry. [argMapper] receives the literal args extracted
     * from the call site (excluding the implicit `this` first arg) and
     * either produces the component body, or returns null to demote the
     * call to Medium.
     */
    data class GoalMapping(
        val javaFqn: String,
        val bedrockComponent: String,
        val argMapper: (List<LiteralArg>) -> JsonObject?,
    )

    private fun LiteralArg.asDouble(): Double? = when (this) {
        is LiteralArg.DoubleArg -> value
        is LiteralArg.IntArg -> value.toDouble()
        else -> null
    }

    private fun LiteralArg.asFloat(): Float? = when (this) {
        is LiteralArg.DoubleArg -> value.toFloat()
        is LiteralArg.IntArg -> value.toFloat()
        else -> null
    }

    @Suppress("UNUSED")
    private fun LiteralArg.asBool(): Boolean? = (this as? LiteralArg.BoolArg)?.value
    private fun LiteralArg.asInt(): Int? = (this as? LiteralArg.IntArg)?.value

    private fun num(d: Double): JsonPrimitive =
        if (d == d.toLong().toDouble()) JsonPrimitive(d.toLong()) else JsonPrimitive(d)

    private fun num(f: Float): JsonPrimitive = num(f.toDouble())

    /** ---- argMapper helpers (named so we can `return null` cleanly) ---- */

    private fun mapMoveTowardsTarget(args: List<LiteralArg>): JsonObject? {
        if (args.size < 3) return null
        val speed = args[1].asDouble() ?: return null
        val maxDist = args[2].asFloat() ?: return null
        return buildJsonObject {
            put("speed_multiplier", num(speed))
            put("within_radius", num(maxDist))
        }
    }

    private fun mapMoveBackToVillage(args: List<LiteralArg>): JsonObject? {
        if (args.size < 3) return null
        val speed = args[1].asDouble() ?: return null
        return buildJsonObject {
            put("speed_multiplier", num(speed))
            // Bedrock's `move_to_village` uses `goal_radius`. Vanilla default 2.
            put("goal_radius", num(2.0))
        }
    }

    private fun mapStrollInVillage(args: List<LiteralArg>): JsonObject? {
        if (args.size < 2) return null
        val speed = args[1].asDouble() ?: return null
        return buildJsonObject {
            put("speed_multiplier", num(speed))
            put("interval", JsonPrimitive(240))
        }
    }

    private fun mapRandomStroll(args: List<LiteralArg>): JsonObject? {
        if (args.size < 2) return null
        val speed = args[1].asDouble() ?: return null
        return buildJsonObject {
            put("speed_multiplier", num(speed))
            val intervalArg = args.getOrNull(2)?.asInt()
            if (intervalArg != null) put("interval", JsonPrimitive(intervalArg))
        }
    }

    private fun mapLookAtPlayer(args: List<LiteralArg>): JsonObject? {
        if (args.size < 3) return null
        val dist = args[2].asFloat() ?: return null
        return buildJsonObject {
            put("look_distance", num(dist))
            put("probability", JsonPrimitive(0.02))
        }
    }

    private fun mapRandomLookAround(@Suppress("UNUSED_PARAMETER") args: List<LiteralArg>): JsonObject =
        buildJsonObject { }

    private fun mapFloat(@Suppress("UNUSED_PARAMETER") args: List<LiteralArg>): JsonObject =
        buildJsonObject {
            put("sink_with_passengers", JsonPrimitive(true))
        }

    private fun mapPanic(args: List<LiteralArg>): JsonObject? {
        if (args.size < 2) return null
        val speed = args[1].asDouble() ?: return null
        return buildJsonObject {
            put("speed_multiplier", num(speed))
        }
    }

    private fun mapMeleeAttack(args: List<LiteralArg>): JsonObject? {
        if (args.size < 3) return null
        val speed = args[1].asDouble() ?: return null
        return buildJsonObject {
            put("speed_multiplier", num(speed))
            put("track_target", JsonPrimitive(true))
        }
    }

    private fun mapWaterAvoidingStroll(args: List<LiteralArg>): JsonObject? {
        if (args.size < 2) return null
        val speed = args[1].asDouble() ?: return null
        return buildJsonObject {
            put("speed_multiplier", num(speed))
            put("avoid_water", JsonPrimitive(true))
        }
    }

    // Java's DefendVillageTargetGoal targets any non-villager hostile within
    // village bounds; the Bedrock equivalent's `entity_types` field expects a
    // list of `{ filters, max_dist }` filter objects, not an empty container.
    // Emitting `entity_types: {}` triggers a content-error log and a no-op
    // behavior. Omit the field so Bedrock falls back to its default
    // village-defender filter set.
    private fun mapDefendVillage(@Suppress("UNUSED_PARAMETER") args: List<LiteralArg>): JsonObject =
        buildJsonObject {
            put("must_reach", JsonPrimitive(true))
        }

    // Bedrock's hurt_by_target derives the target from the damage event; the
    // optional `entity_types` field, if present, must be a filter-object list.
    // We have no Java-side type list to translate so omit the field entirely.
    private fun mapHurtByTarget(@Suppress("UNUSED_PARAMETER") args: List<LiteralArg>): JsonObject =
        buildJsonObject { }

    private val mappings: Map<String, GoalMapping> = listOf(
        // ----- goalSelector goals -----

        // (Mob mob, double speedModifier, float maxDistance)
        GoalMapping(
            javaFqn = "net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal",
            bedrockComponent = "minecraft:behavior.move_towards_target",
            argMapper = ::mapMoveTowardsTarget,
        ),

        // (PathfinderMob mob, double speedModifier, boolean onlyAtNight)
        GoalMapping(
            javaFqn = "net.minecraft.world.entity.ai.goal.MoveBackToVillageGoal",
            bedrockComponent = "minecraft:behavior.move_to_village",
            argMapper = ::mapMoveBackToVillage,
        ),

        // (PathfinderMob mob, double speedModifier)
        GoalMapping(
            javaFqn = "net.minecraft.world.entity.ai.goal.GolemRandomStrollInVillageGoal",
            bedrockComponent = "minecraft:behavior.random_stroll",
            argMapper = ::mapStrollInVillage,
        ),

        // (PathfinderMob mob, double speedModifier) | (mob, speed, interval)
        GoalMapping(
            javaFqn = "net.minecraft.world.entity.ai.goal.RandomStrollGoal",
            bedrockComponent = "minecraft:behavior.random_stroll",
            argMapper = ::mapRandomStroll,
        ),

        // (Mob mob, Class<? extends LivingEntity> lookAtType, float lookDistance)
        GoalMapping(
            javaFqn = "net.minecraft.world.entity.ai.goal.LookAtPlayerGoal",
            bedrockComponent = "minecraft:behavior.look_at_player",
            argMapper = ::mapLookAtPlayer,
        ),

        // (Mob mob)
        GoalMapping(
            javaFqn = "net.minecraft.world.entity.ai.goal.RandomLookAroundGoal",
            bedrockComponent = "minecraft:behavior.random_look_around",
            argMapper = ::mapRandomLookAround,
        ),

        // (Mob mob)
        GoalMapping(
            javaFqn = "net.minecraft.world.entity.ai.goal.FloatGoal",
            bedrockComponent = "minecraft:behavior.float",
            argMapper = ::mapFloat,
        ),

        // (PathfinderMob mob, double speedModifier)
        GoalMapping(
            javaFqn = "net.minecraft.world.entity.ai.goal.PanicGoal",
            bedrockComponent = "minecraft:behavior.panic",
            argMapper = ::mapPanic,
        ),

        // (PathfinderMob mob, double speedModifier, boolean followingTargetEvenIfNotSeen)
        GoalMapping(
            javaFqn = "net.minecraft.world.entity.ai.goal.MeleeAttackGoal",
            bedrockComponent = "minecraft:behavior.melee_attack",
            argMapper = ::mapMeleeAttack,
        ),

        // (PathfinderMob mob, double speedModifier)
        GoalMapping(
            javaFqn = "net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal",
            bedrockComponent = "minecraft:behavior.random_stroll",
            argMapper = ::mapWaterAvoidingStroll,
        ),

        // ----- targetSelector goals -----

        // (IronGolem golem)
        GoalMapping(
            javaFqn = "net.minecraft.world.entity.ai.goal.target.DefendVillageTargetGoal",
            bedrockComponent = "minecraft:behavior.defend_village_target",
            argMapper = ::mapDefendVillage,
        ),

        // (PathfinderMob mob, Class<?>... toIgnoreDamage)
        GoalMapping(
            javaFqn = "net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal",
            bedrockComponent = "minecraft:behavior.hurt_by_target",
            argMapper = ::mapHurtByTarget,
        ),

        // No direct Bedrock equivalent — demoted unconditionally. Argmapper
        // returns null to force the matcher to log to UNTRANSLATABLE.
        GoalMapping(
            javaFqn = "net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal",
            bedrockComponent = "minecraft:behavior.nearest_attackable_target",
            argMapper = { null },
        ),

        // We can't statically resolve the target Class<?> to a Bedrock
        // type-family — demote so Phase 3 picks it up.
        GoalMapping(
            javaFqn = "net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal",
            bedrockComponent = "minecraft:behavior.nearest_attackable_target",
            argMapper = { null },
        ),

        // No Bedrock counterpart — demoted; entry exists so the matcher
        // recognizes the FQN as known-but-undowngradable.
        GoalMapping(
            javaFqn = "net.minecraft.world.entity.ai.goal.OfferFlowerGoal",
            bedrockComponent = "minecraft:behavior.offer_flower",
            argMapper = { null },
        ),

    ).associateBy { it.javaFqn }

    /** Look up a goal mapping by its Java FQN. Null = catalog miss. */
    fun lookup(javaFqn: String): GoalMapping? = mappings[javaFqn]

    /** All FQNs the catalog knows about (used by tests for sanity checks). */
    fun knownFqns(): Set<String> = mappings.keys
}
