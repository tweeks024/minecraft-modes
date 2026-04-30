package com.tweeks.translator.json

import com.tweeks.translator.emit.Untranslatable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText
import kotlin.streams.asSequence

/**
 * Translates Java vanilla loot-table JSON into Bedrock loot-table JSON.
 *
 * Reads every JSON file (recursively) under
 *     <mod>/src/generated/serverData/data/<modid>/loot_table/
 *
 * Writes (note plural `loot_tables` in Bedrock) under
 *     bedrock-out/<modid>/behavior_pack/loot_tables/<sub_path>/<name>.json
 *
 * Differences vs Java:
 *   - Bedrock drops the `minecraft:` namespace on `type`, function names, and
 *     entry types (`minecraft:item` → `item`, `minecraft:empty` → `empty`,
 *     `minecraft:set_count` → `set_count`, etc.).
 *   - Bedrock has no `random_sequence` concept; that field is dropped.
 *   - Empty loot tables become `{ "pools": [] }`.
 *   - There is no top-level `format_version` in a Bedrock loot table — emit
 *     the JSON directly.
 */
class LootTableTransform(
    private val untranslatable: Untranslatable,
) {

    fun translate(modRoot: Path, modId: String, outputRoot: Path) {
        val inputDir = modRoot.resolve("src/generated/serverData/data/$modId/loot_table")
        if (!inputDir.isDirectory()) return

        val outputDir = outputRoot.resolve("$modId/behavior_pack/loot_tables")
        outputDir.createDirectories()

        Files.walk(inputDir).use { stream ->
            stream
                .asSequence()
                .filter { it.isRegularFile() && it.extension == "json" }
                .sortedBy { it.toString() }
                .forEach { src ->
                    val rel = src.relativeTo(inputDir)
                    val dest = outputDir.resolve(rel.toString())
                    dest.parent?.createDirectories()
                    val translated = translateJson(src.readText(), modId, rel.toString())
                    dest.writeText(translated)
                }
        }
    }

    /** Translate raw Java loot-table JSON to Bedrock JSON (pretty-printed). */
    fun translateJson(javaJson: String, modId: String, relPath: String): String {
        val parsed = Json.parseToJsonElement(javaJson).jsonObject

        if (parsed.containsKey("random_sequence")) {
            untranslatable.recordLootRandomSequenceDropped(modId, relPath)
        }

        val pools = parsed["pools"]?.jsonArray ?: JsonArray(emptyList())
        val translatedPools = buildJsonArray {
            for (pool in pools) {
                add(translatePool(pool.jsonObject))
            }
        }

        val out = buildJsonObject {
            put("pools", translatedPools)
        }
        return JSON.encodeToString(JsonElement.serializer(), out) + "\n"
    }

    private fun translatePool(pool: JsonObject): JsonObject = buildJsonObject {
        // `rolls` is required in Bedrock; carry through whatever Java had.
        pool["rolls"]?.let { put("rolls", it) }
        pool["bonus_rolls"]?.let { put("bonus_rolls", it) }
        pool["conditions"]?.let { put("conditions", stripMinecraftNamespaces(it)) }

        val entries = pool["entries"]?.jsonArray ?: JsonArray(emptyList())
        put(
            "entries",
            buildJsonArray { for (e in entries) add(translateEntry(e.jsonObject)) },
        )
    }

    private fun translateEntry(entry: JsonObject): JsonObject = buildJsonObject {
        // `type` drops `minecraft:` in Bedrock.
        val rawType = entry["type"]?.jsonPrimitive?.contentOrNull
        if (rawType != null) put("type", dropMinecraftPrefix(rawType))
        entry["name"]?.let { put("name", it) }
        entry["weight"]?.let { put("weight", it) }
        entry["quality"]?.let { put("quality", it) }
        entry["functions"]?.jsonArray?.let { fns ->
            put("functions", buildJsonArray { for (f in fns) add(translateFunction(f.jsonObject)) })
        }
        entry["conditions"]?.let { put("conditions", stripMinecraftNamespaces(it)) }
        entry["children"]?.jsonArray?.let { kids ->
            put("children", buildJsonArray { for (k in kids) add(translateEntry(k.jsonObject)) })
        }
    }

    private fun translateFunction(fn: JsonObject): JsonObject = buildJsonObject {
        for ((k, v) in fn) {
            when {
                k == "function" && v is JsonPrimitive ->
                    put("function", JsonPrimitive(dropMinecraftPrefix(v.content)))
                else -> put(k, stripMinecraftNamespaces(v))
            }
        }
    }

    /** Drop a leading `minecraft:` if present. */
    private fun dropMinecraftPrefix(s: String): String =
        if (s.startsWith("minecraft:")) s.removePrefix("minecraft:") else s

    /**
     * Recursively walk a JSON tree, dropping the `minecraft:` prefix from any
     * value of a key named `condition`, `function`, or `type`. Other strings
     * (notably item ids in `name`) are preserved.
     */
    private fun stripMinecraftNamespaces(elem: JsonElement): JsonElement = when (elem) {
        is JsonObject -> JsonObject(
            elem.mapValues { (k, v) ->
                if (v is JsonPrimitive && v.contentOrNull != null &&
                    (k == "condition" || k == "function" || k == "type") &&
                    v.content.startsWith("minecraft:")
                ) {
                    JsonPrimitive(v.content.removePrefix("minecraft:"))
                } else {
                    stripMinecraftNamespaces(v)
                }
            }
        )
        is JsonArray -> JsonArray(elem.map { stripMinecraftNamespaces(it) })
        else -> elem
    }

    companion object {
        @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
        val JSON: Json = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }
    }
}
