package net.claustra01.tfcmu2;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public enum Tfcmu2VanillaStone {
    NETHERRACK("netherrack", Blocks.NETHERRACK),
    ENDSTONE("endstone", Blocks.END_STONE);

    private final String serializedName;
    private final Block baseBlock;

    Tfcmu2VanillaStone(String serializedName, Block baseBlock) {
        this.serializedName = serializedName;
        this.baseBlock = baseBlock;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public Block baseBlock() {
        return baseBlock;
    }
}

