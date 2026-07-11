package com.tweeks.starwars.client;

import com.tweeks.starwars.network.S2CBlasterTracerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class TracerClientHandler {
    private TracerClientHandler() {}

    private static final double STEP = 0.5;

    public static void handle(S2CBlasterTracerPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var level = Minecraft.getInstance().level;
            if (level == null) return;
            int rgb = pkt.argbColor() & 0xFFFFFF;
            DustParticleOptions dust = new DustParticleOptions(rgb, 0.6f);
            Vec3 delta = pkt.end().subtract(pkt.start());
            double len = delta.length();
            if (len < 1.0e-3) return;
            Vec3 dir = delta.scale(1.0 / len);
            for (double d = 0; d <= len; d += STEP) {
                Vec3 p = pkt.start().add(dir.scale(d));
                level.addParticle(dust, p.x, p.y, p.z, 0, 0, 0);
            }
        });
    }
}
