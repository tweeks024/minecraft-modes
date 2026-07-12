package com.tweeks.starwars.item;

import com.tweeks.starwars.ModEntities;
import com.tweeks.starwars.entity.LandspeederEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Boat-style placement: raycast at the ground, spawn a {@link LandspeederEntity}
 * facing the player's yaw, consume the item (not in creative). Placement flow
 * mirrors decompiled {@code net.minecraft.world.item.BoatItem#use} /
 * {@code #getBoat}: {@code Item.getPlayerPOVHitResult} (static, returns
 * {@code BlockHitResult}), {@code EntityType#create(Level, EntitySpawnReason)},
 * {@code CollisionGetter#noCollision(Entity, AABB)}, and
 * {@code ItemStack#consume(int, LivingEntity)} all confirmed to exist with
 * these exact signatures against the decompiled sources. Uses
 * {@link ClipContext.Fluid#ANY} (matching {@code BoatItem}'s raycast) so the
 * speeder places on the water surface instead of raycasting through to the
 * lakebed.
 */
public class LandspeederItem extends Item {

    public LandspeederItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel sl)) {
            return InteractionResult.SUCCESS;
        }
        LandspeederEntity speeder = ModEntities.LANDSPEEDER.get()
            .create(sl, EntitySpawnReason.SPAWN_ITEM_USE);
        if (speeder == null) {
            return InteractionResult.FAIL;
        }
        Vec3 pos = hit.getLocation();
        speeder.snapTo(pos.x, pos.y, pos.z, player.getYRot(), 0.0f);
        if (!sl.noCollision(speeder, speeder.getBoundingBox())) {
            return InteractionResult.FAIL;
        }
        sl.addFreshEntity(speeder);
        sl.gameEvent(player, GameEvent.ENTITY_PLACE, BlockPos.containing(pos));
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
