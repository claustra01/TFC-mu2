package net.claustra01.tfcmu2;

import java.util.Comparator;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Tfcmu2CreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Tfcmu2Mod.MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register("main",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tfcmu2"))
            .icon(() -> Tfcmu2Items.METAL_INGOTS.get(Tfcmu2Metal.TUNGSTEN_STEEL).get().getDefaultInstance())
            .displayItems((parameters, output) -> Tfcmu2Items.ITEMS.getEntries().stream()
                .sorted(Comparator.comparing(item -> item.getId().toString()))
                .map(item -> item.get())
                .forEach(output::accept))
            .build());

    private Tfcmu2CreativeTabs() {
    }
}
