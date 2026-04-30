package com.tweeks.translator.emit

import com.tweeks.translator.discover.ModDiscovery.DiscoveredMod
import com.tweeks.translator.manifest.ManifestWriter
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/**
 * Writes a Bedrock Add-On directory tree for a single discovered mod.
 *
 * Phase 0 only emits the two `manifest.json` files needed to make the pack
 * structurally valid. JSON-pipeline output (recipes, lang, loot, textures,
 * geometry, animation) lands in Phase 1; entity / item / behavior content
 * lands in Phases 2–3.
 *
 * Output layout:
 *   bedrock-out/<mod_id>/
 *     behavior_pack/manifest.json
 *     resource_pack/manifest.json
 */
class AddonWriter(
    private val outputRoot: Path,
    private val manifestWriter: ManifestWriter,
) {
    /**
     * Emit the Bedrock Add-On scaffold for [mod]. Existing files for this
     * specific mod are overwritten; sibling mods' subdirectories under
     * [outputRoot] are left untouched.
     */
    fun write(mod: DiscoveredMod, inputs: ManifestWriter.ModManifestInputs): WriteResult {
        require(mod.modId == inputs.modId) {
            "ModManifestInputs.modId (${inputs.modId}) must match DiscoveredMod.modId (${mod.modId})"
        }

        val modOut = outputRoot.resolve(mod.modId)
        val bpDir = modOut.resolve("behavior_pack")
        val rpDir = modOut.resolve("resource_pack")
        bpDir.createDirectories()
        rpDir.createDirectories()

        val manifests = manifestWriter.build(inputs)
        val bpManifest = bpDir.resolve("manifest.json")
        val rpManifest = rpDir.resolve("manifest.json")
        bpManifest.writeText(manifests.behaviorPackManifest)
        rpManifest.writeText(manifests.resourcePackManifest)

        return WriteResult(
            modId = mod.modId,
            outputDir = modOut,
            behaviorManifest = bpManifest,
            resourceManifest = rpManifest,
        )
    }

    data class WriteResult(
        val modId: String,
        val outputDir: Path,
        val behaviorManifest: Path,
        val resourceManifest: Path,
    )

    companion object {
        /**
         * Default output root: `<repoRoot>/bedrock-out`. Created if missing.
         * Does not delete or scrub the directory — siblings' subdirs are
         * preserved across runs.
         */
        fun defaultOutputRoot(repoRoot: Path): Path {
            val out = repoRoot.resolve("bedrock-out")
            if (!Files.exists(out)) out.createDirectories()
            return out
        }
    }
}
