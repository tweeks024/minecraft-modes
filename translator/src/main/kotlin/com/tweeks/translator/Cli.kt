package com.tweeks.translator

import com.tweeks.translator.bbmodel.BbmodelConverter
import com.tweeks.translator.discover.ModDiscovery
import com.tweeks.translator.discover.ModMetadata
import com.tweeks.translator.emit.AddonWriter
import com.tweeks.translator.emit.Untranslatable
import com.tweeks.translator.java.ClasspathResolver
import com.tweeks.translator.java.JavaSourceLoader
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
import kotlin.io.path.exists
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
    val assetCopier = { unt: Untranslatable -> AssetCopier(unt) }
    val atlasBuilder = ItemAtlasBuilder()
    val bbmodelConverter = { unt: Untranslatable -> BbmodelConverter(target, unt) }

    // Phase 2a Java pipeline foundation. The full discovered list is
    // needed so [JavaSourceLoader] can wire sibling mods' src dirs as
    // type solvers when we run analysis on a single mod.
    val classpathResolver = ClasspathResolver.fromSystemProperties()
    if (!classpathResolver.isAvailable()) {
        System.err.println(
            "[translator] note: ${ClasspathResolver.SYSTEM_PROPERTY} not set; skipping Java pipeline. " +
                "Run via :translator:translate to enable it."
        )
    }
    val allDiscovered = discovery.discover()

    for (mod in mods) {
        // Clean the per-mod output dir so stale files (e.g. removed/renamed
        // recipes) don't survive across runs. Sibling-mod subdirs are
        // untouched.
        cleanModOutputDir(outputRoot, mod.modId)

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
        val copyResult = assetCopier(unt).copy(mod.rootDir, mod.modId, outputRoot)
        atlasBuilder.build(mod.modId, copyResult.itemTextureShortNames, outputRoot)
        bbmodelConverter(unt).convert(mod.modId, mod.rootDir.resolve("tools"), outputRoot)

        // Phase 2a: parse the Java sources and report the unit count.
        // No analysis yet — Phase 2b builds EntityAnalyzer/ItemAnalyzer
        // on top of this. The classpath property is set by the Gradle
        // task; if it isn't available, skip silently (we already warned
        // once at startup).
        if (classpathResolver.isAvailable()) {
            val loader = JavaSourceLoader(classpathResolver, unt)
            val resolved = loader.load(mod, allDiscovered)
            System.err.println("[translator] ${mod.modId}: parsed ${resolved.units.size} java files")
        }

        unt.writeFor(mod.modId, outputRoot)

        println("[translator] Wrote ${result.modId}: ${result.outputDir.absolutePathString()}")
    }
}

/**
 * Recursively delete `<outputRoot>/<modId>/` so the next translate run
 * produces a from-scratch tree for that mod. No-op if the directory does
 * not yet exist. Other mods' subdirectories under [outputRoot] are not
 * touched.
 *
 * Guards against pathological inputs: if [modId] is blank or contains a
 * path separator, refuse to delete anything (would otherwise risk wiping
 * the bedrock-out root or escaping it entirely).
 */
internal fun cleanModOutputDir(outputRoot: Path, modId: String) {
    require(modId.isNotBlank() && !modId.contains('/') && !modId.contains('\\') && modId != "." && modId != "..") {
        "Refusing to clean output dir for suspicious modId '$modId'."
    }
    val modOut = outputRoot.resolve(modId)
    if (!modOut.exists()) return
    // Sanity-check: confirm modOut actually lives under outputRoot. (Defense
    // in depth — `resolve` with a benign modId can never escape, but the
    // require() above is the only thing standing between us and a
    // user-supplied path.)
    val realRoot = outputRoot.toAbsolutePath().normalize()
    val realMod = modOut.toAbsolutePath().normalize()
    require(realMod.startsWith(realRoot) && realMod != realRoot) {
        "Refusing to clean '$realMod' — not strictly inside output root '$realRoot'."
    }
    modOut.toFile().deleteRecursively()
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
