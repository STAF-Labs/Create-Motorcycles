package ru.lotuze.createmoto;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MotorcycleInputPayload(boolean forward, boolean backward, boolean left, boolean right) implements CustomPacketPayload {
    public static final Type<MotorcycleInputPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateMotorcycles.MODID, "motorcycle_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MotorcycleInputPayload> STREAM_CODEC = StreamCodec.ofMember(
            MotorcycleInputPayload::write,
            MotorcycleInputPayload::read
    );

    private static MotorcycleInputPayload read(RegistryFriendlyByteBuf buffer) {
        return new MotorcycleInputPayload(buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(this.forward);
        buffer.writeBoolean(this.backward);
        buffer.writeBoolean(this.left);
        buffer.writeBoolean(this.right);
    }

    public static void handle(MotorcycleInputPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Entity vehicle = context.player().getVehicle();
            if (vehicle instanceof MotorcycleEntity motorcycle) {
                motorcycle.setInput(payload.forward, payload.backward, payload.left, payload.right);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
