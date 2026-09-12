package ru.lotuze.createmoto.station;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;

public class ServiceStationBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public static final EnumProperty<ServiceStationPart> PART = EnumProperty.create("part", ServiceStationPart.class);

    private static final VoxelShape BASE_SHAPE =
            Block.box(
                    0.0D, 0.0D, 0.0D,
                    16.0D, 3.0D, 16.0D
            );

    public ServiceStationBlock() {
        super(
                BlockBehaviour.Properties.of()
                        .strength(3.0F, 6.0F)
                        .noOcclusion()
        );

        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(PART, ServiceStationPart.MASTER)
        );
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(PART) != ServiceStationPart.MASTER) {
            return null;
        }

        return new ServiceStationBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return BASE_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return BASE_SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        BlockPos masterPos = getMasterPos(pos, state);

        BlockState masterState = level.getBlockState(masterPos);

        if (!masterState.is(this)
                || masterState.getValue(PART) != ServiceStationPart.MASTER) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            handleMasterInteraction(
                    level,
                    masterPos,
                    masterState,
                    player
            );
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING, PART);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();

        BlockPos masterPos = context.getClickedPos();
        BlockPos frontPos = getFrontPos(masterPos, facing);
        BlockPos backPos = getBackPos(masterPos, facing);

        if (!context.getLevel().getBlockState(frontPos).canBeReplaced(context)
                || !context.getLevel().getBlockState(backPos).canBeReplaced(context)) {

            if (!context.getLevel().isClientSide) {
                Player player = context.getPlayer();

                if (player != null) {
                    player.displayClientMessage(
                            Component.translatable(
                                    "message.create_motorcycles.service_station.no_space"
                            ), true
                    );
                }
            }

            return null;
        }

        return this.defaultBlockState().setValue(FACING, facing).setValue(PART, ServiceStationPart.MASTER);
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            LivingEntity placer,
            ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.isClientSide) { return; }
        if (state.getValue(PART) != ServiceStationPart.MASTER) { return; }

        Direction facing = state.getValue(FACING);

        BlockPos frontPos = getFrontPos(pos, facing);
        BlockPos backPos = getBackPos(pos, facing);

        level.setBlock(frontPos, state.setValue(PART, ServiceStationPart.FRONT), Block.UPDATE_ALL);
        level.setBlock(backPos, state.setValue(PART, ServiceStationPart.BACK), Block.UPDATE_ALL);
    }

    @Override
    public BlockState playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            net.minecraft.world.entity.player.Player player
    ) {
        if (!level.isClientSide) {
            destroyWholeStation(level, pos, state);
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    private void destroyWholeStation(
            Level level,
            BlockPos brokenPos,
            BlockState state
    ) {
        BlockPos masterPos = getMasterPos(brokenPos, state);

        BlockState masterState = level.getBlockState(masterPos);

        if (!masterState.is(this)) { return; }

        Direction facing = masterState.getValue(FACING);

        BlockPos frontPos = getFrontPos(masterPos, facing);
        BlockPos backPos = getBackPos(masterPos, facing);

        removePartIfNotBroken(level, frontPos, brokenPos);
        removePartIfNotBroken(level, masterPos, brokenPos);
        removePartIfNotBroken(level, backPos, brokenPos);
    }

    private void removePartIfNotBroken(
            Level level,
            BlockPos pos,
            BlockPos brokenPos
    ) {
        if (pos.equals(brokenPos)) {
            return;
        }

        BlockState state = level.getBlockState(pos);

        if (state.is(this)) {
            level.removeBlock(pos, false);
        }
    }

    public static BlockPos getMasterPos(
            BlockPos pos,
            BlockState state
    ) {
        Direction facing = state.getValue(FACING);

        return switch (state.getValue(PART)) {
            case MASTER -> pos;
            case FRONT -> pos.relative(facing.getOpposite());
            case BACK -> pos.relative(facing);
        };
    }

    private static BlockPos getFrontPos(
            BlockPos masterPos,
            Direction facing
    ) {
        return masterPos.relative(facing);
    }

    private static BlockPos getBackPos(
            BlockPos masterPos,
            Direction facing
    ) {
        return masterPos.relative(facing.getOpposite());
    }

    private void handleMasterInteraction(
            Level level,
            BlockPos masterPos,
            BlockState masterState,
            Player player
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(masterPos);

        if (!(blockEntity instanceof ServiceStationBlockEntity station)) {
            return;
        }

        MotorcycleDetectionResult detection = station.detectMotorcycle();

        ServiceStationMenuData data = ServiceStationMenuData.from(detection, station.hasLockedMotorcycle());

        serverPlayer.openMenu(
                new SimpleMenuProvider(
                        (containerId, inventory, menuPlayer) ->
                                new ServiceStationMenu(
                                        containerId,
                                        inventory,
                                        masterPos,
                                        data
                                ),
                        Component.translatable(
                                "menu.create_motorcycles.service_station"
                        )
                ),
                buffer -> {
                    buffer.writeBlockPos(masterPos);
                    buffer.writeEnum(data.status());
                    buffer.writeVarInt(data.count());
                    buffer.writeVarInt(data.entityId());
                    buffer.writeBoolean(data.entityUuid() != null);

                    if (data.entityUuid() != null) {

                        buffer.writeUUID(data.entityUuid());

                    }

                    buffer.writeDouble(data.x());
                    buffer.writeDouble(data.y());
                    buffer.writeDouble(data.z());

                    buffer.writeBoolean(data.locked());
                }
        );
    }
}
