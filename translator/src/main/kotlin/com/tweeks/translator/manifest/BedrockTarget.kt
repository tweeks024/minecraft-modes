package com.tweeks.translator.manifest

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

/**
 * Loads `translator/bedrock-target.json` — the single source of truth for
 * Bedrock's per-document format-version regime and the pinned engine version.
 */
@Serializable
data class BedrockTarget(
    val min_engine_version: List<Int>,
    val format_versions: FormatVersions,
    val scripting_api_version: String,
) {
    @Serializable
    data class FormatVersions(
        val entity: String,
        val item: String,
        val recipe: String,
        val geometry: String,
        val animation: String,
        val loot_table: String,
    )

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun load(path: Path): BedrockTarget {
            require(path.isRegularFile()) { "bedrock-target.json not found at $path" }
            return json.decodeFromString(serializer(), path.readText())
        }
    }
}
