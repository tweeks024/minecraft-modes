package com.tweeks.starwars.entity;

import com.tweeks.starwars.faction.SwFaction;
import com.tweeks.starwars.item.LightsaberItem;
import com.tweeks.starwars.item.SaberColor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class JediKnightEntity extends SwMob {

    public static final double MAX_HEALTH = 30.0;
    public static final double ATTACK_DAMAGE = 7.0;
    public static final double MOVEMENT_SPEED = 0.32;

    public JediKnightEntity(EntityType<? extends JediKnightEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HEALTH)
            .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
            .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    public SwFaction getFaction() { return SwFaction.LIGHT; }

    @Override
    public boolean usesBlaster() { return false; }

    @Override
    public boolean usesRifleBlaster() { return false; }

    @Override
    protected ItemStack getWeaponStack() {
        // Half the knights ignite blue, half green.
        return LightsaberItem.stackWithColor(
            this.getRandom().nextBoolean() ? SaberColor.BLUE : SaberColor.GREEN);
    }
}
