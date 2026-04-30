package com.tweeks.translator.java.llm

import com.tweeks.translator.discover.ModDiscovery
import com.tweeks.translator.manifest.BedrockTarget
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

class TranslationPromptTest {

    private val repoRoot: Path = ModDiscovery.findRepoRoot(Path.of("").toAbsolutePath())
    private val target = BedrockTarget.load(repoRoot.resolve("translator/bedrock-target.json"))
    private val prompt = TranslationPrompt.load(target)

    @Test
    fun `system blocks include type declarations and worked examples and rules and family filters`() {
        val blocks = prompt.systemBlocks()
        // Phase 4: a fourth block was added for family-filter targeting
        // (SecurityAlly / SecurityHostile → Bedrock family tags).
        assertEquals(4, blocks.size)
        val combined = blocks.joinToString("\n") { it.text }
        assertTrue(combined.contains("@minecraft/server")) {
            "system prompt must include the type declarations"
        }
        assertTrue(combined.contains("Worked examples")) {
            "system prompt must include the worked examples"
        }
        assertTrue(combined.contains("performance rules") || combined.contains("Performance") || combined.contains("performance")) {
            "system prompt must include the performance rules"
        }
        assertTrue(combined.contains("security_hostile") && combined.contains("is_family")) {
            "system prompt must include the family-filter reference"
        }
    }

    @Test
    fun `at least 3 of 4 blocks are marked cached`() {
        val cached = prompt.systemBlocks().count { it.cached }
        assertTrue(cached >= 3) { "expected at least 3 cached blocks, got $cached" }
    }

    @Test
    fun `user message includes goal source and resolved type info`() {
        val ctx = TranslationPrompt.GoalContext(
            goalClassSimpleName = "StunningMeleeGoal",
            goalClassFqn = "com.example.StunningMeleeGoal",
            goalSource = "public class StunningMeleeGoal extends Goal {\n  public void tick() {}\n}\n",
            parentClassFqn = "net.minecraft.world.entity.ai.goal.Goal",
            resolvedMethodSignatures = listOf("com.example.Util.doStun(Entity, int): void"),
            resolvedFieldReferences = listOf("com.example.Const.STUN_TICKS: int"),
            entityClassSummary = "entity id: `guard`",
            modManifestExcerpt = "modId: securityguard",
        )

        val msg = prompt.userMessageFor(ctx)
        assertTrue(msg.contains("com.example.StunningMeleeGoal"))
        assertTrue(msg.contains("public class StunningMeleeGoal"))
        assertTrue(msg.contains("net.minecraft.world.entity.ai.goal.Goal"))
        assertTrue(msg.contains("com.example.Util.doStun"))
        assertTrue(msg.contains("com.example.Const.STUN_TICKS"))
        assertTrue(msg.contains("entity id: `guard`"))
        assertTrue(msg.contains("modId: securityguard"))
    }
}
