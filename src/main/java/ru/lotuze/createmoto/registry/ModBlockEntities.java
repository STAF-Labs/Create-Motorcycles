package ru.lotuze.createmoto.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.lotuze.createmoto.CreateMotorcycles;
import ru.lotuze.createmoto.station.ServiceStationBlockEntity;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateMotorcycles.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ServiceStationBlockEntity>>
            SERVICE_STATION = BLOCK_ENTITIES.register(
            "service_station",
            () -> BlockEntityType.Builder.of(
                    ServiceStationBlockEntity::new,
                    ModBlocks.SERVICE_STATION.get()
            ).build(null)
    );

    private ModBlockEntities() {
    }
}