package com.tweeks.translator.discover

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readLines

/**
 * Walks the root `settings.gradle` to enumerate Gradle subprojects, then
 * filters to those that look like NeoForge mods (have `src/main/java` and
 * `src/main/resources`). The translator subproject itself is intentionally
 * excluded by this filter — it has neither.
 */
class ModDiscovery(private val repoRoot: Path) {

    /** A discovered NeoForge mod. */
    data class DiscoveredMod(
        val modId: String,
        val rootDir: Path,
    )

    /**
     * Read `settings.gradle` and return the subset of subprojects that are
     * mod modules. Order matches the order of `include` lines in
     * `settings.gradle`.
     */
    fun discover(): List<DiscoveredMod> {
        val settings = repoRoot.resolve("settings.gradle")
        require(settings.isRegularFile()) { "settings.gradle not found at $settings" }

        val subprojects = parseIncludes(settings)
        return subprojects
            .map { DiscoveredMod(modId = it, rootDir = repoRoot.resolve(it)) }
            .filter { isModModule(it.rootDir) }
    }

    /**
     * Return a single discovered mod by id, or null if absent / not a mod.
     */
    fun findById(modId: String): DiscoveredMod? = discover().firstOrNull { it.modId == modId }

    /**
     * Parse `include 'foo'` / `include "foo"` / `include 'foo', 'bar'`
     * lines out of settings.gradle. Only top-level includes are matched;
     * nested project paths (`:foo:bar`) are unsupported because this repo
     * doesn't use them.
     */
    private fun parseIncludes(settings: Path): List<String> {
        val lineStart = Regex("""^\s*include\s+""")
        val quoted = Regex("""['"]([^'"]+)['"]""")
        val out = mutableListOf<String>()
        for (line in settings.readLines()) {
            if (!lineStart.containsMatchIn(line)) continue
            for (m in quoted.findAll(line)) {
                out.add(m.groupValues[1].removePrefix(":"))
            }
        }
        return out
    }

    /**
     * A module is treated as a mod iff it has `src/main/java`. All four
     * NeoForge mods in this repo have it; the translator subproject has only
     * `src/main/kotlin` and is filtered out.
     *
     * Earlier versions also required `src/main/resources`, but `securitycore`
     * is a library-only mod with sources and templates but no top-level
     * `resources/` directory, so that filter was too strict.
     */
    private fun isModModule(moduleDir: Path): Boolean {
        if (!moduleDir.isDirectory()) return false
        val javaSrc = moduleDir.resolve("src/main/java")
        return javaSrc.isDirectory()
    }

    companion object {
        /**
         * Locate the repo root by walking up from a starting directory until
         * a `settings.gradle` is found. Used to bridge the test/runtime gap
         * (tests can't always rely on cwd).
         */
        fun findRepoRoot(start: Path): Path {
            var cur: Path? = start.toAbsolutePath().normalize()
            while (cur != null) {
                if (Files.isRegularFile(cur.resolve("settings.gradle"))) return cur
                cur = cur.parent
            }
            error("Could not find settings.gradle walking up from $start")
        }
    }
}
