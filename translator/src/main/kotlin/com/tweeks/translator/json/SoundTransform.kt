package com.tweeks.translator.json

import com.tweeks.translator.emit.Untranslatable
import com.tweeks.translator.manifest.BedrockTarget
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Translates Java's `assets/<modid>/sounds.json` into Bedrock's
 * `resource_pack/sounds/sound_definitions.json`.
 *
 * Java schema (per-event):
 * ```
 * {
 *   "<event_name>": {
 *     "subtitle": "subtitles.entity.villager.ambient",
 *     "sounds": [ "minecraft:entity/villager/idle1", ... ]
 *   }
 * }
 * ```
 *
 * Bedrock schema:
 * ```
 * {
 *   "format_version": "1.14.0",
 *   "sound_definitions": {
 *     "<modid>:<event_name>": {
 *       "category": "neutral",
 *       "sounds": [ { "name": "sounds/<path>", "stream": false } ]
 *     }
 *   }
 * }
 * ```
 *
 * Phase 1a vanilla-sound mapping is deliberately approximate:
 * `minecraft:entity/villager/idle1` → `sounds/mob/villager/idle1` (drop the
 * `minecraft:` prefix, prepend `sounds/`). Real path mapping requires the
 * vanilla Bedrock sound table — we record each translated path in
 * UNTRANSLATABLE.md so reviewers can verify or correct in-game.
 *
 * Mod-local sounds (no `minecraft:` prefix) get `sounds/<path>` directly.
 *
 * The `subtitle` field is dropped (Bedrock's subtitle system is structurally
 * different) and recorded in UNTRANSLATABLE.md.
 */
class SoundTransform(
    private val target: BedrockTarget,
    private val untranslatable: Untranslatable,
) {

    fun translate(modRoot: Path, modId: String, outputRoot: Path) {
        val input = modRoot.resolve("src/main/resources/assets/$modId/sounds.json")
        if (!input.isRegularFile()) return

        val outputDir = outputRoot.resolve("$modId/resource_pack/sounds")
        outputDir.createDirectories()

        val translated = translateJson(input.readText(), modId)
        outputDir.resolve("sound_definitions.json").writeText(translated)
    }

    fun translateJson(javaJson: String, modId: String): String {
        val parsed = Json.parseToJsonElement(javaJson).jsonObject

        val definitions = buildJsonObject {
            // Sort event names for byte-stable output.
            val entries = parsed.entries.sortedBy { it.key }
            for ((eventName, value) in entries) {
                val event = value.jsonObject
                val key = "$modId:$eventName"

                if (event.containsKey("subtitle")) {
                    untranslatable.recordSoundSubtitleDropped(modId, eventName)
                }

                val translatedSounds = buildJsonArray {
                    val javaSounds = event["sounds"]?.jsonArray ?: return@buildJsonArray
                    for (s in javaSounds) {
                        add(translateSoundEntry(s, modId))
                    }
                }

                put(
                    key,
                    buildJsonObject {
                        put("category", "neutral")
                        put("sounds", translatedSounds)
                    },
                )
            }
        }

        val out = buildJsonObject {
            put("format_version", target.format_versions.sounds)
            put("sound_definitions", definitions)
        }
        return JsonFormat.PRETTY.encodeToString(JsonElement.serializer(), out) + "\n"
    }

    /**
     * A Java sound entry can be a bare string ("minecraft:foo/bar") or an
     * object ({"name": "minecraft:foo/bar", "stream": true, ...}).
     * Bedrock entries are always objects.
     */
    private fun translateSoundEntry(entry: JsonElement, modId: String): JsonObject {
        val (rawName, stream) = when (entry) {
            is JsonPrimitive -> entry.content to false
            is JsonObject -> {
                val name = entry["name"]?.jsonPrimitive?.content ?: ""
                val s = entry["stream"]?.jsonPrimitive?.booleanOrNull ?: false
                name to s
            }
            else -> "" to false
        }

        val translatedPath = translateSoundPath(rawName, modId)
        return buildJsonObject {
            put("name", translatedPath)
            put("stream", stream)
        }
    }

    /**
     * Approximate Java→Bedrock sound-path mapping for Phase 1a.
     *
     *   "minecraft:entity/villager/idle1" → "sounds/mob/villager/idle1"
     *     (drop `minecraft:`, swap `entity/` for the conventional Bedrock
     *      `mob/` directory, prepend `sounds/`)
     *   "minecraft:foo/bar"               → "sounds/foo/bar" (no swap)
     *   "modid:foo/bar" / "foo/bar"       → "sounds/foo/bar" (mod-local)
     */
    private fun translateSoundPath(rawName: String, modId: String): String {
        val path = when {
            rawName.startsWith("minecraft:") -> rawName.removePrefix("minecraft:")
            rawName.startsWith("$modId:") -> rawName.removePrefix("$modId:")
            else -> rawName
        }
        // Bedrock vanilla mob sounds live under `sounds/mob/`; Java names them
        // under `entity/`. Best-effort swap; recorded as approximate so a
        // human can verify in-game.
        val swapped = if (rawName.startsWith("minecraft:") && path.startsWith("entity/")) {
            "mob/" + path.removePrefix("entity/")
        } else {
            path
        }
        if (rawName.startsWith("minecraft:")) {
            untranslatable.recordVanillaSoundPath(modId, rawName, "sounds/$swapped")
        }
        return "sounds/$swapped"
    }
}
