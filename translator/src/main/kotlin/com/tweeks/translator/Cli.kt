package com.tweeks.translator

import com.tweeks.translator.discover.ModDiscovery
import com.tweeks.translator.discover.ModMetadata
import com.tweeks.translator.emit.AddonWriter
import com.tweeks.translator.emit.Untranslatable
import com.tweeks.translator.json.AssetCopier
import com.tweeks.translator.json.ItemAtlasBuilder
import com.tweeks.translator.json.LangTransform
import com.tweeks.translator.json.LootTableTransform
import com.tweeks.translator.json.RecipeTransform
import com.tweeks.translator.json.SoundTransform
import com.tweeks.translator.manifest.BedrockTarget
import com.tweeks.translator.manifest.ManifestWriter
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.system.exitProcess

/**
 * CLI entry point for the Bedrock translator.
 *
 * Usage (via Gradle wrapper):
 *   ./gradlew :translator:translate
 *   ./gradlew :translator:translate --args="securityguard"
 *   ./gradlew :translator:translate --args="--diff"            # not yet implemented (exits 2)
 *   ./gradlew :translator:translate --args="--no-llm"          # not yet implemented (exits 2)
 *   ./gradlew :translator:translate --args="--clear-cache"     # not yet implemented (exits 2)
 *
 * Phase 0 implements only:
 *   - mod discovery + manifest emission to bedrock-out/.
 *   - flag recognition (--diff / --no-llm / --clear-cache exit 2 with a clear
 *     "implemented in Phase X" message — see [unimplementedFlagMessage]).
 *
 * Phases 1–3 land the real translation passes.
 */
fun main(args: Array<String>) {
    val opts = parseArgs(args)

    val unimplemented = collectUnimplementedFlags(opts)
    if (unimplemented.isNotEmpty()) {
        System.err.println(unimplementedFlagMessage(unimplemented))
        exitProcess(2)
    }

    val workingDir = Path.of(System.getProperty("user.dir"))
    val repoRoot = ModDiscovery.findRepoRoot(workingDir)
    val target = BedrockTarget.load(repoRoot.resolve("translator/bedrock-target.json"))
    val discovery = ModDiscovery(repoRoot)
    val writer = ManifestWriter(target)
    val addonWriter = AddonWriter(
        outputRoot = AddonWriter.defaultOutputRoot(repoRoot),
        manifestWriter = writer,
    )

    val mods = if (opts.modId != null) {
        val one = discovery.findById(opts.modId)
        if (one == null) {
            System.err.println("[translator] mod '${opts.modId}' not found in settings.gradle, or has no src/main/{java,resources}.")
            exitProcess(2)
        }
        listOf(one)
    } else {
        discovery.discover()
    }

    if (mods.isEmpty()) {
        println("[translator] No mods discovered. Nothing to do.")
        return
    }

    val outputRoot = AddonWriter.defaultOutputRoot(repoRoot)
    val recipeTransform = { unt: Untranslatable -> RecipeTransform(target, unt) }
    val lootTransform = { unt: Untranslatable -> LootTableTransform(unt) }
    val langTransform = LangTransform()
    val soundTransform = { unt: Untranslatable -> SoundTransform(target, unt) }
    val assetCopier = AssetCopier()
    val atlasBuilder = ItemAtlasBuilder()

    for (mod in mods) {
        val metadata = ModMetadata.read(mod.rootDir, mod.modId)
        val inputs = ManifestWriter.ModManifestInputs(
            modId = metadata.modId,
            displayName = metadata.displayName,
            description = metadata.description,
            requiresSecurityCore = metadata.requiresSecurityCore,
        )
        val result = addonWriter.write(mod, inputs)

        // JSON pipeline: each transform shares one Untranslatable so the
        // per-mod report aggregates findings across the whole pipeline.
        val unt = Untranslatable()
        recipeTransform(unt).translate(mod.rootDir, mod.modId, outputRoot)
        lootTransform(unt).translate(mod.rootDir, mod.modId, outputRoot)
        langTransform.translate(mod.rootDir, mod.modId, outputRoot)
        soundTransform(unt).translate(mod.rootDir, mod.modId, outputRoot)
        val copyResult = assetCopier.copy(mod.rootDir, mod.modId, outputRoot)
        atlasBuilder.build(mod.modId, copyResult.itemTextureShortNames, outputRoot)
        unt.writeFor(mod.modId, outputRoot)

        println("[translator] Wrote ${result.modId}: ${result.outputDir.absolutePathString()}")
    }
}

internal data class CliOptions(
    val modId: String?,
    val diff: Boolean,
    val noLlm: Boolean,
    val clearCache: Boolean,
)

/**
 * Phase in which an unimplemented flag is scheduled to land. Surfaces in the
 * usage-error message when one of these flags is passed in Phase 0.
 */
internal enum class UnimplementedFlag(val flag: String, val plannedPhase: String) {
    DIFF("--diff", "Phase 1+"),
    NO_LLM("--no-llm", "Phase 3"),
    CLEAR_CACHE("--clear-cache", "Phase 3"),
}

/**
 * Returns every unimplemented flag the user asked for, in flag-declaration
 * order (DIFF, NO_LLM, CLEAR_CACHE) so error output is stable.
 */
internal fun collectUnimplementedFlags(opts: CliOptions): List<UnimplementedFlag> {
    val out = mutableListOf<UnimplementedFlag>()
    if (opts.diff) out += UnimplementedFlag.DIFF
    if (opts.noLlm) out += UnimplementedFlag.NO_LLM
    if (opts.clearCache) out += UnimplementedFlag.CLEAR_CACHE
    return out
}

/**
 * Format a usage-error message naming every unimplemented flag the user
 * passed and the phase that will implement it. The exact format is part of
 * the CLI contract — see [CliFlagsTest].
 */
internal fun unimplementedFlagMessage(flags: List<UnimplementedFlag>): String {
    require(flags.isNotEmpty()) { "unimplementedFlagMessage called with empty flag list" }
    val header = if (flags.size == 1) {
        "[translator] Flag not yet implemented:"
    } else {
        "[translator] Flags not yet implemented:"
    }
    val lines = flags.map { "  ${it.flag}: planned for ${it.plannedPhase}" }
    return (listOf(header) + lines).joinToString("\n")
}

internal fun parseArgs(args: Array<String>): CliOptions {
    var modId: String? = null
    var diff = false
    var noLlm = false
    var clearCache = false

    for (arg in args) {
        when {
            arg == "--diff" -> diff = true
            arg == "--no-llm" -> noLlm = true
            arg == "--clear-cache" -> clearCache = true
            arg.startsWith("--") -> {
                System.err.println("[translator] Unknown flag: $arg")
                exitProcess(2)
            }
            modId == null -> modId = arg
            else -> {
                System.err.println("[translator] Multiple mod ids supplied; only one is allowed: '$modId' and '$arg'.")
                exitProcess(2)
            }
        }
    }

    return CliOptions(modId = modId, diff = diff, noLlm = noLlm, clearCache = clearCache)
}
