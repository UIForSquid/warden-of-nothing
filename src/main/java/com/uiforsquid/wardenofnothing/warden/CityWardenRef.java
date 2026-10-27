package com.uiforsquid.wardenofnothing.warden;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Attached to a Warden entity to record that it is a City Warden, and which city + slot it belongs to.
 * The attachment's presence (see {@code hasData}) is what marks an entity as a City Warden; its fields
 * say which slot's state it mirrors. This is the authoritative record, not the {@code city_warden}
 * entity tag (which is only a label for selectors).
 */
public record CityWardenRef(long cityId, int slotIndex) {

    /** Sentinel used only as the attachment default; real City Wardens always have an explicit value. */
    public static final CityWardenRef NONE = new CityWardenRef(Long.MIN_VALUE, -1);

    public static final Codec<CityWardenRef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("cityId").forGetter(CityWardenRef::cityId),
            Codec.INT.fieldOf("slotIndex").forGetter(CityWardenRef::slotIndex)
    ).apply(instance, CityWardenRef::new));
}
