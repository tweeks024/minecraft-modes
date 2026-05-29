package com.tweeks.wildwest.entity;

import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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

public class CrabEntity extends Animal implements NeutralMob {

    private static final UniformInt PERSISTENT_ANGER_TIME =
        TimeUtil.rangeOfSeconds(CrabEntityConstants.ANGER_SECONDS_MIN, CrabEntityConstants.ANGER_SECONDS_MAX);

    private long persistentAngerEndTime = NeutralMob.NO_ANGER_END_TIME;
    private @Nullable EntityReference<LivingEntity> persistentAngerTarget;

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
        return com.tweeks.wildwest.ModEntities.CRAB.get().create(level, EntitySpawnReason.BREEDING);
    }

    // NeutralMob impl
    @Override public long getPersistentAngerEndTime() { return persistentAngerEndTime; }
    @Override public void setPersistentAngerEndTime(long endTime) { this.persistentAngerEndTime = endTime; }
    @Override public @Nullable EntityReference<LivingEntity> getPersistentAngerTarget() { return persistentAngerTarget; }
    @Override public void setPersistentAngerTarget(@Nullable EntityReference<LivingEntity> target) { this.persistentAngerTarget = target; }
    @Override public void startPersistentAngerTimer() {
        setTimeToRemainAngry(PERSISTENT_ANGER_TIME.sample(random));
    }
}
