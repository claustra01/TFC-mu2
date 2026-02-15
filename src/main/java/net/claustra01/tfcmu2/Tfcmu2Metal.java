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
    COMPRESSED_IRON("compressed_iron", Rarity.UNCOMMON, 0x6F6C6B),
    PLATINUM("platinum", Rarity.RARE, 0x627C8B),
    NAQUADAH("naquadah", Rarity.RARE, 0x4D5742),
    IRIDIUM("iridium", Rarity.UNCOMMON, 0xADC4CE),
    OSMIUM("osmium", Rarity.UNCOMMON, 0xA5B4CA),
    OSMIRIDIUM("osmiridium", Rarity.UNCOMMON, 0x718383),
    MYTHRIL("mythril", Rarity.COMMON, 0x5383A2),
    REFINED_GLOWSTONE("refined_glowstone", Rarity.UNCOMMON, 0xE2B446),
    REFINED_OBSIDIAN("refined_obsidian", Rarity.RARE, 0x473752),
    ANTIMONY("antimony", Rarity.COMMON, 0xB4AFBD),
    TITANIUM("titanium", Rarity.UNCOMMON, 0x898B93),
    TUNGSTEN("tungsten", Rarity.EPIC, 0x585F6B),
    SOLDER("solder", Rarity.UNCOMMON, 0x888888),
    TUNGSTEN_STEEL("tungsten_steel", Rarity.EPIC, 0x2F353E),
    NETHERITE("netherite", Rarity.EPIC, 0x3B3230);

    private final String serializedName;
    private final Rarity rarity;
    private final int color;

    Tfcmu2Metal(String serializedName, Rarity rarity, int color) {
        this.serializedName = serializedName;
        this.rarity = rarity;
        this.color = color;
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
        if (type == Metal.BlockType.BLOCK_SLAB) {
            return Tfcmu2Blocks.METAL_BLOCK_SLABS.get(this).get();
        }
        if (type == Metal.BlockType.BLOCK_STAIRS) {
            return Tfcmu2Blocks.METAL_BLOCK_STAIRS.get(this).get();
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

    public int color() {
        return color;
    }

    @Override
    public float weatheringResistance() {
        return -1f;
    }

    private UnsupportedOperationException unsupported(String method) {
        return new UnsupportedOperationException(method + " is not used by the implemented metal subset: " + serializedName);
    }
}
