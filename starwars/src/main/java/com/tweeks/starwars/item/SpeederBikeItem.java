package com.tweeks.starwars.item;

import com.tweeks.starwars.ModEntities;
import com.tweeks.starwars.entity.SpeederBikeEntity;
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
 * Speeder-bike placement item — mirrors {@link LandspeederItem}: raycast at
 * the ground (fluids count as a surface, so the bike sets down on water like
 * the landspeeder), spawn a {@link SpeederBikeEntity} facing the player, and
 * consume the item outside creative. Rejects a fully-submerged column so the
 * bike never spawns trapped underwater.
 */
public class SpeederBikeItem extends Item {

    public SpeederBikeItem(Properties properties) {
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
        SpeederBikeEntity bike = ModEntities.SPEEDER_BIKE.get()
            .create(sl, EntitySpawnReason.SPAWN_ITEM_USE);
        if (bike == null) {
            return InteractionResult.FAIL;
        }
        Vec3 pos = hit.getLocation();
        BlockPos hitBlockPos = BlockPos.containing(pos);
        if (!sl.getFluidState(hitBlockPos).isEmpty() && !sl.getFluidState(hitBlockPos.above()).isEmpty()) {
            return InteractionResult.FAIL;
        }
        bike.snapTo(pos.x, pos.y, pos.z, player.getYRot(), 0.0f);
        if (!sl.noCollision(bike, bike.getBoundingBox())) {
            return InteractionResult.FAIL;
        }
        sl.addFreshEntity(bike);
        sl.gameEvent(player, GameEvent.ENTITY_PLACE, BlockPos.containing(pos));
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
