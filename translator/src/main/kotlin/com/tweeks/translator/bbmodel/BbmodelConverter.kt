package com.tweeks.translator.bbmodel

import com.tweeks.translator.emit.Untranslatable
import com.tweeks.translator.json.JsonFormat
import com.tweeks.translator.manifest.BedrockTarget
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
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
 * Translates Blockbench `.bbmodel` JSON into Bedrock geometry
 * (`<name>.geo.json`) and, when present, animation
 * (`<name>.animation.json`) files.
 *
 * Reads every `*.bbmodel` under `<modRoot>/tools/`. Per file we emit:
 *   - `bedrock-out/<modid>/resource_pack/models/entity/<name>.geo.json`
 *   - `bedrock-out/<modid>/resource_pack/animations/<name>.animation.json`
 *     (only when the bbmodel has at least one animation defined)
 *
 * **Pivot/origin coordinate system.** For Bedrock geometry
 * `format_version: "1.16.0"` (the version pinned in `bedrock-target.json`):
 * both bone `pivot` and cube `origin` live in **global model space**.
 * Blockbench stores group `origin` as the bone pivot in global model space
 * and element `from` as the cube's lower-corner global coord — so direct
 * copy is correct. Older Bedrock geometry versions (`1.8.0`, `1.12.0`)
 * interpret cube `origin` differently relative to the bone's pivot, which
 * is what the design spec's "walk the bone tree once to compute parent
 * chains" note guards against. As long as we stay on `1.16.0` we don't
 * need a parent-walk transform; do not reintroduce one without re-reading
 * the spec.
 *
 * Visual alignment in-game has not been verified against a Bedrock client
 * yet (we have no headless renderer). If a model spawns mis-aligned the
 * spec calls for an offset-correction step here, not hand edits to the
 * emitted `.geo.json`.
 */
class BbmodelConverter(
    private val target: BedrockTarget,
    private val unt: Untranslatable,
) {

    /**
     * Walk every `.bbmodel` under [toolsDir] (sorted) and emit Bedrock
     * outputs under [outputRoot]. No-op if [toolsDir] doesn't exist (e.g.
     * mods without any models).
     */
    fun convert(modId: String, toolsDir: Path, outputRoot: Path) {
        if (!toolsDir.isDirectory()) return

        Files.list(toolsDir).use { stream ->
            stream
                .filter { it.isRegularFile() && it.extension == "bbmodel" }
                .sorted()
                .forEach { convertOne(it, modId, outputRoot) }
        }
    }

    /** Convert a single `.bbmodel`. Public for tests. */
    fun convertOne(input: Path, modId: String, outputRoot: Path) {
        val modelName = input.nameWithoutExtension
        val raw = input.readText()
        val bb = JSON.decodeFromString(Bbmodel.serializer(), raw)

        val geoText = buildGeometryJson(bb, modId, modelName)
        val geoOut = outputRoot.resolve("$modId/resource_pack/models/entity/$modelName.geo.json")
        geoOut.parent?.createDirectories()
        geoOut.writeText(geoText)

        if (bb.animations.isNotEmpty()) {
            val animText = buildAnimationJson(bb, modId, modelName)
            val animOut = outputRoot.resolve("$modId/resource_pack/animations/$modelName.animation.json")
            animOut.parent?.createDirectories()
            animOut.writeText(animText)
        }
    }

    // ---------- Geometry ----------

    /** Build the geometry JSON text for one bbmodel. Internal for tests. */
    internal fun buildGeometryJson(bb: Bbmodel, modId: String, modelName: String): String {
        val identifier = "geometry.$modId.$modelName"
        val elementsByUuid = bb.elements.associateBy { it.uuid }

        // Walk the outliner to produce bones, in traversal order. Bedrock's
        // `bones` is an ordered array — parents must come before children, so
        // do NOT alphabetize.
        val bones = mutableListOf<JsonObject>()
        val orphanUuids = mutableListOf<String>()
        for (entry in bb.outliner) {
            walkOutlinerEntry(
                entry = entry,
                parentName = null,
                elementsByUuid = elementsByUuid,
                bones = bones,
                orphanUuids = orphanUuids,
                modId = modId,
                modelName = modelName,
            )
        }
        // Top-level orphan cubes (UUIDs sitting directly under outliner with
        // no group) get a synthetic root bone. None of the four current
        // bbmodels with a humanoid layout hit this; the creeper armor pieces
        // do.
        if (orphanUuids.isNotEmpty()) {
            val rootCubes = orphanUuids.mapNotNull { elementsByUuid[it] }
                .mapNotNull { elementToCube(it, modId, modelName) }
            val root = buildJsonObject {
                put("name", "root")
                put("pivot", jsonArrayOfDoubles(listOf(0.0, 0.0, 0.0)))
                if (rootCubes.isNotEmpty()) {
                    put("cubes", buildJsonArray { rootCubes.forEach { add(it) } })
                }
            }
            // Insert at front so it's available as a parent if other bones
            // ever reference it. (None do today; the orphan path is leaf-only.)
            bones.add(0, root)
        }

        val description = buildJsonObject {
            put("identifier", identifier)
            put("texture_width", bb.resolution.width)
            put("texture_height", bb.resolution.height)
            // Blockbench `visible_box` is `[width, height, depth_offset]`.
            // Bedrock's `visible_bounds_offset` is `[x, y, z]`; Blockbench
            // only stores a single Y-offset (the third element), with X/Z
            // implicitly zero. Default everything to 0 if not present.
            val vb = bb.visible_box
            val vw = vb?.getOrNull(0) ?: 0.0
            val vh = vb?.getOrNull(1) ?: 0.0
            val voff = vb?.getOrNull(2) ?: 0.0
            put("visible_bounds_width", numberPrim(vw))
            put("visible_bounds_height", numberPrim(vh))
            put(
                "visible_bounds_offset",
                jsonArrayOfDoubles(listOf(0.0, voff, 0.0)),
            )
        }

        val geometryEntry = buildJsonObject {
            put("description", description)
            put("bones", buildJsonArray { bones.forEach { add(it) } })
        }

        val out = buildJsonObject {
            put("format_version", target.format_versions.geometry)
            put(
                "minecraft:geometry",
                buildJsonArray { add(geometryEntry) },
            )
        }
        return JsonFormat.PRETTY.encodeToString(JsonElement.serializer(), out) + "\n"
    }

    /**
     * Recursively walk an outliner entry. Groups become bones; UUID strings
     * either become cubes on their parent bone (if there is one) or
     * accumulate into [orphanUuids] for synthetic-root assignment.
     */
    private fun walkOutlinerEntry(
        entry: JsonElement,
        parentName: String?,
        elementsByUuid: Map<String, BbElement>,
        bones: MutableList<JsonObject>,
        orphanUuids: MutableList<String>,
        modId: String,
        modelName: String,
    ) {
        when (entry) {
            is JsonPrimitive -> {
                if (entry.isString) {
                    if (parentName == null) {
                        orphanUuids.add(entry.content)
                    }
                    // else: shouldn't happen — UUID children of groups are
                    // emitted in the group's `cubes` block when the group
                    // itself is processed. We only land here for top-level
                    // orphan UUIDs.
                }
            }
            is JsonObject -> {
                val group = JSON.decodeFromJsonElement(BbGroup.serializer(), entry)
                if (group.locator != null) {
                    unt.recordBbmodelLocatorSkipped(modId, "$modelName/${group.name}")
                    return
                }
                val bone = buildBone(group, parentName, elementsByUuid, modId, modelName)
                bones.add(bone)
                // Recurse into nested groups (subgroups become child bones).
                for (child in group.children) {
                    if (child is JsonObject) {
                        walkOutlinerEntry(
                            entry = child,
                            parentName = group.name,
                            elementsByUuid = elementsByUuid,
                            bones = bones,
                            orphanUuids = orphanUuids,
                            modId = modId,
                            modelName = modelName,
                        )
                    }
                }
            }
            else -> { /* JsonNull / array / unexpected — ignore */ }
        }
    }

    private fun buildBone(
        group: BbGroup,
        parentName: String?,
        elementsByUuid: Map<String, BbElement>,
        modId: String,
        modelName: String,
    ): JsonObject {
        // Resolve cube children only — sub-groups become their own bones.
        val cubes = group.children
            .mapNotNull {
                if (it is JsonPrimitive && it.isString) elementsByUuid[it.content] else null
            }
            .mapNotNull { elementToCube(it, modId, modelName) }

        return buildJsonObject {
            put("name", group.name)
            if (parentName != null) put("parent", parentName)
            put("pivot", jsonArrayOfDoubles(group.origin ?: listOf(0.0, 0.0, 0.0)))
            val rot = group.rotation
            if (rot != null && rot.any { it != 0.0 }) {
                put("rotation", jsonArrayOfDoubles(rot))
            }
            if (cubes.isNotEmpty()) {
                put("cubes", buildJsonArray { cubes.forEach { add(it) } })
            }
        }
    }

    /**
     * Translate one Blockbench element (cube) → Bedrock cube. Returns null
     * for non-cube types (mesh, etc.) and records them in the untranslatable
     * report.
     */
    private fun elementToCube(
        e: BbElement,
        modId: String,
        modelName: String,
    ): JsonObject? {
        // Default type when omitted is "cube" (Blockbench convention).
        val type = e.type ?: "cube"
        if (type != "cube") {
            unt.recordBbmodelElementTypeSkipped(modId, "$modelName/${e.name ?: e.uuid}", type)
            return null
        }

        val from = e.from ?: return null
        val to = e.to ?: return null
        val size = listOf(to[0] - from[0], to[1] - from[1], to[2] - from[2])

        // Face-uv vs box-uv: per Blockbench's bbmodel layout, `box_uv: true`
        // (the default) means the cube uses a single `uv_offset` to atlas the
        // texture; the `faces` blob may still be present in that case but is
        // computed-on-export data and should be ignored. Only when
        // `box_uv: false` does the cube genuinely use per-face UV that needs
        // structural translation.
        val usesFaceUv = e.box_uv == false

        return buildJsonObject {
            put("origin", jsonArrayOfDoubles(from))
            put("size", jsonArrayOfDoubles(size))
            if (usesFaceUv) {
                unt.recordBbmodelFaceUv(modId, "$modelName/${e.name ?: e.uuid}")
                val faceUv = buildFaceUv(e.faces)
                put("uv", faceUv)
            } else {
                val uv = e.uv_offset ?: listOf(0.0, 0.0)
                put("uv", jsonArrayOfDoubles(uv))
            }
            val infl = e.inflate
            if (infl != null && infl != 0.0) put("inflate", numberPrim(infl))
            if (e.mirror_uv == true) put("mirror", true)
            // Cube-level rotation. Bedrock cubes accept their own
            // `pivot` + `rotation`. Only emit when there's actually a
            // rotation, and emit pivot from the element's `origin` (which
            // for Blockbench is the cube's rotation pivot).
            val rot = e.rotation
            if (rot != null && rot.any { it != 0.0 }) {
                put("pivot", jsonArrayOfDoubles(e.origin ?: listOf(0.0, 0.0, 0.0)))
                put("rotation", jsonArrayOfDoubles(rot))
            }
        }
    }

    /**
     * Best-effort translation of Blockbench's per-face UV blob into
     * Bedrock's per-face `uv` object. Blockbench's `faces` is keyed by
     * face name (`north`, `south`, `east`, `west`, `up`, `down`) with each
     * value being an object that includes `uv: [x1, y1, x2, y2]` and
     * `texture` (an index, or -1 for "no texture"). Bedrock per-face uv
     * is `{ "<face>": { "uv": [u, v], "uv_size": [w, h] } }`.
     *
     * We can't always reconstruct true UV size from Blockbench's
     * `[x1, y1, x2, y2]` — we approximate as
     * `uv = [x1, y1]`, `uv_size = [x2 - x1, y2 - y1]`. Faces with
     * `texture: null` or missing UV are omitted.
     */
    private fun buildFaceUv(faces: JsonElement?): JsonObject = buildJsonObject {
        if (faces !is JsonObject) return@buildJsonObject
        // Sort face names for byte-stable output.
        val entries = faces.entries.sortedBy { it.key }
        for ((faceName, faceVal) in entries) {
            if (faceVal !is JsonObject) continue
            val uv = (faceVal["uv"] as? JsonArray) ?: continue
            if (uv.size < 4) continue
            val x1 = uv[0].jsonPrimitive.doubleOrNull ?: continue
            val y1 = uv[1].jsonPrimitive.doubleOrNull ?: continue
            val x2 = uv[2].jsonPrimitive.doubleOrNull ?: continue
            val y2 = uv[3].jsonPrimitive.doubleOrNull ?: continue
            put(
                faceName,
                buildJsonObject {
                    put("uv", jsonArrayOfDoubles(listOf(x1, y1)))
                    put("uv_size", jsonArrayOfDoubles(listOf(x2 - x1, y2 - y1)))
                },
            )
        }
    }

    // ---------- Animations ----------

    internal fun buildAnimationJson(bb: Bbmodel, modId: String, modelName: String): String {
        // Resolve animator-uuid → bone-name once, walking the outliner. We
        // need this because Blockbench keys animators by group uuid, but
        // Bedrock animations target bones by name.
        val boneNamesByUuid = mutableMapOf<String, String>()
        collectGroupNames(bb.outliner, boneNamesByUuid)

        val animations = buildJsonObject {
            // Sort animation names so byte-stable output doesn't depend on
            // bbmodel insertion order.
            val sortedAnims = bb.animations.sortedBy { it.name }
            for (anim in sortedAnims) {
                val animKey = "animation.$modId.$modelName.${anim.name}"
                put(animKey, buildOneAnimation(anim, boneNamesByUuid, modId, modelName))
            }
        }

        val out = buildJsonObject {
            put("format_version", target.format_versions.animation)
            put("animations", animations)
        }
        return JsonFormat.PRETTY.encodeToString(JsonElement.serializer(), out) + "\n"
    }

    private fun collectGroupNames(
        outliner: List<JsonElement>,
        out: MutableMap<String, String>,
    ) {
        for (entry in outliner) {
            if (entry is JsonObject) {
                val group = JSON.decodeFromJsonElement(BbGroup.serializer(), entry)
                out[group.uuid] = group.name
                collectGroupNames(group.children, out)
            }
        }
    }

    private fun buildOneAnimation(
        anim: BbAnimation,
        boneNamesByUuid: Map<String, String>,
        modId: String,
        modelName: String,
    ): JsonObject = buildJsonObject {
        put("loop", parseLoop(anim.loop))
        put("animation_length", numberPrim(anim.length))

        val bones = buildJsonObject {
            // Sort by bone name for byte-stable output.
            val orderedAnimators = anim.animators.entries
                .map { (uuid, animator) ->
                    val boneName = boneNamesByUuid[uuid] ?: animator.name ?: uuid
                    boneName to animator
                }
                .sortedBy { it.first }
            for ((boneName, animator) in orderedAnimators) {
                put(boneName, buildBoneAnimator(animator, modId, modelName))
            }
        }
        if (bones.isNotEmpty()) put("bones", bones)
    }

    private fun parseLoop(raw: String?): JsonElement {
        // Blockbench stores `loop` as a string ("true"/"false"/"once"/"hold").
        // Bedrock's `loop` is a boolean (true/false) or the string "hold_on_last_frame".
        return when (raw) {
            "true" -> JsonPrimitive(true)
            "hold" -> JsonPrimitive("hold_on_last_frame")
            null, "false", "once" -> JsonPrimitive(false)
            else -> JsonPrimitive(false)
        }
    }

    private fun buildBoneAnimator(
        animator: BbAnimator,
        modId: String,
        modelName: String,
    ): JsonObject = buildJsonObject {
        // Group keyframes by channel (rotation/position/scale).
        val byChannel = animator.keyframes.groupBy { it.channel }
        val channelOrder = listOf("position", "rotation", "scale")
        for (channel in channelOrder) {
            val kfs = byChannel[channel] ?: continue
            val sortedKfs = kfs.sortedBy { it.time }
            put(
                channel,
                buildJsonObject {
                    for (kf in sortedKfs) {
                        if (kf.data_points.size > 1) {
                            unt.recordBbmodelMultiDataPointKeyframe(
                                modId,
                                "$modelName/${animator.name ?: "?"}#$channel@${kf.time}",
                            )
                        }
                        if (kf.interpolation != null && kf.interpolation != "linear") {
                            unt.recordBbmodelNonLinearInterpolation(
                                modId,
                                "$modelName/${animator.name ?: "?"}#$channel@${kf.time}",
                                kf.interpolation,
                            )
                        }
                        val dp = kf.data_points.firstOrNull() ?: continue
                        val x = dp["x"]?.toDoubleOrZero() ?: 0.0
                        val y = dp["y"]?.toDoubleOrZero() ?: 0.0
                        val z = dp["z"]?.toDoubleOrZero() ?: 0.0
                        // Format the key as a numeric-style string. Bedrock
                        // accepts string keys; emit "0" / "0.5" / "1" rather
                        // than "0.0" / "1.0" so the output reads naturally.
                        put(formatTimeKey(kf.time), jsonArrayOfDoubles(listOf(x, y, z)))
                    }
                },
            )
        }
    }

    private fun JsonElement.toDoubleOrZero(): Double {
        if (this !is JsonPrimitive) return 0.0
        // Blockbench occasionally serializes data_points as strings ("0").
        return doubleOrNull ?: content.toDoubleOrNull() ?: 0.0
    }

    /**
     * `0.0` → `"0"`, `0.5` → `"0.5"`, `1.0` → `"1"`. Bedrock animation
     * keyframe keys are conventionally bare integers when a frame lands on
     * a whole second, decimal otherwise.
     */
    private fun formatTimeKey(t: Double): String {
        return if (t == t.toLong().toDouble()) t.toLong().toString() else t.toString()
    }

    // ---------- Helpers ----------

    /**
     * Build a JSON array of numbers. Integer-valued doubles emit as
     * integers (e.g. `0` rather than `0.0`); fractional values keep their
     * full precision. This matches Bedrock geometry output convention and
     * keeps goldens easy to read.
     */
    private fun jsonArrayOfDoubles(vals: List<Double>): JsonArray = buildJsonArray {
        for (v in vals) add(numberPrim(v))
    }

    private fun numberPrim(v: Double): JsonPrimitive {
        return if (v == v.toLong().toDouble()) JsonPrimitive(v.toLong())
        else JsonPrimitive(v)
    }

    companion object {
        internal val JSON: Json = Json {
            ignoreUnknownKeys = true
            // Some bbmodels write `null` for fields like `rotation` instead of
            // omitting them; tolerate that.
            explicitNulls = false
        }
    }
}
