package org.aliebashy.svctts;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
        public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
        public static final ForgeConfigSpec CLIENT_CONFIG;

        public static final ForgeConfigSpec.ConfigValue<String> VOICE;
        public static final ForgeConfigSpec.DoubleValue VOLUME;
        public static final ForgeConfigSpec.DoubleValue LENGTH;
        public static final ForgeConfigSpec.DoubleValue VARIATION;
        public static final ForgeConfigSpec.DoubleValue W_VARIATION;
        public static final ForgeConfigSpec.BooleanValue NORMALIZE;
        public static final ForgeConfigSpec.ConfigValue<String> PYTHON_PATH;

        static {
                BUILDER.push("SVCTTS");

                VOICE = BUILDER.comment("Selected Piper voice model")
                        .define("voice", "en_US-lessac-medium");

                VOLUME = BUILDER.comment("Volume scale (0.0 - 2.0)")
                        .defineInRange("volume", 1.0, 0.0, 2.0);

                LENGTH = BUILDER.comment("Length scale (speech speed)")
                        .defineInRange("length", 1.0, 0.5, 3.0);

                VARIATION = BUILDER.comment("Noise scale (variation)")
                        .defineInRange("variation", 1.0, 0.0, 3.0);

                W_VARIATION = BUILDER.comment("Noise W scale")
                        .defineInRange("w_variation", 1.0, 0.0, 3.0);

                NORMALIZE = BUILDER.comment("Normalize output audio")
                        .define("normalize", false);

                PYTHON_PATH = BUILDER.comment("Full path to python executable (optional)")
                        .define("pythonPath", "");

                BUILDER.pop();
                CLIENT_CONFIG = BUILDER.build();
                }
}