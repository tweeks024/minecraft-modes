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
            val requiresSecurityCore: Boolean
            if (tomlPath.isRegularFile()) {
                val toml = tomlPath.readText()
                displayName = parseDisplayName(toml)
                    ?: gradleProps["mod_name"]
                    ?: defaultDisplayName(modId)
                description = parseDescription(toml) ?: defaultDescription(modId)
                requiresSecurityCore = parseSecurityCoreDep(toml, modId)
            } else {
                displayName = gradleProps["mod_name"] ?: defaultDisplayName(modId)
                description = defaultDescription(modId)
                requiresSecurityCore = false
            }
            return ModMetadata(
                modId = modId,
                displayName = displayName,
                description = description,
                requiresSecurityCore = requiresSecurityCore,
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
         * Scan the toml for a `modId="securitycore"` line that belongs to a
         * `[[dependencies.<this-mod>]]` block. The toml uses
         * `${mod_id}` placeholders for the block header but literal mod ids
         * inside, so a substring match is sufficient — securitycore is the
         * one cross-mod dep we care about.
         *
         * A mod cannot depend on itself, so securitycore itself always
         * returns false even though its toml mentions its own id elsewhere.
         */
        internal fun parseSecurityCoreDep(toml: String, modId: String): Boolean {
            if (modId == "securitycore") return false
            // Look for `modId="securitycore"` (with optional whitespace
            // around the `=`). Anything matching that pattern in the toml
            // signals a declared dep.
            val r = Regex("""modId\s*=\s*"securitycore"""")
            return r.containsMatchIn(toml)
        }

        /**
         * Match `displayName="..."` (basic string) or `displayName='...'`
         * (literal string). TOML also allows triple-quoted forms (`"""..."""`
         * and `'''...'''`); those are valid in spec but unused in this repo's
         * tomls for displayName, so the simple form is sufficient.
         *
         * Returns null when the value is a NeoForge `${...}` placeholder
         * (we have no expansion context at translator time).
         */
        private fun parseDisplayName(toml: String): String? {
            val doubleQ = Regex("""displayName\s*=\s*"([^"]*)"""")
            val singleQ = Regex("""displayName\s*=\s*'([^']*)'""")
            val value = doubleQ.find(toml)?.groupValues?.get(1)
                ?: singleQ.find(toml)?.groupValues?.get(1)
                ?: return null
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
