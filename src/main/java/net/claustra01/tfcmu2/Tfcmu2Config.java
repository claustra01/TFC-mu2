package net.claustra01.tfcmu2;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Tfcmu2Config {
    public static final ModConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        COMMON = new Common(builder);
        COMMON_SPEC = builder.build();
    }

    public static final class Common {
        public final ModConfigSpec.BooleanValue enableCustomVeinGeneration;

        private Common(ModConfigSpec.Builder builder) {
            builder.push("worldgen");
            enableCustomVeinGeneration = builder
                .comment(
                    "If true, TFMCU2 will replace TFC's '#tfc:in_biome/veins' placed features with veins loaded from",
                    "config/tfcmu2/{overworld,nether,end}.yaml. Changes require a full game restart."
                )
                .define("enableCustomVeinGeneration", false);
            builder.pop();
        }
    }

    private Tfcmu2Config() {
    }
}

