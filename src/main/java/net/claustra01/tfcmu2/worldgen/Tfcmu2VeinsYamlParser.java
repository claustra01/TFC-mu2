package net.claustra01.tfcmu2.worldgen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.mojang.logging.LogUtils;

import net.dries007.tfc.util.collections.IWeighted;
import net.dries007.tfc.util.collections.Weighted;
import net.dries007.tfc.world.feature.vein.ClusterVeinConfig;
import net.dries007.tfc.world.feature.vein.DiscVeinConfig;
import net.dries007.tfc.world.feature.vein.Indicator;
import net.dries007.tfc.world.feature.vein.PipeVeinConfig;
import net.dries007.tfc.world.feature.vein.VeinConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import org.slf4j.Logger;

/**
 * A tiny, purpose-built parser for tfcmu2 custom veins YAML.
 *
 * Supported structure (subset of YAML):
 * - Root key: veins:
 * - List items: - namespace:path:
 * - Key/value pairs inside each item
 * - Lists (rocks/tier) as dash items, indentation tolerant
 * - A nested mapping: indicator:
 */
public final class Tfcmu2VeinsYamlParser {
    private static final Logger LOGGER = LogUtils.getLogger();

    private Tfcmu2VeinsYamlParser() {
    }

    public record IndicatorDefinition(
        String block,
        int depth,
        int rarity,
        int undergroundRarity,
        int undergroundCount
    ) {
    }

    public record PipeParams(
        int height,
        int radius,
        int minSkew,
        int maxSkew,
        int minSlant,
        int maxSlant,
        float sign
    ) {
    }

    public record VeinDefinition(
        ResourceLocation id,
        String blockTemplate,
        ResourceLocation type,
        int minY,
        int maxY,
        int rarity,
        float density,
        int size,
        Integer height,
        PipeParams pipe,
        List<String> rocks,
        LinkedHashMap<String, Integer> tierWeights,
        IndicatorDefinition indicator,
        boolean project,
        boolean projectOffset,
        boolean nearLava,
        String randomName,
        Long seedOverride
    ) {
        public ConfiguredFeature<?, ?> buildConfiguredFeature() {
            final FeatureConfiguration veinConfig = switch (type.toString()) {
                case "tfc:cluster_vein" -> new ClusterVeinConfig(buildVeinConfig(), size);
                case "tfc:disc_vein", "tfc:kaolin_disc_vein" -> {
                    final int h = height != null ? height : 0;
                    yield new DiscVeinConfig(buildVeinConfig(), size, h);
                }
                case "tfc:pipe_vein" -> {
                    if (pipe == null) {
                        throw new IllegalStateException("pipe_vein requires pipe parameters (height/radius/...)");
                    }
                    yield new PipeVeinConfig(buildVeinConfig(), pipe.height(), pipe.radius(), pipe.minSkew(), pipe.maxSkew(), pipe.minSlant(), pipe.maxSlant(), pipe.sign());
                }
                default -> throw new IllegalArgumentException("Unsupported vein feature type: " + type);
            };

            @SuppressWarnings({ "rawtypes", "unchecked" })
            final Feature<FeatureConfiguration> feature = (Feature) BuiltInRegistries.FEATURE.get(type);

            return new ConfiguredFeature<>(feature, veinConfig);
        }

        private VeinConfig buildVeinConfig() {
            final Map<Block, IWeighted<BlockState>> states = new LinkedHashMap<>();

            for (String rock : rocks) {
                final Block replace = resolveReplaceBlock(rock);
                if (replace == Blocks.AIR) {
                    LOGGER.warn("Unknown replace block for rock '{}' in {}", rock, id);
                    continue;
                }

                final Weighted<BlockState> weighted = new Weighted<>(new ArrayList<>());
                final String rockToken = resolveOreRockToken(rock);
                for (Map.Entry<String, Integer> entry : tierWeights.entrySet()) {
                    final String tier = entry.getKey();
                    final int weight = entry.getValue();
                    if (weight <= 0) {
                        continue;
                    }
                    final String out = blockTemplate
                        .replace("{tier}", tier)
                        .replace("{rock}", rockToken);
                    final ResourceLocation outId = ResourceLocation.tryParse(out);
                    if (outId == null) {
                        LOGGER.warn("Invalid ore block id '{}' (template '{}') in {}", out, blockTemplate, id);
                        continue;
                    }

                    final Block oreBlock = BuiltInRegistries.BLOCK.get(outId);
                    if (oreBlock == Blocks.AIR && !BuiltInRegistries.BLOCK.containsKey(outId)) {
                        LOGGER.warn("Missing ore block {} referenced by {} (did you install the correct mods?)", outId, id);
                        continue;
                    }
                    weighted.add(weight, oreBlock.defaultBlockState());
                }

                if (!weighted.isEmpty()) {
                    states.put(replace, weighted);
                }
            }

            final Indicator indicatorValue = indicator != null ? buildIndicator() : null;
            final Optional<Indicator> indicatorOpt = indicatorValue != null ? Optional.of(indicatorValue) : Optional.empty();

            final long seed = seedOverride != null ? seedOverride : hashSeed(randomName);

            return new VeinConfig(states, indicatorOpt, rarity, density, minY, maxY, project, projectOffset, seed, nearLava);
        }

        private Indicator buildIndicator() {
            final ResourceLocation blockId = ResourceLocation.tryParse(indicator.block());
            if (blockId == null) {
                LOGGER.warn("Invalid indicator block id '{}' in {}", indicator.block(), id);
                return null;
            }
            final Block b = BuiltInRegistries.BLOCK.get(blockId);
            if (b == Blocks.AIR && !BuiltInRegistries.BLOCK.containsKey(blockId)) {
                LOGGER.warn("Missing indicator block {} in {}", blockId, id);
                return null;
            }
            final Weighted<BlockState> w = new Weighted<>(new ArrayList<>());
            w.add(1, b.defaultBlockState());
            return new Indicator(indicator.depth(), indicator.rarity(), indicator.undergroundRarity(), indicator.undergroundCount(), w);
        }
    }

    public static List<VeinDefinition> parseVeins(Path yamlPath) throws IOException {
        return parseVeins(yamlPath, List.of());
    }

    public static List<VeinDefinition> parseVeins(Path yamlPath, List<String> defaultRocks) throws IOException {
        if (!Files.exists(yamlPath)) {
            return List.of();
        }

        final List<String> lines = Files.readAllLines(yamlPath, StandardCharsets.UTF_8);
        int i = 0;

        // Find 'veins:' root key (ignore blanks/comments).
        while (i < lines.size()) {
            final String trimmed = stripComment(lines.get(i)).trim();
            if (!trimmed.isEmpty()) {
                break;
            }
            i++;
        }
        if (i >= lines.size() || !"veins:".equals(stripComment(lines.get(i)).trim())) {
            LOGGER.warn("Expected root key 'veins:' in {}", yamlPath);
            return List.of();
        }
        i++;

        final List<VeinDefinition> defs = new ArrayList<>();
        while (i < lines.size()) {
            final String line = stripComment(lines.get(i));
            if (line.trim().isEmpty()) {
                i++;
                continue;
            }
            final int indent = countIndent(line);
            final String trimmed = line.trim();
            if (!trimmed.startsWith("- ")) {
                // Unexpected line at root; skip to be tolerant.
                LOGGER.warn("Skipping unexpected line in {}: {}", yamlPath, trimmed);
                i++;
                continue;
            }

            final ParsedListKey listKey = parseListKey(trimmed);
            if (listKey == null) {
                LOGGER.warn("Skipping malformed vein entry line in {}: {}", yamlPath, trimmed);
                i++;
                continue;
            }

            final ResourceLocation id = ResourceLocation.tryParse(listKey.key);
            if (id == null) {
                LOGGER.warn("Skipping invalid vein id '{}' in {}", listKey.key, yamlPath);
                i++;
                continue;
            }

            i++;

            // Parse mapping block under this list item.
            final LinkedHashMap<String, Object> props = new LinkedHashMap<>();
            final int baseIndent = listKey.nextIndentHint > 0 ? listKey.nextIndentHint : (indent + 2);
            while (i < lines.size()) {
                final String raw = stripComment(lines.get(i));
                if (raw.trim().isEmpty()) {
                    i++;
                    continue;
                }
                final int ind = countIndent(raw);
                final String t = raw.trim();
                if (ind <= indent && t.startsWith("- ")) {
                    break; // next vein
                }
                if (ind < baseIndent) {
                    break;
                }

                final ParsedKeyValue kv = parseKeyValue(t);
                if (kv == null) {
                    LOGGER.warn("Skipping malformed line for {} in {}: {}", id, yamlPath, t);
                    i++;
                    continue;
                }

                // Block keys that are lists or nested maps.
                if ("rocks".equals(kv.key) && kv.value.isEmpty()) {
                    i++;
                    final List<String> rocks = new ArrayList<>();
                    while (i < lines.size()) {
                        final String r = stripComment(lines.get(i));
                        if (r.trim().isEmpty()) {
                            i++;
                            continue;
                        }
                        final int rind = countIndent(r);
                        final String rt = r.trim();
                        if (rind < ind) {
                            break;
                        }
                        if (!rt.startsWith("- ")) {
                            break;
                        }
                        rocks.add(rt.substring(2).trim());
                        i++;
                    }
                    props.put("rocks", rocks);
                    continue;
                }

                if ("tier".equals(kv.key) && kv.value.isEmpty()) {
                    i++;
                    final LinkedHashMap<String, Integer> tiers = new LinkedHashMap<>();
                    while (i < lines.size()) {
                        final String r = stripComment(lines.get(i));
                        if (r.trim().isEmpty()) {
                            i++;
                            continue;
                        }
                        final int rind = countIndent(r);
                        final String rt = r.trim();
                        if (rind < ind) {
                            break;
                        }
                        if (!rt.startsWith("- ")) {
                            break;
                        }
                        final String item = rt.substring(2).trim();
                        final ParsedKeyValue ikv = parseKeyValue(item);
                        if (ikv != null) {
                            try {
                                tiers.put(ikv.key, Integer.parseInt(ikv.value));
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Invalid tier weight '{}' for tier '{}' in {}", ikv.value, ikv.key, id);
                            }
                        }
                        i++;
                    }
                    props.put("tier", tiers);
                    continue;
                }

                if ("indicator".equals(kv.key) && kv.value.isEmpty()) {
                    i++;
                    final LinkedHashMap<String, String> indProps = new LinkedHashMap<>();
                    while (i < lines.size()) {
                        final String r = stripComment(lines.get(i));
                        if (r.trim().isEmpty()) {
                            i++;
                            continue;
                        }
                        final int rind = countIndent(r);
                        final String rt = r.trim();
                        if (rind <= ind) {
                            break;
                        }
                        final ParsedKeyValue ikv = parseKeyValue(rt);
                        if (ikv != null) {
                            indProps.put(ikv.key, ikv.value);
                        }
                        i++;
                    }
                    props.put("indicator", indProps);
                    continue;
                }

                props.put(kv.key, kv.value);
                i++;
            }

            final VeinDefinition def = toDefinition(id, props, yamlPath, defaultRocks);
            if (def != null) {
                defs.add(def);
            }
        }

        return List.copyOf(defs);
    }

    private static VeinDefinition toDefinition(ResourceLocation id, LinkedHashMap<String, Object> props, Path owner, List<String> defaultRocks) {
        try {
            final String block = requireString(props, "block", id);
            final ResourceLocation type = requireResourceLocation(props, "type", id);

            final int minY = getInt(props, id, "ymin", "min_y");
            final int maxY = getInt(props, id, "ymax", "max_y");
            final int rarity = requireInt(props, "rarity", id);
            final float density = requireFloat(props, "density", id);
            final boolean isPipe = "tfc:pipe_vein".equals(type.toString());
            final int size = isPipe ? (props.containsKey("size") ? requireInt(props, "size", id) : 0) : requireInt(props, "size", id);

            @SuppressWarnings("unchecked")
            List<String> rocks = (List<String>) props.getOrDefault("rocks", List.of());
            if (rocks.isEmpty()) {
                if (defaultRocks == null || defaultRocks.isEmpty()) {
                    LOGGER.warn("Missing rocks for {} in {}", id, owner);
                    return null;
                }
                rocks = defaultRocks;
            }

            @SuppressWarnings("unchecked")
            final LinkedHashMap<String, Integer> tiers = (LinkedHashMap<String, Integer>) props.getOrDefault("tier", new LinkedHashMap<>());
            if (tiers.isEmpty()) {
                LOGGER.warn("Missing tier weights for {} in {}", id, owner);
                return null;
            }

            final boolean project = getBool(props, "project", false);
            final boolean projectOffset = getBool(props, "project_offset", false);
            final boolean nearLava = getBool(props, "near_lava", false);

            final String randomName = Optional.ofNullable((String) props.get("random_name")).orElseGet(() -> lastPathSegment(id));
            final Long seedOverride = props.containsKey("seed") ? Long.valueOf(requireString(props, "seed", id)) : null;

            final Integer height = props.containsKey("height") ? Integer.valueOf(requireInt(props, "height", id)) : null;

            final PipeParams pipeParams = parsePipeParams(props);

            final IndicatorDefinition indicator = parseIndicator(props.get("indicator"));

            return new VeinDefinition(
                id,
                block,
                type,
                minY,
                maxY,
                rarity,
                density,
                size,
                height,
                pipeParams,
                List.copyOf(rocks),
                new LinkedHashMap<>(tiers),
                indicator,
                project,
                projectOffset,
                nearLava,
                randomName,
                seedOverride
            );
        } catch (Exception e) {
            LOGGER.warn("Failed to build vein definition for {}: {}", id, e.getMessage());
            return null;
        }
    }

    private static PipeParams parsePipeParams(Map<String, Object> props) {
        if (!props.containsKey("pipe_height") && !props.containsKey("height")) {
            return null;
        }
        // Accept either explicit pipe_* keys, or fall back to generic names when type is pipe_vein.
        final int height = getInt(props, null, "pipe_height", "height");
        final int radius = getInt(props, null, "pipe_radius", "radius");
        final int minSkew = getInt(props, null, "min_skew", "pipe_min_skew");
        final int maxSkew = getInt(props, null, "max_skew", "pipe_max_skew");
        final int minSlant = getInt(props, null, "min_slant", "pipe_min_slant");
        final int maxSlant = getInt(props, null, "max_slant", "pipe_max_slant");
        final float sign = props.containsKey("sign") ? Float.parseFloat(props.get("sign").toString()) : 1.0f;
        return new PipeParams(height, radius, minSkew, maxSkew, minSlant, maxSlant, sign);
    }

    private static IndicatorDefinition parseIndicator(Object obj) {
        if (!(obj instanceof Map<?, ?> map)) {
            return null;
        }
        final Object blockObj = map.get("block");
        final String block = blockObj == null ? "" : blockObj.toString();
        if (block.isEmpty()) {
            return null;
        }
        final int depth = parseInt(map.get("depth"), 0);
        final int rarity = parseInt(map.get("rarity"), 0);
        final int undergroundRarity = parseInt(map.get("underground_rarity"), 0);
        final int undergroundCount = parseInt(map.get("underground_count"), 0);
        return new IndicatorDefinition(block, depth, rarity, undergroundRarity, undergroundCount);
    }

    private static int getInt(Map<String, Object> props, ResourceLocation id, String primary, String fallback) {
        if (props.containsKey(primary)) {
            return parseInt(props.get(primary), 0);
        }
        if (props.containsKey(fallback)) {
            return parseInt(props.get(fallback), 0);
        }
        if (id != null) {
            throw new IllegalArgumentException("Missing int field '" + primary + "' for " + id);
        }
        return 0;
    }

    private static int requireInt(Map<String, Object> props, String key, ResourceLocation id) {
        if (!props.containsKey(key)) {
            throw new IllegalArgumentException("Missing int field '" + key + "' for " + id);
        }
        return parseInt(props.get(key), 0);
    }

    private static float requireFloat(Map<String, Object> props, String key, ResourceLocation id) {
        if (!props.containsKey(key)) {
            throw new IllegalArgumentException("Missing float field '" + key + "' for " + id);
        }
        final String s = props.get(key).toString();
        return Float.parseFloat(s);
    }

    private static String requireString(Map<String, Object> props, String key, ResourceLocation id) {
        final Object v = props.get(key);
        if (v == null) {
            throw new IllegalArgumentException("Missing field '" + key + "' for " + id);
        }
        return v.toString();
    }

    private static ResourceLocation requireResourceLocation(Map<String, Object> props, String key, ResourceLocation id) {
        final String s = requireString(props, key, id);
        final ResourceLocation rl = ResourceLocation.tryParse(s);
        if (rl == null) {
            throw new IllegalArgumentException("Invalid resource location '" + s + "' for " + id);
        }
        return rl;
    }

    private static boolean getBool(Map<String, Object> props, String key, boolean def) {
        if (!props.containsKey(key)) {
            return def;
        }
        final String s = props.get(key).toString().trim().toLowerCase(Locale.ROOT);
        return "true".equals(s) || "1".equals(s) || "yes".equals(s);
    }

    private static int parseInt(Object v, int def) {
        if (v == null) {
            return def;
        }
        final String s = v.toString().trim();
        if (s.isEmpty()) {
            return def;
        }
        return Integer.parseInt(s);
    }

    private static String stripComment(String line) {
        final int idx = line.indexOf('#');
        if (idx < 0) {
            return line;
        }
        return line.substring(0, idx);
    }

    private static int countIndent(String line) {
        int i = 0;
        while (i < line.length() && line.charAt(i) == ' ') {
            i++;
        }
        return i;
    }

    private static ParsedKeyValue parseKeyValue(String trimmed) {
        final int idx = trimmed.indexOf(':');
        if (idx <= 0) {
            return null;
        }
        final String key = trimmed.substring(0, idx).trim();
        final String value = trimmed.substring(idx + 1).trim();
        return new ParsedKeyValue(key, value);
    }

    private record ParsedKeyValue(String key, String value) {
    }

    private static ParsedListKey parseListKey(String trimmed) {
        // "- namespace:path:" (note: key itself includes a colon between namespace and path)
        if (!trimmed.startsWith("- ") || !trimmed.endsWith(":")) {
            return null;
        }
        final String key = trimmed.substring(2, trimmed.length() - 1).trim();
        if (key.isEmpty()) {
            return null;
        }
        return new ParsedListKey(key, 0);
    }

    private record ParsedListKey(String key, int nextIndentHint) {
    }

    private static String lastPathSegment(ResourceLocation id) {
        final String path = id.getPath();
        final int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    private static Block resolveReplaceBlock(String rockToken) {
        final String t = rockToken.trim();
        final ResourceLocation id;
        if (t.contains(":")) {
            final ResourceLocation rl = ResourceLocation.tryParse(t);
            id = rl;
        } else if ("netherrack".equals(t)) {
            id = ResourceLocation.fromNamespaceAndPath("minecraft", "netherrack");
        } else if ("endstone".equals(t) || "end_stone".equals(t)) {
            id = ResourceLocation.fromNamespaceAndPath("minecraft", "end_stone");
        } else {
            id = ResourceLocation.fromNamespaceAndPath("tfc", "rock/raw/" + t);
        }

        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
            return Blocks.AIR;
        }
        return BuiltInRegistries.BLOCK.get(id);
    }

    private static String resolveOreRockToken(String rockToken) {
        final String t = rockToken.trim();
        if ("end_stone".equals(t) || "endstone".equals(t) || "minecraft:end_stone".equals(t)) {
            return "endstone";
        }
        if ("minecraft:netherrack".equals(t)) {
            return "netherrack";
        }
        if (!t.contains(":")) {
            return t;
        }
        final ResourceLocation rl = ResourceLocation.tryParse(t);
        if (rl == null) {
            return t;
        }
        final String path = rl.getPath();
        final int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    static long hashSeed(String randomName) {
        final RandomSupport.Seed128bit seed = RandomSupport.seedFromHashOf(randomName);
        return seed.seedLo() ^ seed.seedHi();
    }
}
