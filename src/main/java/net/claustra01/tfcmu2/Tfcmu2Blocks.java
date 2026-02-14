package net.claustra01.tfcmu2;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;

import net.dries007.tfc.common.blocks.GroundcoverBlock;
import net.dries007.tfc.common.blocks.rock.Ore;
import net.dries007.tfc.common.blocks.rock.Rock;
import net.dries007.tfc.util.Metal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Tfcmu2Blocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Tfcmu2Mod.MOD_ID);

    // TFC has "loose ore" / surface sample blocks for only a subset of ore pieces (tfc:ore/small_*).
    // For ore pieces that have no surface sample (e.g. tfc:ore/graphite), we add groundcover blocks that
    // drop the existing ore piece item on break or right-click (handled by TFC's GroundcoverBlock).
    // These are blocks only (no block items registered).
    private static final List<String> ORE_PIECES_WITHOUT_SAMPLES = List.of(
        "amethyst",
        "bituminous_coal",
        "borax",
        "cinnabar",
        "cryolite",
        "diamond",
        "emerald",
        "graphite",
        "gypsum",
        "halite",
        "lapis_lazuli",
        "lignite",
        "opal",
        "pyrite",
        "ruby",
        "saltpeter",
        "sapphire",
        "sulfur",
        "sylvite",
        "topaz"
    );

    public static final Map<Tfcmu2Metal, DeferredBlock<Block>> METAL_BLOCKS = registerMetalBlocks();
    public static final Map<Tfcmu2Metal, DeferredBlock<Block>> METAL_BLOCK_SLABS = registerMetalBlockSlabs();
    public static final Map<Tfcmu2Metal, DeferredBlock<Block>> METAL_BLOCK_STAIRS = registerMetalBlockStairs();
    public static final Map<Rock, Map<Tfcmu2Ore, DeferredBlock<Block>>> ORES = registerOres();
    public static final Map<Rock, Map<Tfcmu2Ore, Map<Ore.Grade, DeferredBlock<Block>>>> GRADED_ORES = registerGradedOres();
    public static final Map<Tfcmu2VanillaStone, Map<Tfcmu2Ore, Map<Ore.Grade, DeferredBlock<Block>>>> VANILLA_GRADED_ORES = registerVanillaGradedOres();
    public static final Map<Tfcmu2VanillaStone, Map<String, DeferredBlock<Block>>> COMPAT_VANILLA_ORES = registerCompatVanillaOres();
    public static final Map<Tfcmu2Ore, DeferredBlock<Block>> SMALL_ORES = registerSmallOres();
    public static final Map<String, DeferredBlock<Block>> COMPAT_SMALL_ORE_PIECES = registerCompatSmallOrePieces();

    private Tfcmu2Blocks() {
    }

    public static Map<Tfcmu2Metal, DeferredItem<?>> registerMetalBlockItems(DeferredRegister.Items items) {
        final EnumMap<Tfcmu2Metal, DeferredItem<?>> blockItems = new EnumMap<>(Tfcmu2Metal.class);
        for (Tfcmu2Metal metal : Tfcmu2Metal.values()) {
            final String id = "metal/block/" + metal.getSerializedName();
            blockItems.put(metal, items.registerSimpleBlockItem(id, METAL_BLOCKS.get(metal)));
        }
        return Collections.unmodifiableMap(blockItems);
    }

    public static Map<Tfcmu2Metal, DeferredItem<?>> registerMetalSlabBlockItems(DeferredRegister.Items items) {
        final EnumMap<Tfcmu2Metal, DeferredItem<?>> blockItems = new EnumMap<>(Tfcmu2Metal.class);
        for (Tfcmu2Metal metal : Tfcmu2Metal.values()) {
            final String id = "metal/block/" + metal.getSerializedName() + "_slab";
            blockItems.put(metal, items.registerSimpleBlockItem(id, METAL_BLOCK_SLABS.get(metal)));
        }
        return Collections.unmodifiableMap(blockItems);
    }

    public static Map<Tfcmu2Metal, DeferredItem<?>> registerMetalStairsBlockItems(DeferredRegister.Items items) {
        final EnumMap<Tfcmu2Metal, DeferredItem<?>> blockItems = new EnumMap<>(Tfcmu2Metal.class);
        for (Tfcmu2Metal metal : Tfcmu2Metal.values()) {
            final String id = "metal/block/" + metal.getSerializedName() + "_stairs";
            blockItems.put(metal, items.registerSimpleBlockItem(id, METAL_BLOCK_STAIRS.get(metal)));
        }
        return Collections.unmodifiableMap(blockItems);
    }

    public static Map<Rock, Map<Tfcmu2Ore, DeferredItem<?>>> registerOreBlockItems(DeferredRegister.Items items) {
        final EnumMap<Rock, Map<Tfcmu2Ore, DeferredItem<?>>> blockItems = new EnumMap<>(Rock.class);
        for (Rock rock : Rock.VALUES) {
            final EnumMap<Tfcmu2Ore, DeferredItem<?>> rockItems = new EnumMap<>(Tfcmu2Ore.class);
            for (Tfcmu2Ore ore : Tfcmu2Ore.VALUES) {
                if (ore.isGraded()) {
                    continue;
                }
                final String id = "ore/" + ore.getSerializedName() + "/" + rock.getSerializedName();
                rockItems.put(ore, items.registerSimpleBlockItem(id, ORES.get(rock).get(ore)));
            }
            blockItems.put(rock, Collections.unmodifiableMap(rockItems));
        }
        return Collections.unmodifiableMap(blockItems);
    }

    public static Map<Rock, Map<Tfcmu2Ore, Map<Ore.Grade, DeferredItem<?>>>> registerGradedOreBlockItems(DeferredRegister.Items items) {
        final EnumMap<Rock, Map<Tfcmu2Ore, Map<Ore.Grade, DeferredItem<?>>>> blockItems = new EnumMap<>(Rock.class);
        for (Rock rock : Rock.VALUES) {
            final EnumMap<Tfcmu2Ore, Map<Ore.Grade, DeferredItem<?>>> rockItems = new EnumMap<>(Tfcmu2Ore.class);
            for (Tfcmu2Ore ore : Tfcmu2Ore.VALUES) {
                if (!ore.isGraded()) {
                    continue;
                }
                final EnumMap<Ore.Grade, DeferredItem<?>> gradeItems = new EnumMap<>(Ore.Grade.class);
                for (Ore.Grade grade : Ore.Grade.values()) {
                    final String gradeName = grade.name().toLowerCase(Locale.ROOT);
                    final String id = "ore/" + gradeName + "_" + ore.getSerializedName() + "/" + rock.getSerializedName();
                    gradeItems.put(grade, items.registerSimpleBlockItem(id, GRADED_ORES.get(rock).get(ore).get(grade)));
                }
                rockItems.put(ore, Collections.unmodifiableMap(gradeItems));
            }
            blockItems.put(rock, Collections.unmodifiableMap(rockItems));
        }
        return Collections.unmodifiableMap(blockItems);
    }

    public static Map<Tfcmu2VanillaStone, Map<Tfcmu2Ore, Map<Ore.Grade, DeferredItem<?>>>> registerVanillaGradedOreBlockItems(DeferredRegister.Items items) {
        final EnumMap<Tfcmu2VanillaStone, Map<Tfcmu2Ore, Map<Ore.Grade, DeferredItem<?>>>> blockItems = new EnumMap<>(Tfcmu2VanillaStone.class);
        for (Tfcmu2VanillaStone stone : Tfcmu2VanillaStone.values()) {
            final EnumMap<Tfcmu2Ore, Map<Ore.Grade, DeferredItem<?>>> stoneItems = new EnumMap<>(Tfcmu2Ore.class);
            for (Tfcmu2Ore ore : Tfcmu2Ore.VALUES) {
                if (!ore.isGraded()) {
                    continue;
                }
                final EnumMap<Ore.Grade, DeferredItem<?>> gradeItems = new EnumMap<>(Ore.Grade.class);
                for (Ore.Grade grade : Ore.Grade.values()) {
                    final String gradeName = grade.name().toLowerCase(Locale.ROOT);
                    final String id = "ore/" + gradeName + "_" + ore.getSerializedName() + "/" + stone.getSerializedName();
                    gradeItems.put(grade, items.registerSimpleBlockItem(id, VANILLA_GRADED_ORES.get(stone).get(ore).get(grade)));
                }
                stoneItems.put(ore, Collections.unmodifiableMap(gradeItems));
            }
            blockItems.put(stone, Collections.unmodifiableMap(stoneItems));
        }
        return Collections.unmodifiableMap(blockItems);
    }

    public static Map<Tfcmu2VanillaStone, Map<String, DeferredItem<?>>> registerCompatVanillaOreBlockItems(DeferredRegister.Items items) {
        final EnumMap<Tfcmu2VanillaStone, Map<String, DeferredItem<?>>> blockItems = new EnumMap<>(Tfcmu2VanillaStone.class);
        for (Tfcmu2VanillaStone stone : Tfcmu2VanillaStone.values()) {
            final Map<String, DeferredItem<?>> stoneItems = new HashMap<>();
            for (Map.Entry<String, DeferredBlock<Block>> entry : COMPAT_VANILLA_ORES.get(stone).entrySet()) {
                final String oreName = entry.getKey();
                final String id = "ore/" + oreName + "/" + stone.getSerializedName();
                stoneItems.put(oreName, items.registerSimpleBlockItem(id, entry.getValue()));
            }
            blockItems.put(stone, Collections.unmodifiableMap(stoneItems));
        }
        return Collections.unmodifiableMap(blockItems);
    }

    public static Map<Tfcmu2Ore, DeferredItem<?>> registerSmallOreBlockItems(DeferredRegister.Items items) {
        final EnumMap<Tfcmu2Ore, DeferredItem<?>> blockItems = new EnumMap<>(Tfcmu2Ore.class);
        for (Tfcmu2Ore ore : Tfcmu2Ore.VALUES) {
            if (!ore.isGraded()) {
                continue;
            }
            final String id = "ore/small_" + ore.getSerializedName();
            blockItems.put(ore, items.registerSimpleBlockItem(id, SMALL_ORES.get(ore)));
        }
        return Collections.unmodifiableMap(blockItems);
    }

    private static Map<Tfcmu2Metal, DeferredBlock<Block>> registerMetalBlocks() {
        final EnumMap<Tfcmu2Metal, DeferredBlock<Block>> blocks = new EnumMap<>(Tfcmu2Metal.class);
        for (Tfcmu2Metal metal : Tfcmu2Metal.values()) {
            final String id = "metal/block/" + metal.getSerializedName();
            blocks.put(metal, BLOCKS.register(id, Metal.BlockType.BLOCK.create(metal)));
        }
        return Collections.unmodifiableMap(blocks);
    }

    private static Map<Tfcmu2Metal, DeferredBlock<Block>> registerMetalBlockSlabs() {
        final EnumMap<Tfcmu2Metal, DeferredBlock<Block>> blocks = new EnumMap<>(Tfcmu2Metal.class);
        for (Tfcmu2Metal metal : Tfcmu2Metal.values()) {
            final String id = "metal/block/" + metal.getSerializedName() + "_slab";
            blocks.put(metal, BLOCKS.register(id, Metal.BlockType.BLOCK_SLAB.create(metal)));
        }
        return Collections.unmodifiableMap(blocks);
    }

    private static Map<Tfcmu2Metal, DeferredBlock<Block>> registerMetalBlockStairs() {
        final EnumMap<Tfcmu2Metal, DeferredBlock<Block>> blocks = new EnumMap<>(Tfcmu2Metal.class);
        for (Tfcmu2Metal metal : Tfcmu2Metal.values()) {
            final String id = "metal/block/" + metal.getSerializedName() + "_stairs";
            blocks.put(metal, BLOCKS.register(id, Metal.BlockType.BLOCK_STAIRS.create(metal)));
        }
        return Collections.unmodifiableMap(blocks);
    }

    private static Map<Rock, Map<Tfcmu2Ore, DeferredBlock<Block>>> registerOres() {
        final EnumMap<Rock, Map<Tfcmu2Ore, DeferredBlock<Block>>> rocks = new EnumMap<>(Rock.class);
        for (Rock rock : Rock.VALUES) {
            final EnumMap<Tfcmu2Ore, DeferredBlock<Block>> ores = new EnumMap<>(Tfcmu2Ore.class);
            for (Tfcmu2Ore ore : Tfcmu2Ore.VALUES) {
                if (ore.isGraded()) {
                    continue;
                }
                final String id = "ore/" + ore.getSerializedName() + "/" + rock.getSerializedName();
                ores.put(ore, BLOCKS.register(id, () -> ore.create(rock)));
            }
            rocks.put(rock, Collections.unmodifiableMap(ores));
        }
        return Collections.unmodifiableMap(rocks);
    }

    private static Map<Rock, Map<Tfcmu2Ore, Map<Ore.Grade, DeferredBlock<Block>>>> registerGradedOres() {
        final EnumMap<Rock, Map<Tfcmu2Ore, Map<Ore.Grade, DeferredBlock<Block>>>> rocks = new EnumMap<>(Rock.class);
        for (Rock rock : Rock.VALUES) {
            final EnumMap<Tfcmu2Ore, Map<Ore.Grade, DeferredBlock<Block>>> ores = new EnumMap<>(Tfcmu2Ore.class);
            for (Tfcmu2Ore ore : Tfcmu2Ore.VALUES) {
                if (!ore.isGraded()) {
                    continue;
                }
                final EnumMap<Ore.Grade, DeferredBlock<Block>> grades = new EnumMap<>(Ore.Grade.class);
                for (Ore.Grade grade : Ore.Grade.values()) {
                    final String gradeName = grade.name().toLowerCase(Locale.ROOT);
                    final String id = "ore/" + gradeName + "_" + ore.getSerializedName() + "/" + rock.getSerializedName();
                    grades.put(grade, BLOCKS.register(id, () -> ore.create(rock)));
                }
                ores.put(ore, Collections.unmodifiableMap(grades));
            }
            rocks.put(rock, Collections.unmodifiableMap(ores));
        }
        return Collections.unmodifiableMap(rocks);
    }

    private static Map<Tfcmu2VanillaStone, Map<Tfcmu2Ore, Map<Ore.Grade, DeferredBlock<Block>>>> registerVanillaGradedOres() {
        final EnumMap<Tfcmu2VanillaStone, Map<Tfcmu2Ore, Map<Ore.Grade, DeferredBlock<Block>>>> stones = new EnumMap<>(Tfcmu2VanillaStone.class);
        for (Tfcmu2VanillaStone stone : Tfcmu2VanillaStone.values()) {
            final EnumMap<Tfcmu2Ore, Map<Ore.Grade, DeferredBlock<Block>>> ores = new EnumMap<>(Tfcmu2Ore.class);
            for (Tfcmu2Ore ore : Tfcmu2Ore.VALUES) {
                if (!ore.isGraded()) {
                    continue;
                }
                final EnumMap<Ore.Grade, DeferredBlock<Block>> grades = new EnumMap<>(Ore.Grade.class);
                for (Ore.Grade grade : Ore.Grade.values()) {
                    final String gradeName = grade.name().toLowerCase(Locale.ROOT);
                    final String id = "ore/" + gradeName + "_" + ore.getSerializedName() + "/" + stone.getSerializedName();
                    grades.put(grade, BLOCKS.register(id, () -> createVanillaOreBlock(stone.baseBlock())));
                }
                ores.put(ore, Collections.unmodifiableMap(grades));
            }
            stones.put(stone, Collections.unmodifiableMap(ores));
        }
        return Collections.unmodifiableMap(stones);
    }

    private static Map<Tfcmu2VanillaStone, Map<String, DeferredBlock<Block>>> registerCompatVanillaOres() {
        final EnumMap<Tfcmu2VanillaStone, Map<String, DeferredBlock<Block>>> stones = new EnumMap<>(Tfcmu2VanillaStone.class);
        final List<String> oreNames = Tfcmu2CompatOres.getLoadedOreNames();
        for (Tfcmu2VanillaStone stone : Tfcmu2VanillaStone.values()) {
            final Map<String, DeferredBlock<Block>> ores = new HashMap<>();
            for (String oreName : oreNames) {
                final String id = "ore/" + oreName + "/" + stone.getSerializedName();
                ores.put(oreName, BLOCKS.register(id, () -> createVanillaOreBlock(stone.baseBlock())));
            }
            stones.put(stone, Collections.unmodifiableMap(ores));
        }
        return Collections.unmodifiableMap(stones);
    }

    private static Map<Tfcmu2Ore, DeferredBlock<Block>> registerSmallOres() {
        final EnumMap<Tfcmu2Ore, DeferredBlock<Block>> ores = new EnumMap<>(Tfcmu2Ore.class);
        for (Tfcmu2Ore ore : Tfcmu2Ore.VALUES) {
            if (!ore.isGraded()) {
                continue;
            }
            final String id = "ore/small_" + ore.getSerializedName();
            ores.put(ore, BLOCKS.register(id, () -> GroundcoverBlock.looseOre(BlockBehaviour.Properties.of()
                .mapColor(MapColor.GRASS)
                .strength(0.05F, 0.0F)
                .sound(SoundType.NETHER_ORE)
                .noCollission()
                .pushReaction(PushReaction.DESTROY))));
        }
        return Collections.unmodifiableMap(ores);
    }

    private static Map<String, DeferredBlock<Block>> registerCompatSmallOrePieces() {
        final Map<String, DeferredBlock<Block>> ores = new HashMap<>();
        for (String oreName : ORE_PIECES_WITHOUT_SAMPLES) {
            final String id = "ore/small_" + oreName;
            ores.put(oreName, BLOCKS.register(id, () -> GroundcoverBlock.looseOre(BlockBehaviour.Properties.of()
                .mapColor(MapColor.GRASS)
                .strength(0.05F, 0.0F)
                .sound(SoundType.NETHER_ORE)
                .noCollission()
                .pushReaction(PushReaction.DESTROY))));
        }
        return Collections.unmodifiableMap(ores);
    }

    private static Block createVanillaOreBlock(Block baseBlock) {
        return new Block(BlockBehaviour.Properties.ofFullCopy(baseBlock).requiresCorrectToolForDrops());
    }
}
