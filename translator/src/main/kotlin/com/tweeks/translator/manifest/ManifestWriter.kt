package com.tweeks.translator.manifest

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Builds Bedrock Add-On `manifest.json` documents for a mod's behavior pack and
 * resource pack.
 *
 * Bedrock manifest schema (format_version 2):
 *   - header: { name, description, uuid, version[3], min_engine_version[3] }
 *   - modules: [{ type, uuid, version[3] }]
 *       type = "data" for behavior pack, "resources" for resource pack
 *   - dependencies (optional): [{ uuid, version[3] }]
 *       Used to:
 *         (a) Pair the BP and RP of the same mod (each lists the other's
 *             *header* uuid), and
 *         (b) Declare dependence on securitycore for sibling mods that need
 *             it (BP only — runtime semantics live on the behavior side).
 *
 * UUIDs are deterministic (UUIDv5) per [UuidGen] so reruns produce stable IDs.
 *
 * The spec defines four canonical UUIDs:
 *   header_uuid     = UUIDv5(ns, mod_id + ":header")
 *   behavior_module = UUIDv5(ns, mod_id + ":modules.behavior")
 *   resource_module = UUIDv5(ns, mod_id + ":modules.resource")
 *   core_dependency = UUIDv5(ns, "securitycore:header")
 *
 * Bedrock requires each pack (BP, RP) to have its own globally-unique header
 * UUID. The spec's `header_uuid` is the BP header; we derive the RP header
 * from `mod_id + ":resource_header"` in the same namespace so it's distinct,
 * stable, and obvious in spec terms. The BP↔RP dependency chain uses these
 * two header UUIDs.
 */
class ManifestWriter(private val target: BedrockTarget) {

    /** Inputs for a single mod's manifests. */
    data class ModManifestInputs(
        val modId: String,
        val displayName: String,
        val description: String,
        /** True if this mod's behavior pack should declare a dependency on securitycore's header UUID. */
        val requiresSecurityCore: Boolean,
        /** Pack version, e.g. [1, 0, 0]. */
        val version: List<Int> = listOf(1, 0, 0),
    )

    /** Result of generating both manifests for a mod. */
    data class ModManifests(
        val behaviorPackManifest: String,
        val resourcePackManifest: String,
    )

    fun build(inputs: ModManifestInputs): ModManifests {
        val behavior = buildBehaviorPack(inputs)
        val resource = buildResourcePack(inputs)
        return ModManifests(
            behaviorPackManifest = JSON.encodeToString(Manifest.serializer(), behavior) + "\n",
            resourcePackManifest = JSON.encodeToString(Manifest.serializer(), resource) + "\n",
        )
    }

    private fun buildBehaviorPack(inputs: ModManifestInputs): Manifest {
        val deps = mutableListOf<DependencyEntry>()
        // BP→RP pairing.
        deps.add(
            DependencyEntry(
                uuid = resourcePackHeaderUuid(inputs.modId).toString(),
                version = inputs.version,
            )
        )
        // securitycore dependency, if applicable.
        if (inputs.requiresSecurityCore && inputs.modId != "securitycore") {
            deps.add(
                DependencyEntry(
                    uuid = UuidGen.coreDependencyUuid().toString(),
                    version = listOf(1, 0, 0),
                )
            )
        }

        return Manifest(
            format_version = 2,
            header = Header(
                name = "${inputs.displayName} (Behavior Pack)",
                description = inputs.description,
                uuid = behaviorPackHeaderUuid(inputs.modId).toString(),
                version = inputs.version,
                min_engine_version = target.min_engine_version,
            ),
            modules = listOf(
                Module(
                    type = "data",
                    uuid = UuidGen.behaviorModuleUuid(inputs.modId).toString(),
                    version = inputs.version,
                )
            ),
            dependencies = deps,
        )
    }

    private fun buildResourcePack(inputs: ModManifestInputs): Manifest {
        val deps = listOf(
            DependencyEntry(
                uuid = behaviorPackHeaderUuid(inputs.modId).toString(),
                version = inputs.version,
            )
        )

        return Manifest(
            format_version = 2,
            header = Header(
                name = "${inputs.displayName} (Resource Pack)",
                description = inputs.description,
                uuid = resourcePackHeaderUuid(inputs.modId).toString(),
                version = inputs.version,
                min_engine_version = target.min_engine_version,
            ),
            modules = listOf(
                Module(
                    type = "resources",
                    uuid = UuidGen.resourceModuleUuid(inputs.modId).toString(),
                    version = inputs.version,
                )
            ),
            dependencies = deps,
        )
    }

    @Serializable
    data class Manifest(
        val format_version: Int,
        val header: Header,
        val modules: List<Module>,
        val dependencies: List<DependencyEntry> = emptyList(),
    )

    @Serializable
    data class Header(
        val name: String,
        val description: String,
        val uuid: String,
        val version: List<Int>,
        val min_engine_version: List<Int>,
    )

    @Serializable
    data class Module(
        val type: String,
        val uuid: String,
        val version: List<Int>,
    )

    @Serializable
    data class DependencyEntry(
        val uuid: String,
        val version: List<Int>,
    )

    companion object {
        // Pretty-printed for human review and golden-file diffs. encodeDefaults
        // ensures the dependencies array always appears, even when empty.
        @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
        val JSON = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            encodeDefaults = true
        }

        /**
         * Per-pack header UUID for the *behavior* pack. Thin alias for
         * [UuidGen.behaviorHeaderUuid] kept here so callers reading the
         * manifest writer don't need to jump files.
         */
        fun behaviorPackHeaderUuid(modId: String) = UuidGen.behaviorHeaderUuid(modId)

        /** Per-pack header UUID for the *resource* pack. Alias for [UuidGen.resourceHeaderUuid]. */
        fun resourcePackHeaderUuid(modId: String) = UuidGen.resourceHeaderUuid(modId)
    }
}
