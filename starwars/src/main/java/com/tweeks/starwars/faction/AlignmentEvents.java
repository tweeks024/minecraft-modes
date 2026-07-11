package com.tweeks.starwars.faction;

import com.tweeks.starwars.StarWarsMod;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = StarWarsMod.MOD_ID)
public final class AlignmentEvents {
    private AlignmentEvents() {}

    public static int getScore(Player player) {
        if (!player.hasData(ModAttachments.ALIGNMENT.get())) return 0;
        AlignmentAttachment a = player.getData(ModAttachments.ALIGNMENT.get());
        return a == null ? 0 : a.score();
    }

    public static void adjustScore(Player player, int delta) {
        if (delta == 0) return;
        int next = Alignment.clamp(getScore(player) + delta);
        player.setData(ModAttachments.ALIGNMENT.get(), new AlignmentAttachment(next));
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof SwCombatant combatant)) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        // A lethal hit fires both this event and LivingDeathEvent; skip the
        // hit delta here so a kill scores exactly KILL_DELTA, not KILL+HIT.
        if (event.getEntity().isDeadOrDying()) return;
        adjustScore(player, Alignment.deltaForHit(combatant.getFaction()));
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof SwCombatant combatant)) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        adjustScore(player, Alignment.deltaForKill(combatant.getFaction()));
    }
}
