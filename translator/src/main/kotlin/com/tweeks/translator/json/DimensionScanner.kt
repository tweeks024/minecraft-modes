package com.tweeks.translator.json

import com.tweeks.translator.emit.Untranslatable
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.relativeTo
import kotlin.streams.asSequence

/**
 * Detects datapack dimension/worldgen JSON that a Bedrock add-on cannot
 * express, under both datapack roots:
 *     <mod>/src/generated/serverData/data/<modid>/...
 *     <mod>/src/main/resources/data/<modid>/...
 *
 * Families scanned (one honesty entry per JSON file found):
 *   - `dimension/`               — custom dimensions (new worlds)
 *   - `dimension_type/`          — their sky/light/height rules
 *   - `worldgen/biome/`          — custom biomes
 *   - `worldgen/noise_settings/` — density-function terrain backing a
 *     custom chunk generator
 *
 * Like [WorldgenStructureScanner], this is a recording-only stage — a
 * Bedrock world has exactly the built-in Overworld/Nether/End and behavior
 * packs cannot register dimensions, biomes, noise routers, or chunk
 * generators, so there is nothing to emit into `bedrock-out/`. The point is
 * honesty: without this stage, whole planet dimensions would vanish from
 * the translated output with zero trace in `UNTRANSLATABLE.md`.
 */
class DimensionScanner(
    private val untranslatable: Untranslatable,
) {

    fun scan(modRoot: Path, modId: String) {
        scanFamily(modRoot, modId, "dimension") { id ->
            untranslatable.recordDatapackDimension(
                modId,
                "dimension/$id",
                "custom dimension not expressible in a Bedrock add-on — " +
                    "data/$modId/dimension/$id.json; the planet world and travel into it are Java-only",
            )
        }
        scanFamily(modRoot, modId, "dimension_type") { id ->
            untranslatable.recordDatapackDimension(
                modId,
                "dimension_type/$id",
                "custom dimension type not expressible in a Bedrock add-on — " +
                    "data/$modId/dimension_type/$id.json; sky/light/height rules for the planet are Java-only",
            )
        }
        scanFamily(modRoot, modId, "worldgen/biome") { id ->
            untranslatable.recordDatapackBiome(
                modId,
                id,
                "custom biome not translated (no Bedrock biome emitter) — " +
                    "data/$modId/worldgen/biome/$id.json; surface rules, ambience, and spawn lists stay Java-only",
            )
        }
        scanFamily(modRoot, modId, "worldgen/noise_settings") { id ->
            untranslatable.recordDatapackNoiseSettings(
                modId,
                id,
                "custom noise settings / chunk generation not expressible — " +
                    "data/$modId/worldgen/noise_settings/$id.json; terrain shaping lives in the datapack " +
                    "noise router and the mod's Java chunk generator",
            )
        }
        scanFamily(modRoot, modId, "jukebox_song") { id ->
            untranslatable.recordDatapackJukeboxSong(
                modId,
                id,
                "jukebox song not translatable — data/$modId/jukebox_song/$id.json; Bedrock has no " +
                    "data-driven jukebox songs, so the disc item translates as a plain item and the " +
                    "tune plays on Java only",
            )
        }
    }

    /**
     * Walk `data/<modId>/<family>` under both datapack roots and invoke
     * [record] once per JSON file, keyed by its extension-stripped relative
     * path. If both roots define the same relative path, only the first is
     * recorded — generated is the canonical post-build form, mirroring
     * [WorldgenStructureScanner] and LootTableTransform.
     */
    private fun scanFamily(modRoot: Path, modId: String, family: String, record: (String) -> Unit) {
        val inputDirs = listOf(
            modRoot.resolve("src/generated/serverData/data/$modId/$family"),
            modRoot.resolve("src/main/resources/data/$modId/$family"),
        ).filter { it.isDirectory() }
        if (inputDirs.isEmpty()) return

        val seen = mutableSetOf<String>()
        for (inputDir in inputDirs) {
            Files.walk(inputDir).use { stream ->
                stream
                    .asSequence()
                    .filter { it.isRegularFile() && it.extension == "json" }
                    .sortedBy { it.toString() }
                    .forEach { src ->
                        val rel = src.relativeTo(inputDir).toString()
                        if (!seen.add(rel)) return@forEach
                        record(rel.removeSuffix(".json").replace('\\', '/'))
                    }
            }
        }
    }
}
