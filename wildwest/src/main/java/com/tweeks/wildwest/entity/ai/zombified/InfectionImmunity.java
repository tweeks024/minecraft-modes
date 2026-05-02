package com.tweeks.wildwest.entity.ai.zombified;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;

public final class InfectionImmunity {
    private InfectionImmunity() {}

    /** Test seam: a {@code Subject} is the bits-of-state we care about, no Minecraft mocks needed. */
    public interface Subject {
        boolean isUndead();
        boolean isBoss();
        boolean isWalker();
        boolean isCreativeOrSpectatorPlayer();
        boolean isMobOrPlayer();
    }

    public static boolean isImmune(Subject s) {
        if (!s.isMobOrPlayer()) return true;
        if (s.isUndead()) return true;
        if (s.isBoss()) return true;
        if (s.isWalker()) return true;
        if (s.isCreativeOrSpectatorPlayer()) return true;
        return false;
    }

    /** Adapter for live LivingEntity → Subject. */
    public static boolean isImmune(LivingEntity e) {
        return isImmune(new Subject() {
            @Override public boolean isUndead() {
                return e.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD);
            }
            @Override public boolean isBoss() {
                // EntityTypeTags.BOSS not available in this MC version; class-check the two known bosses.
                return e instanceof EnderDragon || e instanceof WitherBoss;
            }
            @Override public boolean isWalker() {
                // String compare to avoid forward dependency on WalkerEntity (created in Task 10).
                return e.getClass().getSimpleName().equals("WalkerEntity");
            }
            @Override public boolean isCreativeOrSpectatorPlayer() {
                return e instanceof Player p && (p.isCreative() || p.isSpectator());
            }
            @Override public boolean isMobOrPlayer() {
                return e instanceof Mob || e instanceof Player;
            }
        });
    }
}
