package com.uiforsquid.wardenofnothing;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

/** Tag keys the mod defines. */
public final class WardenTags {

    /**
     * Structures that count as Ancient Cities for the mod's city features (interim rule Q13).
     * Defaults to just {@code minecraft:ancient_city}; datapacks can add modded city structures.
     */
    public static final TagKey<Structure> BOSS_CITIES =
            TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(WardenOfNothing.MOD_ID, "boss_cities"));

    private WardenTags() {
    }
}
