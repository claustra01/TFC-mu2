package net.claustra01.tfcmu2;

import java.util.Optional;

import net.dries007.tfc.common.blockentities.IngotPileBlockEntity;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.devices.DoubleIngotPileBlock;
import net.dries007.tfc.common.blocks.devices.IngotPileBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Tfcmu2Mod.MOD_ID)
public final class Tfcmu2Interactions {
    private static final TagKey<Item> SHEETS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "sheets"));
    private static final TagKey<Item> DOUBLE_SHEETS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "double_sheets"));

    private Tfcmu2Interactions() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        final ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        final boolean isSheet = stack.is(SHEETS);
        final boolean isDoubleSheet = stack.is(DOUBLE_SHEETS);
        if (!isSheet && !isDoubleSheet) {
            return;
        }

        final Level level = event.getLevel();
        final Player player = event.getEntity();
        final BlockHitResult hitResult = event.getHitVec();
        final InteractionHand hand = event.getHand();

        final InteractionResult result = isDoubleSheet
            ? tryPilePlacement(stack, player, level, hand, hitResult, (IngotPileBlock) TFCBlocks.DOUBLE_INGOT_PILE.get(), DoubleIngotPileBlock.DOUBLE_COUNT, 36)
            : tryPilePlacement(stack, player, level, hand, hitResult, (IngotPileBlock) TFCBlocks.INGOT_PILE.get(), IngotPileBlock.COUNT, 64);

        if (result.consumesAction()) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    private static InteractionResult tryPilePlacement(ItemStack stack, Player player, Level level, InteractionHand hand, BlockHitResult hitResult, IngotPileBlock pileBlock, IntegerProperty countProperty, int maxCount) {
        if (!player.mayBuild()) {
            return InteractionResult.PASS;
        }

        BlockPos targetPos = hitResult.getBlockPos();
        BlockState targetState = level.getBlockState(targetPos);

        if (targetState.is(pileBlock)) {
            return addToPile(stack, player, level, targetPos, pileBlock, countProperty, maxCount);
        }

        final UseOnContext useContext = new UseOnContext(player, hand, hitResult);
        final BlockPlaceContext placeContext = new BlockPlaceContext(useContext);
        if (!targetState.canBeReplaced(placeContext)) {
            targetPos = targetPos.relative(hitResult.getDirection());
            targetState = level.getBlockState(targetPos);
        }

        if (targetState.is(pileBlock)) {
            return addToPile(stack, player, level, targetPos, pileBlock, countProperty, maxCount);
        }

        return placeNewPile(stack, player, level, targetPos, targetState, pileBlock, countProperty);
    }

    private static InteractionResult addToPile(ItemStack stack, Player player, Level level, BlockPos pilePos, IngotPileBlock pileBlock, IntegerProperty countProperty, int maxCount) {
        BlockPos topPos = pilePos;
        while (level.getBlockState(topPos.above()).is(pileBlock)) {
            topPos = topPos.above();
        }

        final Optional<IngotPileBlockEntity> existingTop = level.getBlockEntity(topPos, TFCBlockEntities.INGOT_PILE.get());
        if (existingTop.isEmpty()) {
            return InteractionResult.PASS;
        }

        final ItemStack topStack = existingTop.get().getPickedItemStack();
        if (!topStack.isEmpty() && !ItemStack.isSameItemSameComponents(topStack, stack)) {
            return InteractionResult.PASS;
        }

        final BlockState topState = level.getBlockState(topPos);
        final int count = topState.getValue(countProperty);
        if (count < maxCount) {
            final BlockState updatedState = topState.setValue(countProperty, count + 1);
            if (!level.setBlock(topPos, updatedState, 3)) {
                return InteractionResult.PASS;
            }

            final Optional<IngotPileBlockEntity> updatedPile = level.getBlockEntity(topPos, TFCBlockEntities.INGOT_PILE.get());
            if (updatedPile.isEmpty()) {
                return InteractionResult.PASS;
            }

            insertOne(stack, player, updatedPile.get());
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        final BlockPos abovePos = topPos.above();
        final BlockState aboveState = level.getBlockState(abovePos);
        return placeNewPile(stack, player, level, abovePos, aboveState, pileBlock, countProperty);
    }

    private static InteractionResult placeNewPile(ItemStack stack, Player player, Level level, BlockPos pos, BlockState stateAtPos, IngotPileBlock pileBlock, IntegerProperty countProperty) {
        if (!stateAtPos.canBeReplaced()) {
            return InteractionResult.PASS;
        }

        final BlockState placedState = pileBlock.defaultBlockState().setValue(countProperty, 1);
        if (!placedState.canSurvive(level, pos)) {
            return InteractionResult.PASS;
        }

        if (!level.setBlock(pos, placedState, 3)) {
            return InteractionResult.PASS;
        }

        final Optional<IngotPileBlockEntity> placedPile = level.getBlockEntity(pos, TFCBlockEntities.INGOT_PILE.get());
        if (placedPile.isEmpty()) {
            return InteractionResult.PASS;
        }

        insertOne(stack, player, placedPile.get());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void insertOne(ItemStack stack, Player player, IngotPileBlockEntity pile) {
        pile.addIngot(stack.copyWithCount(1));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }
}
