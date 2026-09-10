package ru.lotuze.createmoto.registry;

import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.lotuze.createmoto.CreateMotorcycles;
import ru.lotuze.createmoto.station.ServiceStationBlock;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CreateMotorcycles.MODID);

    public static final DeferredBlock<ServiceStationBlock> SERVICE_STATION = BLOCKS.register(
            "service_station",
            ServiceStationBlock::new
    );

    private ModBlocks() {}
}
