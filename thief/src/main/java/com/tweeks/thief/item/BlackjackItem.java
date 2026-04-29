package com.tweeks.thief.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.resources.Identifier;

/**
 * Small leather sap. 2 raw damage; on hit, applies Slowness II for 2 seconds.
 * Carried by Thieves and dropped at 25% on death.
 */
public class BlackjackItem extends Item {

    private static final Identifier ATTACK_DAMAGE_ID =
        Identifier.fromNamespaceAndPath("thief", "blackjack_damage");

    public BlackjackItem(Properties properties) {
        super(properties
            .stacksTo(1)
            .component(DataComponents.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.builder()
                    .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(ATTACK_DAMAGE_ID, 2.0,
                            AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                    .build()));
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
        super.hurtEnemy(stack, target, attacker);
    }
}
