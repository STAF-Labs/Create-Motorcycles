package ru.lotuze.createmoto;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(CreateMotorcycles.MODID)
public class CreateMotorcycles {
    public static final String MODID = "create_motorcycles";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<MotorcycleEntity>> MOTORCYCLE = ENTITY_TYPES.register("motorcycle",
            () -> EntityType.Builder.<MotorcycleEntity>of(MotorcycleEntity::new, MobCategory.MISC)
                    .sized(2.6F, 1.35F)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build("motorcycle"));

    public CreateMotorcycles(IEventBus modEventBus, ModContainer modContainer) {
        ENTITY_TYPES.register(modEventBus);
    }
}
