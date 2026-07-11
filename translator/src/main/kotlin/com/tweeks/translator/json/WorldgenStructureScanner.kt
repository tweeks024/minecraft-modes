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
 * Detects datapack worldgen structures under
 *     <mod>/src/generated/serverData/data/<modid>/worldgen/structure/
 *     <mod>/src/main/resources/data/<modid>/worldgen/structure/
 *
 * and records one [Untranslatable.recordDatapackWorldgenStructure] entry per
 * structure JSON found. This is a recording-only stage — there is no Bedrock
 * counterpart to Java's procedural `Structure`/`StructurePiece` datapack API
 * (no `.nbt`/jigsaw template exists for these on disk to translate), so
 * nothing is written to `bedrock-out/`. The point is honesty: before this
 * stage existed, datapack structures vanished from the translated output
 * with zero trace in `UNTRANSLATABLE.md`.
 */
class WorldgenStructureScanner(
    private val untranslatable: Untranslatable,
) {

    fun scan(modRoot: Path, modId: String) {
        val inputDirs = listOf(
            modRoot.resolve("src/generated/serverData/data/$modId/worldgen/structure"),
            modRoot.resolve("src/main/resources/data/$modId/worldgen/structure"),
        ).filter { it.isDirectory() }
        if (inputDirs.isEmpty()) return

        // If both roots define the same relative path, only record it once —
        // generated is the canonical post-build form, mirroring LootTableTransform.
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
                        val structureId = rel.removeSuffix(".json").replace('\\', '/')
                        untranslatable.recordDatapackWorldgenStructure(
                            modId,
                            structureId,
                            "datapack worldgen structure not translatable to Bedrock — $structureId; " +
                                "garrison/loot behavior lives in the Java piece",
                        )
                    }
            }
        }
    }
}
