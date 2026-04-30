package com.tweeks.translator.discover

import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

/**
 * Lightweight metadata for a mod, read from its `neoforge.mods.toml` template
 * (or filled with sensible fallbacks if the template is absent / unparseable).
 *
 * Phase 0 only needs:
 *   - `displayName` and `description` for the manifest's user-facing strings.
 *   - `requiresSecurityCore` for the dependency wiring in the BP manifest.
 *
 * The TOML used here is heavily shaped by NeoForge's `${...}` placeholders,
 * which is why we avoid pulling in a full TOML parser and instead use targeted
 * regexes. If parsing complexity grows, swap in `tomlj` or similar.
 */
data class ModMetadata(
    val modId: String,
    val displayName: String,
    val description: String,
    val requiresSecurityCore: Boolean,
) {
    companion object {
        /**
         * Read metadata from the conventional templates path. Falls back to
         * mod-id-based defaults if the toml file is missing.
         *
         * The `requiresSecurityCore` flag is hard-coded for the four mods this
         * repo currently ships, since the toml uses `${mod_id}` placeholders
         * that aren't expanded at translator time. We mirror what the TOMLs
         * declare today: `securityguard` and `thief` require `securitycore`;
         * `securitycore` and `creeperskin` do not.
         */
        fun read(modRoot: Path, modId: String): ModMetadata {
            val tomlPath = modRoot.resolve("src/main/templates/META-INF/neoforge.mods.toml")
            val gradlePropsPath = modRoot.resolve("gradle.properties")

            val gradleProps = if (gradlePropsPath.isRegularFile()) parseProperties(gradlePropsPath.readText()) else emptyMap()

            val displayName: String
            val description: String
            if (tomlPath.isRegularFile()) {
                val toml = tomlPath.readText()
                displayName = parseDisplayName(toml)
                    ?: gradleProps["mod_name"]
                    ?: defaultDisplayName(modId)
                description = parseDescription(toml) ?: defaultDescription(modId)
            } else {
                displayName = gradleProps["mod_name"] ?: defaultDisplayName(modId)
                description = defaultDescription(modId)
            }
            return ModMetadata(
                modId = modId,
                displayName = displayName,
                description = description,
                requiresSecurityCore = requiresSecurityCoreFor(modId),
            )
        }

        /** Minimal `key=value` parser for gradle.properties; `#` comments and blank lines are skipped. */
        private fun parseProperties(text: String): Map<String, String> {
            val out = mutableMapOf<String, String>()
            for (raw in text.lineSequence()) {
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) continue
                val eq = line.indexOf('=')
                if (eq <= 0) continue
                val key = line.substring(0, eq).trim()
                val value = line.substring(eq + 1).trim()
                out[key] = value
            }
            return out
        }

        /**
         * Hard-coded dependency map. Keep in sync with each mod's
         * `neoforge.mods.toml`. This is the simple "data-driven" wiring the
         * spec asks for — a single source of truth here, easy to update.
         */
        fun requiresSecurityCoreFor(modId: String): Boolean = when (modId) {
            "securityguard" -> true
            "thief" -> true
            "securitycore" -> false
            "creeperskin" -> false
            else -> false
        }

        /** `displayName="..."` or `displayName='...'` — the value is plain (no `${...}`). */
        private fun parseDisplayName(toml: String): String? {
            val r = Regex("""displayName\s*=\s*"([^"]*)"""")
            val match = r.find(toml) ?: return null
            val value = match.groupValues[1]
            // The TOML file uses `${mod_name}` as a placeholder; we have no
            // expansion context here, so reject placeholder values.
            if (value.contains("\${")) return null
            return value.takeIf { it.isNotBlank() }
        }

        /**
         * `description='''...'''` triple-quoted block. Captures multi-line
         * content; trims surrounding whitespace so the JSON output stays clean.
         */
        private fun parseDescription(toml: String): String? {
            val r = Regex("""description\s*=\s*'''([\s\S]*?)'''""")
            val match = r.find(toml) ?: return null
            return match.groupValues[1].trim().takeIf { it.isNotBlank() }
        }

        private fun defaultDisplayName(modId: String): String =
            modId.replaceFirstChar { it.uppercaseChar() }

        private fun defaultDescription(modId: String): String =
            "Bedrock Add-On translated from the $modId NeoForge mod."
    }
}
