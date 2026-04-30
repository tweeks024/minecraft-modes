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
        /**
         * Format version for `resource_pack/sounds/sound_definitions.json`.
         * Defaulted to "1.14.0" — the published Bedrock value for sound
         * definitions today — so that older `bedrock-target.json` files (no
         * `sounds` key) continue to load. Override in the JSON if Mojang ships
         * a newer sounds format and we want to opt in.
         */
        val sounds: String = "1.14.0",
        /**
         * Format version for `resource_pack/entity/<entity_id>.entity.json`
         * (the client-side entity description). Defaulted to "1.10.0" — the
         * Bedrock value still in active use — so older `bedrock-target.json`
         * files load. Bump in the JSON if a newer client_entity schema is
         * needed.
         */
        val client_entity: String = "1.10.0",
        /**
         * Format version for `resource_pack/attachables/<item_id>.json`
         * (held-item attachable descriptions). Defaulted to "1.10.0" for the
         * same reasons as [client_entity].
         */
        val attachable: String = "1.10.0",
    )

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun load(path: Path): BedrockTarget {
            require(path.isRegularFile()) { "bedrock-target.json not found at $path" }
            return json.decodeFromString(serializer(), path.readText())
        }
    }
}
