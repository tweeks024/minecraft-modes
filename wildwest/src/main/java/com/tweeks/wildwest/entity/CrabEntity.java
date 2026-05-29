package com.tweeks.wildwest.entity;

import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CrabEntity extends Animal implements NeutralMob {

    private static final UniformInt PERSISTENT_ANGER_TIME =
        TimeUtil.rangeOfSeconds(CrabEntityConstants.ANGER_SECONDS_MIN, CrabEntityConstants.ANGER_SECONDS_MAX);

    private int remainingPersistentAngerTime;
    private @Nullable UUID persistentAngerTarget;

    public CrabEntity(EntityType<? extends CrabEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
            .add(Attributes.MAX_HEALTH, CrabEntityConstants.MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, CrabEntityConstants.MOVEMENT_SPEED)
            .add(Attributes.ATTACK_DAMAGE, CrabEntityConstants.ATTACK_DAMAGE)
            .add(Attributes.FOLLOW_RANGE, CrabEntityConstants.FOLLOW_RANGE);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.SEAGRASS);
    }

    @Override
    public @Nullable CrabEntity getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return com.tweeks.wildwest.ModEntities.CRAB.get().create(level);
    }

    // NeutralMob impl
    @Override public int getRemainingPersistentAngerTime() { return remainingPersistentAngerTime; }
    @Override public void setRemainingPersistentAngerTime(int t) { this.remainingPersistentAngerTime = t; }
    @Override public @Nullable UUID getPersistentAngerTarget() { return persistentAngerTarget; }
    @Override public void setPersistentAngerTarget(@Nullable UUID id) { this.persistentAngerTarget = id; }
    @Override public void startPersistentAngerTimer() {
        setRemainingPersistentAngerTime(PERSISTENT_ANGER_TIME.sample(random));
    }
}
