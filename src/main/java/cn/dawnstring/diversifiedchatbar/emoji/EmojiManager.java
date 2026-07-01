package cn.dawnstring.diversifiedchatbar.emoji;

import cn.dawnstring.diversifiedchatbar.DiversifiedChatBar;
import cn.dawnstring.diversifiedchatbar.config.DCBConfig;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EmojiManager
{
    private static EmojiManager instance;
    public static final int MAX_SIZE = 256;

    private final Map<String, Emoji> emojiMap = new ConcurrentHashMap<>();
    private final List<Emoji> emojiList = new ArrayList<>();
    private final Map<String, AnimatedEmojiData> animatedEmojis = new HashMap<>();
    private boolean loaded = false;
    private Path emojiFolderPath = null;
    private DCBConfig config;

    private EmojiManager() {}

    public static EmojiManager getInstance()
    {
        if (instance == null) instance = new EmojiManager();
        return instance;
    }

    public void load(DCBConfig config)
    {
        this.config = config;
        loaded = false;
        emojiMap.clear();
        emojiList.clear();
        animatedEmojis.clear();
        emojiFolderPath = Minecraft.getInstance().gameDirectory.toPath().resolve(config.emojiFolder);
        ensureFolder();
        scanAndLoad();
        loaded = true;
    }

    public void reload()
    {
        if (config == null) return;
        
        Minecraft mc = Minecraft.getInstance();
        for (Emoji emoji : emojiList)
        {
            mc.getTextureManager().release(emoji.textureLocation());
        }
        animatedEmojis.clear();
        load(config);
    }

    public boolean addEmojiFromSource(Path sourcePath, String preferredName)
    {
        try
        {
            ensureFolder();
            String fileName = preferredName != null ? preferredName :
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("'emoji_'yyyyMMdd_HHmmss"));

            String lowerName = sourcePath.getFileName().toString().toLowerCase();
            boolean isGif = lowerName.endsWith(".gif");

            if (!isGif)
            {
                
                if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg"))
                {
                    fileName += ".png";
                }
                else if (lowerName.endsWith(".png"))
                {
                    fileName += ".png";
                }
                else
                {
                    fileName += ".png";
                }
            }
            else
            {
                fileName += ".gif";
            }

            fileName = fileName.toLowerCase();

            Path dest = emojiFolderPath.resolve(fileName);
            int counter = 1;
            while (Files.exists(dest))
            {
                String base = fileName.substring(0, fileName.length() - 4);
                dest = emojiFolderPath.resolve(base + "_" + counter + ".png");
                counter++;
                
                if (isGif)
                {
                    dest = emojiFolderPath.resolve(base + "_" + counter + ".gif");
                }
            }

            if (sourcePath.getFileName().toString().toLowerCase().endsWith(".gif"))
            {
                Files.copy(sourcePath, dest, StandardCopyOption.REPLACE_EXISTING);
                return loadGifEmoji(dest.toFile(), emojiList.size());
            }
            else if (lowerName.endsWith(".png"))
            {
                Files.copy(sourcePath, dest, StandardCopyOption.REPLACE_EXISTING);
                return loadSingleEmoji(dest.toFile(), emojiList.size());
            }
            else
            {
                
                BufferedImage bi = ImageIO.read(sourcePath.toFile());
                if (bi == null)
                {
                    DiversifiedChatBar.LOGGER.error("Failed to decode image: {}", sourcePath);
                    return false;
                }
                BufferedImage argb = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
                argb.getGraphics().drawImage(bi, 0, 0, null);
                ImageIO.write(argb, "PNG", dest.toFile());
                return loadSingleEmoji(dest.toFile(), emojiList.size());
            }
        }
        catch (IOException e)
        {
            DiversifiedChatBar.LOGGER.error("Failed to add emoji: {}", e.getMessage());
            return false;
        }
    }

    public boolean removeEmoji(Emoji emoji)
    {
        try
        {
            Files.deleteIfExists(emoji.filePath());
            Minecraft.getInstance().getTextureManager().release(emoji.textureLocation());
            emojiMap.remove(emoji.shortcode().toLowerCase());
            emojiList.remove(emoji);
            animatedEmojis.remove(emoji.shortcode().toLowerCase());
            DiversifiedChatBar.LOGGER.info("Removed emoji: {}", emoji.shortcode());
            return true;
        }
        catch (IOException e)
        {
            DiversifiedChatBar.LOGGER.error("Failed to remove emoji {}: {}", emoji.shortcode(), e.getMessage());
            return false;
        }
    }

    private void ensureFolder()
    {
        if (emojiFolderPath == null) return;
        try
        {
            Files.createDirectories(emojiFolderPath);
        }
        catch (IOException e)
        {
            DiversifiedChatBar.LOGGER.error("Failed to create emoji folder: {}", e.getMessage());
        }
    }

    private void scanAndLoad()
    {
        if (emojiFolderPath == null || !Files.isDirectory(emojiFolderPath)) return;
        File[] files = emojiFolderPath.toFile().listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".gif");
        });
        if (files == null) return;
        int index = 0;
        for (File file : files)
        {
            boolean loaded;
            if (file.getName().toLowerCase().endsWith(".gif"))
            {
                loaded = loadGifEmoji(file, index);
            }
            else
            {
                loaded = loadSingleEmoji(file, index);
            }
            if (loaded) index++;
        }
        DiversifiedChatBar.LOGGER.info("Loaded {} emoji(s) from {} ({} animated)", emojiList.size(), emojiFolderPath, animatedEmojis.size());
    }

    private boolean loadSingleEmoji(File file, int index)
    {
        if (config == null) return false;
        String fileName = file.getName();
        String shortcode = fileName.substring(0, fileName.length() - 4)
                .toLowerCase()
                .replaceAll("[^a-z0-9/._-]", "_");
        Minecraft mc = Minecraft.getInstance();

        try (var inputStream = Files.newInputStream(file.toPath()))
        {
            NativeImage image = NativeImage.read(inputStream);
            NativeImage processed = compressIfNeeded(image);
            int w = processed.getWidth();
            int h = processed.getHeight();

            ResourceLocation texLoc = ResourceLocation.fromNamespaceAndPath(
                    DiversifiedChatBar.MODID, "emoji/" + shortcode + "_" + index);

            DynamicTexture dynamicTexture = new DynamicTexture(processed);
            mc.getTextureManager().register(texLoc, dynamicTexture);

            Emoji emoji = new Emoji(shortcode, texLoc, w, h, file.toPath());
            emojiMap.put(shortcode.toLowerCase(), emoji);
            emojiList.add(emoji);
            DiversifiedChatBar.LOGGER.debug("Loaded emoji: {} ({}x{})", shortcode, w, h);
            return true;
        }
        catch (IOException e)
        {
            DiversifiedChatBar.LOGGER.error("Failed to load emoji {}: {}", fileName, e.getMessage());
            return false;
        }
    }

    private boolean loadGifEmoji(File file, int index)
    {
        if (config == null) return false;
        String fileName = file.getName();
        String shortcode = fileName.substring(0, fileName.length() - 4)
                .toLowerCase()
                .replaceAll("[^a-z0-9/._-]", "_");

        try
        {
            ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
            try (ImageInputStream stream = ImageIO.createImageInputStream(file))
            {
                reader.setInput(stream);
                int count = reader.getNumImages(true);
                if (count <= 1)
                {
                    
                    BufferedImage bi = reader.read(0);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(bi, "PNG", baos);
                    try (var in = new ByteArrayInputStream(baos.toByteArray()))
                    {
                        NativeImage image = NativeImage.read(in);
                        return loadSingleNativeImage(image, shortcode, index, file.toPath());
                    }
                }

                NativeImage[] frames = new NativeImage[count];
                int[] delays = new int[count];

                
                int canvasW = reader.getWidth(0);
                int canvasH = reader.getHeight(0);
                for (int i = 1; i < count; i++)
                {
                    canvasW = Math.max(canvasW, reader.getWidth(i));
                    canvasH = Math.max(canvasH, reader.getHeight(i));
                }

                BufferedImage canvas = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_ARGB);

                for (int i = 0; i < count; i++)
                {
                    int disposal = getDisposalMethod(reader, i);
                    int[] pos = getFramePosition(reader, i);

                    BufferedImage rawFrame = reader.read(i);
                    int fw = rawFrame.getWidth();
                    int fh = rawFrame.getHeight();

                    
                    
                    if (disposal == 0)
                    {
                        Graphics2D g0 = canvas.createGraphics();
                        g0.setComposite(AlphaComposite.Clear);
                        g0.fillRect(pos[0], pos[1], fw, fh);
                        g0.dispose();
                    }

                    BufferedImage saved = (disposal == 3) ? copyBufferedImage(canvas) : null;

                    Graphics2D g = canvas.createGraphics();
                    g.drawImage(rawFrame, pos[0], pos[1], null);
                    g.dispose();

                    frames[i] = biToNativeImage(canvas);
                    delays[i] = getGifDelay(reader, i);

                    if (disposal == 2)
                    {
                        g = canvas.createGraphics();
                        g.setComposite(AlphaComposite.Clear);
                        g.fillRect(pos[0], pos[1], fw, fh);
                        g.dispose();
                    }
                    else if (disposal == 3)
                    {
                        canvas = saved;
                    }
                }

                
                boolean needsResize = canvasW > MAX_SIZE || canvasH > MAX_SIZE;
                if (needsResize)
                {
                    for (int i = 0; i < count; i++)
                    {
                        frames[i] = compressNativeImage(frames[i]);
                    }
                    canvasW = frames[0].getWidth();
                    canvasH = frames[0].getHeight();
                }

                int w = frames[0].getWidth();
                int h = frames[0].getHeight();

                ResourceLocation texLoc = ResourceLocation.fromNamespaceAndPath(
                        DiversifiedChatBar.MODID, "emoji/" + shortcode + "_" + index);

                
                NativeImage firstCopy = copyNativeImage(frames[0]);
                DynamicTexture dynTex = new DynamicTexture(firstCopy);
                Minecraft mc = Minecraft.getInstance();
                mc.getTextureManager().register(texLoc, dynTex);

                Emoji emoji = new Emoji(shortcode, texLoc, w, h, file.toPath(), Emoji.Source.LOCAL, null, true);
                emojiMap.put(shortcode.toLowerCase(), emoji);
                emojiList.add(emoji);
                animatedEmojis.put(shortcode.toLowerCase(), new AnimatedEmojiData(frames, delays, texLoc, dynTex, w, h));

                DiversifiedChatBar.LOGGER.debug("Loaded animated emoji: {} ({} frames, {}x{})", shortcode, count, w, h);
                return true;
            }
        }
        catch (IOException | IllegalArgumentException e)
        {
            DiversifiedChatBar.LOGGER.error("Failed to load GIF emoji {}: {}", fileName, e.getMessage());
            return false;
        }
    }

    
    public void loadServerGif(java.util.UUID owner, String shortcode, byte[] gifData) throws IOException
    {
        ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(gifData)))
        {
            reader.setInput(stream);
            int count = reader.getNumImages(true);
            if (count <= 1)
            {
                    
                BufferedImage bi = reader.read(0);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bi, "PNG", baos);
                try (var in = new ByteArrayInputStream(baos.toByteArray()))
                {
                    NativeImage image = NativeImage.read(in);
                    NativeImage processed = compressIfNeededStatic(image);
                    String texPath = "server_emoji/" + owner + "_" + shortcode;
                    ResourceLocation texLoc = ResourceLocation.fromNamespaceAndPath(DiversifiedChatBar.MODID, texPath);
                    DynamicTexture tex = new DynamicTexture(processed);
                    Minecraft.getInstance().getTextureManager().register(texLoc, tex);
                    Emoji emoji = new Emoji(shortcode, texLoc, processed.getWidth(), processed.getHeight(),
                            null, Emoji.Source.SERVER, owner, false);
                    addServerEmoji(emoji);
                }
                return;
            }

            NativeImage[] frames = new NativeImage[count];
            int[] delays = new int[count];

            
            int canvasW = reader.getWidth(0);
            int canvasH = reader.getHeight(0);
            for (int i = 1; i < count; i++)
            {
                canvasW = Math.max(canvasW, reader.getWidth(i));
                canvasH = Math.max(canvasH, reader.getHeight(i));
            }

            BufferedImage canvas = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_ARGB);

            for (int i = 0; i < count; i++)
            {
                int disposal = getDisposalMethod(reader, i);
                int[] pos = getFramePosition(reader, i);

                BufferedImage rawFrame = reader.read(i);
                int fw = rawFrame.getWidth();
                int fh = rawFrame.getHeight();

                if (disposal == 0)
                {
                    Graphics2D g0 = canvas.createGraphics();
                    g0.setComposite(AlphaComposite.Clear);
                    g0.fillRect(pos[0], pos[1], fw, fh);
                    g0.dispose();
                }

                BufferedImage saved = (disposal == 3) ? copyBufferedImage(canvas) : null;

                Graphics2D g = canvas.createGraphics();
                g.drawImage(rawFrame, pos[0], pos[1], null);
                g.dispose();

                frames[i] = biToNativeImage(canvas);
                delays[i] = getGifDelay(reader, i);

                if (disposal == 2)
                {
                    g = canvas.createGraphics();
                    g.setComposite(AlphaComposite.Clear);
                    g.fillRect(pos[0], pos[1], fw, fh);
                    g.dispose();
                }
                else if (disposal == 3)
                {
                    canvas = saved;
                }
            }

            
            if (canvasW > MAX_SIZE || canvasH > MAX_SIZE)
            {
                for (int i = 0; i < count; i++)
                {
                    frames[i] = compressNativeImage(frames[i]);
                }
                canvasW = frames[0].getWidth();
                canvasH = frames[0].getHeight();
            }

            String texPath = "server_emoji/" + owner + "_" + shortcode;
            ResourceLocation texLoc = ResourceLocation.fromNamespaceAndPath(DiversifiedChatBar.MODID, texPath);
            NativeImage firstCopy = copyNativeImage(frames[0]);
            DynamicTexture dynTex = new DynamicTexture(firstCopy);
            Minecraft.getInstance().getTextureManager().register(texLoc, dynTex);

            Emoji emoji = new Emoji(shortcode, texLoc, canvasW, canvasH, null, Emoji.Source.SERVER, owner, true);
            addServerEmoji(emoji);
            animatedEmojis.put(shortcode.toLowerCase(), new AnimatedEmojiData(frames, delays, texLoc, dynTex, canvasW, canvasH));
        }
    }

    
    public void updateAnimatedTexture(Emoji emoji)
    {
        if (!emoji.isAnimated()) return;
        AnimatedEmojiData data = animatedEmojis.get(emoji.shortcode().toLowerCase());
        if (data == null) return;

        long now = System.currentTimeMillis();
        if (now < data.nextFrameTime) return;

        data.currentFrame = (data.currentFrame + 1) % data.frames.length;
        data.nextFrameTime = now + data.delays[data.currentFrame];

        
        NativeImage src = data.frames[data.currentFrame];
        NativeImage copy = copyNativeImage(src);
        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager().release(data.textureLocation);
        DynamicTexture newTex = new DynamicTexture(copy);
        mc.getTextureManager().register(data.textureLocation, newTex);
        data.dynamicTexture = newTex;
    }

    public void clearServerEmoji()
    {
        Minecraft mc = Minecraft.getInstance();
        Iterator<Emoji> iter = emojiList.iterator();
        while (iter.hasNext())
        {
            Emoji e = iter.next();
            if (e.source() == Emoji.Source.SERVER)
            {
                mc.getTextureManager().release(e.textureLocation());
                emojiMap.remove(e.shortcode().toLowerCase());
                animatedEmojis.remove(e.shortcode().toLowerCase());
                iter.remove();
            }
        }
    }

    public void addServerEmoji(Emoji emoji)
    {
        emojiMap.put(emoji.shortcode().toLowerCase(), emoji);
        emojiList.add(emoji);
    }

    

    private NativeImage compressIfNeeded(NativeImage image)
    {
        int w = image.getWidth();
        int h = image.getHeight();
        if (w <= MAX_SIZE && h <= MAX_SIZE) return image;

        NativeImage resized = resizeNativeImage(image, w, h);
        if (image != resized) image.close();
        return resized;
    }

    public static NativeImage compressIfNeededStatic(NativeImage image)
    {
        int w = image.getWidth();
        int h = image.getHeight();
        if (w <= MAX_SIZE && h <= MAX_SIZE) return image;

        NativeImage resized = resizeNativeImage(image, w, h);
        if (image != resized) image.close();
        return resized;
    }

    private static NativeImage resizeNativeImage(NativeImage image, int w, int h)
    {
        float scale;
        if (w > h)
        {
            scale = (float) MAX_SIZE / w;
        }
        else
        {
            scale = (float) MAX_SIZE / h;
        }
        int newW = Math.max(1, Math.round(w * scale));
        int newH = Math.max(1, Math.round(h * scale));

        DiversifiedChatBar.LOGGER.info("Compressing emoji from {}x{} to {}x{}", w, h, newW, newH);
        NativeImage resized = new NativeImage(newW, newH, false);
        for (int y = 0; y < newH; y++)
        {
            for (int x = 0; x < newW; x++)
            {
                int sx = Math.min(x * w / newW, w - 1);
                int sy = Math.min(y * h / newH, h - 1);
                resized.setPixelRGBA(x, y, image.getPixelRGBA(sx, sy));
            }
        }
        return resized;
    }

    private NativeImage compressNativeImage(NativeImage image)
    {
        return resizeNativeImage(image, image.getWidth(), image.getHeight());
    }

    private static NativeImage copyNativeImage(NativeImage src)
    {
        int w = src.getWidth();
        int h = src.getHeight();
        NativeImage copy = new NativeImage(w, h, true);
        for (int y = 0; y < h; y++)
        {
            for (int x = 0; x < w; x++)
            {
                copy.setPixelRGBA(x, y, src.getPixelRGBA(x, y));
            }
        }
        return copy;
    }

    private static NativeImage padToSize(NativeImage image, int targetW, int targetH)
    {
        int w = image.getWidth();
        int h = image.getHeight();
        if (w == targetW && h == targetH) return image;
        NativeImage padded = new NativeImage(targetW, targetH, true);
        for (int y = 0; y < h && y < targetH; y++)
        {
            for (int x = 0; x < w && x < targetW; x++)
            {
                padded.setPixelRGBA(x, y, image.getPixelRGBA(x, y));
            }
        }
        if (padded != image) image.close();
        return padded;
    }

    
    private static NativeImage biToNativeImage(BufferedImage bi) throws IOException
    {
        if (bi.getType() != BufferedImage.TYPE_INT_ARGB)
        {
            BufferedImage argb = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
            argb.getGraphics().drawImage(bi, 0, 0, null);
            bi = argb;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi, "PNG", baos);
        return NativeImage.read(new ByteArrayInputStream(baos.toByteArray()));
    }

    
    private static int getGifDelay(ImageReader reader, int index) throws IOException
    {
        try
        {
            IIOMetadata metadata = reader.getImageMetadata(index);
            String[] formats = metadata.getMetadataFormatNames();
            if (formats == null) return 100;
            for (String fmt : formats)
            {
                IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(fmt);
                int delay = findDelayInNode(root);
                if (delay > 0) return Math.max(20, delay); 
            }
        }
        catch (Exception ignored)
        {
        }
        return 100;
    }

    private static int findDelayInNode(IIOMetadataNode node)
    {
        if ("GraphicControlExtension".equals(node.getNodeName()))
        {
            String delay = node.getAttribute("delayTime");
            if (delay != null && !delay.isEmpty())
            {
                try
                {
                    return Integer.parseInt(delay) * 10; 
                }
                catch (NumberFormatException ignored)
                {
                }
            }
        }
        for (int i = 0; i < node.getLength(); i++)
        {
            var child = node.item(i);
            if (child instanceof IIOMetadataNode metaChild)
            {
                int delay = findDelayInNode(metaChild);
                if (delay > 0) return delay;
            }
        }
        return -1;
    }

    
    private static int[] getFramePosition(ImageReader reader, int index) throws IOException
    {
        try
        {
            IIOMetadata metadata = reader.getImageMetadata(index);
            String[] formats = metadata.getMetadataFormatNames();
            if (formats == null) return new int[]{0, 0};
            for (String fmt : formats)
            {
                IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(fmt);
                for (int i = 0; i < root.getLength(); i++)
                {
                    var child = root.item(i);
                    if (child instanceof IIOMetadataNode metaChild
                            && "ImageDescriptor".equals(metaChild.getNodeName()))
                            {
                        int x = parseInt(metaChild.getAttribute("imageLeftPosition"), 0);
                        int y = parseInt(metaChild.getAttribute("imageTopPosition"), 0);
                        return new int[]{x, y};
                    }
                }
            }
        }
        catch (Exception ignored)
        {
        }
        return new int[]{0, 0};
    }


    private static int getDisposalMethod(ImageReader reader, int index) throws IOException
    {
        try
        {
            IIOMetadata metadata = reader.getImageMetadata(index);
            String[] formats = metadata.getMetadataFormatNames();
            if (formats == null) return 0;
            for (String fmt : formats)
            {
                IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(fmt);
                for (int i = 0; i < root.getLength(); i++)
                {
                    var child = root.item(i);
                    if (child instanceof IIOMetadataNode metaChild
                            && "GraphicControlExtension".equals(metaChild.getNodeName()))
                            {
                        return parseInt(metaChild.getAttribute("disposalMethod"), 0);
                    }
                }
            }
        }
        catch (Exception ignored)
        {
        }
        return 0;
    }

    private static int parseInt(String value, int defaultVal)
    {
        if (value == null || value.isEmpty()) return defaultVal;
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return defaultVal; }
    }

    private static BufferedImage copyBufferedImage(BufferedImage src)
    {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }

    private boolean loadSingleNativeImage(NativeImage image, String shortcode, int index, Path filePath)
    {
        if (config == null) return false;
        Minecraft mc = Minecraft.getInstance();
        NativeImage processed = compressIfNeeded(image);
        int w = processed.getWidth();
        int h = processed.getHeight();

        ResourceLocation texLoc = ResourceLocation.fromNamespaceAndPath(
                DiversifiedChatBar.MODID, "emoji/" + shortcode + "_" + index);

        DynamicTexture dynamicTexture = new DynamicTexture(processed);
        mc.getTextureManager().register(texLoc, dynamicTexture);

        Emoji emoji = new Emoji(shortcode, texLoc, w, h, filePath);
        emojiMap.put(shortcode.toLowerCase(), emoji);
        emojiList.add(emoji);
        return true;
    }

    

    public Emoji getEmoji(String shortcode) { return emojiMap.get(shortcode.toLowerCase()); }
    public List<Emoji> getAllEmojis() { return Collections.unmodifiableList(emojiList); }
    public boolean isLoaded() { return loaded; }
    public Path getEmojiFolderPath() { return emojiFolderPath; }

    

    private static class AnimatedEmojiData
    {
        final NativeImage[] frames;
        final int[] delays; 
        final ResourceLocation textureLocation;
        DynamicTexture dynamicTexture;
        int currentFrame;
        long nextFrameTime;

        AnimatedEmojiData(NativeImage[] frames, int[] delays, ResourceLocation texLoc, DynamicTexture tex, int w, int h)
        {
            this.frames = frames;
            this.delays = delays;
            this.textureLocation = texLoc;
            this.dynamicTexture = tex;
            this.currentFrame = 0;
            this.nextFrameTime = System.currentTimeMillis() + (delays.length > 0 ? delays[0] : 100);
        }
    }
}
