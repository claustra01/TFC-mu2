package net.claustra01.tfcmu2;

import net.dries007.tfc.common.LevelTier;
import net.dries007.tfc.util.Metal;
import net.dries007.tfc.util.registry.RegistryMetal;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;

public enum Tfcmu2Metal implements RegistryMetal {
    COMPRESSED_IRON("compressed_iron", Rarity.UNCOMMON),
    PLATINUM("platinum", Rarity.RARE),
    IRIDIUM("iridium", Rarity.UNCOMMON),
    OSMIUM("osmium", Rarity.UNCOMMON),
    OSMIRIDIUM("osmiridium", Rarity.UNCOMMON),
    MYTHRIL("mythril", Rarity.COMMON),
    ANTIMONY("antimony", Rarity.COMMON),
    TITANIUM("titanium", Rarity.UNCOMMON),
    TUNGSTEN("tungsten", Rarity.EPIC),
    SOLDER("solder", Rarity.UNCOMMON),
    TUNGSTEN_STEEL("tungsten_steel", Rarity.EPIC);

    private final String serializedName;
    private final Rarity rarity;

    Tfcmu2Metal(String serializedName, Rarity rarity) {
        this.serializedName = serializedName;
        this.rarity = rarity;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    @Override
    public LevelTier toolTier() {
        throw unsupported("toolTier");
    }

    @Override
    public Holder<ArmorMaterial> armorMaterial() {
        throw unsupported("armorMaterial");
    }

    @Override
    public int armorDurability(ArmorItem.Type type) {
        throw unsupported("armorDurability");
    }

    @Override
    public Block getBlock(Metal.BlockType type) {
        if (type == Metal.BlockType.BLOCK) {
            return Tfcmu2Blocks.METAL_BLOCKS.get(this).get();
        }
        throw unsupported("getBlock(" + type.name() + ")");
    }

    @Override
    public MapColor mapColor() {
        return MapColor.METAL;
    }

    @Override
    public Rarity rarity() {
        return rarity;
    }

    @Override
    public float weatheringResistance() {
        return -1f;
    }

    private UnsupportedOperationException unsupported(String method) {
        return new UnsupportedOperationException(method + " is not used by the implemented metal subset: " + serializedName);
    }
}
