package com.uiforsquid.wardenofnothing.mixin;

import com.uiforsquid.wardenofnothing.WardenTags;
import com.uiforsquid.wardenofnothing.city.CityTracker;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records a city as it generates. F2: when an Ancient City (a structure in the
 * {@code wardenofnothing:boss_cities} tag) places part of itself into a chunk, we ensure the city is
 * tracked (bounds + Warden_1) and record the natural shriekers placed in that chunk.
 *
 * <p>{@code placeInChunk} only runs at world generation, never on reload &mdash; this is exactly the
 * "new cities only" signal (old cities load without generating). No NeoForge event covers structure
 * generation, so a mixin is used.
 */
@Mixin(StructureStart.class)
public abstract class StructureStartMixin {

    @Shadow
    @Final
    private Structure structure;

    @Shadow
    public abstract BoundingBox getBoundingBox();

    @Shadow
    public abstract ChunkPos getChunkPos();

    @Inject(method = "placeInChunk", at = @At("TAIL"))
    private void wardenofnothing$recordCity(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunkPos,
            CallbackInfo ci) {

        ServerLevel serverLevel = level.getLevel();
        Registry<Structure> registry = serverLevel.registryAccess().registryOrThrow(Registries.STRUCTURE);
        if (!registry.wrapAsHolder(this.structure).is(WardenTags.BOSS_CITIES)) {
            return;
        }

        BoundingBox cityBounds = this.getBoundingBox();
        long cityId = this.getChunkPos().toLong();

        // Scan the chunk-limited placement box (clamped to the city's vertical extent) for the
        // shriekers this chunk placed. Reading the WorldGenLevel here is safe on the generation thread.
        LongArrayList found = new LongArrayList();
        int minY = Math.max(box.minY(), cityBounds.minY());
        int maxY = Math.min(box.maxY(), cityBounds.maxY());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        if (box.minX() >= chunkPos.getMinBlockX() && box.maxX() <= chunkPos.getMaxBlockX()
                && box.minZ() >= chunkPos.getMinBlockZ() && box.maxZ() <= chunkPos.getMaxBlockZ()) {
            // Usual case, the box is inside one chunk: read its sections directly, skipping every
            // section whose palette has no shrieker (most of them).
            ChunkAccess chunk = level.getChunk(chunkPos.x, chunkPos.z);
            for (int sectionY = SectionPos.blockToSectionCoord(minY); sectionY <= SectionPos.blockToSectionCoord(maxY); sectionY++) {
                LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sectionY));
                if (section.hasOnlyAir() || !section.maybeHas(state -> state.is(Blocks.SCULK_SHRIEKER))) {
                    continue;
                }
                int fromY = Math.max(minY, SectionPos.sectionToBlockCoord(sectionY));
                int toY = Math.min(maxY, SectionPos.sectionToBlockCoord(sectionY) + 15);
                for (int x = box.minX(); x <= box.maxX(); x++) {
                    for (int z = box.minZ(); z <= box.maxZ(); z++) {
                        for (int y = fromY; y <= toY; y++) {
                            if (section.getBlockState(x & 15, y & 15, z & 15).is(Blocks.SCULK_SHRIEKER)) {
                                found.add(cursor.set(x, y, z).asLong());
                            }
                        }
                    }
                }
            }
        } else {
            for (int x = box.minX(); x <= box.maxX(); x++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    for (int y = minY; y <= maxY; y++) {
                        cursor.set(x, y, z);
                        if (level.getBlockState(cursor).is(Blocks.SCULK_SHRIEKER)) {
                            found.add(cursor.asLong());
                        }
                    }
                }
            }
        }

        // Mutate saved data on the main server thread, not the worldgen worker thread.
        MinecraftServer server = serverLevel.getServer();
        if (server != null) {
            server.execute(() -> CityTracker.recordChunk(serverLevel, cityId, cityBounds, found));
        }
    }
}
