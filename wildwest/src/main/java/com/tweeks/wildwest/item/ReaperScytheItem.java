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
            // Client returns SUCCESS so the swing/animation fires; server
            // does the real work below.
            return InteractionResult.SUCCESS;
        }

        // Full summon implementation arrives in Task 18 once
        // ScytheSkeletonEntity supports owner UUID + spawn helper.
        // For now: stamp the cooldown so we can verify plumbing.
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
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
