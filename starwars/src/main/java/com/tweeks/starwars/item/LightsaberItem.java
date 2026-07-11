package com.tweeks.starwars.item;

import com.tweeks.starwars.ModSounds;
import com.tweeks.starwars.StarWarsMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

/**
 * Melee weapon wielded by saber-using {@code SwMob}s and equippable by players.
 *
 * <p>Attribute wiring mirrors {@code wildwest.item.RapierItem} — an
 * {@link ItemAttributeModifiers} component composed onto a plain {@link Item}
 * rather than a vanilla tier-based {@code SwordItem} subclass, matching this
 * codebase's canonical melee-weapon construction style. Attack speed bonus
 * matches the rapier's (-1.6, giving 2.4 attacks/sec); damage is raised to
 * {@link #SABER_DAMAGE}.</p>
 */
public class LightsaberItem extends Item {

    public static final float SABER_DAMAGE = 9.0F;

    /** Adds to player base 4.0 attack speed → total 2.4 attacks/sec (matches RapierItem). */
    private static final double ATTACK_SPEED_BONUS = -1.6;

    private static final Identifier ATTACK_DAMAGE_ID =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "lightsaber_damage");
    private static final Identifier ATTACK_SPEED_ID =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "lightsaber_speed");

    public LightsaberItem(Properties properties) {
        super(properties
            .stacksTo(1)
            .durability(1500)
            .component(DataComponents.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.builder()
                    .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(ATTACK_DAMAGE_ID, SABER_DAMAGE - 1.0,
                            AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                    .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(ATTACK_SPEED_ID, ATTACK_SPEED_BONUS,
                            AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                    .build()));
    }

    public static SaberColor colorOf(ItemStack stack) {
        return SaberColor.byIndex(stack.getOrDefault(
            ModDataComponents.BLADE_COLOR.get(), 0));
    }

    public static ItemStack stackWithColor(SaberColor color) {
        ItemStack stack = new ItemStack(com.tweeks.starwars.Registration.LIGHTSABER.get());
        stack.set(ModDataComponents.BLADE_COLOR.get(), color.ordinal());
        return stack;
    }

    /** Ignition flourish: plays {@link ModSounds#SABER_IGNITE} on right-click, on a 1s cooldown. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && !player.getCooldowns().isOnCooldown(stack)) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SABER_IGNITE.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
            player.getCooldowns().addCooldown(stack, 20);
        }
        return InteractionResult.CONSUME;
    }

    /** Clash sound at the target's position on every successful hit. */
    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        Level level = attacker.level();
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
            ModSounds.SABER_CLASH.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }
}
