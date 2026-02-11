package net.claustra01.tfcmu2;

import java.util.Locale;

import net.dries007.tfc.util.registry.RegistryRock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public enum Tfcmu2Ore {
    NATIVE_PLATINUM(true, "platinum"),
    NATIVE_IRIDIUM(true, "iridium"),
    NATIVE_OSMIUM(true, "osmium"),
    RUTILE(true, "titanium"),
    STIBNITE(true, "antimony"),
    WOLFRAMITE(true, "tungsten");

    public static final Tfcmu2Ore[] VALUES = values();

    private final String serializedName;
    private final boolean graded;
    private final String metalTagName;

    Tfcmu2Ore(boolean graded, String metalTagName) {
        this.serializedName = name().toLowerCase(Locale.ROOT);
        this.graded = graded;
        this.metalTagName = metalTagName;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public boolean isGraded() {
        return graded;
    }

    public String metalTagName() {
        return metalTagName;
    }

    public Block create(RegistryRock rock) {
        return new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .sound(SoundType.STONE)
            .strength(rock.category().hardness(6.5F), 10.0F)
            .requiresCorrectToolForDrops());
    }
}
