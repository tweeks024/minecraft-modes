package com.tweeks.starwars.item;

import com.tweeks.starwars.ModEntities;
import com.tweeks.starwars.entity.StarfighterEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import org.jspecify.annotations.Nullable;

/** Places a {@link com.tweeks.starwars.entity.TieFighterEntity} on solid ground. */
public class TieFighterItem extends StarfighterItem {

    public TieFighterItem(Properties properties) {
        super(properties);
    }

    @Override
    @Nullable
    protected StarfighterEntity createFighter(ServerLevel level) {
        return ModEntities.TIE_FIGHTER.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
    }
}
