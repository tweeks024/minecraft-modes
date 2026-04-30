package com.tweeks.translator.java.llm

import com.tweeks.translator.discover.ModDiscovery
import com.tweeks.translator.emit.Untranslatable
import com.tweeks.translator.java.ClasspathResolver
import com.tweeks.translator.java.EntityAnalyzer
import com.tweeks.translator.java.JavaSourceLoader
import com.tweeks.translator.manifest.BedrockTarget
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Phase 3 end-to-end check: pre-populating the cache for a Medium-bucket
 * goal makes its `behavior_pack/scripts/goals/<X>.ts` come from the cached
 * JS rather than a TODO stub, and the UNTRANSLATABLE.md report moves the
 * goal to the "translated by LLM (cache hit)" section.
 *
 * Uses [MockClaudeClient] (with no canned responses) — the cache lookup
 * short-circuits before the client is ever consulted, so this test does not
 * require a live API key.
 */
class EndToEndCacheHitTest {

    private val repoRoot: Path = ModDiscovery.findRepoRoot(Path.of("").toAbsolutePath())
    private val target = BedrockTarget.load(repoRoot.resolve("translator/bedrock-target.json"))

    @Test
    fun `pre-populated cache produces JS in scripts goals`(@TempDir outDir: Path, @TempDir cacheDir: Path) {
        val resolver = ClasspathResolver.fromSystemProperties()
        assertTrue(resolver.isAvailable())
        val all = ModDiscovery(repoRoot).discover()
        val securityguard = all.first { it.modId == "securityguard" }

        val unt = Untranslatable()
        val sources = JavaSourceLoader(resolver, unt).load(securityguard, all)
        val cache = TranslationCache(cacheDir)
        val prompt = TranslationPrompt.load(target)
        val client = MockClaudeClient(emptyMap()) // no live calls

        // Phase 1: dry run with --no-llm and an empty cache. Expect a TODO stub.
        run {
            val gate = ConfidenceGate(client, cache, prompt, unt, liveCallsEnabled = false)
            val analyzer = EntityAnalyzer(target, unt, gate)
            analyzer.analyze(securityguard, sources, outDir.resolve("dry"))
        }
        val stubPath = outDir.resolve("dry/securityguard/behavior_pack/scripts/goals/StunningMeleeGoal.ts")
        assertTrue(stubPath.exists())
        assertTrue(stubPath.readText().contains("// TODO LLM:"))

        // Phase 2: pre-populate the cache with a known-good translation. We
        // peek the key by running the analyzer once and reading the recorded
        // findings — but the simpler path is to compute it directly via the
        // gate using the same goal context the analyzer would build.
        val unt2 = Untranslatable()
        val gate2 = ConfidenceGate(client, cache, prompt, unt2, liveCallsEnabled = false)

        // The analyzer constructs the GoalContext internally; we recreate it
        // here with the same fields so the cache keys match.
        val javaSrcPath = securityguard.rootDir.resolve("../securitycore/src/main/java/com/tweeks/securitycore/ai/StunningMeleeGoal.java")
        val javaSrc = javaSrcPath.toRealPath().readText()
        val ctx = TranslationPrompt.GoalContext(
            goalClassSimpleName = "StunningMeleeGoal",
            goalClassFqn = "com.tweeks.securitycore.ai.StunningMeleeGoal",
            goalSource = javaSrc,
            parentClassFqn = "net.minecraft.world.entity.ai.goal.Goal",
            resolvedMethodSignatures = emptyList(),
            resolvedFieldReferences = emptyList(),
            entityClassSummary = "entity id: `guard`\nclass: `SecurityGuardEntity`\ncategory: `MISC`\nsize: 0.6 × 1.95\n",
            modManifestExcerpt = "modId: securityguard",
        )
        val key = gate2.computeKey(ctx)
        cache.put(
            key,
            ClaudeClient.TranslationResult.Ok(
                js = "// === pre-populated cache ===\nimport { world } from \"@minecraft/server\";\n",
                confidence = 0.95,
            ),
        )

        // Phase 3: re-run the analyzer. The Stunning goal should now come
        // from the cache, not the stub.
        val analyzer3 = EntityAnalyzer(target, unt2, gate2)
        analyzer3.analyze(securityguard, sources, outDir.resolve("hit"))

        val hitPath = outDir.resolve("hit/securityguard/behavior_pack/scripts/goals/StunningMeleeGoal.ts")
        assertTrue(hitPath.exists())
        val hitContent = hitPath.readText()
        assertTrue(hitContent.contains("=== pre-populated cache ===")) {
            "expected cached JS to land at $hitPath, got:\n$hitContent"
        }
        assertFalse(hitContent.contains("// TODO LLM:"))

        val report = unt2.renderReport("securityguard")
        assertTrue(report.contains("translated by LLM (cache hit)")) { report }
        assertTrue(report.contains("StunningMeleeGoal")) { report }
    }
}
