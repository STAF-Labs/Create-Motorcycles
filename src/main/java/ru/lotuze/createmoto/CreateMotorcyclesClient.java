package ru.lotuze.createmoto;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import ru.lotuze.createmoto.motorcycle.client.MotorcycleKeyMappings;
import ru.lotuze.createmoto.motorcycle.client.MotorcycleRenderer;
import ru.lotuze.createmoto.registry.ModMenus;
import ru.lotuze.createmoto.station.client.ServiceStationScreen;

@Mod(value = CreateMotorcycles.MODID, dist = Dist.CLIENT)
public class CreateMotorcyclesClient {

    public CreateMotorcyclesClient(IEventBus modEventBus, ModContainer container) {

        modEventBus.addListener(CreateMotorcyclesClient::registerKeyMappings);
        modEventBus.addListener(CreateMotorcyclesClient::registerRenderers);
        modEventBus.addListener(CreateMotorcyclesClient::registerScreen);

        NeoForge.EVENT_BUS.addListener(MotorcycleKeyMappings::onClientTick);

        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
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

    static void registerScreen(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SERVICE_STATION.get(), ServiceStationScreen::new);
    }
}
