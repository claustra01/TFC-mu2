package net.claustra01.tfcmu2;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(Tfcmu2Mod.MOD_ID)
public final class Tfcmu2Mod {
    public static final String MOD_ID = "tfcmu2";
    public static final String TFC_MORE_ITEMS_MOD_ID = "tfc_items";
    public static final String TFC_ORE_WASHING_MOD_ID = "tfcorewashing";

    public Tfcmu2Mod(IEventBus modEventBus) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            Tfcmu2ClientEvents.register(modEventBus);
        }
        Tfcmu2Fluids.FLUID_TYPES.register(modEventBus);
        Tfcmu2Fluids.FLUIDS.register(modEventBus);
        Tfcmu2Blocks.BLOCKS.register(modEventBus);
        Tfcmu2CreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        Tfcmu2Items.ITEMS.register(modEventBus);
    }
}
