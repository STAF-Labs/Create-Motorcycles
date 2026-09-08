package ru.lotuze.createmoto;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = CreateMotorcycles.MODID, dist = Dist.CLIENT)
public class CreateMotorcyclesClient {
    public CreateMotorcyclesClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(CreateMotorcyclesClient::onClientSetup);
        modEventBus.addListener(CreateMotorcyclesClient::registerKeyMappings);
        modEventBus.addListener(CreateMotorcyclesClient::registerRenderers);
        NeoForge.EVENT_BUS.addListener(MotorcycleKeyMappings::onClientTick);

        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    static void onClientSetup(FMLClientSetupEvent event) {
        CreateMotorcycles.LOGGER.info("Minecraft client user: {}", Minecraft.getInstance().getUser().getName());
    }

    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CreateMotorcycles.MOTORCYCLE.get(), MotorcycleRenderer::new);
    }

    static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(MotorcycleKeyMappings.FORWARD);
        event.register(MotorcycleKeyMappings.BACKWARD);
        event.register(MotorcycleKeyMappings.LEFT);
        event.register(MotorcycleKeyMappings.RIGHT);
    }
}
