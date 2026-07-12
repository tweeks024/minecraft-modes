package com.tweeks.translator.java

import com.tweeks.translator.discover.ModDiscovery
import com.tweeks.translator.emit.Untranslatable
import com.tweeks.translator.manifest.BedrockTarget
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * End-to-end test for [EntityAnalyzer] over the real `securityguard` mod.
 *
 * Verifies that:
 *   - the entity JSON is emitted with the expected identifier;
 *   - vanilla `MAX_HEALTH` / `ATTACK_DAMAGE` etc. attributes round-trip;
 *   - High-bucket goals appear as `minecraft:behavior.*` components with
 *     priority pulled from the source `addGoal(priority, ...)` call site;
 *   - custom goals (`StunningMeleeGoal`, `GuardTargetHostilesGoal`) do
 *     **not** appear as components but are logged on Untranslatable.
 *   - the resource_pack `<entity_id>.entity.json` carries the expected
 *     securityguard-specific `security_guard` geometry/texture mapping.
 */
class EntityAnalyzerTest {

    private val repoRoot: Path = ModDiscovery.findRepoRoot(Path.of("").toAbsolutePath())
    private val target = BedrockTarget.load(repoRoot.resolve("translator/bedrock-target.json"))

    @Test
    fun `securityguard guard entity emits expected components`(@TempDir outDir: Path) {
        val resolver = ClasspathResolver.fromSystemProperties()
        assertTrue(resolver.isAvailable())
        val all = ModDiscovery(repoRoot).discover()
        val securityguard = all.first { it.modId == "securityguard" }
        val unt = Untranslatable()
        val sources = JavaSourceLoader(resolver, unt).load(securityguard, all)

        EntityAnalyzer(target, unt).analyze(securityguard, sources, outDir)

        val behaviorPath = outDir.resolve("securityguard/behavior_pack/entities/guard.json")
        assertTrue(behaviorPath.toFile().exists(), "entity behavior pack JSON must be written")

        val parsed = Json.parseToJsonElement(behaviorPath.readText()) as JsonObject
        val entity = parsed["minecraft:entity"]!!.jsonObject
        val desc = entity["description"]!!.jsonObject
        assertEquals("securityguard:guard", desc["identifier"]!!.jsonPrimitive.content)
        assertEquals(true, desc["is_spawnable"]!!.jsonPrimitive.content.toBoolean())

        val components = entity["components"]!!.jsonObject

        // Attributes pulled from createAttributes().
        assertEquals(50, components["minecraft:health"]!!.jsonObject["value"]!!.jsonPrimitive.intOrNull)
        assertEquals(5, components["minecraft:attack"]!!.jsonObject["damage"]!!.jsonPrimitive.intOrNull)

        // High-bucket vanilla goals should be present, with priority injected.
        val moveTowards = components["minecraft:behavior.move_towards_target"]!!.jsonObject
        assertEquals(2, moveTowards["priority"]!!.jsonPrimitive.intOrNull)

        // Two goals map to random_stroll: GolemRandomStrollInVillageGoal at
        // priority 4 (higher priority — lower number) and RandomStrollGoal at
        // priority 7 (lower priority). Bedrock entity JSON requires unique
        // component keys, so the higher-priority one wins; the dropped goal
        // is recorded via Untranslatable.recordDuplicateBehaviorComponent.
        val randomStroll = components["minecraft:behavior.random_stroll"]!!.jsonObject
        assertEquals(4, randomStroll["priority"]!!.jsonPrimitive.intOrNull)

        val defendVillage = components["minecraft:behavior.defend_village_target"]!!.jsonObject
        assertEquals(1, defendVillage["priority"]!!.jsonPrimitive.intOrNull)

        // Custom goals must NOT appear as components.
        assertNull(components["minecraft:behavior.stunning_melee"])
        assertNull(components["minecraft:behavior.guard_target_hostiles"])

        // The collision_box should be a clean 0.6 × 1.95 (no float-noise tail).
        val box = components["minecraft:collision_box"]!!.jsonObject
        assertEquals(0.6, box["width"]!!.jsonPrimitive.content.toDouble())
        assertEquals(1.95, box["height"]!!.jsonPrimitive.content.toDouble())

        // The resource_pack client_entity wires geometry to security_guard.
        val clientPath = outDir.resolve("securityguard/resource_pack/entity/guard.entity.json")
        assertTrue(clientPath.toFile().exists())
        val clientJson = Json.parseToJsonElement(clientPath.readText()) as JsonObject
        val clientDesc = clientJson["minecraft:client_entity"]!!.jsonObject["description"]!!.jsonObject
        val geometry = clientDesc["geometry"]!!.jsonObject["default"]!!.jsonPrimitive.content
        assertEquals("geometry.securityguard.security_guard", geometry)

        val tex = clientDesc["textures"]!!.jsonObject["default"]!!.jsonPrimitive.content
        assertEquals("textures/entity/security_guard.png", tex)

        // Untranslatable should record the deferred custom goals. With no
        // Phase 3 gate wired in this test, the goals land in the
        // "deferred (Phase 3 LLM stage not run)" section.
        val report = unt.renderReport("securityguard")
        assertTrue(report.contains("Entity goals deferred")) { report }
        assertTrue(report.contains("StunningMeleeGoal"))
        assertTrue(report.contains("GuardTargetHostilesGoal"))
    }

    @Test
    fun `thief entity emits vanilla goals only and defers custom ones`(@TempDir outDir: Path) {
        val resolver = ClasspathResolver.fromSystemProperties()
        assertTrue(resolver.isAvailable())
        val all = ModDiscovery(repoRoot).discover()
        val thief = all.first { it.modId == "thief" }
        val unt = Untranslatable()
        val sources = JavaSourceLoader(resolver, unt).load(thief, all)

        EntityAnalyzer(target, unt).analyze(thief, sources, outDir)

        val behaviorPath = outDir.resolve("thief/behavior_pack/entities/thief.json")
        assertTrue(behaviorPath.toFile().exists())

        val parsed = Json.parseToJsonElement(behaviorPath.readText()) as JsonObject
        val components = parsed["minecraft:entity"]!!.jsonObject["components"]!!.jsonObject

        // Vanilla High-bucket goals.
        assertNotNull(components["minecraft:behavior.float"])
        assertNotNull(components["minecraft:behavior.random_stroll"])
        assertNotNull(components["minecraft:behavior.look_at_player"])
        assertNotNull(components["minecraft:behavior.hurt_by_target"])

        // Thief mob_category is MONSTER → family contains "monster".
        val family = components["minecraft:type_family"]!!.jsonObject["family"]!!
        assertTrue(family.toString().contains("monster"))

        // Phase 4.1: ThiefEntity implements SecurityHostile, so the family
        // list must include `security_hostile`. This is what cross-mod
        // targeting (SecurityGuard's nearest_attackable_target) keys off.
        assertTrue(family.toString().contains("security_hostile")) {
            "Thief implements SecurityHostile; expected `security_hostile` family tag in $family"
        }

        val report = unt.renderReport("thief")
        assertTrue(report.contains("BlackjackStrikeGoal") || report.contains("FleeAndFireCrossbowGoal"))
    }

    @Test
    fun `starwars stormtrooper inherits superclass goals and folds named-constant attributes`(@TempDir outDir: Path) {
        val resolver = ClasspathResolver.fromSystemProperties()
        assertTrue(resolver.isAvailable())
        val all = ModDiscovery(repoRoot).discover()
        val starwars = all.first { it.modId == "starwars" }
        val unt = Untranslatable()
        val sources = JavaSourceLoader(resolver, unt).load(starwars, all)

        EntityAnalyzer(target, unt).analyze(starwars, sources, outDir)

        val behaviorPath = outDir.resolve("starwars/behavior_pack/entities/stormtrooper.json")
        assertTrue(behaviorPath.toFile().exists(), "stormtrooper behavior JSON must be written")

        val parsed = Json.parseToJsonElement(behaviorPath.readText()) as JsonObject
        val components = parsed["minecraft:entity"]!!.jsonObject["components"]!!.jsonObject

        // Named-constant attributes fold to literals (SwMobConstants.*).
        assertEquals(20, components["minecraft:health"]!!.jsonObject["value"]!!.jsonPrimitive.intOrNull)
        assertEquals(0.30, components["minecraft:movement"]!!.jsonObject["value"]!!.jsonPrimitive.content.toDouble())
        assertEquals(24, components["minecraft:follow_range"]!!.jsonObject["value"]!!.jsonPrimitive.intOrNull)

        // Goals declared on the SwMob superclass (StormtrooperEntity has no
        // registerGoals of its own) must be collected.
        assertNotNull(components["minecraft:behavior.float"])
        assertNotNull(components["minecraft:behavior.random_stroll"])
        assertNotNull(components["minecraft:behavior.look_at_player"])
        assertNotNull(components["minecraft:behavior.random_look_around"])
        assertNotNull(components["minecraft:behavior.hurt_by_target"])

        // Custom SwMob goals are deferred (not silently dropped).
        val report = unt.renderReport("starwars")
        assertTrue(report.contains("BlasterAttackGoal")) { report }
        assertTrue(report.contains("SwTargetGoal")) { report }
    }

    @Test
    fun `battle droid folds droid-specific named constants`(@TempDir outDir: Path) {
        val resolver = ClasspathResolver.fromSystemProperties()
        assertTrue(resolver.isAvailable())
        val all = ModDiscovery(repoRoot).discover()
        val starwars = all.first { it.modId == "starwars" }
        val unt = Untranslatable()
        val sources = JavaSourceLoader(resolver, unt).load(starwars, all)

        EntityAnalyzer(target, unt).analyze(starwars, sources, outDir)

        val parsed = Json.parseToJsonElement(
            outDir.resolve("starwars/behavior_pack/entities/battle_droid.json").readText()
        ) as JsonObject
        val components = parsed["minecraft:entity"]!!.jsonObject["components"]!!.jsonObject
        assertEquals(12, components["minecraft:health"]!!.jsonObject["value"]!!.jsonPrimitive.intOrNull)
        assertEquals(0.28, components["minecraft:movement"]!!.jsonObject["value"]!!.jsonPrimitive.content.toDouble())
    }

    @Test
    fun `synthetic super-merge and cross-class constant folding`(@TempDir outDir: Path) {
        // Exercises, in isolation from any real mod:
        //  - super.registerGoals() merge (leaf goals + superclass goals);
        //  - cross-class constant (SynthConstants.SYNTH_HEALTH);
        //  - inherited constant (BASE_SPEED declared on the superclass);
        //  - own-class constant with a unary-minus initializer (LOCAL_DAMAGE).
        val registrationSrc = """
            package com.example.themod;
            import net.minecraft.world.entity.MobCategory;
            class ModEntities {
                public static final Object SYNTH = ENTITY_TYPES.register("synthmob",
                    () -> EntityType.Builder.<SynthMob>of(SynthMob::new, MobCategory.MONSTER)
                        .sized(0.6f, 1.95f)
                        .build("themod:synthmob"));
            }
        """.trimIndent()
        val constantsSrc = """
            package com.example.themod;
            public final class SynthConstants {
                public static final double SYNTH_HEALTH = 30.0;
            }
        """.trimIndent()
        val baseSrc = """
            package com.example.themod;
            import net.minecraft.world.entity.EntityType;
            import net.minecraft.world.entity.PathfinderMob;
            import net.minecraft.world.entity.ai.goal.FloatGoal;
            import net.minecraft.world.level.Level;
            public abstract class SynthBase extends PathfinderMob {
                protected static final double BASE_SPEED = 0.25;
                protected SynthBase(EntityType<? extends SynthBase> type, Level level) { super(type, level); }
                protected void registerGoals() {
                    this.goalSelector.addGoal(0, new FloatGoal(this));
                }
            }
        """.trimIndent()
        val leafSrc = """
            package com.example.themod;
            import net.minecraft.world.entity.EntityType;
            import net.minecraft.world.entity.PathfinderMob;
            import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
            import net.minecraft.world.entity.ai.attributes.Attributes;
            import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
            import net.minecraft.world.entity.player.Player;
            import net.minecraft.world.level.Level;
            public class SynthMob extends SynthBase {
                public static final double LOCAL_DAMAGE = -1.0;
                public SynthMob(EntityType<? extends SynthMob> type, Level level) { super(type, level); }
                public static AttributeSupplier.Builder createAttributes() {
                    return PathfinderMob.createMobAttributes()
                        .add(Attributes.MAX_HEALTH, SynthConstants.SYNTH_HEALTH)
                        .add(Attributes.MOVEMENT_SPEED, BASE_SPEED)
                        .add(Attributes.ATTACK_DAMAGE, LOCAL_DAMAGE);
                }
                @Override
                protected void registerGoals() {
                    super.registerGoals();
                    this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
                }
            }
        """.trimIndent()
        val mod = ModDiscovery.DiscoveredMod(modId = "themod", rootDir = outDir.resolve("themodroot"))
        java.nio.file.Files.createDirectories(mod.rootDir)

        val parser = com.github.javaparser.JavaParser()
        val units = listOf(registrationSrc, constantsSrc, baseSrc, leafSrc)
            .map { parser.parse(it).result.orElseThrow() }
        val sources = JavaSourceLoader.ResolvedModSources(
            mod = mod,
            units = units,
            typeSolver = com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver(),
        )

        val unt = Untranslatable()
        EntityAnalyzer(target, unt).analyze(mod, sources, outDir)

        val parsed = Json.parseToJsonElement(
            outDir.resolve("themod/behavior_pack/entities/synthmob.json").readText()
        ) as JsonObject
        val components = parsed["minecraft:entity"]!!.jsonObject["components"]!!.jsonObject

        // Constant folding.
        assertEquals(30, components["minecraft:health"]!!.jsonObject["value"]!!.jsonPrimitive.intOrNull)
        assertEquals(0.25, components["minecraft:movement"]!!.jsonObject["value"]!!.jsonPrimitive.content.toDouble())
        assertEquals(-1, components["minecraft:attack"]!!.jsonObject["damage"]!!.jsonPrimitive.intOrNull)

        // Super-merge: FloatGoal from SynthBase + LookAtPlayerGoal from SynthMob.
        assertNotNull(components["minecraft:behavior.float"])
        assertNotNull(components["minecraft:behavior.look_at_player"])
    }

    @Test
    fun `projectile entity does not get mob navigation components`(@TempDir outDir: Path) {
        // Synthetic wildwest-style projectile. Critical bug C4: previously
        // every entity got minecraft:navigation.walk + jump.static + pushable
        // regardless of whether it was a Monster or a ThrowableItemProjectile.
        // Result was invalid Bedrock JSON for projectiles.
        val registrationSrc = """
            package com.example.themod;
            import net.minecraft.world.entity.MobCategory;
            class ModEntities {
                public static final Object BULLET = ENTITY_TYPES.register("bullet",
                    () -> EntityType.Builder.<BulletEntity>of(BulletEntity::new, MobCategory.MISC)
                        .sized(0.25f, 0.25f)
                        .build("themod:bullet"));
            }
        """.trimIndent()
        val bulletEntitySrc = """
            package com.example.themod;
            import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
            public class BulletEntity extends ThrowableItemProjectile {
            }
        """.trimIndent()
        val mod = ModDiscovery.DiscoveredMod(modId = "themod", rootDir = outDir.resolve("themodroot"))
        java.nio.file.Files.createDirectories(mod.rootDir)

        val parser = com.github.javaparser.JavaParser()
        val units = listOf(registrationSrc, bulletEntitySrc).map { parser.parse(it).result.orElseThrow() }
        val sources = JavaSourceLoader.ResolvedModSources(
            mod = mod,
            units = units,
            typeSolver = com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver(),
        )

        val unt = Untranslatable()
        EntityAnalyzer(target, unt).analyze(mod, sources, outDir)

        // Projectiles are skipped (not emitted as mob JSON) — better to ship
        // nothing for the entity than invalid Bedrock JSON. The skip is
        // surfaced via the untranslatable report so users see what's missing.
        val behaviorPath = outDir.resolve("themod/behavior_pack/entities/bullet.json")
        assertTrue(!behaviorPath.toFile().exists(), "projectile must not be emitted as mob JSON")

        val report = unt.renderReport("themod")
        assertTrue(report.contains("BulletEntity") || report.contains("bullet")) {
            "Expected projectile reference in untranslatable report: $report"
        }
    }

    @Test
    fun `vehicle entity emits rideable not walking mob`(@TempDir outDir: Path) {
        // Synthetic starwars-style landspeeder: extends VehicleEntity (not
        // Mob), has no createAttributes()/registerGoals() at all — the usual
        // mob pipeline would emit a walking-mob JSON with no seats. Instead
        // it must be routed to the vehicle branch: rideable, ground-input
        // controlled, health/movement folded from the entity's own
        // MAX_HULL_HEALTH and the sibling HoverPhysics.MAX_SPEED constants.
        val registrationSrc = """
            package com.example.themod;
            import net.minecraft.world.entity.MobCategory;
            class ModEntities {
                public static final Object LANDSPEEDER = ENTITY_TYPES.register("landspeeder",
                    () -> EntityType.Builder.<LandspeederEntity>of(LandspeederEntity::new, MobCategory.MISC)
                        .sized(2.0f, 0.8f)
                        .build("themod:landspeeder"));
            }
        """.trimIndent()
        val entitySrc = """
            package com.example.themod;
            import net.minecraft.world.entity.EntityType;
            import net.minecraft.world.entity.vehicle.VehicleEntity;
            import net.minecraft.world.level.Level;
            public class LandspeederEntity extends VehicleEntity {
                public static final float MAX_HULL_HEALTH = 40.0f;
                public LandspeederEntity(EntityType<? extends LandspeederEntity> type, Level level) { super(type, level); }
            }
        """.trimIndent()
        val hoverPhysicsSrc = """
            package com.example.themod;
            public final class HoverPhysics {
                public static final double MAX_SPEED = 0.7;
            }
        """.trimIndent()
        val mod = ModDiscovery.DiscoveredMod(modId = "themod", rootDir = outDir.resolve("themodroot"))
        java.nio.file.Files.createDirectories(mod.rootDir)

        val parser = com.github.javaparser.JavaParser()
        val units = listOf(registrationSrc, entitySrc, hoverPhysicsSrc).map { parser.parse(it).result.orElseThrow() }
        val sources = JavaSourceLoader.ResolvedModSources(
            mod = mod,
            units = units,
            typeSolver = com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver(),
        )

        val unt = Untranslatable()
        EntityAnalyzer(target, unt).analyze(mod, sources, outDir)

        val behaviorPath = outDir.resolve("themod/behavior_pack/entities/landspeeder.json")
        assertTrue(behaviorPath.toFile().exists(), "landspeeder behavior JSON must be written")

        val parsed = Json.parseToJsonElement(behaviorPath.readText()) as JsonObject
        val components = parsed["minecraft:entity"]!!.jsonObject["components"]!!.jsonObject

        val rideable = components["minecraft:rideable"]!!.jsonObject
        assertEquals(2, rideable["seat_count"]!!.jsonPrimitive.intOrNull)
        assertNotNull(components["minecraft:input_ground_controlled"])
        assertEquals(40, components["minecraft:health"]!!.jsonObject["value"]!!.jsonPrimitive.intOrNull)
        assertEquals(0.35, components["minecraft:movement"]!!.jsonObject["value"]!!.jsonPrimitive.content.toDouble())
        assertNotNull(components["minecraft:collision_box"])
        assertNotNull(components["minecraft:physics"])

        // Seat offsets match LandspeederEntity#positionRider's ±0.25 lateral
        // offset (the modeled seat cubes in the bbmodel geometry).
        val seats = rideable["seats"]!!.jsonArray
        assertEquals(2, seats.size)
        val seatXs = seats.map { it.jsonObject["position"]!!.jsonArray[0].jsonPrimitive.content.toDouble() }
        assertEquals(setOf(-0.25, 0.25), seatXs.toSet())

        // Loot parity: minecraft:loot points at a single-roll single-item
        // loot table dropping the vehicle's own item (Java's destroy-drop
        // has no Bedrock equivalent otherwise).
        assertEquals(
            "loot_tables/entities/landspeeder.json",
            components["minecraft:loot"]!!.jsonObject["table"]!!.jsonPrimitive.content,
        )
        val lootPath = outDir.resolve("themod/behavior_pack/loot_tables/entities/landspeeder.json")
        assertTrue(lootPath.toFile().exists(), "landspeeder loot table must be written")
        val lootJson = Json.parseToJsonElement(lootPath.readText()) as JsonObject
        val pools = lootJson["pools"]!!.jsonArray
        assertEquals(1, pools.size)
        val entries = pools[0].jsonObject["entries"]!!.jsonArray
        assertEquals(1, entries.size)
        assertEquals("item", entries[0].jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("themod:landspeeder", entries[0].jsonObject["name"]!!.jsonPrimitive.content)

        assertNull(components["minecraft:navigation.walk"])
        assertNull(components["minecraft:jump.static"])
        assertTrue(components.keys.none { it.startsWith("minecraft:behavior.") }) {
            "vehicle must not carry walking-mob behavior components: ${components.keys}"
        }
    }

    @Test
    fun `vehicle without resolvable constants records untranslatable and omits components`(@TempDir outDir: Path) {
        // Same vehicle shape as above, minus the MAX_HULL_HEALTH / MAX_SPEED
        // constants — the folded health/movement components must be absent
        // rather than emitted with a bogus value, and the analyzer must
        // record why on Untranslatable instead of silently dropping them.
        val registrationSrc = """
            package com.example.themod;
            import net.minecraft.world.entity.MobCategory;
            class ModEntities {
                public static final Object LANDSPEEDER = ENTITY_TYPES.register("landspeeder",
                    () -> EntityType.Builder.<LandspeederEntity>of(LandspeederEntity::new, MobCategory.MISC)
                        .sized(2.0f, 0.8f)
                        .build("themod:landspeeder"));
            }
        """.trimIndent()
        val entitySrc = """
            package com.example.themod;
            import net.minecraft.world.entity.EntityType;
            import net.minecraft.world.entity.vehicle.VehicleEntity;
            import net.minecraft.world.level.Level;
            public class LandspeederEntity extends VehicleEntity {
                public LandspeederEntity(EntityType<? extends LandspeederEntity> type, Level level) { super(type, level); }
            }
        """.trimIndent()
        val mod = ModDiscovery.DiscoveredMod(modId = "themod", rootDir = outDir.resolve("themodroot"))
        java.nio.file.Files.createDirectories(mod.rootDir)

        val parser = com.github.javaparser.JavaParser()
        val units = listOf(registrationSrc, entitySrc).map { parser.parse(it).result.orElseThrow() }
        val sources = JavaSourceLoader.ResolvedModSources(
            mod = mod,
            units = units,
            typeSolver = com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver(),
        )

        val unt = Untranslatable()
        EntityAnalyzer(target, unt).analyze(mod, sources, outDir)

        val behaviorPath = outDir.resolve("themod/behavior_pack/entities/landspeeder.json")
        assertTrue(behaviorPath.toFile().exists())

        val parsed = Json.parseToJsonElement(behaviorPath.readText()) as JsonObject
        val components = parsed["minecraft:entity"]!!.jsonObject["components"]!!.jsonObject

        assertNull(components["minecraft:health"])
        assertNull(components["minecraft:movement"])

        val report = unt.renderReport("themod")
        assertTrue(report.contains("LandspeederEntity")) { report }
        assertTrue(report.contains("not statically resolvable")) { report }
    }

    @Test
    fun `saved data referencing mob records named-character singleton entry`(@TempDir outDir: Path) {
        // The six starwars named characters (Vader, Luke, Obi-Wan, Boba Fett,
        // Han, Leia) each enforce a one-per-server singleton via a
        // `*SavedData` class. Bedrock output has no equivalent — the
        // analyzer must record this honestly instead of staying silent.
        val registrationSrc = """
            package com.example.themod;
            import net.minecraft.world.entity.MobCategory;
            class ModEntities {
                public static final Object VADER = ENTITY_TYPES.register("vader",
                    () -> EntityType.Builder.<VaderEntity>of(VaderEntity::new, MobCategory.MONSTER)
                        .sized(0.6f, 1.95f)
                        .build("themod:vader"));
            }
        """.trimIndent()
        val entitySrc = """
            package com.example.themod;
            import net.minecraft.world.entity.EntityType;
            import net.minecraft.world.entity.monster.Monster;
            import net.minecraft.world.level.Level;
            import net.minecraft.world.level.ServerLevelAccessor;
            public class VaderEntity extends Monster {
                public VaderEntity(EntityType<? extends VaderEntity> type, Level level) { super(type, level); }
                @Override
                public void finalizeSpawn(ServerLevelAccessor level) {
                    VaderSavedData.get(level).claim(this);
                }
            }
        """.trimIndent()
        val mod = ModDiscovery.DiscoveredMod(modId = "themod", rootDir = outDir.resolve("themodroot"))
        java.nio.file.Files.createDirectories(mod.rootDir)

        val parser = com.github.javaparser.JavaParser()
        val units = listOf(registrationSrc, entitySrc).map { parser.parse(it).result.orElseThrow() }
        val sources = JavaSourceLoader.ResolvedModSources(
            mod = mod,
            units = units,
            typeSolver = com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver(),
        )

        val unt = Untranslatable()
        EntityAnalyzer(target, unt).analyze(mod, sources, outDir)

        val behaviorPath = outDir.resolve("themod/behavior_pack/entities/vader.json")
        assertTrue(behaviorPath.toFile().exists(), "vader behavior JSON must still be written")

        val report = unt.renderReport("themod")
        assertTrue(report.contains("Named-character singleton", ignoreCase = true)) { report }
        assertTrue(report.contains("vader") || report.contains("VaderEntity")) { report }
    }
}
