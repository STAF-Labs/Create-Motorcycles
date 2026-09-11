package ru.lotuze.createmoto;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import ru.lotuze.createmoto.motorcycle.MotorcycleEntity;
import ru.lotuze.createmoto.motorcycle.MotorcycleInputPayload;
import ru.lotuze.createmoto.registry.ModBlockEntities;
import ru.lotuze.createmoto.registry.ModBlocks;
import ru.lotuze.createmoto.registry.ModItems;
import ru.lotuze.createmoto.registry.ModMenus;

@Mod(CreateMotorcycles.MODID)
public class CreateMotorcycles {

    public static final String MODID = "create_motorcycles";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<MotorcycleEntity>> MOTORCYCLE =
            ENTITY_TYPES.register(
                    "motorcycle",
                    () -> EntityType.Builder.<MotorcycleEntity>of(MotorcycleEntity::new, MobCategory.MISC)
                        .sized(0.85F, 1.25F)
                        .clientTrackingRange(10)
                        .updateInterval(1)
                        .build("motorcycle"));

    public CreateMotorcycles(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::registerPayloadHandlers);

        ENTITY_TYPES.register(modEventBus);

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
    }

    private void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        event.registrar(MODID)
                .playToServer(MotorcycleInputPayload.TYPE, MotorcycleInputPayload.STREAM_CODEC, MotorcycleInputPayload::handle);
    }
}
