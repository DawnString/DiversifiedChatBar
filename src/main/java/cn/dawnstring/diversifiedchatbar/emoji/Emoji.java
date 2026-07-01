package cn.dawnstring.diversifiedchatbar.emoji;

import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.UUID;

public class Emoji
{
    public enum Source { LOCAL, SERVER }

    private final String shortcode;
    private final ResourceLocation textureLocation;
    private final int width;
    private final int height;
    private final Path filePath;
    private final Source source;
    private final UUID ownerUUID;
    private final boolean animated;

    public Emoji(String shortcode, ResourceLocation textureLocation, int width, int height, Path filePath)
    {
        this(shortcode, textureLocation, width, height, filePath, Source.LOCAL, null, false);
    }

    public Emoji(String shortcode, ResourceLocation textureLocation, int width, int height, Path filePath, Source source, UUID ownerUUID)
    {
        this(shortcode, textureLocation, width, height, filePath, source, ownerUUID, false);
    }

    public Emoji(String shortcode, ResourceLocation textureLocation, int width, int height, Path filePath, Source source, UUID ownerUUID, boolean animated)
    {
        this.shortcode = shortcode;
        this.textureLocation = textureLocation;
        this.width = width;
        this.height = height;
        this.filePath = filePath;
        this.source = source;
        this.ownerUUID = ownerUUID;
        this.animated = animated;
    }

    public String shortcode() { return shortcode; }
    public ResourceLocation textureLocation() { return textureLocation; }
    public int width() { return width; }
    public int height() { return height; }
    public Path filePath() { return filePath; }
    public Source source() { return source; }
    public UUID ownerUUID() { return ownerUUID; }
    public boolean isAnimated() { return animated; }
}
