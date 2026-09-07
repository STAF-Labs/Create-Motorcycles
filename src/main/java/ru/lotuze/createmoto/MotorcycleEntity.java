package ru.lotuze.createmoto;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class MotorcycleEntity extends Entity {
    private static final double RIDER_FORWARD_OFFSET = -0.52D;
    private static final double RIDER_RIGHT_OFFSET = 0.0D;
    private static final double RIDER_UP_OFFSET = 0.12D;

    public MotorcycleEntity(EntityType<? extends MotorcycleEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        this.applyGravity();
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(this.onGround() ? 0.5D : 0.98D));
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return tryMount(player, hand);
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
        return tryMount(player, hand);
    }

    private InteractionResult tryMount(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (!this.level().isClientSide && !player.isPassenger() && this.getPassengers().isEmpty()) {
            this.setYRot(player.getYRot());
            player.setYRot(this.getYRot());
            player.startRiding(this);
        }

        player.swing(hand);
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        Entity passenger = this.getFirstPassenger();
        return passenger instanceof LivingEntity livingEntity ? livingEntity : null;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        if (this.hasPassenger(passenger)) {
            Vec3 offset = riderOffset(this.getYRot());
            moveFunction.accept(passenger, this.getX() + offset.x, this.getY() + offset.y, this.getZ() + offset.z);
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 offset = rotateOffset(new Vec3(0.0D, 0.0D, 1.05D), this.getYRot());
        return this.position().add(offset.x, 0.1D, offset.z);
    }

    private static Vec3 riderOffset(float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        Vec3 right = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));
        return forward.scale(RIDER_FORWARD_OFFSET)
                .add(right.scale(RIDER_RIGHT_OFFSET))
                .add(0.0D, RIDER_UP_OFFSET, 0.0D);
    }

    private static Vec3 rotateOffset(Vec3 localOffset, float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double x = localOffset.z * cos - localOffset.x * sin;
        double z = localOffset.x * cos + localOffset.z * sin;
        return new Vec3(x, localOffset.y, z);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
    }
}
