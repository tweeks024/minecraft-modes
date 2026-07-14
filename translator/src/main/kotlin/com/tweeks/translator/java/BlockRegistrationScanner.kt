package com.tweeks.translator.java

import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.tweeks.translator.emit.Untranslatable

/**
 * Phase 2 recording-only stage: find `BLOCKS.registerBlock("<id>", ...)` /
 * `BLOCKS.registerSimpleBlock("<id>", ...)` registrations in the mod's Java
 * sources and record each block on [Untranslatable]. The translator has no
 * Bedrock block emitter, so a registered block produces no output at all —
 * before this stage existed, custom blocks vanished from the translated
 * Add-On with zero trace in `UNTRANSLATABLE.md`.
 *
 * Same AST pattern-matching approach as [ItemAnalyzer]'s
 * `collectItemRegistrations`: match on the call name plus the conventional
 * `BLOCKS` DeferredRegister scope; non-literal ids are skipped (nothing in
 * this repo registers blocks dynamically).
 */
internal class BlockRegistrationScanner(
    private val unt: Untranslatable,
) {

    fun scan(modId: String, sources: JavaSourceLoader.ResolvedModSources) {
        for (unit in sources.units) {
            for (call in unit.findAll(MethodCallExpr::class.java)) {
                if (call.nameAsString != "registerBlock" && call.nameAsString != "registerSimpleBlock") continue
                val scope = call.scope.orElse(null) ?: continue
                if (scope.toString() != "BLOCKS") continue
                val id = (call.arguments.firstOrNull() as? StringLiteralExpr)?.asString() ?: continue

                // `registerBlock`'s second arg is a `Cls::new` factory ref;
                // `registerSimpleBlock` takes properties only. Simple name
                // from the AST text — mirrors ItemAnalyzer's classRef handling.
                val blockClassName = call.arguments.getOrNull(1)?.toString()
                    ?.takeIf { it.contains("::") }
                    ?.substringBefore("::")
                    ?.substringAfterLast('.')

                unt.recordBlockNotTranslated(modId, id, blockSummary(id, blockClassName))
            }
        }
    }

    private fun blockSummary(blockId: String, blockClassName: String?): String {
        val sb = StringBuilder("custom block '").append(blockId)
            .append("' not translated — translator has no Bedrock block emitter")
        // Portal-family blocks (e.g. starwars' hyperspace gate film): the
        // block's whole point is portal/teleport logic, which would need a
        // scripting harness on top of the missing block itself. Detected
        // deterministically from the id / factory class name.
        if (blockId.contains("portal") || blockClassName?.contains("Portal") == true) {
            sb.append("; portal/teleport behavior impossible without a scripting harness")
        }
        if (blockClassName != null) {
            sb.append(" (block class ").append(blockClassName).append(')')
        }
        return sb.toString()
    }
}
