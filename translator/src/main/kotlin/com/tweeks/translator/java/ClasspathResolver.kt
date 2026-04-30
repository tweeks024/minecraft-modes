package com.tweeks.translator.java

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readLines

/**
 * Reads the per-mod runtime-classpath manifest written by the Gradle
 * `dumpRuntimeClasspathForTranslator` task. The manifest is a plain
 * text file at `translator/build/classpaths/<modId>.txt`, one absolute
 * jar (or dir) path per line.
 *
 * The directory containing the manifests is supplied via the
 * `translator.classpathDir` system property (see
 * `translator/build.gradle.kts`).  The CLI sets this when invoked via
 * `:translator:translate`. Tests get it set via `:translator:test`'s
 * `systemProperty` declaration.
 *
 * If the property is unset (e.g. the CLI was launched directly with
 * `java -cp ...`), [classpathFor] returns an empty list and a warning
 * is left to the caller. The Java pipeline degrades gracefully — the
 * JSON pipeline + bbmodel converter still run.
 */
class ClasspathResolver(private val manifestDir: Path?) {

    /** Whether a manifest dir was supplied at construction time. */
    fun isAvailable(): Boolean = manifestDir != null

    /**
     * Return the absolute paths recorded for [modId]. Empty list if no
     * manifest dir is configured, or if the per-mod manifest is missing.
     */
    fun classpathFor(modId: String): List<Path> {
        val dir = manifestDir ?: return emptyList()
        val file = dir.resolve("$modId.txt")
        if (!file.isRegularFile()) return emptyList()
        return file.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { Paths.get(it) }
    }

    /**
     * Partition the classpath into (existing jar files, existing
     * directories, missing entries). Used by callers that need to feed
     * jars and directories to JavaParser's `JarTypeSolver` and
     * `JavaParserTypeSolver` separately.
     */
    fun partition(modId: String): ClasspathParts {
        val jars = mutableListOf<Path>()
        val dirs = mutableListOf<Path>()
        val missing = mutableListOf<Path>()
        for (p in classpathFor(modId)) {
            when {
                !p.exists() -> missing.add(p)
                Files.isDirectory(p) -> dirs.add(p)
                p.toString().endsWith(".jar") -> jars.add(p)
                else -> missing.add(p)
            }
        }
        return ClasspathParts(jars = jars, directories = dirs, missing = missing)
    }

    data class ClasspathParts(
        val jars: List<Path>,
        val directories: List<Path>,
        val missing: List<Path>,
    )

    companion object {
        /** System-property name that points at the dump directory. */
        const val SYSTEM_PROPERTY = "translator.classpathDir"

        /**
         * Build a resolver from the `translator.classpathDir` system
         * property. Returns a resolver whose [isAvailable] is `false`
         * if the property is unset.
         */
        fun fromSystemProperties(): ClasspathResolver {
            val dir = System.getProperty(SYSTEM_PROPERTY)?.takeIf { it.isNotBlank() }
            return ClasspathResolver(dir?.let { Paths.get(it) })
        }
    }
}
