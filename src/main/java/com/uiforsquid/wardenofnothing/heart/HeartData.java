package com.uiforsquid.wardenofnothing.heart;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * The hidden state a Heart item carries between being mined and placed again (F5): its stage and its
 * trigger count within that stage (e.g. 7 of 10 toward Faint). Stored as a data component, not shown
 * in the tooltip.
 */
public record HeartData(HeartStage stage, int progress) {

    public static final HeartData DEAD = new HeartData(HeartStage.DEAD, 0);

    public static final Codec<HeartData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StringRepresentable.fromEnum(HeartStage::values).fieldOf("stage").forGetter(HeartData::stage),
            Codec.INT.fieldOf("progress").forGetter(HeartData::progress)
    ).apply(instance, HeartData::new));

    public static final StreamCodec<ByteBuf, HeartData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> HeartStage.values()[i], HeartStage::ordinal), HeartData::stage,
            ByteBufCodecs.VAR_INT, HeartData::progress,
            HeartData::new);
}
