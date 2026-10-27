package com.uiforsquid.wardenofnothing;

import com.mojang.logging.LogUtils;
import com.uiforsquid.wardenofnothing.config.Config;
import com.uiforsquid.wardenofnothing.heart.HeartData;
import com.uiforsquid.wardenofnothing.heart.HeartStage;
import com.uiforsquid.wardenofnothing.reg.WonBlockEntities;
import com.uiforsquid.wardenofnothing.reg.WonBlocks;
import com.uiforsquid.wardenofnothing.reg.WonComponents;
import com.uiforsquid.wardenofnothing.reg.WonItems;
import com.uiforsquid.wardenofnothing.warden.AttributeCaps;
import com.uiforsquid.wardenofnothing.warden.WonAttachments;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

/**
 * Entry point for Warden of Nothing.
 *
 * <p>The mod turns the vanilla Warden into a boss that belongs to its Ancient City. Feature
 * registration is wired up here as each feature is built (config, blocks/items, entity attribute
 * changes, event handlers and saved data).
 */
@Mod(WardenOfNothing.MOD_ID)
public final class WardenOfNothing {

    public static final String MOD_ID = "wardenofnothing";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WardenOfNothing(IEventBus modBus, ModContainer modContainer) {
        LOGGER.info("Warden of Nothing loading");
        // Interim rule Q12: a NeoForge SERVER config written to each world's serverconfig folder.
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC, "wardenofnothing-server.toml");

        WonAttachments.ATTACHMENT_TYPES.register(modBus);
        WonComponents.COMPONENTS.register(modBus);
        WonBlocks.BLOCKS.register(modBus);
        WonItems.ITEMS.register(modBus);
        WonBlockEntities.BLOCK_ENTITIES.register(modBus);
        modBus.addListener(WardenOfNothing::addCreativeTabItems);
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(AttributeCaps::raise));
    }

    /** The Heart in each of its three stages, under Functional Blocks. */
    private static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            return;
        }
        for (HeartStage stage : HeartStage.values()) {
            ItemStack stack = new ItemStack(WonItems.HEART.get());
            stack.set(WonComponents.HEART_DATA.get(), new HeartData(stage, 0));
            event.accept(stack);
        }
    }
}
