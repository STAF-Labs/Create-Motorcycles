package ru.lotuze.createmoto.station;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import ru.lotuze.createmoto.motorcycle.MotorcycleEntity;
import ru.lotuze.createmoto.registry.ModBlockEntities;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class ServiceStationBlockEntity extends BlockEntity {

    @Nullable
    private UUID lockedMotorcycleId;

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

    public boolean hasLockedMotorcycle() {
        return this.lockedMotorcycleId != null;
    }

    public boolean lockMotorcycle() {
        if (this.level == null || this.level.isClientSide) {
            return false;
        }

        if (this.lockedMotorcycleId != null) {
            return true;
        }

        MotorcycleDetectionResult detection = this.detectMotorcycle();

        if (detection.status() != MotorcycleDetectionResult.Status.FOUND
                || detection.motorcycle() == null) {
            return false;
        }

        MotorcycleEntity motorcycle = detection.motorcycle();

        if (motorcycle.isServiceLocked()
                && !motorcycle.isLockedToServiceStation(this.worldPosition)) {
            return false;
        }

        Direction facing = this.getBlockState().getValue(ServiceStationBlock.FACING);

        if (!motorcycle.lockToServiceStation(this.worldPosition, facing)) {
            return false;
        }

        this.lockedMotorcycleId = motorcycle.getUUID();
        this.setChanged();
        return true;
    }

    public boolean unlockMotorcycle() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return false;
        }

        if (this.lockedMotorcycleId == null) {
            return true;
        }

        Entity entity = serverLevel.getEntity(this.lockedMotorcycleId);

        if (entity instanceof MotorcycleEntity motorcycle) {
            motorcycle.unlockFromServiceStation();
        }

        this.lockedMotorcycleId = null;
        this.setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(tag, registries);

        if (this.lockedMotorcycleId != null) {
            tag.putUUID("LockedMotorcycle", this.lockedMotorcycleId);
        }
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(tag, registries);

        this.lockedMotorcycleId = tag.hasUUID("LockedMotorcycle")
                ? tag.getUUID("LockedMotorcycle")
                : null;
    }
}