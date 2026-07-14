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
 * Darth Maul's double-bladed lightsaber. Hits harder and faster than a single
 * saber (two blades, no defensive flourish), and its wide arc sweeps adjacent
 * foes — the vanilla sweep attack is enabled by the small negative attack-speed
 * penalty being lighter than a single saber's. Always the Sith red; the blade
 * colour is not configurable.
 */
public class SaberstaffItem extends Item {

    public static final float STAFF_DAMAGE = 11.0F;
    /** Lighter penalty than the single saber (-1.6) — the staff is quicker. */
    private static final double ATTACK_SPEED_BONUS = -1.4;

    private static final Identifier ATTACK_DAMAGE_ID =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "saberstaff_damage");
    private static final Identifier ATTACK_SPEED_ID =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "saberstaff_speed");

    public SaberstaffItem(Properties properties) {
        super(properties
            .stacksTo(1)
            .durability(2000)
            .component(DataComponents.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.builder()
                    .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(ATTACK_DAMAGE_ID, STAFF_DAMAGE - 1.0,
                            AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                    .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(ATTACK_SPEED_ID, ATTACK_SPEED_BONUS,
                            AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                    .build()));
    }

    /** Ignition flourish: both blades snap-hiss on right-click, on a 1s cooldown. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && !player.getCooldowns().isOnCooldown(stack)) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SABER_IGNITE.get(), SoundSource.PLAYERS, 1.0F, 0.85F);
            player.getCooldowns().addCooldown(stack, 20);
        }
        return InteractionResult.CONSUME;
    }

    /** Clash sound at the target on every hit. */
    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        attacker.level().playSound(null, target.getX(), target.getY(), target.getZ(),
            ModSounds.SABER_CLASH.get(), SoundSource.PLAYERS, 0.9F, 0.9F);
    }
}
