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

    // Default-value supplier returns null because the attachment is "not set"
    // for the vast majority of mobs. We MUST pair that with a shouldSerialize
    // predicate that rejects null — otherwise getData() auto-populates the
    // holder with null, and the next world save crashes with NPE inside
    // RecordCodecBuilder trying to encode a null record. Read-paths in the
    // tick handler also use hasData() first to avoid the auto-populate.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<MindCharmAttachment>> MIND_CHARM =
        ATTACHMENTS.register("mind_charm",
            () -> AttachmentType.<MindCharmAttachment>builder(() -> null)
                .serialize(MindCharmAttachment.CODEC, attachment -> attachment != null)
                .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<RealityBubbleAttachment>> REALITY_BUBBLE =
        ATTACHMENTS.register("reality_bubble",
            () -> AttachmentType.<RealityBubbleAttachment>builder(() -> null)
                .serialize(RealityBubbleAttachment.CODEC, attachment -> attachment != null)
                .build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
