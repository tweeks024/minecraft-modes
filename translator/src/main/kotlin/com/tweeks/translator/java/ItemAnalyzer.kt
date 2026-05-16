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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.nio.file.Path
import kotlin.io.path.createDirectories
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
                writeItem(mod, reg, classIndex, classAttrs, classStackSize, classDurability, outputRoot)
                writeAttachableIfBbmodelExists(mod, reg, outputRoot)
            } catch (e: Throwable) {
                unt.recordPhase2Failure(
                    mod.modId,
                    "ItemAnalyzer failed on ${reg.itemId}: ${e.javaClass.simpleName}: ${e.message}",
                )
            }
        }
    }

    /**
     * If a `tools/<itemId>.bbmodel` exists for this item, emit a Bedrock
     * `attachable` linking the item identifier to the bbmodel-derived
     * geometry. This replaces the Java-side `client.model.<X>Model.java`
     * the spec calls out as dropped.
     */
    private fun writeAttachableIfBbmodelExists(
        mod: ModDiscovery.DiscoveredMod,
        reg: ItemRegistration,
        outputRoot: Path,
    ) {
        val toolsDir = mod.rootDir.resolve("tools")
        if (!java.nio.file.Files.isDirectory(toolsDir)) return
        val bbmodel = toolsDir.resolve("${reg.itemId}.bbmodel")
        if (!java.nio.file.Files.isRegularFile(bbmodel)) return

        val identifier = "${mod.modId}:${reg.itemId}"
        val geometryName = "geometry.${mod.modId}.${reg.itemId}"
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
                                    put("default", JsonPrimitive("textures/entity/${reg.itemId}"))
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

    private fun writeItem(
        mod: ModDiscovery.DiscoveredMod,
        reg: ItemRegistration,
        classIndex: Map<String, ClassOrInterfaceDeclaration>,
        classAttrs: Map<String, Double?>,
        classStackSize: Map<String, Int?>,
        classDurability: Map<String, Int?>,
        outputRoot: Path,
    ) {
        val identifier = "${mod.modId}:${reg.itemId}"

        val components = sortedMapOf<String, JsonElement>()
        components["minecraft:icon"] = JsonPrimitive(identifier)
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

        // Armor wearable mapping (creeperskin):
        if (reg.armorSlot != null) {
            components["minecraft:wearable"] = buildJsonObject {
                put("slot", JsonPrimitive(armorSlotToWearableSlot(reg.armorSlot)))
                put("protection", JsonPrimitive(armorSlotProtection(reg.armorSlot)))
            }
            components["minecraft:max_stack_size"] = JsonPrimitive(1)
        }

        if (reg.fireResistant) {
            components["minecraft:fire_resistant"] = JsonObject(emptyMap())
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
            unt.recordItemCustomBehavior(
                mod.modId,
                reg.itemId,
                "${reg.itemClassName} overrides: $names",
            )
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
                                    put("category", JsonPrimitive(if (reg.spawnEggEntityId != null) "items" else "items"))
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

                // Armor: `.humanoidArmor(<material>, ArmorType.HELMET)`
                val armorSlot = builderCalls
                    .firstOrNull { it.nameAsString == "humanoidArmor" }
                    ?.arguments?.getOrNull(1)?.let { armorArg ->
                        (armorArg as? FieldAccessExpr)?.nameAsString
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
        // Java vanilla iron armor protection by slot (same scale as Bedrock):
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
