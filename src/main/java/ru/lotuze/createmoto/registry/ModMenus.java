package ru.lotuze.createmoto.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.lotuze.createmoto.CreateMotorcycles;
import ru.lotuze.createmoto.station.ServiceStationMenu;

public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(
            Registries.MENU,
            CreateMotorcycles.MODID
    );

    public static final DeferredHolder<
            MenuType<?>,
            MenuType<ServiceStationMenu>
            > SERVICE_STATION =
            MENUS.register(
                    "service_station",
                    () -> IMenuTypeExtension.create(
                            ServiceStationMenu::new
                    )
            );

    private ModMenus() {}
}