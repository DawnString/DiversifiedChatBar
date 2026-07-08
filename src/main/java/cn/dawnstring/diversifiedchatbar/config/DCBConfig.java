package cn.dawnstring.diversifiedchatbar.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class DCBConfig
{
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;


    public static volatile int runtimeChatLinesOverride = 0;
    public static volatile double runtimeScreenFractionOverride = 0.0;
    public static volatile double runtimeChatScaleOverride = 0.0;

    public static int getEffectiveChatLines()
    {
        return runtimeChatLinesOverride > 0 ? runtimeChatLinesOverride : EMOJI_CHAT_LINES.get();
    }

    public static double getEffectiveScreenFraction()
    {
        return runtimeScreenFractionOverride > 0.0 ? runtimeScreenFractionOverride : EMOJI_SCREEN_FRACTION.get();
    }

    public static double getEffectiveChatScale()
    {
        return runtimeChatScaleOverride > 0.0 ? runtimeChatScaleOverride : EMOJI_CHAT_SCALE.get();
    }

    public static final ModConfigSpec.ConfigValue<String> EMOJI_FOLDER;
    public static final ModConfigSpec.DoubleValue EMOJI_SCALE;
    public static final ModConfigSpec.IntValue EMOJI_CHAT_LINES;
    public static final ModConfigSpec.DoubleValue EMOJI_SCREEN_FRACTION;
    public static final ModConfigSpec.DoubleValue EMOJI_CHAT_SCALE;

    public final String emojiFolder;
    public final double emojiScale;
    public final int emojiChatLines;
    public final double emojiScreenFraction;
    public final double emojiChatScale;

    static
    {
        BUILDER.push("emoji");

        EMOJI_FOLDER = BUILDER
                .comment("Path to the emoji folder (relative to game directory or config directory)")
                .define("folder", "emoji");

        EMOJI_SCALE = BUILDER
                .comment("Scale factor for emoji rendering (0.25 - 4.0)")
                .defineInRange("scale", 1.0, 0.25, 4.0);

        EMOJI_CHAT_LINES = BUILDER
                .comment("Number of chat lines reserved for emoji in HUD (2-12)")
                .defineInRange("chatLines", 6, 2, 12);

        EMOJI_SCREEN_FRACTION = BUILDER
                .comment("Fraction of screen height for emoji in chat screen (0.1-0.5)")
                .defineInRange("screenFraction", 0.2, 0.1, 0.5);

        EMOJI_CHAT_SCALE = BUILDER
                .comment("Overall scale multiplier for emoji display size (0.25-3.0)")
                .defineInRange("chatScale", 1.0, 0.25, 3.0);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public DCBConfig(ModConfigSpec spec)
    {
        emojiFolder = EMOJI_FOLDER.get();
        emojiScale = EMOJI_SCALE.get();
        emojiChatLines = EMOJI_CHAT_LINES.get();
        emojiScreenFraction = EMOJI_SCREEN_FRACTION.get();
        emojiChatScale = EMOJI_CHAT_SCALE.get();
    }
}
