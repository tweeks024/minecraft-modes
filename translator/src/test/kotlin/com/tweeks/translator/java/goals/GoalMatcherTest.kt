package com.tweeks.translator.java.goals

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.tweeks.translator.emit.Untranslatable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Direct unit tests for [GoalMatcher] over parsed Java fixtures. These
 * skip JavaParser symbol resolution (no [SymbolSolver] is wired up) so
 * the matcher exercises its FQN-from-AST fallback — which is exactly the
 * code path used in production for fully-qualified `new
 * net.minecraft.world.entity.ai.goal.<Goal>(...)` constructor calls.
 */
class GoalMatcherTest {

    @Test
    fun `high-bucket goals emit components with priority`() {
        val src = """
            public class Test {
                protected void registerGoals() {
                    this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal(this, 0.9, 32.0f));
                    this.goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.RandomStrollGoal(this, 0.6));
                }
            }
        """.trimIndent()
        val cls = parseClass(src)
        val unt = Untranslatable()
        val result = GoalMatcher(unt).match("test", cls)
        assertEquals(2, result.components.size)

        val sorted = result.components.sortedBy { it.priority }
        assertEquals(2, sorted[0].priority)
        assertEquals("minecraft:behavior.move_towards_target", sorted[0].componentName)
        assertEquals(7, sorted[1].priority)
        assertEquals("minecraft:behavior.random_stroll", sorted[1].componentName)
    }

    @Test
    fun `catalog miss is logged as Medium`() {
        val src = """
            public class Test {
                protected void registerGoals() {
                    this.goalSelector.addGoal(1, new com.tweeks.securitycore.ai.StunningMeleeGoal(this, 1.0, true, 60, 1, 0, 0.2));
                }
            }
        """.trimIndent()
        val cls = parseClass(src)
        val unt = Untranslatable()
        val result = GoalMatcher(unt).match("test", cls)
        assertEquals(0, result.components.size, "catalog miss must not emit a component")
        // Phase 3: a Medium-bucket goal also surfaces in the typed `deferred`
        // list so EntityAnalyzer can route it through the gate.
        assertEquals(1, result.deferred.size)
        assertEquals("StunningMeleeGoal", result.deferred[0].goalSimpleName)

        val report = unt.renderReport("test")
        assertTrue(report.contains("Entity goals deferred")) { report }
        assertTrue(report.contains("StunningMeleeGoal"))
    }

    @Test
    fun `non-literal arg demotes to Medium`() {
        val src = """
            public class Test {
                static final float DIST = 8.0f;
                protected void registerGoals() {
                    this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal(this, 0.9, DIST));
                }
            }
        """.trimIndent()
        val cls = parseClass(src)
        val unt = Untranslatable()
        val result = GoalMatcher(unt).match("test", cls)
        assertEquals(0, result.components.size, "non-literal arg must not emit a component")
        assertTrue(unt.renderReport("test").contains("non-literal argument"))
    }

    @Test
    fun `OfferFlowerGoal demoted to Medium with no-equivalent reason`() {
        val src = """
            public class Test {
                protected void registerGoals() {
                    this.goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.OfferFlowerGoal(this));
                }
            }
        """.trimIndent()
        val cls = parseClass(src)
        val unt = Untranslatable()
        GoalMatcher(unt).match("test", cls)
        val report = unt.renderReport("test")
        assertTrue(report.contains("OfferFlowerGoal"))
        assertTrue(report.contains("no clean Bedrock 1.21.0 equivalent"))
    }

    @Test
    fun `targetSelector goals are scanned too`() {
        val src = """
            public class Test {
                protected void registerGoals() {
                    this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.DefendVillageTargetGoal(this));
                    this.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
                }
            }
        """.trimIndent()
        val cls = parseClass(src)
        val unt = Untranslatable()
        val result = GoalMatcher(unt).match("test", cls)
        assertEquals(2, result.components.size)
        val components = result.components.map { it.componentName }.sorted()
        assertEquals(
            listOf(
                "minecraft:behavior.defend_village_target",
                "minecraft:behavior.hurt_by_target",
            ),
            components,
        )
    }

    @Test
    fun `imported simple-name goal resolves to the imported FQN`() {
        // Wildwest pattern: import the goal class, then `new <Goal>(...)`
        // without qualifying or via the diamond `<>`. With no symbol solver,
        // resolveFqn used to fail and the goal landed in the report keyed
        // by `<unknown>`. The imports fallback fixes it.
        val src = """
            package com.example;
            import net.minecraft.world.entity.ai.goal.FloatGoal;
            public class Test {
                protected void registerGoals() {
                    this.goalSelector.addGoal(0, new FloatGoal(this));
                }
            }
        """.trimIndent()
        val cls = parseClass(src)
        val unt = Untranslatable()
        val result = GoalMatcher(unt).match("test", cls)
        // FloatGoal is in the catalog and emits a component when resolved.
        // Without the imports fallback it'd be logged as `<unknown>` and no
        // component would be produced.
        assertEquals(1, result.components.size)
        assertEquals("minecraft:behavior.float", result.components[0].componentName)
        assertTrue(!unt.renderReport("test").contains("<unknown>")) {
            "Imported goal must not surface as <unknown>: ${unt.renderReport("test")}"
        }
    }

    // ----- anonymous per-entity gated goals (SwMob MeleeAttackGoal pattern) -----

    /**
     * Superclass that registers an anonymous `MeleeAttackGoal` gated on an
     * abstract `usesBlaster()` — the starwars SwMob pattern. Each leaf
     * overrides `usesBlaster()` with a literal, so the gate is static per leaf.
     */
    private val swMobSrc = """
        public abstract class SwMob {
            public abstract boolean usesBlaster();
            protected void registerGoals() {
                this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
                this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.2, true) {
                    @Override public boolean canUse() { return !SwMob.this.usesBlaster() && super.canUse(); }
                    @Override public boolean canContinueToUse() { return !SwMob.this.usesBlaster() && super.canContinueToUse(); }
                });
            }
        }
    """.trimIndent()

    @Test
    fun `blaster mob drops the gated melee goal and records it`() {
        val leaf = parseClass("public class Trooper extends SwMob { public boolean usesBlaster() { return true; } }")
        val superSw = parseClass(swMobSrc)
        val unt = Untranslatable()
        val result = GoalMatcher(unt).match("test", leaf) { if (it == "SwMob") superSw else null }

        // Float (priority 0) is kept; the gated melee_attack is dropped.
        val names = result.components.map { it.componentName }
        assertEquals(listOf("minecraft:behavior.float"), names)
        assertTrue(!names.contains("minecraft:behavior.melee_attack")) {
            "blaster mob must not melee-charge: $names"
        }
        val report = unt.renderReport("test")
        assertTrue(report.contains("Entity goals gated off per-entity")) { report }
        assertTrue(report.contains("Trooper")) { report }
        assertTrue(report.contains("usesBlaster()")) { report }
    }

    @Test
    fun `saber mob keeps the gated melee goal`() {
        val leaf = parseClass("public class Jedi extends SwMob { public boolean usesBlaster() { return false; } }")
        val superSw = parseClass(swMobSrc)
        val unt = Untranslatable()
        val result = GoalMatcher(unt).match("test", leaf) { if (it == "SwMob") superSw else null }

        val names = result.components.map { it.componentName }.sorted()
        assertEquals(
            listOf("minecraft:behavior.float", "minecraft:behavior.melee_attack"),
            names,
        )
        assertTrue(!unt.renderReport("test").contains("gated off")) {
            "open gate must not record a gated-off finding: ${unt.renderReport("test")}"
        }
    }

    @Test
    fun `unresolvable gate drops the goal and records honestly`() {
        // Leaf's usesBlaster() returns a non-literal — the gate can't be folded.
        val leaf = parseClass(
            "public class Mystery extends SwMob { public boolean usesBlaster() { return computeIt(); } }"
        )
        val superSw = parseClass(swMobSrc)
        val unt = Untranslatable()
        val result = GoalMatcher(unt).match("test", leaf) { if (it == "SwMob") superSw else null }

        val names = result.components.map { it.componentName }
        assertTrue(!names.contains("minecraft:behavior.melee_attack")) {
            "unresolvable gate must drop the melee goal rather than blind-emit: $names"
        }
        val report = unt.renderReport("test")
        assertTrue(report.contains("Entity goals gated off per-entity")) { report }
        assertTrue(report.contains("unresolvable")) { report }
    }

    private fun parseClass(src: String): ClassOrInterfaceDeclaration {
        val parser = JavaParser()
        val unit = parser.parse(src).result.orElseThrow { IllegalStateException("parse failed") }
        return unit.types.filterIsInstance<ClassOrInterfaceDeclaration>().first()
    }
}
