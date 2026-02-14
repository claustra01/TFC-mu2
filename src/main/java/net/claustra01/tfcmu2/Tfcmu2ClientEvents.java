package net.claustra01.tfcmu2;

import net.dries007.tfc.client.extensions.FluidRendererExtension;
import net.dries007.tfc.util.Helpers;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

public final class Tfcmu2ClientEvents {
    private static final ResourceLocation MOLTEN_STILL = Helpers.identifier("block/molten_still");
    private static final ResourceLocation MOLTEN_FLOW = Helpers.identifier("block/molten_flow");

    private Tfcmu2ClientEvents() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(Tfcmu2ClientEvents::clientSetup);
        modEventBus.addListener(Tfcmu2ClientEvents::registerExtensions);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        // Ore models rely on transparent overlay textures. If rendered as solid, the transparent pixels show up as black.
        event.enqueueWork(() -> {
            final RenderType cutout = RenderType.cutoutMipped();

            // Regular ores (non-graded)
            Tfcmu2Blocks.ORES.values().forEach(oresByRock ->
                oresByRock.values().forEach(block -> ItemBlockRenderTypes.setRenderLayer(block.get(), cutout)));

            // Graded ores in TFC rocks
            Tfcmu2Blocks.GRADED_ORES.values().forEach(oresByRock ->
                oresByRock.values().forEach(oresByGrade ->
                    oresByGrade.values().forEach(block -> ItemBlockRenderTypes.setRenderLayer(block.get(), cutout))));

            // Graded ores in vanilla stones (netherrack/endstone)
            Tfcmu2Blocks.VANILLA_GRADED_ORES.values().forEach(oresByStone ->
                oresByStone.values().forEach(oresByGrade ->
                    oresByGrade.values().forEach(block -> ItemBlockRenderTypes.setRenderLayer(block.get(), cutout))));

            // Compat ores in vanilla stones (netherrack/endstone)
            Tfcmu2Blocks.COMPAT_VANILLA_ORES.values().forEach(oresByStone ->
                oresByStone.values().forEach(block -> ItemBlockRenderTypes.setRenderLayer(block.get(), cutout)));

            // Loose small ores
            Tfcmu2Blocks.SMALL_ORES.values().forEach(block -> ItemBlockRenderTypes.setRenderLayer(block.get(), cutout));

            // Loose ore pieces that have no native surface sample blocks (e.g. graphite)
            Tfcmu2Blocks.COMPAT_SMALL_ORE_PIECES.values().forEach(block -> ItemBlockRenderTypes.setRenderLayer(block.get(), cutout));
        });
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
