package com.tweeks.starwars.item;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
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
 * Deploys a vehicle where the player is looking, matching
 * {@link LandspeederItem}'s raycast-and-place flow but parameterized by
 * entity type so the speeder bike and both starfighters share one item.
 * The vehicle only spawns if it has room to fit (no collision), so a
 * fighter needs open ground.
 */
public class VehicleDeployItem extends Item {

    private final Supplier<? extends EntityType<? extends Entity>> type;

    public VehicleDeployItem(Properties properties, Supplier<? extends EntityType<? extends Entity>> type) {
        super(properties.stacksTo(1));
        this.type = type;
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
        Entity vehicle = type.get().create(sl, EntitySpawnReason.SPAWN_ITEM_USE);
        if (vehicle == null) {
            return InteractionResult.FAIL;
        }
        Vec3 pos = hit.getLocation();
        BlockPos hitBlockPos = BlockPos.containing(pos);
        // Same submerged-column guard as the landspeeder — never strand a
        // hovering vehicle at the bottom of a lake.
        if (!sl.getFluidState(hitBlockPos).isEmpty() && !sl.getFluidState(hitBlockPos.above()).isEmpty()) {
            return InteractionResult.FAIL;
        }
        vehicle.snapTo(pos.x, pos.y, pos.z, player.getYRot(), 0.0f);
        if (!sl.noCollision(vehicle, vehicle.getBoundingBox())) {
            return InteractionResult.FAIL;
        }
        sl.addFreshEntity(vehicle);
        sl.gameEvent(player, GameEvent.ENTITY_PLACE, BlockPos.containing(pos));
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
