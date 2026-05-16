package com.tweeks.wildwest.item;

import com.tweeks.wildwest.WildWestMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Pirate melee weapon. Iron-tier feel, faster attack speed than a vanilla iron
 * sword (2.4 vs 1.6 per second) at 5 attack damage. Standard melee interactions —
 * no bespoke right-click behavior.
 *
 * <p>Mirrors the attribute-builder pattern used by {@link BillyClubItem} and
 * {@link BanditKnifeItem} rather than extending {@code SwordItem}, because the
 * canonical melee-weapon shape in this codebase composes {@code Item} with an
 * {@link ItemAttributeModifiers} component instead of subclassing the vanilla
 * tier-based sword. This keeps the construction style consistent across all
 * Wild West hand weapons.</p>
 *
 * <p>Attribute math (player base ATTACK_DAMAGE = 1.0, ATTACK_SPEED = 4.0):
 * <ul>
 *   <li>+4.0 ADD_VALUE to ATTACK_DAMAGE → total 5.0 damage</li>
 *   <li>-1.6 ADD_VALUE to ATTACK_SPEED → total 2.4 attacks/sec</li>
 * </ul></p>
 */
public class RapierItem extends Item {

    private static final Identifier ATTACK_DAMAGE_ID =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "rapier_damage");
    private static final Identifier ATTACK_SPEED_ID =
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "rapier_speed");

    /** Adds to player base 1.0 → total 5.0 attack damage. */
    private static final double ATTACK_DAMAGE_BONUS = 4.0;

    /** Adds to player base 4.0 → total 2.4 attack speed (one above iron's 1.6). */
    private static final double ATTACK_SPEED_BONUS = -1.6;

    public RapierItem(Properties properties) {
        super(properties
            .stacksTo(1)
            .durability(250)
            .component(DataComponents.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.builder()
                    .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(ATTACK_DAMAGE_ID, ATTACK_DAMAGE_BONUS,
                            AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                    .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(ATTACK_SPEED_ID, ATTACK_SPEED_BONUS,
                            AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                    .build()));
    }
}
