package net.claustra01.tfcmu2.worldgen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mojang.logging.LogUtils;

import net.claustra01.tfcmu2.Tfcmu2Config;
import net.dries007.tfc.util.collections.IWeighted;
import net.dries007.tfc.world.feature.vein.IVeinConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import org.slf4j.Logger;

public final class Tfcmu2OreVeinBiomeModifier implements BiomeModifier {
    static final Tfcmu2OreVeinBiomeModifier INSTANCE = new Tfcmu2OreVeinBiomeModifier();

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean LOGGED_CUSTOM_OVERWORLD_STATUS = new AtomicBoolean(false);
    private static final AtomicBoolean LOGGED_CUSTOM_NETHER_STATUS = new AtomicBoolean(false);
    private static final AtomicBoolean LOGGED_CUSTOM_END_STATUS = new AtomicBoolean(false);

    // Matches the tags referenced by TFC biome JSONs.
    private static final TagKey<PlacedFeature> VEINS_TAG = TagKey.create(
        Registries.PLACED_FEATURE, id("tfc", "in_biome/veins")
    );
    private static final TagKey<PlacedFeature> RIVER_VEINS_TAG = TagKey.create(
        Registries.PLACED_FEATURE, id("tfc", "in_biome/veins/river")
    );
    private static final TagKey<PlacedFeature> KAOLIN_VEINS_TAG = TagKey.create(
        Registries.PLACED_FEATURE, id("tfc", "in_biome/veins/kaolin")
    );
    private static final TagKey<Biome> IS_NETHER = TagKey.create(
        Registries.BIOME, id("minecraft", "is_nether")
    );
    private static final TagKey<Biome> IS_END = TagKey.create(
        Registries.BIOME, id("minecraft", "is_end")
    );

    private Tfcmu2OreVeinBiomeModifier() {
    }

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        // Replace at the very end so we always win against earlier biome modifiers, while keeping defaults.
        if (phase != Phase.AFTER_EVERYTHING) {
            return;
        }

        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        final Registry<PlacedFeature> placedFeatures = server.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);

        final List<Holder<PlacedFeature>> ores = builder.getGenerationSettings().getFeatures(GenerationStep.Decoration.UNDERGROUND_ORES);

        if (Tfcmu2Config.COMMON.enableCustomVeinGeneration.get()) {
            if (biome.is(IS_NETHER)) {
                final List<Holder<PlacedFeature>> customVeins = Tfcmu2CustomVeins.resolveNetherPlacedFeatures(placedFeatures);
                if (!customVeins.isEmpty()) {
                    if (LOGGED_CUSTOM_NETHER_STATUS.compareAndSet(false, true)) {
                        LOGGER.info("Custom vein generation enabled: adding {} nether custom veins to minecraft:is_nether biomes.", customVeins.size());
                    }
                    addUniqueFeatures(ores, customVeins);
                } else if (LOGGED_CUSTOM_NETHER_STATUS.compareAndSet(false, true)) {
                    LOGGER.info("Custom vein generation enabled, but no nether custom veins were loaded.");
                }
                return;
            }
            if (biome.is(IS_END)) {
                final List<Holder<PlacedFeature>> customVeins = Tfcmu2CustomVeins.resolveEndPlacedFeatures(placedFeatures);
                if (!customVeins.isEmpty()) {
                    if (LOGGED_CUSTOM_END_STATUS.compareAndSet(false, true)) {
                        LOGGER.info("Custom vein generation enabled: adding {} end custom veins to minecraft:is_end biomes.", customVeins.size());
                    }
                    addUniqueFeatures(ores, customVeins);
                } else if (LOGGED_CUSTOM_END_STATUS.compareAndSet(false, true)) {
                    LOGGER.info("Custom vein generation enabled, but no end custom veins were loaded.");
                }
                return;
            }
        }

        // Mirrors TFC's biome JSON layout where '#tfc:in_biome/veins' sits at feature-step index 6.
        if (!containsAnyFeatureFromTag(ores, placedFeatures, VEINS_TAG)) {
            return;
        }

        if (Tfcmu2Config.COMMON.enableCustomVeinGeneration.get()) {
            final List<Holder<PlacedFeature>> customVeins = Tfcmu2CustomVeins.resolveOverworldPlacedFeatures(placedFeatures);
            if (!customVeins.isEmpty()) {
                if (LOGGED_CUSTOM_OVERWORLD_STATUS.compareAndSet(false, true)) {
                    LOGGER.info("Custom vein generation enabled: replacing ore veins from #tfc:in_biome/veins with {} custom veins.", customVeins.size());
                }
                // Only replace actual ore veins. Keep other features from the veins tag (ex: gravel, dikes, geodes) intact.
                replaceOreVeinsFromTagWithValues(ores, placedFeatures, VEINS_TAG, "#tfc:in_biome/veins", customVeins);
            } else {
                if (LOGGED_CUSTOM_OVERWORLD_STATUS.compareAndSet(false, true)) {
                    LOGGER.warn("Custom vein generation is enabled, but no custom veins were loaded. Falling back to default TFC veins.");
                }
                // If enabled but no custom veins are available, keep default behavior instead of wiping veins.
                replaceFromTag(ores, placedFeatures, VEINS_TAG, "#tfc:in_biome/veins");
            }
        } else {
            replaceFromTag(ores, placedFeatures, VEINS_TAG, "#tfc:in_biome/veins");
        }

        // In TFC biomes that include '#tfc:in_biome/veins/river' or '#tfc:in_biome/veins/kaolin', it appears right after veins at index 7.
        final List<Holder<PlacedFeature>> extra = builder.getGenerationSettings().getFeatures(GenerationStep.Decoration.UNDERGROUND_DECORATION);
        if (containsAnyFeatureFromTag(extra, placedFeatures, RIVER_VEINS_TAG)) {
            replaceFromTag(extra, placedFeatures, RIVER_VEINS_TAG, "#tfc:in_biome/veins/river");
        } else if (containsAnyFeatureFromTag(extra, placedFeatures, KAOLIN_VEINS_TAG)) {
            replaceFromTag(extra, placedFeatures, KAOLIN_VEINS_TAG, "#tfc:in_biome/veins/kaolin");
        }
    }

    private static void addUniqueFeatures(List<Holder<PlacedFeature>> target, List<Holder<PlacedFeature>> additions) {
        for (Holder<PlacedFeature> holder : additions) {
            if (!target.contains(holder)) {
                target.add(holder);
            }
        }
    }

    @Override
    public com.mojang.serialization.MapCodec<? extends BiomeModifier> codec() {
        return Tfcmu2Worldgen.ORE_VEINS.get();
    }

    private static void replaceFromTag(List<Holder<PlacedFeature>> target, Registry<PlacedFeature> placedFeatures, TagKey<PlacedFeature> tagKey, String owner) {
        final Optional<HolderSet.Named<PlacedFeature>> tagOpt = placedFeatures.getTag(tagKey);
        if (tagOpt.isEmpty()) {
            LOGGER.warn("Missing placed_feature tag {} referenced by {}", tagKey.location(), owner);
            return;
        }

        final List<Holder<PlacedFeature>> tagValues = new ArrayList<>();
        for (Holder<PlacedFeature> holder : tagOpt.get()) {
            tagValues.add(holder);
        }

        replaceFromTagWithValues(target, placedFeatures, tagKey, owner, tagValues);
    }

    private static void replaceFromTagWithValues(List<Holder<PlacedFeature>> target, Registry<PlacedFeature> placedFeatures, TagKey<PlacedFeature> tagKey, String owner, List<Holder<PlacedFeature>> values) {
        final Optional<HolderSet.Named<PlacedFeature>> tagOpt = placedFeatures.getTag(tagKey);
        if (tagOpt.isEmpty()) {
            LOGGER.warn("Missing placed_feature tag {} referenced by {}", tagKey.location(), owner);
            return;
        }

        final Set<ResourceLocation> tagKeys = new HashSet<>();
        for (Holder<PlacedFeature> holder : tagOpt.get()) {
            holder.unwrapKey().ifPresent(key -> tagKeys.add(key.location()));
        }

        int insertAt = target.size();
        for (int i = 0; i < target.size(); i++) {
            final Optional<ResourceKey<PlacedFeature>> key = target.get(i).unwrapKey();
            if (key.isPresent() && tagKeys.contains(key.get().location())) {
                insertAt = i;
                break;
            }
        }

        target.removeIf(holder -> holder.unwrapKey().map(key -> tagKeys.contains(key.location())).orElse(false));
        target.addAll(insertAt, values);
    }

    private static void replaceOreVeinsFromTagWithValues(List<Holder<PlacedFeature>> target, Registry<PlacedFeature> placedFeatures, TagKey<PlacedFeature> tagKey, String owner, List<Holder<PlacedFeature>> values) {
        final Optional<HolderSet.Named<PlacedFeature>> tagOpt = placedFeatures.getTag(tagKey);
        if (tagOpt.isEmpty()) {
            LOGGER.warn("Missing placed_feature tag {} referenced by {}", tagKey.location(), owner);
            return;
        }

        final Set<ResourceLocation> oreVeinKeys = new HashSet<>();
        for (Holder<PlacedFeature> holder : tagOpt.get()) {
            if (!isOreVein(holder.value())) {
                continue;
            }
            holder.unwrapKey().ifPresent(key -> oreVeinKeys.add(key.location()));
        }

        if (oreVeinKeys.isEmpty()) {
            LOGGER.warn("No ore veins found in placed_feature tag {} referenced by {}", tagKey.location(), owner);
            return;
        }

        int insertAt = target.size();
        for (int i = 0; i < target.size(); i++) {
            final Optional<ResourceKey<PlacedFeature>> key = target.get(i).unwrapKey();
            if (key.isPresent() && oreVeinKeys.contains(key.get().location())) {
                insertAt = i;
                break;
            }
        }

        target.removeIf(holder -> holder.unwrapKey().map(key -> oreVeinKeys.contains(key.location())).orElse(false));
        target.addAll(insertAt, values);
    }

    private static boolean containsAnyFeatureFromTag(List<Holder<PlacedFeature>> features, Registry<PlacedFeature> placedFeatures, TagKey<PlacedFeature> tagKey) {
        final Optional<HolderSet.Named<PlacedFeature>> tagOpt = placedFeatures.getTag(tagKey);
        if (tagOpt.isEmpty()) {
            return false;
        }
        final Set<ResourceLocation> tagKeys = new HashSet<>();
        for (Holder<PlacedFeature> holder : tagOpt.get()) {
            holder.unwrapKey().ifPresent(key -> tagKeys.add(key.location()));
        }
        if (tagKeys.isEmpty()) {
            return false;
        }
        for (Holder<PlacedFeature> holder : features) {
            final Optional<ResourceKey<PlacedFeature>> keyOpt = holder.unwrapKey();
            if (keyOpt.isPresent() && tagKeys.contains(keyOpt.get().location())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOreVein(PlacedFeature placedFeature) {
        final Holder<ConfiguredFeature<?, ?>> configuredHolder = placedFeature.feature();
        final ConfiguredFeature<?, ?> configured = configuredHolder.value();
        final FeatureConfiguration config = configured.config();
        if (!(config instanceof IVeinConfig veinConfig)) {
            return false;
        }

        for (IWeighted<BlockState> weighted : veinConfig.config().states().values()) {
            for (BlockState state : weighted.values()) {
                final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                if (id == null) {
                    continue;
                }
                final String path = id.getPath();
                if (path.startsWith("ore/")) {
                    return true;
                }
                // Treat non-rock outputs (ex: tfc:halite, tfc:lignite) as replaceable deposits.
                if (!path.startsWith("rock/")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
