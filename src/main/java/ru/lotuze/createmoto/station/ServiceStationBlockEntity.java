package ru.lotuze.createmoto.station;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import ru.lotuze.createmoto.motorcycle.MotorcycleEntity;
import ru.lotuze.createmoto.registry.ModBlockEntities;

import javax.annotation.Nullable;
import java.util.List;

public class ServiceStationBlockEntity extends BlockEntity {

    public ServiceStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SERVICE_STATION.get(), pos, state);
    }

    public List<MotorcycleEntity> findMotorcycles() {
        if (level == null) {
            return List.of();
        }

        BlockState state = getBlockState();
        Direction facing = state.getValue(ServiceStationBlock.FACING);

        BlockPos masterPos = getBlockPos();
        BlockPos frontPos = masterPos.relative(facing);
        BlockPos backPos = masterPos.relative(facing.getOpposite());

        int minX = Math.min(masterPos.getX(), Math.min(frontPos.getX(), backPos.getX()));
        int maxX = Math.max(masterPos.getX(), Math.max(frontPos.getX(), backPos.getX()));
        int minZ = Math.min(masterPos.getZ(), Math.min(frontPos.getZ(), backPos.getZ()));
        int maxZ = Math.max(masterPos.getZ(), Math.max(frontPos.getZ(), backPos.getZ()));

        AABB area = new AABB(
                minX,
                masterPos.getY(),
                minZ,
                maxX + 1.0D,
                masterPos.getY() + 2.0D,
                maxZ + 1.0D
        ).inflate(0.75D, 0.0D, 0.75D);

        return level.getEntitiesOfClass(
                MotorcycleEntity.class,
                area
        );
    }

    public MotorcycleDetectionResult detectMotorcycle() {
        if (level == null) {
            return MotorcycleDetectionResult.notFound();
        }

        AABB workArea = getWorkArea();

        MotorcycleEntity motorcycle = level.getEntitiesOfClass(
                MotorcycleEntity.class,
                workArea,
                Entity::isAlive
        ).stream().findFirst().orElse(null);

        return motorcycle != null
                ? MotorcycleDetectionResult.found(motorcycle)
                : MotorcycleDetectionResult.notFound();
    }

    public AABB getWorkArea() {
        Direction facing = getBlockState().getValue(ServiceStationBlock.FACING);

        BlockPos master = getBlockPos();
        BlockPos front = master.relative(facing);
        BlockPos back = master.relative(facing.getOpposite());

        AABB stationBounds = new AABB(master)
                .minmax(new AABB(front))
                .minmax(new AABB(back));

        return stationBounds.inflate(
                0.75D,
                1.0D,
                0.75D
        );
    }
}