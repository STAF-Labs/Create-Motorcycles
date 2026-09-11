package ru.lotuze.createmoto.station;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import ru.lotuze.createmoto.registry.ModBlocks;
import ru.lotuze.createmoto.registry.ModMenus;

import java.util.UUID;

public class ServiceStationMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess access;
    private final BlockPos stationPos;

    private final MotorcycleDetectionResult.Status status;
    private final int motorcycleCount;

    private final int entityId;
    private final UUID entityUuid;

    private final double entityX;
    private final double entityY;
    private final double entityZ;

    public ServiceStationMenu(
            int containerId,
            Inventory inventory,
            RegistryFriendlyByteBuf buffer
    ) {
        super(ModMenus.SERVICE_STATION.get(), containerId);

        this.stationPos = buffer.readBlockPos();

        this.status = buffer.readEnum(
                MotorcycleDetectionResult.Status.class
        );

        this.motorcycleCount = buffer.readVarInt();
        this.entityId = buffer.readVarInt();

        boolean hasUuid = buffer.readBoolean();

        this.entityUuid = hasUuid
                ? buffer.readUUID()
                : null;

        this.entityX = buffer.readDouble();
        this.entityY = buffer.readDouble();
        this.entityZ = buffer.readDouble();

        this.access = ContainerLevelAccess.NULL;
    }

    public ServiceStationMenu(
            int containerId,
            Inventory inventory,
            BlockPos stationPos,
            ServiceStationMenuData data
    ) {
        super(ModMenus.SERVICE_STATION.get(), containerId);

        this.stationPos = stationPos;

        this.status = data.status();
        this.motorcycleCount = data.count();

        this.entityId = data.entityId();
        this.entityUuid = data.entityUuid();

        this.entityX = data.x();
        this.entityY = data.y();
        this.entityZ = data.z();

        this.access = ContainerLevelAccess.create(
                inventory.player.level(),
                stationPos
        );
    }

    public MotorcycleDetectionResult.Status getStatus() {
        return status;
    }

    public int getMotorcycleCount() {
        return motorcycleCount;
    }

    public int getEntityId() {
        return entityId;
    }

    public UUID getEntityUuid() {
        return entityUuid;
    }

    public double getEntityX() {
        return entityX;
    }

    public double getEntityY() {
        return entityY;
    }

    public double getEntityZ() {
        return entityZ;
    }

    public BlockPos getStationPos() {
        return stationPos;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(
                access,
                player,
                ModBlocks.SERVICE_STATION.get()
        );
    }

    @Override
    public ItemStack quickMoveStack(
            Player player,
            int index
    ) {
        return ItemStack.EMPTY;
    }
}