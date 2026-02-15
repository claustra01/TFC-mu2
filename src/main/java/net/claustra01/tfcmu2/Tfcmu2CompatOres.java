package net.claustra01.tfcmu2;

import java.util.ArrayList;
import java.util.List;

import net.neoforged.fml.ModList;

public final class Tfcmu2CompatOres {
    public static final List<String> TFC_ORES = List.of(
        "amethyst",
        "borax",
        "cinnabar",
        "cryolite",
        "diamond",
        "emerald",
        "fluorite",
        "graphite",
        "gypsum",
        "lapis_lazuli",
        "normal_bismuthinite",
        "normal_cassiterite",
        "normal_garnierite",
        "normal_hematite",
        "normal_limonite",
        "normal_magnetite",
        "normal_malachite",
        "normal_native_copper",
        "normal_native_gold",
        "normal_native_silver",
        "normal_sphalerite",
        "normal_tetrahedrite",
        "opal",
        "poor_bismuthinite",
        "poor_cassiterite",
        "poor_garnierite",
        "poor_hematite",
        "poor_limonite",
        "poor_magnetite",
        "poor_malachite",
        "poor_native_copper",
        "poor_native_gold",
        "poor_native_silver",
        "poor_sphalerite",
        "poor_tetrahedrite",
        "pyrite",
        "rich_bismuthinite",
        "rich_cassiterite",
        "rich_garnierite",
        "rich_hematite",
        "rich_limonite",
        "rich_magnetite",
        "rich_malachite",
        "rich_native_copper",
        "rich_native_gold",
        "rich_native_silver",
        "rich_sphalerite",
        "rich_tetrahedrite",
        "ruby",
        "saltpeter",
        "sapphire",
        "sulfur",
        "sylvite",
        "topaz"
    );

    public static final List<String> FIRMALIFE_ORES = List.of(
        "normal_chromite",
        "poor_chromite",
        "rich_chromite"
    );

    public static final List<String> TFC_IE_ADDON_ORES = List.of(
        "normal_bauxite",
        "normal_galena",
        "normal_uraninite",
        "poor_bauxite",
        "poor_galena",
        "poor_uraninite",
        "rich_bauxite",
        "rich_galena",
        "rich_uraninite"
    );

    private Tfcmu2CompatOres() {
    }

    public static List<String> getLoadedOreNames() {
        final ArrayList<String> ores = new ArrayList<>(TFC_ORES);
        if (ModList.get().isLoaded(Tfcmu2Mod.FIRMALIFE_MOD_ID)) {
            ores.addAll(FIRMALIFE_ORES);
        }
        if (ModList.get().isLoaded(Tfcmu2Mod.TFC_IE_ADDON_MOD_ID)) {
            ores.addAll(TFC_IE_ADDON_ORES);
        }
        return ores;
    }
}
