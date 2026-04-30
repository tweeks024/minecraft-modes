package com.tweeks.translator.java.goals

import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Spot-check the [VanillaGoalCatalog] entries used by the four mods. The
 * catalog is small and stable enough that a hand-rolled test per goal is
 * cheaper than a parameterized fixture.
 */
class VanillaGoalCatalogTest {

    @Test
    fun `MoveTowardsTargetGoal maps speed and radius`() {
        val mapping = VanillaGoalCatalog.lookup(
            "net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal"
        )
        assertNotNull(mapping)
        val body = mapping!!.argMapper(
            listOf(
                VanillaGoalCatalog.LiteralArg.StringArg("this"),
                VanillaGoalCatalog.LiteralArg.DoubleArg(0.9),
                VanillaGoalCatalog.LiteralArg.DoubleArg(32.0),
            )
        )
        assertNotNull(body)
        assertEquals(JsonPrimitive(0.9), body!!["speed_multiplier"])
        assertEquals(JsonPrimitive(32), body["within_radius"])
        assertEquals("minecraft:behavior.move_towards_target", mapping.bedrockComponent)
    }

    @Test
    fun `RandomStrollGoal maps two-arg form`() {
        val mapping = VanillaGoalCatalog.lookup(
            "net.minecraft.world.entity.ai.goal.RandomStrollGoal"
        )!!
        val body = mapping.argMapper(
            listOf(
                VanillaGoalCatalog.LiteralArg.StringArg("this"),
                VanillaGoalCatalog.LiteralArg.DoubleArg(0.6),
            )
        )!!
        assertEquals(JsonPrimitive(0.6), body["speed_multiplier"])
        assertNull(body["interval"])
    }

    @Test
    fun `LookAtPlayerGoal maps look distance`() {
        val mapping = VanillaGoalCatalog.lookup(
            "net.minecraft.world.entity.ai.goal.LookAtPlayerGoal"
        )!!
        val body = mapping.argMapper(
            listOf(
                VanillaGoalCatalog.LiteralArg.StringArg("this"),
                VanillaGoalCatalog.LiteralArg.ClassArg("Player.class"),
                VanillaGoalCatalog.LiteralArg.DoubleArg(8.0),
            )
        )!!
        assertEquals(JsonPrimitive(8), body["look_distance"])
    }

    @Test
    fun `FloatGoal maps with no args`() {
        val mapping = VanillaGoalCatalog.lookup(
            "net.minecraft.world.entity.ai.goal.FloatGoal"
        )!!
        val body = mapping.argMapper(listOf(VanillaGoalCatalog.LiteralArg.StringArg("this")))!!
        assertEquals(JsonPrimitive(true), body["sink_with_passengers"])
    }

    @Test
    fun `OfferFlowerGoal returns null - demoted to Phase 3`() {
        val mapping = VanillaGoalCatalog.lookup(
            "net.minecraft.world.entity.ai.goal.OfferFlowerGoal"
        )!!
        val body = mapping.argMapper(listOf(VanillaGoalCatalog.LiteralArg.StringArg("this")))
        assertNull(body, "OfferFlowerGoal has no Bedrock equivalent — argMapper must return null.")
    }

    @Test
    fun `ResetUniversalAngerTargetGoal returns null - demoted`() {
        val mapping = VanillaGoalCatalog.lookup(
            "net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal"
        )!!
        assertNull(mapping.argMapper(emptyList()))
    }

    @Test
    fun `unknown FQN returns null - catalog miss`() {
        assertNull(VanillaGoalCatalog.lookup("com.tweeks.totally.unknown.Goal"))
    }

    @Test
    fun `MoveTowardsTargetGoal with too few args returns null`() {
        val mapping = VanillaGoalCatalog.lookup(
            "net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal"
        )!!
        assertNull(mapping.argMapper(listOf(VanillaGoalCatalog.LiteralArg.StringArg("this"))))
    }
}
