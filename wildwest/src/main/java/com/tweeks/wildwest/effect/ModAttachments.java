package com.tweeks.wildwest.effect;

import com.tweeks.wildwest.WildWestMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    private ModAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, WildWestMod.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<MindCharmAttachment>> MIND_CHARM =
        ATTACHMENTS.register("mind_charm",
            () -> AttachmentType.<MindCharmAttachment>builder(() -> null)
                .serialize(MindCharmAttachment.CODEC)
                .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<RealityBubbleAttachment>> REALITY_BUBBLE =
        ATTACHMENTS.register("reality_bubble",
            () -> AttachmentType.<RealityBubbleAttachment>builder(() -> null)
                .serialize(RealityBubbleAttachment.CODEC)
                .build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
