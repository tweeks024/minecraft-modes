package com.tweeks.translator.json

import com.tweeks.translator.emit.Untranslatable
import com.tweeks.translator.manifest.BedrockTarget
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
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
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Translates Java vanilla recipe JSON into Bedrock recipe JSON.
 *
 * Reads every JSON file under
 *     <mod>/src/generated/serverData/data/<modid>/recipe/
 *
 * Writes one Bedrock recipe per source file under
 *     bedrock-out/<modid>/behavior_pack/recipes/<recipe_name>.json
 *
 * Supported Java recipe types in this repo today:
 *   - `minecraft:crafting_shaped`   → Bedrock `minecraft:recipe_shaped`
 *   - `minecraft:crafting_shapeless` → Bedrock `minecraft:recipe_shapeless`
 *
 * Anything else is logged via [Untranslatable] and skipped — only the two
 * shapes above appear in the four mods we target for Phase 1a.
 *
 * The transform is intentionally narrow:
 *   - Java `category` (datagen-only metadata) is dropped — Bedrock rejects it.
 *   - Java `result` may use `id` (1.21+) or `item` (older). Normalize to `item`.
 *   - Key entries can be a string ("D": "minecraft:blue_dye") or an object.
 *     Normalize to objects with `item` (or `tag` if input had a tag).
 */
class RecipeTransform(
    private val target: BedrockTarget,
    private val untranslatable: Untranslatable,
) {

    /** Run the transform for one mod. No-op if the input directory doesn't exist. */
    fun translate(modRoot: Path, modId: String, outputRoot: Path) {
        val inputDir = modRoot.resolve("src/generated/serverData/data/$modId/recipe")
        if (!inputDir.isDirectory()) return

        val outputDir = outputRoot.resolve("$modId/behavior_pack/recipes")
        outputDir.createDirectories()

        Files.list(inputDir).use { stream ->
            stream
                .filter { it.isRegularFile() && it.extension == "json" }
                .sorted()
                .forEach { translateOne(it, modId, outputDir) }
        }
    }

    /** Translate a single Java recipe file → Bedrock recipe file. Public for tests. */
    fun translateOne(inputPath: Path, modId: String, outputDir: Path) {
        val recipeName = inputPath.nameWithoutExtension
        val translated = translateJson(inputPath.readText(), modId, recipeName) ?: return
        outputDir.createDirectories()
        outputDir.resolve("$recipeName.json").writeText(translated)
    }

    /**
     * Translate raw Java recipe JSON → pretty-printed Bedrock recipe JSON.
     * Returns null if the recipe type is unsupported (caller treats as skip).
     */
    fun translateJson(javaJson: String, modId: String, recipeName: String): String? {
        val parsed = Json.parseToJsonElement(javaJson).jsonObject
        val type = parsed["type"]?.jsonPrimitive?.content
        val identifier = "$modId:$recipeName"

        if (parsed.containsKey("category")) {
            untranslatable.recordRecipeCategoryDropped(modId, recipeName)
        }

        val bedrockRecipe = when (type) {
            "minecraft:crafting_shaped" -> buildShaped(parsed, identifier)
            "minecraft:crafting_shapeless" -> buildShapeless(parsed, identifier)
            else -> {
                untranslatable.recordRecipeTypeSkipped(modId, recipeName, type ?: "<missing type>")
                return null
            }
        }

        val bedrock = buildJsonObject {
            put("format_version", target.format_versions.recipe)
            put(
                if (type == "minecraft:crafting_shaped") "minecraft:recipe_shaped"
                else "minecraft:recipe_shapeless",
                bedrockRecipe,
            )
        }
        return JsonFormat.PRETTY.encodeToString(JsonElement.serializer(), bedrock) + "\n"
    }

    private fun buildShaped(java: JsonObject, identifier: String): JsonObject = buildJsonObject {
        put("description", buildJsonObject { put("identifier", identifier) })
        put("tags", buildJsonArray { add("crafting_table") })
        put("pattern", java["pattern"]?.jsonArray ?: JsonArray(emptyList()))
        put("key", normalizeKey(java["key"]?.jsonObject))
        put("result", normalizeResult(java["result"]))
    }

    private fun buildShapeless(java: JsonObject, identifier: String): JsonObject = buildJsonObject {
        put("description", buildJsonObject { put("identifier", identifier) })
        put("tags", buildJsonArray { add("crafting_table") })
        put(
            "ingredients",
            buildJsonArray {
                java["ingredients"]?.jsonArray?.forEach { add(normalizeIngredient(it)) }
            },
        )
        put("result", normalizeResult(java["result"]))
    }

    /**
     * Normalize a Java `key` map. Each value may be a bare string id, an
     * object with `item`, or an object with `tag`. Output is always an object
     * with `item` (or `tag`). Keys are sorted alphabetically for byte-stable
     * output.
     */
    private fun normalizeKey(key: JsonObject?): JsonObject {
        if (key == null) return JsonObject(emptyMap())
        val sorted = key.entries.sortedBy { it.key }
        return buildJsonObject {
            for ((k, v) in sorted) {
                put(k, normalizeIngredient(v))
            }
        }
    }

    /** "minecraft:iron_ingot" or {item: ...} or {tag: ...} → {item: ...} or {tag: ...}. */
    private fun normalizeIngredient(value: JsonElement): JsonObject {
        return when (value) {
            is JsonPrimitive -> buildJsonObject { put("item", value.content) }
            is JsonObject -> when {
                value.containsKey("tag") -> buildJsonObject { put("tag", value["tag"]!!.jsonPrimitive.content) }
                value.containsKey("item") -> buildJsonObject { put("item", value["item"]!!.jsonPrimitive.content) }
                value.containsKey("id") -> buildJsonObject { put("item", value["id"]!!.jsonPrimitive.content) }
                else -> JsonObject(emptyMap())
            }
            else -> JsonObject(emptyMap())
        }
    }

    /**
     * Normalize a Java `result`. Java uses either `id` (1.21+) or `item`
     * (older); Bedrock uses `item`. Optional `count` is preserved (and only
     * emitted if > 1, matching Bedrock's recipe-doc convention).
     */
    private fun normalizeResult(value: JsonElement?): JsonObject {
        if (value == null) return JsonObject(emptyMap())
        return when (value) {
            is JsonPrimitive -> buildJsonObject { put("item", value.content) }
            is JsonObject -> {
                val itemId =
                    value["id"]?.jsonPrimitive?.content
                        ?: value["item"]?.jsonPrimitive?.content
                        ?: ""
                val count = value["count"]?.jsonPrimitive?.intOrNull
                buildJsonObject {
                    put("item", itemId)
                    if (count != null && count > 1) put("count", count)
                }
            }
            else -> JsonObject(emptyMap())
        }
    }

}
