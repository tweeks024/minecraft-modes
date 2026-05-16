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
     * Blockbench 5.x stores group metadata (name, origin, rotation) in a
     * sibling top-level array; 4.x embeds groups directly in [outliner].
     * When present, [outliner] entries hold only `{uuid, children}` and the
     * converter looks up the matching entry here by uuid for the metadata.
     */
    val groups: List<BbGroup> = emptyList(),
    /**
     * Heterogeneous list. Each entry is either:
     *   - a [kotlinx.serialization.json.JsonPrimitive] string holding a
     *     UUID that references a top-level cube in [elements] (orphan cube,
     *     no parent group), or
     *   - a [kotlinx.serialization.json.JsonObject] — in 4.x this is a full
     *     [BbGroup]; in 5.x it's a UUID reference into [groups] whose own
     *     `children` field carries the canonical child list.
     * The decoder leaves it as raw [JsonElement] and [BbmodelConverter]
     * dispatches on the runtime type.
     */
    val outliner: List<JsonElement> = emptyList(),
    val animations: List<BbAnimation> = emptyList(),
    /** `[width, height, depthOffset?]` — [w, h] in blocks; offset optional. */
    val visible_box: List<Double>? = null,
    /**
     * Blockbench's `meta` object. Carries `format_version`, `model_format`,
     * and (for modded-entity exports) `modded_entity_flip_y`. The converter
     * inspects only the flip-Y flag.
     */
    val meta: BbMeta? = null,
)

@Serializable
internal data class BbMeta(
    val format_version: String? = null,
    val model_format: String? = null,
    val box_uv: Boolean? = null,
    /**
     * Modded-entity exports flag whether the model's Y axis is flipped to
     * match Java's screen-down convention. When the bbmodel was authored
     * with `modded_entity_flip_y: false`, the Y axis is in its native
     * Blockbench/Bedrock orientation; alignment differs from the typical
     * modded-entity case and is worth flagging in the untranslatable report
     * so a reviewer can verify in-game.
     */
    val modded_entity_flip_y: Boolean? = null,
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
    /**
     * The group's display name (used as the Bedrock bone name). Nullable
     * because Blockbench 5.x outliner entries are stripped-down references
     * `{uuid, children}` with the metadata living in [Bbmodel.groups]. The
     * converter resolves missing names via that lookup.
     */
    val name: String? = null,
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
