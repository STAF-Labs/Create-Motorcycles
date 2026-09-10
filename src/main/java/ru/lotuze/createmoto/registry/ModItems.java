package ru.lotuze.createmoto.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.lotuze.createmoto.CreateMotorcycles;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister
            .createItems(CreateMotorcycles.MODID);

    public static final DeferredItem<BlockItem> SERVICE_STATION = ITEMS.register(
            "service_station",
            () -> new BlockItem(
                    ModBlocks.SERVICE_STATION.get(),
                    new Item.Properties()
            )
    );

    private ModItems() {}
}
