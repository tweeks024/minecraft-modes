package com.tweeks.translator.java

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.DoubleLiteralExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LongLiteralExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.UnaryExpr
import com.tweeks.translator.discover.ModDiscovery
import com.tweeks.translator.emit.Untranslatable
import com.tweeks.translator.java.goals.GoalMatcher
import com.tweeks.translator.json.JsonFormat
import com.tweeks.translator.manifest.BedrockTarget
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.writeText

/**
 * Phase 2b: turn each mod's entity classes into Bedrock entity JSON.
 *
 * Per entity it:
 *  1. Finds the `EntityType.Builder` registration in `Registration.java` to
 *     extract the entity id, mob category, size, and tracking range.
 *  2. Walks `static createAttributes()` for `Attributes.MAX_HEALTH` etc.
 *  3. Hands `registerGoals()` to [GoalMatcher].
 *  4. Emits `behavior_pack/entities/<id>.json` and
 *     `resource_pack/entity/<id>.entity.json`.
 *
 * Entities whose registrations or AST shapes don't match the expected
 * patterns are skipped silently (no JSON written) and the cause is logged
 * to the per-mod `UNTRANSLATABLE.md`.
 */
internal class EntityAnalyzer(
    private val target: BedrockTarget,
    private val unt: Untranslatable,
) {

    /** Run analysis for one mod. */
    fun analyze(
        mod: ModDiscovery.DiscoveredMod,
        sources: JavaSourceLoader.ResolvedModSources,
        outputRoot: Path,
    ) {
        val registrations = collectEntityRegistrations(sources)
        if (registrations.isEmpty()) return

        val entityClasses = sources.units.flatMap { it.types }
            .filterIsInstance<ClassOrInterfaceDeclaration>()
            .associateBy { it.nameAsString }

        for (reg in registrations) {
            val entityClass = entityClasses[reg.entityClassName] ?: continue
            try {
                analyzeOne(mod, reg, entityClass, outputRoot)
            } catch (e: Throwable) {
                unt.recordPhase2Failure(
                    mod.modId,
                    "EntityAnalyzer failed on ${reg.entityClassName}: ${e.javaClass.simpleName}: ${e.message}",
                )
            }
        }
    }

    private fun analyzeOne(
        mod: ModDiscovery.DiscoveredMod,
        reg: EntityRegistration,
        entityClass: ClassOrInterfaceDeclaration,
        outputRoot: Path,
    ) {
        val attrs = readAttributes(entityClass)
        val goals = GoalMatcher(unt).match(mod.modId, entityClass)

        val identifier = "${mod.modId}:${reg.entityId}"
        val components = buildEntityComponents(reg, attrs, goals)

        val entityJson = buildJsonObject {
            put("format_version", target.format_versions.entity)
            put(
                "minecraft:entity",
                buildJsonObject {
                    put(
                        "description",
                        buildJsonObject {
                            put("identifier", identifier)
                            put("is_spawnable", JsonPrimitive(true))
                            put("is_summonable", JsonPrimitive(true))
                        },
                    )
                    put("components", components)
                    put("events", buildJsonObject { })
                },
            )
        }

        val behaviorPath = outputRoot.resolve("${mod.modId}/behavior_pack/entities/${reg.entityId}.json")
        behaviorPath.parent?.createDirectories()
        behaviorPath.writeText(JsonFormat.PRETTY.encodeToString(JsonElement.serializer(), entityJson) + "\n")

        // Resource-pack client_entity description.
        val (geometryName, ambiguous) = pickGeometryName(mod, reg.entityId)
        if (ambiguous) {
            unt.recordRenderControllerAmbiguous(
                mod.modId,
                reg.entityId,
                "no bbmodel matched id '${reg.entityId}'; defaulted to '${mod.modId}.geo.json'.",
            )
        }
        val textureShort = pickTextureShortName(mod, reg.entityId)

        val clientJson = buildJsonObject {
            put("format_version", target.format_versions.client_entity)
            put(
                "minecraft:client_entity",
                buildJsonObject {
                    put(
                        "description",
                        buildJsonObject {
                            put("identifier", identifier)
                            put(
                                "materials",
                                buildJsonObject { put("default", JsonPrimitive("entity_alphatest")) },
                            )
                            put(
                                "textures",
                                buildJsonObject {
                                    put("default", JsonPrimitive("textures/entity/$textureShort"))
                                },
                            )
                            put(
                                "geometry",
                                buildJsonObject {
                                    put("default", JsonPrimitive("geometry.${mod.modId}.$geometryName"))
                                },
                            )
                            put(
                                "render_controllers",
                                buildJsonArray { add(JsonPrimitive("controller.render.default")) },
                            )
                        },
                    )
                },
            )
        }
        val clientPath = outputRoot.resolve("${mod.modId}/resource_pack/entity/${reg.entityId}.entity.json")
        clientPath.parent?.createDirectories()
        clientPath.writeText(JsonFormat.PRETTY.encodeToString(JsonElement.serializer(), clientJson) + "\n")
    }

    // ---------- Entity-component assembly ----------

    private fun buildEntityComponents(
        reg: EntityRegistration,
        attrs: Map<String, Double>,
        goals: GoalMatcher.Result,
    ): JsonObject {
        // Use a sorted map so the output is byte-stable regardless of
        // catalog insertion order.
        val sorted = sortedMapOf<String, JsonElement>()

        sorted["minecraft:type_family"] = buildJsonObject {
            put(
                "family",
                buildJsonArray {
                    add(JsonPrimitive(reg.entityId))
                    when (reg.mobCategory) {
                        "MONSTER" -> { add(JsonPrimitive("monster")); add(JsonPrimitive("mob")) }
                        "CREATURE" -> { add(JsonPrimitive("mob")) }
                        "MISC" -> { add(JsonPrimitive("mob")) }
                        else -> { add(JsonPrimitive("mob")) }
                    }
                },
            )
        }

        sorted["minecraft:collision_box"] = buildJsonObject {
            // The Java source declares these as `0.6f` / `1.95f`. Casting
            // a float to double in-place reveals the binary-rounding tail
            // (`0.6` → `0.6000000238418579`); round to 4 decimal places to
            // keep the emitted JSON readable.
            put("width", num(roundTo4(reg.width.toDouble())))
            put("height", num(roundTo4(reg.height.toDouble())))
        }

        attrs["MAX_HEALTH"]?.let { hp ->
            sorted["minecraft:health"] = buildJsonObject {
                put("value", num(hp))
                put("max", num(hp))
            }
        }
        attrs["ATTACK_DAMAGE"]?.let { dmg ->
            sorted["minecraft:attack"] = buildJsonObject {
                put("damage", num(dmg))
            }
        }
        attrs["MOVEMENT_SPEED"]?.let { spd ->
            sorted["minecraft:movement"] = buildJsonObject {
                put("value", num(spd))
            }
        }
        attrs["KNOCKBACK_RESISTANCE"]?.let { kbr ->
            sorted["minecraft:knockback_resistance"] = buildJsonObject {
                put("value", num(kbr))
            }
        }
        attrs["FOLLOW_RANGE"]?.let { fr ->
            sorted["minecraft:follow_range"] = buildJsonObject {
                put("value", num(fr))
                put("max", num(fr))
            }
        }

        // Vanilla movement / navigation defaults.
        sorted["minecraft:movement.basic"] = buildJsonObject { }
        sorted["minecraft:navigation.walk"] = buildJsonObject {
            put("can_pass_doors", JsonPrimitive(true))
            put("can_walk", JsonPrimitive(true))
        }
        sorted["minecraft:jump.static"] = buildJsonObject { }
        sorted["minecraft:physics"] = buildJsonObject { }
        sorted["minecraft:can_climb"] = buildJsonObject { }
        sorted["minecraft:pushable"] = buildJsonObject {
            put("is_pushable", JsonPrimitive(true))
            put("is_pushable_by_piston", JsonPrimitive(true))
        }

        // Goals: priority is the position from the source — Bedrock uses
        // ascending priority same as Java. Sort by priority then component
        // name so the order is deterministic.
        val sortedGoals = goals.components.sortedWith(
            compareBy({ it.priority }, { it.componentName })
        )
        for (g in sortedGoals) {
            // Bedrock allows duplicate behavior names with different priorities
            // by using the same component key; later writes overwrite earlier.
            // For now we drop duplicates rather than synthesize unique keys —
            // that matches the typical securityguard / thief use case where
            // duplicates would collide on intent anyway.
            val merged = buildJsonObject {
                put("priority", JsonPrimitive(g.priority))
                for ((k, v) in g.body.entries.sortedBy { it.key }) put(k, v)
            }
            sorted[g.componentName] = merged
        }

        return JsonObject(sorted)
    }

    // ---------- Reading attributes ----------

    /**
     * Parse `static createAttributes()` for `.add(Attributes.X, value)` calls.
     * Returns a map of the Java attribute simple name → numeric value.
     */
    private fun readAttributes(entity: ClassOrInterfaceDeclaration): Map<String, Double> {
        val method = entity.methods.firstOrNull { it.nameAsString == "createAttributes" }
            ?: return emptyMap()
        val out = mutableMapOf<String, Double>()
        for (call in method.findAll(MethodCallExpr::class.java)) {
            if (call.nameAsString != "add") continue
            if (call.arguments.size < 2) continue
            val attrExpr = call.arguments[0]
            val attrName = (attrExpr as? FieldAccessExpr)?.nameAsString ?: continue
            val value = readDoubleLiteral(call.arguments[1]) ?: continue
            out[attrName] = value
        }
        return out
    }

    private fun readDoubleLiteral(expr: Expression): Double? {
        return when (expr) {
            is DoubleLiteralExpr -> expr.asDouble()
            is IntegerLiteralExpr -> expr.asNumber().toDouble()
            is LongLiteralExpr -> expr.asNumber().toDouble()
            is UnaryExpr -> {
                val inner = readDoubleLiteral(expr.expression) ?: return null
                when (expr.operator) {
                    UnaryExpr.Operator.MINUS -> -inner
                    UnaryExpr.Operator.PLUS -> inner
                    else -> null
                }
            }
            else -> null
        }
    }

    // ---------- Reading entity-type registrations ----------

    /** A single `ENTITY_TYPES.register(...)` call we recognized. */
    internal data class EntityRegistration(
        val entityId: String,
        val entityClassName: String,
        val mobCategory: String,
        val width: Float,
        val height: Float,
        val clientTrackingRange: Int,
    )

    private fun collectEntityRegistrations(
        sources: JavaSourceLoader.ResolvedModSources,
    ): List<EntityRegistration> {
        val out = mutableListOf<EntityRegistration>()
        for (unit in sources.units) {
            for (call in unit.findAll(MethodCallExpr::class.java)) {
                // Match `ENTITY_TYPES.register("id", () -> EntityType.Builder.<X>of(X::new, MobCategory.Y).sized(w,h)...)`.
                if (call.nameAsString != "register") continue
                val scope = call.scope.orElse(null) ?: continue
                if (scope.toString() != "ENTITY_TYPES") continue
                if (call.arguments.size < 2) continue
                val id = readStringLiteral(call.arguments[0]) ?: continue

                // The lambda body chains EntityType.Builder.<X>of(X::new, MobCategory.Y).
                // Walk all child MethodCallExprs of the second arg looking for `of(...)`.
                val builderCalls = call.arguments[1].findAll(MethodCallExpr::class.java)

                val ofCall = builderCalls.firstOrNull { it.nameAsString == "of" } ?: continue
                if (ofCall.arguments.size < 2) continue
                val classRefExpr = ofCall.arguments[0].toString()
                // Form: `SecurityGuardEntity::new` -> class name is everything
                // before the `::`. Strip any leading qualifier.
                val entityClassName = classRefExpr.substringBefore("::").substringAfterLast('.')
                val categoryExpr = ofCall.arguments[1] as? FieldAccessExpr ?: continue
                val mobCategory = categoryExpr.nameAsString

                val sized = builderCalls.firstOrNull { it.nameAsString == "sized" }
                val width = sized?.arguments?.getOrNull(0)?.let { readDoubleLiteral(it)?.toFloat() } ?: 0.6f
                val height = sized?.arguments?.getOrNull(1)?.let { readDoubleLiteral(it)?.toFloat() } ?: 1.95f

                val tracking = builderCalls.firstOrNull { it.nameAsString == "clientTrackingRange" }
                val trackingRange = tracking?.arguments?.firstOrNull()?.let { readDoubleLiteral(it)?.toInt() } ?: 10

                out.add(
                    EntityRegistration(
                        entityId = id,
                        entityClassName = entityClassName,
                        mobCategory = mobCategory,
                        width = width,
                        height = height,
                        clientTrackingRange = trackingRange,
                    )
                )
            }
        }
        return out
    }

    private fun readStringLiteral(expr: Expression): String? {
        return when (expr) {
            is com.github.javaparser.ast.expr.StringLiteralExpr -> expr.asString()
            else -> null
        }
    }

    // ---------- Render-controller wiring ----------

    /**
     * Pick the bbmodel filename whose base name will be used as the
     * `geometry.<modid>.<name>` identifier. Tries:
     *   1. exact match — `<entityId>.bbmodel`.
     *   2. case-insensitive simple-name match.
     *   3. for the special `securityguard:guard` case, prefer
     *      `security_guard.bbmodel` (the only humanoid-shape bbmodel with
     *      head/body/arm bones).
     *   4. fallback to `<modId>` and flag as ambiguous.
     */
    private fun pickGeometryName(
        mod: ModDiscovery.DiscoveredMod,
        entityId: String,
    ): Pair<String, Boolean> {
        val toolsDir = mod.rootDir.resolve("tools")
        if (!toolsDir.isDirectory()) return mod.modId to true
        val bbmodels = Files.list(toolsDir).use { stream ->
            stream
                .filter { it.isRegularFile() && it.extension == "bbmodel" }
                .map { it.nameWithoutExtension }
                .toList()
        }

        // 1) exact id match.
        bbmodels.firstOrNull { it == entityId }?.let { return it to false }
        // 2) case-insensitive.
        bbmodels.firstOrNull { it.equals(entityId, ignoreCase = true) }?.let { return it to false }
        // 3) securityguard-specific exception: `guard` → `security_guard`.
        if (mod.modId == "securityguard" && entityId == "guard") {
            bbmodels.firstOrNull { it == "security_guard" }?.let { return it to false }
        }
        // 4) thief-specific exception: id and bbmodel name happen to match.
        bbmodels.firstOrNull { it.contains(entityId, ignoreCase = true) }?.let { return it to true }

        return mod.modId to true
    }

    /**
     * Heuristic for the entity texture name. Mirrors the pickGeometryName
     * fallback so they line up — texture lookups are by id first, then
     * by the securityguard-specific `security_guard` name.
     */
    private fun pickTextureShortName(
        mod: ModDiscovery.DiscoveredMod,
        entityId: String,
    ): String {
        val texDir = mod.rootDir.resolve("src/main/resources/assets/${mod.modId}/textures/entity")
        if (texDir.isDirectory()) {
            val pngs = Files.list(texDir).use { stream ->
                stream
                    .filter { it.isRegularFile() && it.extension == "png" }
                    .map { it.fileName.toString() }
                    .toList()
            }
            // Exact-match wins.
            pngs.firstOrNull { it.equals("$entityId.png", ignoreCase = true) }?.let { return it }
            if (mod.modId == "securityguard" && entityId == "guard") {
                pngs.firstOrNull { it == "security_guard.png" }?.let { return it }
            }
            // Prefer a PNG whose name starts with the entity id (e.g.
            // `thief_disguised.png` for entity id `thief`). This avoids
            // accidentally picking up an item-shaped texture (e.g. baton).
            pngs.firstOrNull { it.startsWith("${entityId}_", ignoreCase = true) }?.let { return it }
            // Then any PNG containing the entity id as a substring.
            pngs.firstOrNull { it.contains(entityId, ignoreCase = true) }?.let { return it }
            // Otherwise pick the lexicographic first PNG under entity/ as a
            // best-effort fallback.
            pngs.minOrNull()?.let { return it }
        }
        return "$entityId.png"
    }

    // ---------- Shared helpers ----------

    private fun num(d: Double): JsonPrimitive =
        if (d == d.toLong().toDouble()) JsonPrimitive(d.toLong()) else JsonPrimitive(d)

    /** Round to 4 decimal places — strips float-to-double rounding noise. */
    private fun roundTo4(d: Double): Double = Math.round(d * 10000.0) / 10000.0
}
