package com.tweeks.translator.java

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import com.tweeks.translator.discover.ModDiscovery.DiscoveredMod
import com.tweeks.translator.emit.Untranslatable
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

/**
 * Phase 2a Java pipeline foundation. Given a [DiscoveredMod], walks
 * the mod's `src/main/java` tree and parses every `.java` file with a
 * JavaParser configured for full symbol resolution.
 *
 * Symbol resolution is layered, in priority order:
 *   1. JDK types via [ReflectionTypeSolver].
 *   2. The mod's own `src/main/java/` via [JavaParserTypeSolver].
 *   3. Sibling mods' `src/main/java/` directories via
 *      [JavaParserTypeSolver] (so e.g. `securityguard` can resolve
 *      `securitycore`'s helper classes).
 *   4. Every jar on the mod's runtime classpath, fed via
 *      [JarTypeSolver]. The order matters — JavaParser source solvers
 *      precede jars so duplicate types resolve to the source-of-truth.
 *
 * Parse errors are recorded on [Untranslatable] (one entry per file)
 * but never abort the whole load; the partial AST is still useful for
 * Phase 2b's analysers.
 */
class JavaSourceLoader(
    private val classpathResolver: ClasspathResolver,
    private val unt: Untranslatable,
) {

    /** Result of a successful (or partial) load. */
    data class ResolvedModSources(
        val mod: DiscoveredMod,
        val units: List<CompilationUnit>,
        val typeSolver: CombinedTypeSolver,
    )

    /**
     * Load and parse every Java file under `<mod>/src/main/java`.
     * [allMods] is the full discovered list — siblings are added as
     * source-aware type solvers so cross-mod symbol resolution works.
     */
    fun load(mod: DiscoveredMod, allMods: List<DiscoveredMod>): ResolvedModSources {
        val combined = CombinedTypeSolver()

        // 1) JDK
        combined.add(ReflectionTypeSolver())

        // 2 + 3) All mods' source dirs as JavaParser solvers. The
        // current mod's own dir is included alongside the siblings —
        // CombinedTypeSolver's first-match-wins rule means it
        // effectively takes precedence over jar lookups for its own
        // classes.
        for (m in allMods) {
            val srcDir = m.rootDir.resolve("src/main/java")
            if (srcDir.isDirectory()) {
                combined.add(JavaParserTypeSolver(srcDir.toFile()))
            }
        }

        // 4) Jars from the mod's runtime classpath.
        val parts = classpathResolver.partition(mod.modId)
        for (jar in parts.jars) {
            try {
                combined.add(JarTypeSolver(jar))
            } catch (e: Exception) {
                // A malformed/empty jar shouldn't sink the whole load.
                System.err.println(
                    "[translator] warning: skipping unreadable jar ${jar} for ${mod.modId}: ${e.message}"
                )
            }
        }
        // Build-output `classes/` directories sometimes contain compiled
        // class files whose types we want to resolve. JavaParser doesn't
        // ship a class-folder solver out of the box, but those types are
        // typically the mod's own (already covered by source) or
        // generated stubs we can ignore for Phase 2a.
        // `parts.directories` is intentionally not wired in here.

        // Build the parser with the symbol solver attached. We share one
        // configuration across every CompilationUnit so they all use the
        // same solver instance. The language level must match (or
        // exceed) what the mods compile against — they target Java 25
        // (Minecraft 26.1.2 ships JDK 25), but JavaParser 3.26 caps out
        // at JAVA_21. JAVA_21 covers all features the four mods use
        // today (records, switch expressions, instanceof patterns,
        // sealed types). If a future feature needs JAVA_22+ we'll need
        // to bump JavaParser; until then JAVA_21 is the right pin.
        val config = ParserConfiguration().apply {
            setSymbolResolver(JavaSymbolSolver(combined))
            languageLevel = ParserConfiguration.LanguageLevel.JAVA_21
        }
        val parser = JavaParser(config)

        val srcDir = mod.rootDir.resolve("src/main/java")
        val units = mutableListOf<CompilationUnit>()
        if (srcDir.isDirectory()) {
            val javaFiles = mutableListOf<Path>()
            Files.walk(srcDir).use { stream ->
                stream.forEach { path ->
                    if (path.isRegularFile() && path.extension == "java") {
                        javaFiles.add(path)
                    }
                }
            }
            javaFiles.sort()
            for (path in javaFiles) {
                val rel = mod.rootDir.relativize(path).toString()
                try {
                    val parseResult = parser.parse(path)
                    if (!parseResult.isSuccessful) {
                        val msg = parseResult.problems.joinToString("; ") { it.message }
                        unt.recordJavaParseError(mod.modId, rel, msg)
                        continue
                    }
                    val unit = parseResult.result.orElse(null)
                    if (unit != null) units.add(unit)
                } catch (e: Exception) {
                    unt.recordJavaParseError(mod.modId, rel, e.message ?: e.javaClass.simpleName)
                }
            }
        }

        return ResolvedModSources(mod = mod, units = units, typeSolver = combined)
    }
}
