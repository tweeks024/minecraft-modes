package com.tweeks.translator.java.llm

import com.tweeks.translator.json.JsonFormat
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Phase 3: on-disk cache for [ClaudeClient.TranslationResult]s.
 *
 * Layout: `<rootDir>/<key>.json`. `rootDir` is `translator/.cache/llm/` in
 * the default CLI run; tests pass a temp directory.
 *
 * Both [Ok] and the failure variants ([Refused] / [Error]) are cached so we
 * don't repeatedly hit the API for inputs the model already declined. Phase 3
 * caches forever; Phase 4+ may add TTLs (the spec mentions cache invalidation
 * via prompt_version / model_id / bedrock_api_version, all of which are
 * already part of the cache key — see [ConfidenceGate.computeKey]).
 *
 * The on-disk format is a plain [CachedEntry] in compact JSON. Compact rather
 * than [JsonFormat.PRETTY] so cache files stay small; cache files are not
 * meant to be human-edited.
 */
class TranslationCache(private val rootDir: Path) {

    /** Wraps a [TranslationResult] with a discriminator for serialization. */
    @Serializable
    internal data class CachedEntry(
        val kind: String,
        val js: String? = null,
        val confidence: Double? = null,
        val message: String? = null,
    ) {
        companion object {
            fun of(result: ClaudeClient.TranslationResult): CachedEntry = when (result) {
                is ClaudeClient.TranslationResult.Ok -> CachedEntry(
                    kind = "ok",
                    js = result.js,
                    confidence = result.confidence,
                )
                is ClaudeClient.TranslationResult.Refused -> CachedEntry(
                    kind = "refused",
                    message = result.reason,
                )
                is ClaudeClient.TranslationResult.Error -> CachedEntry(
                    kind = "error",
                    message = result.message,
                )
            }
        }

        fun toResult(): ClaudeClient.TranslationResult = when (kind) {
            "ok" -> ClaudeClient.TranslationResult.Ok(
                js = js ?: "",
                confidence = confidence ?: 0.0,
            )
            "refused" -> ClaudeClient.TranslationResult.Refused(message ?: "")
            else -> ClaudeClient.TranslationResult.Error(message ?: "unknown")
        }
    }

    /** Compact JSON config for cache files. */
    private val compact = Json {
        prettyPrint = false
        encodeDefaults = false
    }

    /**
     * Read the cached result for [key]. Returns null on miss or on any I/O /
     * parse error — the gate treats those as misses, which means the next
     * `--with-llm` run rewrites the entry.
     */
    fun get(key: String): ClaudeClient.TranslationResult? {
        val file = pathFor(key)
        if (!file.exists()) return null
        return try {
            val entry = compact.decodeFromString(CachedEntry.serializer(), file.readText())
            entry.toResult()
        } catch (e: Throwable) {
            null
        }
    }

    /** Write [value] to disk under [key]. Creates the cache root if needed. */
    fun put(key: String, value: ClaudeClient.TranslationResult) {
        rootDir.createDirectories()
        val file = pathFor(key)
        file.writeText(compact.encodeToString(CachedEntry.serializer(), CachedEntry.of(value)))
    }

    /**
     * Recursively delete the entire cache directory. Backs the
     * `--clear-cache` CLI flag.
     */
    fun clear() {
        if (rootDir.exists() && rootDir.isDirectory()) {
            rootDir.toFile().deleteRecursively()
        }
    }

    private fun pathFor(key: String): Path {
        require(key.matches(Regex("^[a-f0-9]{1,128}$"))) {
            "TranslationCache key must be a hex digest, got '$key'"
        }
        return rootDir.resolve("$key.json")
    }
}
