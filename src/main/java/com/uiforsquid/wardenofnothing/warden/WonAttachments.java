package com.uiforsquid.wardenofnothing.warden;

import com.uiforsquid.wardenofnothing.WardenOfNothing;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Registry for the mod's entity data attachments. */
public final class WonAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, WardenOfNothing.MOD_ID);

    /**
     * Marks a Warden as a City Warden and records its city id + slot index. Persists across save/load.
     * Not copied on death: a dead City Warden's slot is handled by the mod, and a revived Warden is a
     * brand-new entity.
     */
    public static final Supplier<AttachmentType<CityWardenRef>> CITY_WARDEN = ATTACHMENT_TYPES.register(
            "city_warden",
            () -> AttachmentType.builder(() -> CityWardenRef.NONE).serialize(CityWardenRef.CODEC).build());

    private WonAttachments() {
    }
}
