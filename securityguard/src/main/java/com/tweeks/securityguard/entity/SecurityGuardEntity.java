package com.tweeks.securityguard.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.Level;

/**
 * The Security Guard entity. Subclasses {@link IronGolem} for its target-selection,
 * persistence, friendly-mob defaults, and iron-ingot repair behavior. Combat goals
 * are swapped in Task 11.
 */
public class SecurityGuardEntity extends IronGolem {

    public SecurityGuardEntity(EntityType<? extends SecurityGuardEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return IronGolem.createAttributes()
            .add(Attributes.MAX_HEALTH, 50.0)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.MOVEMENT_SPEED, 0.32)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }
}
