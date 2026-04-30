package com.tweeks.translator.bbmodel

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Minimal kotlinx-serialization data classes for the subset of `.bbmodel`
 * JSON the [BbmodelConverter] reads. Unknown keys are ignored when parsing
 * — see [BbmodelConverter.JSON] — so this file lists only fields the
 * converter actually uses.
 *
 * The schema mostly matches Blockbench's modded-entity format
 * (`meta.format_version` 4.x and 5.0). Everything else is intentionally
 * absent: locked/visibility/autouv/colors etc. don't influence Bedrock
 * geometry output.
 */
@Serializable
internal data class Bbmodel(
    val name: String? = null,
    val model_identifier: String? = null,
    val resolution: BbResolution = BbResolution(),
    val elements: List<BbElement> = emptyList(),
    /**
     * Heterogeneous list. Each entry is either:
     *   - a [kotlinx.serialization.json.JsonPrimitive] string holding a
     *     UUID that references a top-level cube in [elements] (orphan cube,
     *     no parent group), or
     *   - a [kotlinx.serialization.json.JsonObject] holding a [BbGroup].
     * The decoder leaves it as raw [JsonElement] and [BbmodelConverter]
     * dispatches on the runtime type.
     */
    val outliner: List<JsonElement> = emptyList(),
    val animations: List<BbAnimation> = emptyList(),
    /** `[width, height, depthOffset?]` — [w, h] in blocks; offset optional. */
    val visible_box: List<Double>? = null,
)

@Serializable
internal data class BbResolution(
    val width: Int = 64,
    val height: Int = 64,
)

@Serializable
internal data class BbElement(
    val name: String? = null,
    val type: String? = null,
    val uuid: String,
    val from: List<Double>? = null,
    val to: List<Double>? = null,
    val origin: List<Double>? = null,
    val rotation: List<Double>? = null,
    val uv_offset: List<Double>? = null,
    val mirror_uv: Boolean? = null,
    val inflate: Double? = null,
    /**
     * Raw `faces` blob for face-by-face UV. When present, the cube uses
     * per-face UV (Blockbench's "Face UV" mode) and we emit a best-effort
     * Bedrock per-face translation.
     */
    val faces: JsonElement? = null,
    /** True for box-uv (default), false when faces is in use. */
    val box_uv: Boolean? = null,
)

@Serializable
internal data class BbGroup(
    val name: String,
    val uuid: String,
    val origin: List<Double>? = null,
    val rotation: List<Double>? = null,
    /**
     * Each child entry is either a UUID string referencing an element, or
     * a nested group object — same heterogeneous shape as
     * [Bbmodel.outliner].
     */
    val children: List<JsonElement> = emptyList(),
    /** Locator nodes have a `locator` field; we skip them per the spec. */
    val locator: JsonElement? = null,
)

@Serializable
internal data class BbAnimation(
    val name: String,
    /** `"true"`, `"false"`, `"once"`, or `"hold"` in Blockbench. */
    val loop: String? = null,
    val length: Double = 0.0,
    /** Map of bone-uuid → animator. */
    val animators: Map<String, BbAnimator> = emptyMap(),
)

@Serializable
internal data class BbAnimator(
    val name: String? = null,
    val type: String? = null,
    val keyframes: List<BbKeyframe> = emptyList(),
)

@Serializable
internal data class BbKeyframe(
    val channel: String,
    val time: Double = 0.0,
    val interpolation: String? = null,
    val data_points: List<Map<String, JsonElement>> = emptyList(),
)
