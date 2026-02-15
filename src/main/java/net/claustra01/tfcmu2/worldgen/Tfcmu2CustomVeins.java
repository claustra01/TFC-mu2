package net.claustra01.tfcmu2.worldgen;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.logging.LogUtils;

import net.claustra01.tfcmu2.Tfcmu2Mod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import org.slf4j.Logger;

/**
 * Loads custom veins from config/tfcmu2/{overworld,nether,end}.yaml and builds
 * direct placed features from them.
 * These features are inserted into biomes by {@link Tfcmu2OreVeinBiomeModifier}
 * when enabled via config.
 */
public final class Tfcmu2CustomVeins {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String DEFAULT_CONFIG_SUBDIR = Tfcmu2Mod.MOD_ID;
    private static final String OVERWORLD_CONFIG_FILE = "overworld.yaml";
    private static final String LEGACY_OVERWORLD_CONFIG_FILE = "veins.yaml";
    private static final String NETHER_CONFIG_FILE = "nether.yaml";
    private static final String END_CONFIG_FILE = "end.yaml";

    private static final String CLASSPATH_OVERWORLD_SAMPLE = "/tfcmu2/overworld.yaml";
    private static final String CLASSPATH_NETHER_SAMPLE = "/tfcmu2/nether.yaml";
    private static final String CLASSPATH_END_SAMPLE = "/tfcmu2/end.yaml";

    private static List<Tfcmu2VeinsYamlParser.VeinDefinition> CACHED_OVERWORLD_DEFS = List.of();
    private static List<Tfcmu2VeinsYamlParser.VeinDefinition> CACHED_NETHER_DEFS = List.of();
    private static List<Tfcmu2VeinsYamlParser.VeinDefinition> CACHED_END_DEFS = List.of();

    private static List<net.minecraft.core.Holder<PlacedFeature>> CACHED_OVERWORLD_PLACED_FEATURES = List.of();
    private static List<net.minecraft.core.Holder<PlacedFeature>> CACHED_NETHER_PLACED_FEATURES = List.of();
    private static List<net.minecraft.core.Holder<PlacedFeature>> CACHED_END_PLACED_FEATURES = List.of();

    private static boolean BUILT_OVERWORLD_PLACED_FEATURES = false;
    private static boolean BUILT_NETHER_PLACED_FEATURES = false;
    private static boolean BUILT_END_PLACED_FEATURES = false;

    private Tfcmu2CustomVeins() {
    }

    public static void bootstrap() {
        final Path configDir = getConfigDir();
        final Path overworldPath = configDir.resolve(OVERWORLD_CONFIG_FILE);
        final Path legacyOverworldPath = configDir.resolve(LEGACY_OVERWORLD_CONFIG_FILE);
        final Path netherPath = configDir.resolve(NETHER_CONFIG_FILE);
        final Path endPath = configDir.resolve(END_CONFIG_FILE);

        if (!Files.exists(overworldPath) && Files.exists(legacyOverworldPath)) {
            LOGGER.info(
                "Using legacy custom vein config {}. Rename it to {} to match the current default.",
                legacyOverworldPath,
                overworldPath
            );
        } else {
            ensureSampleFileExists(overworldPath, CLASSPATH_OVERWORLD_SAMPLE);
        }
        ensureSampleFileExists(netherPath, CLASSPATH_NETHER_SAMPLE);
        ensureSampleFileExists(endPath, CLASSPATH_END_SAMPLE);

        final Path overworldSourcePath = Files.exists(overworldPath) ? overworldPath : legacyOverworldPath;
        CACHED_OVERWORLD_DEFS = loadDefinitions("overworld", overworldSourcePath);
        CACHED_NETHER_DEFS = loadDefinitions("nether", netherPath);
        CACHED_END_DEFS = loadDefinitions("end", endPath);

        CACHED_OVERWORLD_PLACED_FEATURES = List.of();
        CACHED_NETHER_PLACED_FEATURES = List.of();
        CACHED_END_PLACED_FEATURES = List.of();
        BUILT_OVERWORLD_PLACED_FEATURES = false;
        BUILT_NETHER_PLACED_FEATURES = false;
        BUILT_END_PLACED_FEATURES = false;
    }

    public static List<net.minecraft.core.Holder<PlacedFeature>> resolvePlacedFeatures(net.minecraft.core.Registry<PlacedFeature> placedFeatures) {
        return resolveOverworldPlacedFeatures(placedFeatures);
    }

    public static List<net.minecraft.core.Holder<PlacedFeature>> resolveOverworldPlacedFeatures(net.minecraft.core.Registry<PlacedFeature> placedFeatures) {
        if (CACHED_OVERWORLD_DEFS.isEmpty()) {
            return List.of();
        }
        if (BUILT_OVERWORLD_PLACED_FEATURES) {
            return CACHED_OVERWORLD_PLACED_FEATURES;
        }
        CACHED_OVERWORLD_PLACED_FEATURES = buildPlacedFeatures(CACHED_OVERWORLD_DEFS, "overworld");
        BUILT_OVERWORLD_PLACED_FEATURES = true;
        return CACHED_OVERWORLD_PLACED_FEATURES;
    }

    public static List<net.minecraft.core.Holder<PlacedFeature>> resolveNetherPlacedFeatures(net.minecraft.core.Registry<PlacedFeature> placedFeatures) {
        if (CACHED_NETHER_DEFS.isEmpty()) {
            return List.of();
        }
        if (BUILT_NETHER_PLACED_FEATURES) {
            return CACHED_NETHER_PLACED_FEATURES;
        }
        CACHED_NETHER_PLACED_FEATURES = buildPlacedFeatures(CACHED_NETHER_DEFS, "nether");
        BUILT_NETHER_PLACED_FEATURES = true;
        return CACHED_NETHER_PLACED_FEATURES;
    }

    public static List<net.minecraft.core.Holder<PlacedFeature>> resolveEndPlacedFeatures(net.minecraft.core.Registry<PlacedFeature> placedFeatures) {
        if (CACHED_END_DEFS.isEmpty()) {
            return List.of();
        }
        if (BUILT_END_PLACED_FEATURES) {
            return CACHED_END_PLACED_FEATURES;
        }
        CACHED_END_PLACED_FEATURES = buildPlacedFeatures(CACHED_END_DEFS, "end");
        BUILT_END_PLACED_FEATURES = true;
        return CACHED_END_PLACED_FEATURES;
    }

    private static List<net.minecraft.core.Holder<PlacedFeature>> buildPlacedFeatures(List<Tfcmu2VeinsYamlParser.VeinDefinition> defs, String scopeName) {
        // Build direct placed features lazily, after all builtin registries
        // (including other mods' Feature/Block entries) are available.
        final List<net.minecraft.core.Holder<PlacedFeature>> built = new ArrayList<>(defs.size());
        for (Tfcmu2VeinsYamlParser.VeinDefinition def : defs) {
            try {
                final var configured = def.buildConfiguredFeature();
                final var placed = new PlacedFeature(net.minecraft.core.Holder.direct(configured), List.of());
                built.add(net.minecraft.core.Holder.direct(placed));
            } catch (Exception e) {
                LOGGER.error("Failed to build {} custom vein {}. Skipping.", scopeName, def.id(), e);
            }
        }
        return List.copyOf(built);
    }

    private static List<Tfcmu2VeinsYamlParser.VeinDefinition> loadDefinitions(String scopeName, Path yamlPath) {
        if (yamlPath == null || !Files.exists(yamlPath)) {
            return List.of();
        }

        final List<Tfcmu2VeinsYamlParser.VeinDefinition> defs;
        try {
            defs = Tfcmu2VeinsYamlParser.parseVeins(yamlPath, defaultRocksForScope(scopeName));
        } catch (Exception e) {
            LOGGER.error("Failed to parse {} custom veins YAML at {}. This scope will be skipped.", scopeName, yamlPath, e);
            return List.of();
        }

        if (defs.isEmpty()) {
            LOGGER.info("No {} custom veins found in {}.", scopeName, yamlPath);
            return List.of();
        }

        final Set<ResourceLocation> seen = new HashSet<>();
        final List<Tfcmu2VeinsYamlParser.VeinDefinition> accepted = new ArrayList<>(defs.size());
        for (Tfcmu2VeinsYamlParser.VeinDefinition def : defs) {
            final ResourceLocation outId = toTfcmu2Id(def.id());
            if (!seen.add(outId)) {
                LOGGER.warn("Duplicate {} custom vein id {} (from {}), skipping.", scopeName, outId, def.id());
                continue;
            }
            if (!canLoadDefinition(def)) {
                continue;
            }
            if (!isSupportedType(def)) {
                continue;
            }
            accepted.add(def);
        }

        if (accepted.isEmpty()) {
            LOGGER.warn("No {} custom veins were registered from {}.", scopeName, yamlPath);
            return List.of();
        }

        // Note: placed/configured features are datapack registries in 1.21.1 and
        // are not part of the mod-registerable builtin registries.
        // We therefore build & inject direct placed features at runtime.
        LOGGER.info("Loaded {} {} custom veins from {} (namespace {}).", accepted.size(), scopeName, yamlPath, Tfcmu2Mod.MOD_ID);
        return List.copyOf(accepted);
    }

    private static List<String> defaultRocksForScope(String scopeName) {
        return switch (scopeName) {
            case "nether" -> List.of("netherrack");
            case "end" -> List.of("endstone");
            default -> List.of();
        };
    }

    private static boolean canLoadDefinition(Tfcmu2VeinsYamlParser.VeinDefinition def) {
        final String template = def.blockTemplate();
        final int colonIdx = template.indexOf(':');
        if (colonIdx > 0) {
            final String ns = template.substring(0, colonIdx);
            if (!"minecraft".equals(ns) && !ModList.get().isLoaded(ns)) {
                LOGGER.warn("Skipping custom vein {} because referenced mod '{}' is not loaded (block template: {}).", def.id(), ns, template);
                return false;
            }
        }
        if (def.indicator() != null) {
            final String indicator = def.indicator().block();
            final int idx = indicator.indexOf(':');
            if (idx > 0) {
                final String ns = indicator.substring(0, idx);
                if (!"minecraft".equals(ns) && !ModList.get().isLoaded(ns)) {
                    LOGGER.warn("Skipping custom vein {} because referenced mod '{}' is not loaded (indicator: {}).", def.id(), ns, indicator);
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isSupportedType(Tfcmu2VeinsYamlParser.VeinDefinition def) {
        final String type = def.type().toString();
        if (!type.equals("tfc:cluster_vein") && !type.equals("tfc:disc_vein") && !type.equals("tfc:kaolin_disc_vein") && !type.equals("tfc:pipe_vein")) {
            LOGGER.warn("Skipping custom vein {} due to unsupported type '{}'.", def.id(), def.type());
            return false;
        }
        if (type.equals("tfc:pipe_vein") && def.pipe() == null) {
            LOGGER.warn("Skipping custom vein {} because type '{}' requires pipe parameters.", def.id(), def.type());
            return false;
        }
        return true;
    }

    private static ResourceLocation toTfcmu2Id(ResourceLocation sourceId) {
        return ResourceLocation.fromNamespaceAndPath(Tfcmu2Mod.MOD_ID, sourceId.getPath());
    }

    private static Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get().resolve(DEFAULT_CONFIG_SUBDIR);
    }

    private static void ensureSampleFileExists(Path path, String classpathSample) {
        if (Files.exists(path)) {
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            try (InputStream in = Tfcmu2CustomVeins.class.getResourceAsStream(classpathSample)) {
                if (in == null) {
                    // Last resort: create a minimal stub if the sample wasn't packaged.
                    Files.writeString(path, "veins:\n", StandardCharsets.UTF_8);
                    LOGGER.warn("Missing packaged sample {}. Wrote a minimal stub at {}.", classpathSample, path);
                    return;
                }
                Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
            }
            LOGGER.info("Created sample custom veins YAML at {}.", path);
        } catch (IOException e) {
            LOGGER.error("Failed to create sample custom veins YAML at {}.", path, e);
        }
    }
}
