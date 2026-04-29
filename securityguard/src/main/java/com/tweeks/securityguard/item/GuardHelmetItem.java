package com.tweeks.securityguard.item;

import com.tweeks.securityguard.Registration;
import com.tweeks.securityguard.entity.SecurityGuardEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class GuardHelmetItem extends Item {

    public GuardHelmetItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos top = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;

        if (!SpawnPattern.matches(
                pos -> level.getBlockState(pos).is(Blocks.IRON_BLOCK),
                pos -> level.getBlockState(pos).isAir(),
                top)) {
            return InteractionResult.PASS;
        }

        for (int dy = 0; dy >= -2; dy--) {
            BlockPos pos = top.offset(0, dy, 0);
            if (!level.mayInteract(player, pos)) {
                return InteractionResult.FAIL;
            }
        }

        if (level instanceof ServerLevel serverLevel) {
            for (int dy = 0; dy >= -2; dy--) {
                serverLevel.setBlockAndUpdate(top.offset(0, dy, 0), Blocks.AIR.defaultBlockState());
            }

            BlockPos spawnAt = top.below(2);
            SecurityGuardEntity guard = Registration.SECURITY_GUARD.get().create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
            if (guard != null) {
                guard.snapTo(
                    spawnAt.getX() + 0.5,
                    spawnAt.getY(),
                    spawnAt.getZ() + 0.5,
                    player.getYRot() + 180.0f,
                    0.0f);
                guard.setPlayerCreated(true);
                guard.finalizeSpawn(serverLevel,
                    serverLevel.getCurrentDifficultyAt(spawnAt),
                    EntitySpawnReason.MOB_SUMMONED, null);
                serverLevel.addFreshEntity(guard);

                serverLevel.playSound(null, spawnAt,
                    SoundEvents.IRON_GOLEM_REPAIR, SoundSource.BLOCKS, 1.0f, 1.0f);
            }

            ItemStack stack = ctx.getItemInHand();
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResult.SUCCESS;
    }
}
