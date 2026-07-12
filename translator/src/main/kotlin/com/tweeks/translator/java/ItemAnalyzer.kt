package com.tweeks.translator.java

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.expr.DoubleLiteralExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LongLiteralExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.expr.UnaryExpr
import com.tweeks.translator.discover.ModDiscovery
import com.tweeks.translator.emit.Untranslatable
import com.tweeks.translator.json.JsonFormat
import com.tweeks.translator.manifest.BedrockTarget
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Phase 2b: turn each mod's item registrations into Bedrock item JSON.
 *
 * Registration shapes recognized:
 *   - `ITEMS.registerItem("id", Cls::new, p -> p.stacksTo(N).durability(D))`
 *   - `ITEMS.registerItem("id", Cls::new, p -> p.attributes(X).durability(D))`
 *   - `ITEMS.registerItem("id", Cls::new, p -> p.humanoidArmor(...).fireResistant().stacksTo(N))`
 *   - `ITEMS.registerItem("id", SpawnEggItem::new, p -> p.spawnEgg(<entityHolder>.get()))`
 *
 * Items whose Java class overrides behavior methods (e.g. `postHurtEnemy`,
 * `useOn`, `hurtEnemy`) are still emitted (with the static fields) but the
 * behavior is logged to UNTRANSLATABLE for Phase 3's LLM stage.
 */
internal class ItemAnalyzer(
    private val target: BedrockTarget,
    private val unt: Untranslatable,
) {

    /** Run analysis for one mod. */
    fun analyze(
        mod: ModDiscovery.DiscoveredMod,
        sources: JavaSourceLoader.ResolvedModSources,
        outputRoot: Path,
    ) {
        val registrations = collectItemRegistrations(sources)
        if (registrations.isEmpty()) return

        // Items that place a VehicleEntity subclass (e.g. starwars'
        // landspeeder) need a `minecraft:entity_placer` component so the
        // crafted item actually spawns something on Bedrock — see
        // [EntityAnalyzer.vehicleEntityIds].
        val vehicleEntityIds = EntityAnalyzer.vehicleEntityIds(sources)

        // Index item-class declarations so we can detect custom overrides.
        val classIndex = sources.units.flatMap { it.types }
            .filterIsInstance<ClassOrInterfaceDeclaration>()
            .associateBy { it.nameAsString }

        // Index item-class default attribute modifiers (e.g. BlackjackItem
        // configures its data components inside its own constructor).
        val classAttrs = classIndex.mapValues { (_, decl) -> readAttackDamageInClass(decl) }
        // Also extract `properties.stacksTo(N)` / `.durability(N)` from each
        // item class's constructor so e.g. BlackjackItem's `stacksTo(1)` or
        // PistolItem's `durability(300)` show up even when the registration
        // site passes a bare `p -> p` lambda.
        val classStackSize = classIndex.mapValues { (_, decl) -> readStackSizeInClass(decl) }
        val classDurability = classIndex.mapValues { (_, decl) -> readDurabilityInClass(decl) }

        for (reg in registrations) {
            try {
                val icon = resolveIconBasis(mod, reg)
                writeItem(
                    mod, reg, icon, classIndex, classAttrs, classStackSize, classDurability,
                    vehicleEntityIds, outputRoot,
                )
                writeAttachableIfBbmodelExists(mod, reg, icon, reg.itemId in vehicleEntityIds, outputRoot)
            } catch (e: Throwable) {
                unt.recordPhase2Failure(
                    mod.modId,
                    "ItemAnalyzer failed on ${reg.itemId}: ${e.javaClass.simpleName}: ${e.message}",
                )
            }
        }
    }

    /**
     * The static icon/texture basis for one item: what `minecraft:icon`
     * should point at, and the `textures/items/<name>` short name backing
     * it (used both for the icon component and as an attachable-texture
     * fallback — see [writeAttachableIfBbmodelExists]).
     */
    private data class IconBasis(
        val identifier: String,
        val textureShortName: String,
        val selectorNote: String?,
    )

    /**
     * Read `assets/<modid>/items/<id>.json` (the Java client item-model
     * definition) and pick the icon Bedrock should use.
     *
     * The common case is a plain `minecraft:model` reference, in which case
     * the item's own identifier is already a valid `textures/items/<id>`
     * short name (the two conventionally share a name), matching the prior
     * (unconditional) behavior.
     *
     * When the definition is a `minecraft:select` (a component-keyed model
     * — e.g. a data-component-driven blade/dye color), the
     * translator does not attempt full selector translation (out of scope).
     * Instead it picks the FIRST case's model (falling back to the
     * `fallback` model if there are no cases), resolves that model's own
     * texture reference, and uses that as a *static* icon — so the emitted
     * icon key actually exists in `item_texture.json` instead of being a
     * bare item-id key nothing defines. [IconBasis.selectorNote] is set so
     * the caller can record the loss in UNTRANSLATABLE.md.
     */
    private fun resolveIconBasis(mod: ModDiscovery.DiscoveredMod, reg: ItemRegistration): IconBasis {
        val default = IconBasis(
            identifier = "${mod.modId}:${reg.itemId}",
            textureShortName = reg.itemId,
            selectorNote = null,
        )
        val itemModelPath = mod.rootDir.resolve("src/main/resources/assets/${mod.modId}/items/${reg.itemId}.json")
        if (!itemModelPath.isRegularFile()) return default

        val root = runCatching { Json.parseToJsonElement(itemModelPath.readText()).jsonObject }.getOrNull()
            ?: return default
        val model = root["model"]?.jsonObject ?: return default
        if (model["type"]?.jsonPrimitive?.contentOrNull != "minecraft:select") return default

        // First case's model, or the selector's fallback model if there are
        // no cases (both shapes are `{"type": "minecraft:model", "model": "<ref>"}`).
        val chosenModel = model["cases"]?.jsonArray?.firstOrNull()?.jsonObject?.get("model")?.jsonObject
            ?: model["fallback"]?.jsonObject
            ?: return default
        val modelRef = chosenModel["model"]?.jsonPrimitive?.contentOrNull ?: return default

        val resolved = resolveTextureFromModelRef(mod, modelRef)
            // The referenced model file couldn't be read/resolved to a
            // texture; fall back to the model reference's own last path
            // segment as a best-effort short name (Java model/texture names
            // conventionally match for single-texture item models).
            ?: (modelRef.substringAfter(':').substringAfterLast('/').let {
                "${mod.modId}:$it" to it
            })

        return IconBasis(
            identifier = resolved.first,
            textureShortName = resolved.second,
            selectorNote = "item model selector not translatable — using ${resolved.first} as static icon",
        )
    }

    /**
     * Resolve a Java client model reference (e.g. `starwars:item/lightsaber_blue`)
     * to the texture identifier its own model JSON declares (`textures.0` /
     * `textures.particle`), returned as (full identifier, short name) —
     * mirroring how [com.tweeks.translator.json.AssetCopier] names copied
     * `textures/item/<name>.png` files. Returns null if the model file is
     * missing or its texture can't be read.
     */
    private fun resolveTextureFromModelRef(mod: ModDiscovery.DiscoveredMod, modelRef: String): Pair<String, String>? {
        val namespace = modelRef.substringBefore(':', missingDelimiterValue = mod.modId)
        val path = modelRef.substringAfter(':', missingDelimiterValue = modelRef)
        val modelFile = mod.rootDir.resolve("src/main/resources/assets/$namespace/models/$path.json")
        if (!modelFile.isRegularFile()) return null

        val modelJson = runCatching { Json.parseToJsonElement(modelFile.readText()).jsonObject }.getOrNull()
            ?: return null
        val textures = modelJson["textures"]?.jsonObject ?: return null
        val texRef = textures["0"]?.jsonPrimitive?.contentOrNull
            ?: textures["particle"]?.jsonPrimitive?.contentOrNull
            ?: textures.values.firstOrNull()?.jsonPrimitive?.contentOrNull
            ?: return null

        val texNamespace = texRef.substringBefore(':', missingDelimiterValue = namespace)
        val texPath = texRef.substringAfter(':', missingDelimiterValue = texRef)
        val shortName = texPath.substringAfterLast('/')
        return "$texNamespace:$shortName" to shortName
    }

    /**
     * If a `tools/<itemId>.bbmodel` exists for this item, emit a Bedrock
     * `attachable` linking the item identifier to the bbmodel-derived
     * geometry. This replaces the Java-side `client.model.<X>Model.java`
     * the spec calls out as dropped.
     *
     * Suppressed when [isVehicleItem] is true: a vehicle's bbmodel is the
     * full multi-block vehicle geometry (e.g. starwars' 2×0.8-block
     * landspeeder hull), not a compact held-item model — rendered as an
     * attachable it would show the entire vehicle floating in the player's
     * hand. The vehicle already gets its own `minecraft:client_entity`
     * geometry via [EntityAnalyzer]; the item only needs `entity_placer`.
     */
    private fun writeAttachableIfBbmodelExists(
        mod: ModDiscovery.DiscoveredMod,
        reg: ItemRegistration,
        icon: IconBasis,
        isVehicleItem: Boolean,
        outputRoot: Path,
    ) {
        if (isVehicleItem) return
        val toolsDir = mod.rootDir.resolve("tools")
        if (!Files.isDirectory(toolsDir)) return
        val bbmodel = toolsDir.resolve("${reg.itemId}.bbmodel")
        if (!bbmodel.isRegularFile()) return

        val identifier = "${mod.modId}:${reg.itemId}"
        val geometryName = "geometry.${mod.modId}.${reg.itemId}"
        val texturePath = resolveAttachableTexture(mod, reg, icon, outputRoot)
        val attachable = buildJsonObject {
            put("format_version", target.format_versions.attachable)
            put(
                "minecraft:attachable",
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
                                    put("default", JsonPrimitive(texturePath))
                                },
                            )
                            put(
                                "geometry",
                                buildJsonObject { put("default", JsonPrimitive(geometryName)) },
                            )
                            put(
                                "render_controllers",
                                buildJsonArray {
                                    add(JsonPrimitive("controller.render.item_default"))
                                },
                            )
                        },
                    )
                },
            )
        }
        val outPath = outputRoot.resolve("${mod.modId}/resource_pack/attachables/${reg.itemId}.json")
        outPath.parent?.createDirectories()
        outPath.writeText(JsonFormat.PRETTY.encodeToString(JsonElement.serializer(), attachable) + "\n")
    }

    /**
     * Pick the texture the attachable's `default` material should point at.
     *
     * The conventional path is `textures/entity/<itemId>` — a dedicated
     * 3rd-person/held-item texture authored alongside the `.bbmodel`
     * (verified against the output tree, since [com.tweeks.translator.json.AssetCopier]
     * runs before the Java pipeline). When no such texture was authored (as
     * with the starwars weapon bbmodels, which reuse the flat item-icon
     * texture), fall back to the already-resolved item icon's
     * `textures/items/<name>` and record the substitution. If neither
     * exists, the conventional path is still emitted (unchanged JSON shape)
     * but the gap is recorded so it isn't silent.
     */
    private fun resolveAttachableTexture(
        mod: ModDiscovery.DiscoveredMod,
        reg: ItemRegistration,
        icon: IconBasis,
        outputRoot: Path,
    ): String {
        val entityCandidate = "textures/entity/${reg.itemId}"
        val rpDir = outputRoot.resolve("${mod.modId}/resource_pack")
        if (rpDir.resolve("$entityCandidate.png").isRegularFile()) return entityCandidate

        val itemCandidate = "textures/items/${icon.textureShortName}"
        if (rpDir.resolve("$itemCandidate.png").isRegularFile()) {
            unt.recordAttachableTextureFallback(
                mod.modId,
                reg.itemId,
                "no dedicated $entityCandidate texture was authored; the attachable falls back to the " +
                    "item icon texture $itemCandidate for the 3D held-item view — verify visually in-game.",
            )
            return itemCandidate
        }

        unt.recordAttachableTextureFallback(
            mod.modId,
            reg.itemId,
            "referenced texture $entityCandidate does not exist in the output, and no item-icon " +
                "texture ($itemCandidate) was found as a fallback either; the 3D held-item view will be " +
                "missing in Bedrock.",
        )
        return entityCandidate
    }

    private fun writeItem(
        mod: ModDiscovery.DiscoveredMod,
        reg: ItemRegistration,
        icon: IconBasis,
        classIndex: Map<String, ClassOrInterfaceDeclaration>,
        classAttrs: Map<String, Double?>,
        classStackSize: Map<String, Int?>,
        classDurability: Map<String, Int?>,
        vehicleEntityIds: Set<String>,
        outputRoot: Path,
    ) {
        val identifier = "${mod.modId}:${reg.itemId}"

        val components = sortedMapOf<String, JsonElement>()
        components["minecraft:icon"] = JsonPrimitive(icon.identifier)
        icon.selectorNote?.let { unt.recordItemModelSelectorStatic(mod.modId, reg.itemId, it) }
        // Stack size resolution priority: registration `.stacksTo(N)` →
        // item-class constructor `.stacksTo(N)` → vanilla default 64.
        val stackSize = reg.stackSize ?: classStackSize[reg.itemClassName] ?: 64
        components["minecraft:max_stack_size"] = JsonPrimitive(stackSize)

        // Durability resolution priority: registration `.durability(N)` →
        // item-class constructor `.durability(N)` (wildwest Pistol/Rifle/etc.
        // pattern where the registration lambda is `p -> p`).
        val durability = reg.durability ?: classDurability[reg.itemClassName]
        if (durability != null) {
            components["minecraft:durability"] = buildJsonObject {
                put("max_durability", JsonPrimitive(durability))
            }
            // Items with durability are conventionally hand-equipped tools
            // in Bedrock — emit hand_equipped so the held-pose works in Bedrock.
            components["minecraft:hand_equipped"] = JsonPrimitive(true)
        }

        // Attack damage from the registration's `.attributes(...)` ItemAttributeModifiers
        // builder, or from inside the item class's constructor.
        val damage = reg.attackDamage ?: classAttrs[reg.itemClassName]
        if (damage != null) {
            components["minecraft:damage"] = JsonPrimitive(damage.toInt())
            components["minecraft:hand_equipped"] = JsonPrimitive(true)
        }

        // Per-item honesty notes, recorded once at the end of this method:
        // Untranslatable.recordItemCustomBehavior keys on itemId with a
        // single summary per item, so separate calls would overwrite each
        // other (armor note vs. overrides note).
        val behaviorNotes = mutableListOf<String>()

        // Armor wearable mapping (creeperskin, starwars):
        if (reg.armorSlot != null) {
            // Fold the piece's real defense value out of the mod's own
            // ArmorMaterial-holder class (`DEFENSE = Map.of(ArmorType.X, n, ...)`).
            // Falls back to vanilla iron-armor values (HELMET=2, CHESTPLATE=6,
            // LEGGINGS=5, BOOTS=2) only when the material can't be statically
            // resolved — e.g. it lives outside the mod's sources.
            val resolvedProtection = reg.armorMaterialClass
                ?.let { classIndex[it] }
                ?.let { readArmorDefense(it, reg.armorSlot) }
            components["minecraft:wearable"] = buildJsonObject {
                put("slot", JsonPrimitive(armorSlotToWearableSlot(reg.armorSlot)))
                put("protection", JsonPrimitive(resolvedProtection ?: armorSlotProtection(reg.armorSlot)))
            }
            components["minecraft:max_stack_size"] = JsonPrimitive(1)
            if (resolvedProtection == null) {
                behaviorNotes += "armor protection emitted as iron-armor defaults (${reg.armorSlot}); " +
                    "verify against the source ArmorMaterial if the mod customized it."
            }
            // Worn-body visuals: Java renders the equipment-asset layers on
            // the player model; the Bedrock pack does get the armor geometry
            // models + equipment textures, but no attachable is emitted to
            // consume them, so nothing renders on the body. True for every
            // modded wearable.
            behaviorNotes += "worn-armor visuals are absent on Bedrock — the item equips and protects " +
                "(minecraft:wearable); the armor geometry/textures are emitted but no attachable " +
                "consumes them, so nothing renders on the player's body."
        }

        if (reg.fireResistant) {
            components["minecraft:fire_resistant"] = JsonObject(emptyMap())
        }

        // Vehicle placer: this item's id matches a VehicleEntity subclass's
        // registered entity id (e.g. starwars' "landspeeder") — without
        // `minecraft:entity_placer`, the crafted item does nothing when used
        // on Bedrock.
        if (reg.itemId in vehicleEntityIds) {
            components["minecraft:entity_placer"] = buildJsonObject {
                put("entity", identifier)
            }
        }

        // Spawn egg.
        if (reg.spawnEggEntityId != null) {
            components["minecraft:spawn_egg"] = buildJsonObject {
                put("type_id", JsonPrimitive("${mod.modId}:${reg.spawnEggEntityId}"))
                put("base_color", JsonPrimitive("#444444"))
                put("overlay_color", JsonPrimitive("#888888"))
            }
            unt.recordSpawnEggColorsHardcoded(
                mod.modId,
                reg.itemId,
                "Java side computes colors via EntityType.Builder defaults; Phase 2 hardcodes #444444/#888888.",
            )
        }

        // Detect custom-behavior overrides on the item class.
        val itemClass = classIndex[reg.itemClassName]
        val overriddenMethods = itemClass?.methods?.filter {
            it.nameAsString in CUSTOM_BEHAVIOR_OVERRIDES
        } ?: emptyList()
        if (overriddenMethods.isNotEmpty()) {
            val names = overriddenMethods.joinToString(", ") { it.nameAsString }
            val summary = StringBuilder("${reg.itemClassName} overrides: $names")
            // Scoundrel's Luck set bonus (starwars' Han Solo armor set):
            // recordItemCustomBehavior keys on itemId with a single summary
            // per item, so a class already flagged above for its `use()`
            // override (e.g. BlasterPistolItem) gets the set-bonus honesty
            // note appended here rather than via a second, overwriting call.
            // Detected the same way EntityAnalyzer flags SavedData-backed
            // singletons: an AST scan for the simple name, since there's no
            // shared type to resolve against across modules.
            // TODO: the set-bonus note only fires when the class also overrides a
            // CUSTOM_BEHAVIOR_OVERRIDES method (e.g. inherited-use subclasses like
            // BlasterRifleItem are missed).
            if (itemClass != null && referencesSimpleName(itemClass, "ScoundrelLuck")) {
                summary.append(
                    ". Scoundrel's Luck set bonus (full Han Solo set doubles the first blaster " +
                        "shot against each new target) is server-side Java logic — absent on Bedrock.",
                )
            }
            behaviorNotes += summary.toString()
        }
        if (behaviorNotes.isNotEmpty()) {
            unt.recordItemCustomBehavior(mod.modId, reg.itemId, behaviorNotes.joinToString(" "))
        }

        val itemJson = buildJsonObject {
            put("format_version", target.format_versions.item)
            put(
                "minecraft:item",
                buildJsonObject {
                    put(
                        "description",
                        buildJsonObject {
                            put("identifier", identifier)
                            put(
                                "menu_category",
                                buildJsonObject {
                                    put("category", JsonPrimitive(if (reg.spawnEggEntityId != null) "nature" else "items"))
                                },
                            )
                        },
                    )
                    put("components", JsonObject(components))
                },
            )
        }

        val outPath = outputRoot.resolve("${mod.modId}/behavior_pack/items/${reg.itemId}.json")
        outPath.parent?.createDirectories()
        outPath.writeText(JsonFormat.PRETTY.encodeToString(JsonElement.serializer(), itemJson) + "\n")
    }

    // ---------- Reading the registration AST ----------

    internal data class ItemRegistration(
        val itemId: String,
        val itemClassName: String,
        val stackSize: Int?,
        val durability: Int?,
        val attackDamage: Double?,
        val armorSlot: String?,
        /** Simple name of the ArmorMaterial-holder class from
         *  `.humanoidArmor(<Class>.<CONSTANT>, ...)`, when present. Resolved
         *  against the mod's own sources to fold the material's real
         *  `DEFENSE` values into `minecraft:wearable.protection`. */
        val armorMaterialClass: String?,
        val fireResistant: Boolean,
        val spawnEggEntityId: String?,
    )

    private fun collectItemRegistrations(
        sources: JavaSourceLoader.ResolvedModSources,
    ): List<ItemRegistration> {
        val out = mutableListOf<ItemRegistration>()

        // Build the entity-constant → entity-id map across ALL units so that
        // items registered in Registration.java can resolve constants declared
        // in ModEntities.java (the wildwest layout). Within-unit registration
        // (the securityguard layout) is still covered.
        val entityIdsByConstant = mutableMapOf<String, String>()
        for (unit in sources.units) {
            for (call in unit.findAll(MethodCallExpr::class.java)) {
                if (call.nameAsString != "register") continue
                val scope = call.scope.orElse(null) ?: continue
                if (scope.toString() != "ENTITY_TYPES") continue
                if (call.arguments.size < 2) continue
                val id = (call.arguments[0] as? StringLiteralExpr)?.asString() ?: continue
                val fieldDecl = call.findAncestor(com.github.javaparser.ast.body.FieldDeclaration::class.java)
                fieldDecl.ifPresent { fd ->
                    fd.variables.forEach { v -> entityIdsByConstant[v.nameAsString] = id }
                }
            }
        }

        for (unit in sources.units) {
            for (call in unit.findAll(MethodCallExpr::class.java)) {
                if (call.nameAsString != "registerItem") continue
                val scope = call.scope.orElse(null) ?: continue
                if (scope.toString() != "ITEMS") continue
                if (call.arguments.size < 2) continue
                val id = (call.arguments[0] as? StringLiteralExpr)?.asString() ?: continue

                // arg[1] is `Cls::new` — pull simple class name from the AST text.
                val classRef = call.arguments[1].toString()
                val itemClassName = classRef.substringBefore("::").substringAfterLast('.')

                // arg[2], when present, is the `p -> p.stacksTo(...)...` lambda.
                val builderCalls = if (call.arguments.size >= 3) {
                    call.arguments[2].findAll(MethodCallExpr::class.java)
                } else emptyList()

                val stackSize = builderCalls.firstOrNull { it.nameAsString == "stacksTo" }
                    ?.arguments?.firstOrNull()?.let { readIntLiteral(it) }
                val durability = builderCalls.firstOrNull { it.nameAsString == "durability" }
                    ?.arguments?.firstOrNull()?.let { readIntLiteral(it) }

                // Spawn egg: `.spawnEgg(SECURITY_GUARD.get())` (securityguard
                // layout — same file) or `.spawnEgg(ModEntities.DEPUTY.get())`
                // (wildwest layout — qualified field). Take the rightmost
                // identifier so both shapes resolve.
                val spawnEggEntityId = builderCalls
                    .firstOrNull { it.nameAsString == "spawnEgg" }
                    ?.arguments?.firstOrNull()?.let { sgArg ->
                        val mc = sgArg as? MethodCallExpr ?: return@let null
                        val sc = mc.scope.orElse(null) ?: return@let null
                        val constantName = when (sc) {
                            is FieldAccessExpr -> sc.nameAsString
                            is NameExpr -> sc.nameAsString
                            else -> sc.toString().substringAfterLast('.')
                        }
                        entityIdsByConstant[constantName]
                    }

                // Armor: `.humanoidArmor(<MaterialClass>.<CONSTANT>, ArmorType.HELMET)`
                val armorCall = builderCalls.firstOrNull { it.nameAsString == "humanoidArmor" }
                val armorSlot = armorCall
                    ?.arguments?.getOrNull(1)?.let { armorArg ->
                        (armorArg as? FieldAccessExpr)?.nameAsString
                    }
                // Simple name of the material-holder class: the last scope
                // segment of the material constant reference. Handles both
                // `CreeperArmorMaterials.CREEPER` (NameExpr scope) and the
                // fully-qualified starwars shape
                // `com.tweeks.starwars.item.HanSoloArmorMaterials.HAN_SOLO`
                // (chained FieldAccessExpr scope).
                val armorMaterialClass = armorCall
                    ?.arguments?.firstOrNull()?.let { matArg ->
                        when (val sc = (matArg as? FieldAccessExpr)?.scope) {
                            is NameExpr -> sc.nameAsString
                            is FieldAccessExpr -> sc.nameAsString
                            null -> null
                            else -> sc.toString().substringAfterLast('.')
                        }
                    }

                val fireResistant = builderCalls.any { it.nameAsString == "fireResistant" }

                // Attack damage: `.attributes(X)` referring to a static
                // `ItemAttributeModifiers` field declared elsewhere in the
                // same unit.
                val attrsRefName = builderCalls
                    .firstOrNull { it.nameAsString == "attributes" }
                    ?.arguments?.firstOrNull()
                    ?.let { (it as? NameExpr)?.nameAsString ?: it.toString() }
                val attackDamage = attrsRefName?.let { findAttackDamageOnNamedField(unit, it) }

                out.add(
                    ItemRegistration(
                        itemId = id,
                        itemClassName = itemClassName,
                        stackSize = stackSize,
                        durability = durability,
                        attackDamage = attackDamage,
                        armorSlot = armorSlot,
                        armorMaterialClass = armorMaterialClass,
                        fireResistant = fireResistant,
                        spawnEggEntityId = spawnEggEntityId,
                    )
                )
            }
        }
        return out.sortedBy { it.itemId }
    }

    /**
     * Find `private static final ItemAttributeModifiers BATON_ATTRIBUTES = builder()...`
     * and pull the first `Attributes.ATTACK_DAMAGE` modifier value out.
     * Returns null if not found.
     */
    private fun findAttackDamageOnNamedField(
        unit: com.github.javaparser.ast.CompilationUnit,
        fieldName: String,
    ): Double? {
        for (fd in unit.findAll(com.github.javaparser.ast.body.FieldDeclaration::class.java)) {
            val matchedVar = fd.variables.firstOrNull { it.nameAsString == fieldName } ?: continue
            return readAttackDamageFromInitializer(matchedVar.initializer.orElse(null) ?: continue)
        }
        return null
    }

    /**
     * Walk an item-class declaration looking for `<properties>.stacksTo(N)`
     * inside a constructor. Used as a fallback when the registration site
     * doesn't carry a stack-size hint (e.g. BlackjackItem, where
     * `stacksTo(1)` is configured inside `BlackjackItem`'s own constructor
     * rather than at registration).
     */
    private fun readStackSizeInClass(decl: ClassOrInterfaceDeclaration): Int? {
        for (call in decl.findAll(MethodCallExpr::class.java)) {
            if (call.nameAsString != "stacksTo") continue
            val v = call.arguments.firstOrNull()?.let { readIntLiteral(it) }
            if (v != null) return v
        }
        return null
    }

    /**
     * Mirror of [readStackSizeInClass] for `properties.durability(N)` calls
     * inside an item-class constructor (the wildwest weapon pattern).
     */
    private fun readDurabilityInClass(decl: ClassOrInterfaceDeclaration): Int? {
        for (call in decl.findAll(MethodCallExpr::class.java)) {
            if (call.nameAsString != "durability") continue
            val v = call.arguments.firstOrNull()?.let { readIntLiteral(it) }
            if (v != null) return v
        }
        return null
    }

    /**
     * Fold one piece's defense value out of an ArmorMaterial-holder class:
     * find its `DEFENSE` field initialized with
     * `Map.of(ArmorType.BOOTS, 3, ArmorType.LEGGINGS, 6, ...)` and return
     * the integer paired with [armorType] (e.g. `HELMET`). Returns null when
     * the field, the `Map.of` shape, or the requested type key is absent —
     * the caller then falls back to iron-armor defaults and records the gap.
     */
    private fun readArmorDefense(decl: ClassOrInterfaceDeclaration, armorType: String): Int? {
        for (fd in decl.findAll(com.github.javaparser.ast.body.FieldDeclaration::class.java)) {
            val defenseVar = fd.variables.firstOrNull { it.nameAsString == "DEFENSE" } ?: continue
            val call = defenseVar.initializer.orElse(null) as? MethodCallExpr ?: continue
            if (call.nameAsString != "of") continue
            // `Map.of` args alternate key, value.
            var i = 0
            while (i + 1 < call.arguments.size) {
                val keyName = when (val key = call.arguments[i]) {
                    is FieldAccessExpr -> key.nameAsString
                    is NameExpr -> key.nameAsString
                    else -> key.toString().substringAfterLast('.')
                }
                if (keyName == armorType) return readIntLiteral(call.arguments[i + 1])
                i += 2
            }
        }
        return null
    }

    /**
     * Walk an item-class declaration (e.g. `BlackjackItem`) for any constructor
     * that builds an `ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE, ...)` call.
     */
    private fun readAttackDamageInClass(decl: ClassOrInterfaceDeclaration): Double? {
        for (call in decl.findAll(MethodCallExpr::class.java)) {
            if (call.nameAsString != "add") continue
            val first = call.arguments.firstOrNull() ?: continue
            if (first.toString().endsWith("ATTACK_DAMAGE")) {
                // The next arg is `new AttributeModifier(id, value, op)`.
                val ctor = call.arguments.getOrNull(1)
                    as? com.github.javaparser.ast.expr.ObjectCreationExpr ?: continue
                val damageArg = ctor.arguments.getOrNull(1) ?: continue
                val v = readDoubleLiteral(damageArg)
                if (v != null) return v
            }
        }
        return null
    }

    private fun readAttackDamageFromInitializer(init: Expression): Double? {
        for (call in init.findAll(MethodCallExpr::class.java)) {
            if (call.nameAsString != "add") continue
            val first = call.arguments.firstOrNull() ?: continue
            if (first.toString().endsWith("ATTACK_DAMAGE")) {
                val ctor = call.arguments.getOrNull(1)
                    as? com.github.javaparser.ast.expr.ObjectCreationExpr ?: continue
                val damageArg = ctor.arguments.getOrNull(1) ?: continue
                val v = readDoubleLiteral(damageArg)
                if (v != null) return v
            }
        }
        return null
    }

    // ---------- Helpers ----------

    /**
     * Does [decl]'s AST reference the given simple name anywhere — as a type
     * (`ClassOrInterfaceType`, e.g. a field/local declared with that type) or
     * as a bare name expression (e.g. the scope of a static call like
     * `ScoundrelLuck.isWearingFullHanSoloSet(...)`)? Same technique as
     * [EntityAnalyzer]'s SavedData detection: a plain simple-name AST scan,
     * since the translator doesn't resolve cross-module types.
     */
    private fun referencesSimpleName(decl: ClassOrInterfaceDeclaration, simpleName: String): Boolean =
        decl.findAll(com.github.javaparser.ast.type.ClassOrInterfaceType::class.java)
            .any { it.nameAsString == simpleName } ||
            decl.findAll(NameExpr::class.java).any { it.nameAsString == simpleName }

    private fun readIntLiteral(expr: Expression): Int? {
        return when (expr) {
            is IntegerLiteralExpr -> expr.asNumber().toInt()
            is LongLiteralExpr -> expr.asNumber().toInt()
            is UnaryExpr -> {
                val inner = readIntLiteral(expr.expression) ?: return null
                when (expr.operator) {
                    UnaryExpr.Operator.MINUS -> -inner
                    UnaryExpr.Operator.PLUS -> inner
                    else -> null
                }
            }
            else -> null
        }
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

    private fun armorSlotToWearableSlot(javaArmorType: String): String = when (javaArmorType) {
        "HELMET" -> "slot.armor.head"
        "CHESTPLATE" -> "slot.armor.chest"
        "LEGGINGS" -> "slot.armor.legs"
        "BOOTS" -> "slot.armor.feet"
        else -> "slot.armor.chest"
    }

    private fun armorSlotProtection(javaArmorType: String): Int = when (javaArmorType) {
        // Java vanilla iron armor protection by slot (same scale as Bedrock).
        // TODO: read the actual ArmorMaterial from the AST so we don't lie
        // when a mod customizes protection. Until then, the caller logs an
        // UNTRANSLATABLE entry so reviewers know the value is approximate.
        "HELMET" -> 2
        "CHESTPLATE" -> 6
        "LEGGINGS" -> 5
        "BOOTS" -> 2
        else -> 0
    }

    companion object {
        private val CUSTOM_BEHAVIOR_OVERRIDES = setOf(
            "use",
            "useOn",
            "useOnEntity",
            "hurtEnemy",
            "postHurtEnemy",
            "inventoryTick",
            "onCraftedBy",
            "finishUsingItem",
            "releaseUsing",
        )
    }
}
