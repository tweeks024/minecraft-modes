package com.tweeks.wildwest.client;

import com.tweeks.wildwest.network.S2CTracerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class TracerClientHandler {
    private TracerClientHandler() {}

    public static void handle(S2CTracerPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level == null) return;
            Vec3 start = packet.start();
            Vec3 end = packet.end();
            level.addParticle(ParticleTypes.SMOKE, start.x, start.y, start.z, 0, 0.02, 0);
            int steps = 6;
            for (int i = 1; i <= steps; i++) {
                double t = (double) i / (double) (steps + 1);
                double x = start.x + (end.x - start.x) * t;
                double y = start.y + (end.y - start.y) * t;
                double z = start.z + (end.z - start.z) * t;
                level.addParticle(ParticleTypes.CRIT, x, y, z, 0, 0, 0);
            }
        });
    }
}
