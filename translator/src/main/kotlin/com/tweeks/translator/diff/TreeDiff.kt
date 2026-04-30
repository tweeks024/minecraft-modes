package com.tweeks.translator.diff

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile

/**
 * Phase 5.4: file-level diff between two trees, used by `--diff` mode.
 *
 * Walks `expected` and `actual` recursively, treating regular files as the
 * comparison unit. Reports added (only in actual), deleted (only in
 * expected), and modified (present in both, byte-different) files. Empty
 * directories are ignored — Bedrock's importer doesn't care about them and
 * neither does CI drift detection.
 *
 * The summary is sorted by relative path so diff output is stable across
 * runs.
 */
object TreeDiff {

    /** A single file-level difference between the two trees. */
    sealed class Entry {
        abstract val relPath: String

        data class Added(override val relPath: String, val sizeBytes: Long) : Entry()
        data class Deleted(override val relPath: String, val sizeBytes: Long) : Entry()
        data class Modified(
            override val relPath: String,
            val expectedSize: Long,
            val actualSize: Long,
        ) : Entry()
    }

    /**
     * Compute the diff. [expected] is the on-disk committed tree;
     * [actual] is the freshly-translated tree. Either may be missing —
     * a missing root is treated as an empty tree (so every file in the
     * other root shows up).
     *
     * Optionally restrict comparison to a subset of mod ids (matched as
     * top-level directory names). Useful when the user passes `--diff
     * securityguard` and only wants drift on that one.
     */
    fun diff(expected: Path, actual: Path, modFilter: String? = null): List<Entry> {
        val expectedFiles = listFiles(expected, modFilter)
        val actualFiles = listFiles(actual, modFilter)

        val out = mutableListOf<Entry>()
        val keys = (expectedFiles.keys + actualFiles.keys).toSortedSet()
        for (rel in keys) {
            val ep = expectedFiles[rel]
            val ap = actualFiles[rel]
            when {
                ep == null && ap != null -> out.add(Entry.Added(rel, Files.size(ap)))
                ep != null && ap == null -> out.add(Entry.Deleted(rel, Files.size(ep)))
                ep != null && ap != null -> {
                    if (!filesEqual(ep, ap)) {
                        out.add(Entry.Modified(rel, Files.size(ep), Files.size(ap)))
                    }
                }
            }
        }
        return out
    }

    /**
     * Format the diff as a human-readable summary block. Returns the
     * empty string for an empty diff.
     */
    fun summary(entries: List<Entry>): String {
        if (entries.isEmpty()) return ""
        val sb = StringBuilder()
        sb.append("Drift detected: ${entries.size} file(s).\n")
        for (e in entries) {
            when (e) {
                is Entry.Added -> sb.append("  + ${e.relPath} (${e.sizeBytes} bytes)\n")
                is Entry.Deleted -> sb.append("  - ${e.relPath} (${e.sizeBytes} bytes)\n")
                is Entry.Modified -> sb.append(
                    "  ~ ${e.relPath} (${e.expectedSize} → ${e.actualSize} bytes)\n"
                )
            }
        }
        sb.append("\n")
        sb.append("To fix: rerun `./gradlew :translator:translate` and commit the diff.\n")
        return sb.toString()
    }

    /**
     * Walk [root] and return a map of repo-relative path → absolute Path
     * for every regular file underneath. If [modFilter] is given, only
     * descendants of `<root>/<modFilter>/` are included.
     *
     * Build artifacts at the root of [root] (notably `.mcaddon` zips
     * produced by `:translator:packAddon`) are excluded — they're
     * gitignored and would otherwise show up as drift on every CI run
     * since the temp output never contains them.
     */
    private fun listFiles(root: Path, modFilter: String?): Map<String, Path> {
        if (!Files.isDirectory(root)) return emptyMap()
        val out = mutableMapOf<String, Path>()
        Files.walk(root).use { stream ->
            for (p in stream) {
                if (!p.isRegularFile()) continue
                val rel = root.relativize(p).toString().replace('\\', '/')
                // Skip top-level .mcaddon zips — build artifacts.
                if (!rel.contains('/') && rel.endsWith(".mcaddon")) continue
                if (modFilter != null) {
                    val firstSegment = rel.substringBefore('/')
                    if (firstSegment != modFilter) continue
                }
                out[rel] = p
            }
        }
        return out
    }

    /** Byte-equal comparison. Short-circuits on size mismatch. */
    private fun filesEqual(a: Path, b: Path): Boolean {
        val sizeA = Files.size(a)
        val sizeB = Files.size(b)
        if (sizeA != sizeB) return false
        // For typical Bedrock-pack file sizes (KB-range), a single-shot
        // readAllBytes is fine. If files balloon past a few MB we'd want
        // a streaming compare, but Bedrock manifests / json / images
        // don't get that big in practice.
        val ba = Files.readAllBytes(a)
        val bb = Files.readAllBytes(b)
        return ba.contentEquals(bb)
    }
}
