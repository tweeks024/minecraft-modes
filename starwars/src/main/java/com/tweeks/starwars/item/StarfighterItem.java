package com.tweeks.starwars.item;

import com.tweeks.starwars.entity.StarfighterEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Shared placement item for the starfighters. Mirrors {@link LandspeederItem}
 * but raycasts against solid ground only ({@link ClipContext.Fluid#NONE} — a
 * fighter parks on land, it does not settle onto a water surface like the
 * hover craft) and requires a 3-wide clearance so the broad airframe is not
 * spawned clipping into terrain. Concrete subclasses only supply the entity
 * to create.
 */
public abstract class StarfighterItem extends Item {

    /** Half-width of the clearance box required around the spawn point. */
    private static final double CLEARANCE = 1.5;

    protected StarfighterItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** Create the airframe entity (uninitialized position) or {@code null}. */
    @Nullable
    protected abstract StarfighterEntity createFighter(ServerLevel level);

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel sl)) {
            return InteractionResult.SUCCESS;
        }
        StarfighterEntity fighter = this.createFighter(sl);
        if (fighter == null) {
            return InteractionResult.FAIL;
        }
        Vec3 pos = hit.getLocation();
        fighter.snapTo(pos.x, pos.y, pos.z, player.getYRot(), 0.0f);
        // Require a 3-wide (2*CLEARANCE) clear footprint around the spawn so
        // the wide fuselage/wings do not intersect nearby blocks.
        AABB footprint = new AABB(pos.x, pos.y, pos.z, pos.x, pos.y, pos.z)
            .inflate(CLEARANCE, 0.5, CLEARANCE);
        if (!sl.noCollision(fighter, fighter.getBoundingBox()) || !sl.noCollision(fighter, footprint)) {
            return InteractionResult.FAIL;
        }
        sl.addFreshEntity(fighter);
        sl.gameEvent(player, GameEvent.ENTITY_PLACE, BlockPos.containing(pos));
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
