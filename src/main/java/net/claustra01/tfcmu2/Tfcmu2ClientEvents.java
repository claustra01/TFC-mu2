package net.claustra01.tfcmu2;

import net.dries007.tfc.client.extensions.FluidRendererExtension;
import net.dries007.tfc.util.Helpers;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

public final class Tfcmu2ClientEvents {
    private static final ResourceLocation MOLTEN_STILL = Helpers.identifier("block/molten_still");
    private static final ResourceLocation MOLTEN_FLOW = Helpers.identifier("block/molten_flow");

    private Tfcmu2ClientEvents() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(Tfcmu2ClientEvents::registerExtensions);
    }

    private static void registerExtensions(RegisterClientExtensionsEvent event) {
        for (Tfcmu2Metal metal : Tfcmu2Metal.values()) {
            event.registerFluidType(new FluidRendererExtension(
                0xFF000000 | metal.color(),
                MOLTEN_STILL,
                MOLTEN_FLOW,
                null,
                null
            ), Tfcmu2Fluids.METAL_FLUIDS.get(metal).getType());
        }
    }
}
