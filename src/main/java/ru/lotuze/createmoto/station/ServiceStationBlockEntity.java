package ru.lotuze.createmoto.station;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ru.lotuze.createmoto.registry.ModBlockEntities;

public class ServiceStationBlockEntity extends BlockEntity {

    public ServiceStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SERVICE_STATION.get(), pos, state);
    }
}