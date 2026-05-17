package com.tweeks.wildwest.event;

import com.tweeks.wildwest.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.List;
import java.util.function.Predicate;

/**
 * Passive trigger for the Void Mark drop from
 * {@link com.tweeks.wildwest.entity.NullEntity}. On a fatal
 * {@link LivingDamageEvent.Pre}, consumes the lowest-slot Void Mark in the
 * player's main inventory and teleports them to their respawn point at 1 HP.
 *
 * <p>The {@link #findFirstMatchingSlot} helper is broken out as a pure function
 * for unit testing — it takes a predicate so tests can pass a sentinel object
 * comparator without having to instantiate vanilla {@link ItemStack}s (which
 * would require a full Minecraft bootstrap that's not viable in plain JUnit).
 *
 * <p>Registered explicitly in {@code WildWestMod}'s constructor against the
 * game event bus (rather than via {@code @EventBusSubscriber}) so the
 * registration is colocated with the other one-off subscriptions there.
 *
 * <p>API notes (NeoForge 26.1.2.30-beta):
 * <ul>
 *   <li>{@code LivingDamageEvent.Pre#getNewDamage()} / {@code #setNewDamage(float)}
 *       — post-armor damage value; setting to 0 fully cancels.</li>
 *   <li>{@code Inventory#getNonEquipmentItems()} is the public accessor for
 *       the 36-slot main inventory (the {@code items} field is private).</li>
 *   <li>{@code ServerPlayer#getRespawnConfig()} returns a nullable
 *       {@code RespawnConfig} record; null means "no bed/anchor set, use
 *       the overworld shared spawn".</li>
 *   <li>{@code ServerPlayer#teleportTo(double, double, double)} for same-dim;
 *       the 8-arg {@code teleportTo(ServerLevel, x, y, z, Set<Relative>, yaw,
 *       pitch, resetCamera)} for cross-dim.</li>
 * </ul>
 */
public final class VoidMarkHandler {

    private VoidMarkHandler() {}

    /**
     * Returns the lowest-index slot in {@code items} matching {@code predicate},
     * or -1 if none match. Empty stacks are skipped — callers do not need to
     * check {@code isEmpty()} in their predicate.
     */
    public static <T> int findFirstMatchingSlot(List<T> items, Predicate<T> predicate) {
        for (int i = 0; i < items.size(); i++) {
            T s = items.get(i);
            if (s != null && predicate.test(s)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Convenience overload for the runtime call site: finds the lowest-index
     * slot whose stack is a non-empty instance of {@code voidMarkItem}.
     */
    public static int findFirstVoidMarkSlot(NonNullList<ItemStack> items, Item voidMarkItem) {
        return findFirstMatchingSlot(items, s -> !s.isEmpty() && s.is(voidMarkItem));
    }

    /**
     * On a fatal damage event against a Player, consume one Void Mark from the
     * main inventory, cancel the damage, set HP to 1, play SFX + particles at
     * source and destination, teleport to the respawn point (or world spawn),
     * and show an actionbar message.
     *
     * <p>Creative-mode kills are skipped — creative players don't take fatal
     * damage anyway, but the early-return keeps the logic explicit.
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;
        if (!(player.level() instanceof ServerLevel sourceLevel)) return;
        if (player.getAbilities().instabuild) return;

        float incoming = event.getNewDamage();
        if (player.getHealth() - incoming > 0.0f) return; // not lethal

        NonNullList<ItemStack> main = player.getInventory().getNonEquipmentItems();
        Item voidMark = Registration.VOID_MARK.get();
        int slot = findFirstVoidMarkSlot(main, voidMark);
        if (slot < 0) return; // no Void Mark — player dies normally

        // Consume the trigger and cancel the lethal damage.
        main.get(slot).shrink(1);
        event.setNewDamage(0.0f);
        player.setHealth(1.0f);

        // Source-side feedback: portal swirl + totem-use whoosh at the death spot.
        double sx = player.getX();
        double sy = player.getY() + 1.0;
        double sz = player.getZ();
        sourceLevel.sendParticles(ParticleTypes.PORTAL, sx, sy, sz, 48, 0.5, 1.0, 0.5, 0.2);
        sourceLevel.playSound(null, sx, sy, sz,
            SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);

        // Resolve respawn destination. Cross-dim teleport requires the 8-arg
        // overload; same-dim uses the 3-arg shortcut (which routes through
        // the ServerGamePacketListener, properly resetting falling etc.).
        if (!(player instanceof ServerPlayer sp)) return;

        ServerPlayer.RespawnConfig respawn = sp.getRespawnConfig();
        ServerLevel destLevel;
        double dx, dy, dz;
        float yaw, pitch;

        if (respawn != null) {
            LevelData.RespawnData rd = respawn.respawnData();
            ResourceKey<Level> destDimKey = rd.dimension();
            ServerLevel resolved = sp.level().getServer().getLevel(destDimKey);
            // If the saved dimension no longer exists, fall back to overworld spawn.
            if (resolved != null) {
                destLevel = resolved;
                BlockPos pos = rd.pos();
                dx = pos.getX() + 0.5;
                dy = pos.getY();
                dz = pos.getZ() + 0.5;
                yaw = rd.yaw();
                pitch = rd.pitch();
            } else {
                destLevel = sp.level().getServer().overworld();
                LevelData.RespawnData worldSpawn = destLevel.getLevelData().getRespawnData();
                BlockPos spawn = worldSpawn.pos();
                dx = spawn.getX() + 0.5;
                dy = spawn.getY();
                dz = spawn.getZ() + 0.5;
                yaw = worldSpawn.yaw();
                pitch = worldSpawn.pitch();
            }
        } else {
            destLevel = sp.level().getServer().overworld();
            LevelData.RespawnData worldSpawn = destLevel.getLevelData().getRespawnData();
            BlockPos spawn = worldSpawn.pos();
            dx = spawn.getX() + 0.5;
            dy = spawn.getY();
            dz = spawn.getZ() + 0.5;
            yaw = worldSpawn.yaw();
            pitch = worldSpawn.pitch();
        }

        if (destLevel == sourceLevel) {
            sp.teleportTo(dx, dy, dz);
        } else {
            sp.teleportTo(destLevel, dx, dy, dz, java.util.Set.of(), yaw, pitch, true);
        }

        // Destination-side feedback. Use the destination level so the particles /
        // sound spawn in whichever dimension the player landed in.
        destLevel.sendParticles(ParticleTypes.PORTAL, dx, dy + 1.0, dz, 48, 0.5, 1.0, 0.5, 0.2);
        destLevel.playSound(null, dx, dy, dz,
            SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5f, 1.0f);

        // overlay=true → actionbar message (vs chat).
        sp.sendSystemMessage(
            Component.translatable("item.wildwest.void_mark.triggered"), true);
    }
}
