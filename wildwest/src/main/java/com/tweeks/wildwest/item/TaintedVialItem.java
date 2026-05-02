package com.tweeks.wildwest.item;

import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.entity.projectile.TaintedVialEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TaintedVialItem extends Item {
    public TaintedVialItem(Properties props) {
        super(props.stacksTo(16));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5F, 0.4F);

        if (level instanceof ServerLevel sl) {
            TaintedVialEntity proj = new TaintedVialEntity(
                ModEntities.TAINTED_VIAL_PROJECTILE.get(), level);
            proj.setOwner(player);
            proj.setItem(stack.copyWithCount(1));
            proj.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            proj.shootFromRotation(player, player.getXRot(), player.getYRot(),
                -20.0F, 0.5F, 1.0F);
            sl.addFreshEntity(proj);
        }

        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }
}
