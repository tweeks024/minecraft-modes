package com.tweeks.translator

import com.tweeks.translator.discover.ModDiscovery
import com.tweeks.translator.discover.ModMetadata
import com.tweeks.translator.emit.AddonWriter
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
 *   ./gradlew :translator:translate --args="--diff"
 *   ./gradlew :translator:translate --args="--no-llm"
 *   ./gradlew :translator:translate --args="--clear-cache"
 *
 * Phase 0 implements only:
 *   - mod discovery + manifest emission to bedrock-out/.
 *   - flag recognition (--diff / --no-llm / --clear-cache print "not yet implemented").
 *
 * Phases 1–3 land the real translation passes.
 */
fun main(args: Array<String>) {
    val opts = parseArgs(args)

    if (opts.diff) {
        println("[translator] --diff: dry-run diff is not yet implemented (Phase 0).")
        return
    }
    if (opts.noLlm) {
        println("[translator] --no-llm: no-LLM mode is not yet implemented (Phase 0); no LLM exists yet.")
    }
    if (opts.clearCache) {
        println("[translator] --clear-cache: cache clear is not yet implemented (Phase 0); no cache exists yet.")
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

    for (mod in mods) {
        val metadata = ModMetadata.read(mod.rootDir, mod.modId)
        val inputs = ManifestWriter.ModManifestInputs(
            modId = metadata.modId,
            displayName = metadata.displayName,
            description = metadata.description,
            requiresSecurityCore = metadata.requiresSecurityCore,
        )
        val result = addonWriter.write(mod, inputs)
        println("[translator] Wrote ${result.modId}: ${result.outputDir.absolutePathString()}")
    }
}

private data class CliOptions(
    val modId: String?,
    val diff: Boolean,
    val noLlm: Boolean,
    val clearCache: Boolean,
)

private fun parseArgs(args: Array<String>): CliOptions {
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
