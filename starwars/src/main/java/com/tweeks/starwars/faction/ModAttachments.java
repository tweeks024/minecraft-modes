package com.tweeks.starwars.faction;

import com.tweeks.starwars.StarWarsMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    private ModAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, StarWarsMod.MOD_ID);

    // Null default + shouldSerialize-rejects-null: without the predicate,
    // getData() auto-populates a null record and the next world save NPEs
    // inside RecordCodecBuilder. Read paths use hasData() first.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<AlignmentAttachment>> ALIGNMENT =
        ATTACHMENTS.register("alignment",
            () -> AttachmentType.<AlignmentAttachment>builder(() -> null)
                .serialize(AlignmentAttachment.CODEC, attachment -> attachment != null)
                .copyOnDeath()
                .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PacifyAttachment>> PACIFIED =
        ATTACHMENTS.register("pacified",
            () -> AttachmentType.<PacifyAttachment>builder(() -> null)
                .serialize(PacifyAttachment.CODEC, attachment -> attachment != null)
                .build());

    /** Which planets a player has set foot on — drives the galaxy map. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<com.tweeks.starwars.world.planet.VisitedPlanets>> VISITED_PLANETS =
        ATTACHMENTS.register("visited_planets",
            () -> AttachmentType.<com.tweeks.starwars.world.planet.VisitedPlanets>builder(() -> null)
                .serialize(com.tweeks.starwars.world.planet.VisitedPlanets.CODEC, attachment -> attachment != null)
                .copyOnDeath()
                .build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
