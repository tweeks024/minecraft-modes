package com.tweeks.starwars.faction;

import com.tweeks.starwars.Registration;
import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.entity.ai.QuickdrawState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "Scoundrel's Luck" full-set bonus for the Han Solo armor: the wearer's
 * first blaster shot against each newly acquired target deals double damage
 * (the player-side twin of HanQuickdrawGoal). Per-player ambush memory is a
 * transient server-side map reusing {@link QuickdrawState} — deliberately
 * NOT persisted: restart/logout clears it. Entries are removed on logout;
 * access only from the server thread (item use + the logout event).
 */
@EventBusSubscriber(modid = StarWarsMod.MOD_ID)
public final class ScoundrelLuck {
    private ScoundrelLuck() {}

    private static final Map<UUID, QuickdrawState> STATES = new HashMap<>();

    public static boolean isWearingFullHanSoloSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(Registration.HAN_SOLO_HELMET.get())
            && entity.getItemBySlot(EquipmentSlot.CHEST).is(Registration.HAN_SOLO_CHESTPLATE.get())
            && entity.getItemBySlot(EquipmentSlot.LEGS).is(Registration.HAN_SOLO_LEGGINGS.get())
            && entity.getItemBySlot(EquipmentSlot.FEET).is(Registration.HAN_SOLO_BOOTS.get());
    }

    public static QuickdrawState stateFor(UUID playerId) {
        return STATES.computeIfAbsent(playerId, id -> new QuickdrawState());
    }

    public static void clear(UUID playerId) {
        STATES.remove(playerId);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        clear(event.getEntity().getUUID());
    }
}
