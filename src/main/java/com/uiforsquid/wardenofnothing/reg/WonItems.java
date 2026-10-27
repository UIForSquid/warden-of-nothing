package com.uiforsquid.wardenofnothing.reg;

import com.uiforsquid.wardenofnothing.WardenOfNothing;
import com.uiforsquid.wardenofnothing.heart.HeartItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item registry (F5). */
public final class WonItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WardenOfNothing.MOD_ID);

    /** The Heart item: never stacks, fire-resistant (full indestructibility is in ItemEntityMixin). */
    public static final DeferredItem<HeartItem> HEART = ITEMS.register("heart_of_the_warden",
            () -> new HeartItem(WonBlocks.HEART.get(), new Item.Properties().stacksTo(1).fireResistant()));

    private WonItems() {
    }
}
