package com.tweeks.wildwest.item;

import com.tweeks.wildwest.WildWestMod;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Reaper Scythe — Grim Reaper's signature drop. Dual-purpose:
 *
 * <ul>
 *   <li>Left-click melee: 6 attack damage (3 hearts), ~1.0 attack speed.</li>
 *   <li>Right-click cast: summons one bone servant (cap {@value #MAX_MINIONS} per owner,
 *       {@value #COOLDOWN_TICKS}t cooldown).</li>
 * </ul>
 *
 * <p>Stack 1, unbreakable, EPIC rarity. Right-click summon implementation
 * lives in {@link #use(Level, Player, InteractionHand)} — fully populated
 * once {@link com.tweeks.wildwest.entity.ScytheSkeletonEntity} is wired up
 * in Task 18.
 */
public class ReaperScytheItem extends Item {

    public static final int COOLDOWN_TICKS = 100; // 5 s
    public static final int MAX_MINIONS = 3;
    public static final double SUMMON_RANGE = 4.0;
    public static final double ATTACK_DAMAGE = 6.0;
    public static final double ATTACK_SPEED = -2.4; // ~1.0 atk/s

    private static final Identifier ATTACK_DAMAGE_ID =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "reaper_scythe_damage");
    private static final Identifier ATTACK_SPEED_ID =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "reaper_scythe_speed");

    public ReaperScytheItem(Properties properties) {
        super(properties
            .stacksTo(1)
            .rarity(Rarity.EPIC)
            .component(DataComponents.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.builder()
                    .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(ATTACK_DAMAGE_ID, ATTACK_DAMAGE,
                            AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                    .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(ATTACK_SPEED_ID, ATTACK_SPEED,
                            AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                    .build()));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) {
            return InteractionResult.PASS;
        }

        int alive = com.tweeks.wildwest.entity.ScytheSkeletonEntity.countMinionsOwnedBy(sl, player.getUUID());
        if (alive >= MAX_MINIONS) {
            player.sendOverlayMessage(
                net.minecraft.network.chat.Component.translatable("item.wildwest.reaper_scythe.cap_reached"));
            return InteractionResult.FAIL;
        }

        var eye = player.getEyePosition();
        var look = player.getLookAngle();
        double sx = eye.x + look.x * SUMMON_RANGE;
        double sy = eye.y + look.y * SUMMON_RANGE;
        double sz = eye.z + look.z * SUMMON_RANGE;
        net.minecraft.core.BlockPos.MutableBlockPos cursor =
            new net.minecraft.core.BlockPos.MutableBlockPos(
                (int) Math.floor(sx), (int) Math.floor(sy), (int) Math.floor(sz));
        for (int dy = 0; dy < 4; dy++) {
            net.minecraft.core.BlockPos below = cursor.below();
            if (!sl.getBlockState(below).isAir()) {
                break;
            }
            cursor.setY(below.getY());
        }
        double spawnY = cursor.getY();

        com.tweeks.wildwest.entity.ScytheSkeletonEntity minion =
            com.tweeks.wildwest.ModEntities.SCYTHE_SKELETON.get()
                .create(sl, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (minion == null) {
            return InteractionResult.FAIL;
        }
        minion.setPos(sx, spawnY, sz);
        minion.setOwnerUUID(player.getUUID());
        minion.finalizeSpawn(sl, sl.getCurrentDifficultyAt(minion.blockPosition()),
            net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, null);
        sl.addFreshEntity(minion);

        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,
            sx, spawnY + 0.5, sz, 15, 0.3, 0.5, 0.3, 0.05);
        sl.playSound(null, sx, spawnY, sz,
            net.minecraft.sounds.SoundEvents.ZOMBIE_VILLAGER_CONVERTED,
            net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.2f);

        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        player.swing(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                Item.TooltipContext context,
                                TooltipDisplay display,
                                Consumer<Component> adder,
                                TooltipFlag flag) {
        adder.accept(Component.translatable("item.wildwest.reaper_scythe.tooltip.melee")
            .withStyle(ChatFormatting.GRAY));
        adder.accept(Component.translatable("item.wildwest.reaper_scythe.tooltip.summon")
            .withStyle(ChatFormatting.GRAY));
        adder.accept(Component.translatable("item.wildwest.reaper_scythe.tooltip.servant")
            .withStyle(ChatFormatting.DARK_GRAY));
    }
}
