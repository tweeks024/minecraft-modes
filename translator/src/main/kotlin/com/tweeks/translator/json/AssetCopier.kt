package com.tweeks.translator.json

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension

/**
 * Copies textures from a Java mod's `assets/<modid>/textures/` tree into the
 * Bedrock resource pack, mapping Java's per-category subdirectories to
 * Bedrock's expected locations.
 *
 * Java → Bedrock mapping:
 *   textures/item/<name>.png  → resource_pack/textures/items/<name>.png
 *   textures/entity/<rel>     → resource_pack/textures/entity/<rel>
 *   textures/block/<rel>      → resource_pack/textures/blocks/<rel>
 *
 * Other top-level categories under `textures/` log a warning and are skipped.
 *
 * The list of item-texture short names collected during the walk feeds
 * [ItemAtlasBuilder] so the atlas accurately reflects what was copied.
 */
class AssetCopier {

    /**
     * Result of a copy pass. `itemTextureShortNames` is the set of item PNG
     * names (no extension) ready to be wired into `item_texture.json`.
     */
    data class CopyResult(
        val itemTextureShortNames: List<String>,
    )

    fun copy(modRoot: Path, modId: String, outputRoot: Path): CopyResult {
        val texturesRoot = modRoot.resolve("src/main/resources/assets/$modId/textures")
        if (!texturesRoot.isDirectory()) return CopyResult(emptyList())

        val rpDir = outputRoot.resolve("$modId/resource_pack")
        val itemNames = mutableListOf<String>()

        Files.list(texturesRoot).use { stream ->
            for (categoryDir in stream.sorted()) {
                if (!categoryDir.isDirectory()) continue
                when (categoryDir.fileName.toString()) {
                    "item" -> {
                        val dest = rpDir.resolve("textures/items")
                        dest.createDirectories()
                        for (file in walkPngs(categoryDir)) {
                            val name = file.fileName.toString()
                            Files.copy(
                                file,
                                dest.resolve(name),
                                StandardCopyOption.REPLACE_EXISTING,
                            )
                            itemNames += file.nameWithoutExtension
                        }
                    }
                    "entity" -> copyTreeOfPngs(categoryDir, rpDir.resolve("textures/entity"))
                    "block" -> copyTreeOfPngs(categoryDir, rpDir.resolve("textures/blocks"))
                    else -> {
                        System.err.println(
                            "[translator] Skipping unknown texture category " +
                                "'$modId/textures/${categoryDir.fileName}'."
                        )
                    }
                }
            }
        }

        return CopyResult(itemTextureShortNames = itemNames.sorted())
    }

    /** Walk a category tree and yield every regular `.png`. */
    private fun walkPngs(root: Path): Sequence<Path> {
        return Files.walk(root).use { it.toList() }
            .asSequence()
            .filter { it.isRegularFile() && it.extension == "png" }
            .sortedBy { it.toString() }
    }

    /** Mirror a category tree of PNGs into [destRoot], preserving subpaths. */
    private fun copyTreeOfPngs(categoryDir: Path, destRoot: Path) {
        for (file in walkPngs(categoryDir)) {
            val rel = categoryDir.relativize(file)
            val out = destRoot.resolve(rel.toString())
            out.parent?.createDirectories()
            Files.copy(file, out, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
