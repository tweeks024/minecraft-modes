package com.tweeks.wildwest.entity.projectile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure AoE victim-selection logic for the cannonball projectile. Given an
 * impact center, an AoE radius, a list of candidate entities (with positions),
 * and an optional directly-hit entity id to exclude, returns the set of ids
 * that should take AoE damage.
 *
 * <p>The real CannonballEntity resolves Level.getEntitiesOfClass(...) into
 * Candidate records before delegating here — this keeps Minecraft types out
 * of the test surface.
 */
public final class CannonballImpactLogic {
    private CannonballImpactLogic() {}

    public record Candidate(String id, double x, double y, double z) {}

    public static Set<String> victims(double cx, double cy, double cz,
                                      double radius,
                                      List<Candidate> candidates,
                                      String directHitId) {
        Set<String> out = new HashSet<>();
        for (Candidate c : candidates) {
            if (c.id.equals(directHitId)) continue;
            double dx = c.x - cx;
            double dy = c.y - cy;
            double dz = c.z - cz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist <= radius) {
                out.add(c.id);
            }
        }
        return out;
    }
}
