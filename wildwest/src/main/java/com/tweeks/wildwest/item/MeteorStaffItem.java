package com.tweeks.wildwest.item;

import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.entity.projectile.MeteorEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Player-fired meteor weapon dropped by Herobrine. Right-click fires a
 * {@link MeteorEntity} along the player's look direction with a 3-second
 * cooldown and 10-heart direct-hit damage. Unbreakable, stack size 1, EPIC.
 */
public class MeteorStaffItem extends Item {

    public static final int COOLDOWN_TICKS = 60; // 3 s
    public static final int DIRECT_HIT_DAMAGE = 20; // 10 hearts
    public static final double FIRE_VELOCITY = 1.5;

    public MeteorStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level instanceof ServerLevel sl) {
            MeteorEntity meteor = ModEntities.METEOR.get().create(sl,
                EntitySpawnReason.MOB_SUMMONED);
            if (meteor != null) {
                Vec3 eye = player.getEyePosition();
                meteor.setPos(eye.x, eye.y, eye.z);
                Vec3 look = player.getLookAngle().scale(FIRE_VELOCITY);
                meteor.setDeltaMovement(look);
                meteor.setOwner(player);
                meteor.setDirectHitDamage(DIRECT_HIT_DAMAGE);
                sl.addFreshEntity(meteor);

                sl.playSound(null, eye.x, eye.y, eye.z,
                    SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        }

        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        player.swing(hand);

        return InteractionResult.CONSUME;
    }
}
