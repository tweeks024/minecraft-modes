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
import com.tweeks.translator.java.llm.ConfidenceGate
import com.tweeks.translator.java.llm.RouteOutcome
import com.tweeks.translator.java.llm.TranslationPrompt
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
    /**
     * Phase 3: optional LLM stage. Null means the analyzer behaves as in
     * Phase 2b — Medium-bucket goals are logged on Untranslatable but no
     * `behavior_pack/scripts/goals/<X>.ts` files are emitted. With a gate
     * provided, every Medium-bucket goal is routed through it and either
     * cache-hit JS or a TODO stub lands on disk.
     */
    private val gate: ConfidenceGate? = null,
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
        val securityFamilies = readSecurityFamilies(entityClass)

        // Phase 3: route every Medium-bucket goal through the LLM gate. Each
        // route call writes either cache-hit JS or a TODO stub to
        // `behavior_pack/scripts/goals/<GoalSimpleName>.ts`. We sort the
        // deferred list by simple name first so the order of disk writes is
        // deterministic across runs.
        if (gate != null) {
            val deferred = goals.deferred.sortedBy { it.goalSimpleName }
            for (d in deferred) {
                val source = findGoalSource(mod, d.goalFqn) ?: d.callSiteSource
                val ctx = TranslationPrompt.GoalContext(
                    goalClassSimpleName = d.goalSimpleName,
                    goalClassFqn = d.goalFqn,
                    goalSource = source,
                    parentClassFqn = inferParentClassFqn(d.goalFqn),
                    resolvedMethodSignatures = emptyList(),
                    resolvedFieldReferences = emptyList(),
                    entityClassSummary = summarizeEntity(reg, entityClass),
                    modManifestExcerpt = "modId: ${mod.modId}",
                )
                val outcome = gate.route(ctx, mod.modId)
                val script = when (outcome) {
                    is RouteOutcome.MediumJs -> outcome.script
                    is RouteOutcome.TodoStub -> outcome.script
                    is RouteOutcome.HighEmit -> continue // not produced by Phase 3
                }
                val scriptPath = outputRoot.resolve(
                    "${mod.modId}/behavior_pack/scripts/goals/${d.goalSimpleName}.ts"
                )
                scriptPath.parent?.createDirectories()
                scriptPath.writeText(script)
            }
        }

        val identifier = "${mod.modId}:${reg.entityId}"
        val components = buildEntityComponents(reg, attrs, goals, securityFamilies)

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
        // Pick geometry by checking the emitted `.geo.json` files first
        // (Phase 1b's BbmodelConverter output) so the reference is guaranteed
        // to resolve. If nothing matches, fall back to a vanilla geometry —
        // Bedrock 1.16+ ships `geometry.humanoid` as the safe default.
        val emittedGeoNames = listEmittedGeometryNames(mod, outputRoot)
        val (geometryRef, ambiguous) = pickGeometryReference(mod, reg.entityId, emittedGeoNames)
        if (ambiguous) {
            unt.recordRenderControllerAmbiguous(
                mod.modId,
                reg.entityId,
                "no bbmodel matched id '${reg.entityId}'; defaulted to '$geometryRef'.",
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
                                    put("default", JsonPrimitive(geometryRef))
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
        securityFamilies: List<String>,
    ): JsonObject {
        // Use a sorted map so the output is byte-stable regardless of
        // catalog insertion order.
        val sorted = sortedMapOf<String, JsonElement>()

        sorted["minecraft:type_family"] = buildJsonObject {
            put(
                "family",
                buildJsonArray {
                    add(JsonPrimitive(reg.entityId))
                    // Security marker interfaces (`SecurityAlly`/`SecurityHostile`)
                    // become Bedrock family tags. This is what cross-mod targeting
                    // (e.g. SecurityGuard's `nearest_attackable_target`) keys off
                    // — see `bedrock-api/family-filters.md` and the LLM prompt.
                    // Insert them in stable order for byte-deterministic output.
                    for (family in securityFamilies) {
                        add(JsonPrimitive(family))
                    }
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

    // ---------- Reading SecurityAlly / SecurityHostile marker interfaces ----------

    /**
     * Detect whether [entity] implements `com.tweeks.securitycore.api.SecurityAlly`
     * or `com.tweeks.securitycore.api.SecurityHostile` (directly or transitively),
     * and return the corresponding Bedrock family tags in stable order.
     *
     * Per the spec's "securitycore deduplication" section, these Java marker
     * interfaces have no Bedrock equivalent. They are translated to family tags
     * on the Bedrock entity JSON so cross-mod targeting becomes family-based.
     *
     * Symbol resolution may legitimately fail when the source's classpath isn't
     * fully wired (e.g. test fixtures that mock the resolver), so this falls
     * back to a string-match on the raw `implements` clause if `resolve()`
     * throws — the marker's simple name is unique enough in practice.
     */
    private fun readSecurityFamilies(entity: ClassOrInterfaceDeclaration): List<String> {
        val out = sortedSetOf<String>()
        val implemented = entity.implementedTypes ?: return emptyList()
        for (t in implemented) {
            // Try fully-resolved qualifiedName via JavaSymbolSolver. Falls back
            // to the bare simple name from the AST if the classpath can't
            // resolve the marker (e.g. test fixtures with no symbol solver, or
            // securitycore not on the per-mod source-solver chain). The two
            // markers' simple names don't collide with anything else in this
            // repo, so the fallback is safe in practice.
            val fqn: String = try {
                val resolved = t.resolve()
                if (resolved.isReferenceType) {
                    resolved.asReferenceType().qualifiedName
                } else {
                    t.nameAsString
                }
            } catch (_: Throwable) {
                t.nameAsString
            }
            when {
                fqn == "com.tweeks.securitycore.api.SecurityAlly" || fqn == "SecurityAlly" ->
                    out.add("security_ally")
                fqn == "com.tweeks.securitycore.api.SecurityHostile" || fqn == "SecurityHostile" ->
                    out.add("security_hostile")
            }
        }
        return out.toList()
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
     * Enumerate the `<name>.geo.json` files BbmodelConverter has already
     * emitted for [mod]. Returns the set of base names (no extension), used to
     * make sure every entity's geometry reference resolves to a real file.
     *
     * BbmodelConverter writes geometries with `identifier:
     * "geometry.<modid>.<name>"` matching the bbmodel basename, so we use the
     * file basename as the canonical name.
     */
    private fun listEmittedGeometryNames(mod: ModDiscovery.DiscoveredMod, outputRoot: Path): Set<String> {
        val geoDir = outputRoot.resolve("${mod.modId}/resource_pack/models/entity")
        if (geoDir.isDirectory()) {
            val emitted = Files.list(geoDir).use { stream ->
                stream
                    .filter { it.isRegularFile() && it.fileName.toString().endsWith(".geo.json") }
                    .map { it.fileName.toString().removeSuffix(".geo.json") }
                    .toList()
                    .toSet()
            }
            if (emitted.isNotEmpty()) return emitted
        }
        // Fallback: BbmodelConverter hasn't run yet (typical in unit tests
        // exercising the analyzer in isolation). Treat the source bbmodels
        // as authoritative — they're what BbmodelConverter would emit.
        val toolsDir = mod.rootDir.resolve("tools")
        if (!toolsDir.isDirectory()) return emptySet()
        return Files.list(toolsDir).use { stream ->
            stream
                .filter { it.isRegularFile() && it.extension == "bbmodel" }
                .map { it.nameWithoutExtension }
                .toList()
                .toSet()
        }
    }

    /**
     * Pick the geometry identifier for the resource_pack `<entity>.entity.json`'s
     * `geometry.default` slot.
     *
     * The returned String is the *Bedrock geometry identifier* (e.g.
     * `geometry.securityguard.security_guard`) — already ready to drop into the
     * client_entity JSON. The returned Boolean is `ambiguous`: true when the
     * pick was a fallback, so the caller logs it on Untranslatable.
     *
     * Resolution order:
     *   1. exact-match against [emittedGeometries] for `<entityId>`.
     *   2. case-insensitive match.
     *   3. `securityguard:guard` → `security_guard` exception (humanoid shape).
     *   4. substring contains the entity id (ambiguous).
     *   5. fallback: vanilla `geometry.humanoid` (Bedrock built-in) — ambiguous.
     *
     * The vanilla fallback guarantees the entity JSON's geometry reference
     * resolves at load time even when no bbmodel ships with the mod (e.g.
     * thief, which has no bbmodel under `tools/` today).
     */
    private fun pickGeometryReference(
        mod: ModDiscovery.DiscoveredMod,
        entityId: String,
        emittedGeometries: Set<String>,
    ): Pair<String, Boolean> {
        fun geom(name: String) = "geometry.${mod.modId}.$name"

        // 1) exact id match against emitted geometry.
        if (emittedGeometries.contains(entityId)) return geom(entityId) to false
        // 2) case-insensitive.
        emittedGeometries.firstOrNull { it.equals(entityId, ignoreCase = true) }
            ?.let { return geom(it) to false }
        // 3) securityguard-specific exception.
        if (mod.modId == "securityguard" && entityId == "guard") {
            emittedGeometries.firstOrNull { it == "security_guard" }
                ?.let { return geom(it) to false }
        }
        // 4) substring contains.
        emittedGeometries.firstOrNull { it.contains(entityId, ignoreCase = true) }
            ?.let { return geom(it) to true }

        // 5) Vanilla fallback. Every Bedrock client ships `geometry.humanoid`
        // as a baseline — references resolve and the entity at least renders
        // as a Steve-shape until a bbmodel is added.
        return "geometry.humanoid" to true
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

    // ---------- Phase 3: goal-source lookup helpers ----------

    /**
     * Locate the Java source for [goalFqn] under the mod (or any sibling mod)
     * and return the full file contents. Walks the FQN's package path under
     * `src/main/java/`. Returns null if not found — the caller falls back to
     * the `addGoal(...)` call-site source.
     *
     * Inner-class goals (`SecurityGuardEntity.GuardTargetHostilesGoal`) live
     * inside their enclosing class's source, so we strip the trailing inner
     * class segment until we find a file.
     */
    private fun findGoalSource(mod: ModDiscovery.DiscoveredMod, goalFqn: String): String? {
        val candidates = mutableListOf<Path>()
        var fqnTry: String? = goalFqn
        while (fqnTry != null && fqnTry.contains('.')) {
            val rel = fqnTry.replace('.', '/') + ".java"
            // Try the mod itself first, then sibling mods discovered in the same
            // repo. We don't have the full discovered list here, so check the
            // immediate parent dir of `mod.rootDir` for siblings.
            candidates.add(mod.rootDir.resolve("src/main/java/$rel"))
            mod.rootDir.parent?.let { repoRoot ->
                Files.list(repoRoot).use { stream ->
                    stream.forEach { sibling ->
                        if (sibling.isDirectory()) {
                            candidates.add(sibling.resolve("src/main/java/$rel"))
                        }
                    }
                }
            }
            for (c in candidates) {
                if (c.isRegularFile()) {
                    return Files.readString(c)
                }
            }
            // Try the enclosing class — strip last segment.
            val newFqn = fqnTry.substringBeforeLast('.')
            if (newFqn == fqnTry) break
            fqnTry = newFqn
        }
        return null
    }

    /**
     * One-line guess at the goal's parent FQN. Custom goals in this repo
     * extend `net.minecraft.world.entity.ai.goal.Goal`. Vanilla goals from
     * Mojang's package extend their own siblings — for those we return the
     * goal FQN's package + ".Goal" as a placeholder.
     */
    private fun inferParentClassFqn(goalFqn: String): String? {
        return if (goalFqn.startsWith("net.minecraft.world.entity.ai.goal.")) {
            "net.minecraft.world.entity.ai.goal.Goal"
        } else {
            "net.minecraft.world.entity.ai.goal.Goal"
        }
    }

    /** Two-line summary of the owning entity for the LLM prompt. */
    private fun summarizeEntity(
        reg: EntityRegistration,
        entityClass: ClassOrInterfaceDeclaration,
    ): String {
        return buildString {
            append("entity id: `").append(reg.entityId).append("`\n")
            append("class: `").append(entityClass.nameAsString).append("`\n")
            append("category: `").append(reg.mobCategory).append("`\n")
            append("size: ").append(reg.width).append(" × ").append(reg.height).append('\n')
        }
    }
}
