package com.tweeks.translator.json

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/**
 * Emits Bedrock's `resource_pack/textures/item_texture.json` atlas given the
 * set of item-texture short names produced by [AssetCopier].
 *
 * Output (alphabetized by short name for byte-stable diffs):
 * ```
 * {
 *   "resource_pack_name": "<modid>",
 *   "texture_name": "atlas.items",
 *   "texture_data": {
 *     "<modid>:<short_name>": { "textures": "textures/items/<short_name>" }
 *   }
 * }
 * ```
 *
 * Skips emission entirely if no item textures were collected — Bedrock is
 * happy without an atlas if the pack has no items.
 */
class ItemAtlasBuilder {

    fun build(modId: String, itemTextureShortNames: List<String>, outputRoot: Path) {
        if (itemTextureShortNames.isEmpty()) return

        val outputPath = outputRoot.resolve("$modId/resource_pack/textures/item_texture.json")
        outputPath.parent?.createDirectories()

        val atlas = buildJsonObject {
            put("resource_pack_name", modId)
            put("texture_name", "atlas.items")
            put(
                "texture_data",
                buildJsonObject {
                    for (name in itemTextureShortNames.sorted()) {
                        put(
                            "$modId:$name",
                            buildJsonObject {
                                put("textures", "textures/items/$name")
                            },
                        )
                    }
                },
            )
        }

        outputPath.writeText(JSON.encodeToString(JsonElement.serializer(), atlas) + "\n")
    }

    companion object {
        @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
        val JSON: Json = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }
    }
}
