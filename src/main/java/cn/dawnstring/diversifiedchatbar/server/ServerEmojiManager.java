package cn.dawnstring.diversifiedchatbar.server;

import cn.dawnstring.diversifiedchatbar.DiversifiedChatBar;
import cn.dawnstring.diversifiedchatbar.emoji.EmojiManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ServerEmojiManager
{
    private static final LevelResource EMOJI_DIR = new LevelResource("diversifiedchatbar/emoji");
    private static ServerEmojiManager instance;

    private Path emojiRoot;
    private final List<ServerEmoji> allEmoji = new ArrayList<>();
    private final Map<String, ServerEmoji> emojiByShortcode = new HashMap<>();

    public static ServerEmojiManager getInstance()
    {
        if (instance == null) instance = new ServerEmojiManager();
        return instance;
    }

    public void init(MinecraftServer server)
    {
        emojiRoot = server.getWorldPath(EMOJI_DIR);
        try
        {
            Files.createDirectories(emojiRoot);
        }
        catch (IOException e)
        {
            DiversifiedChatBar.LOGGER.error("Failed to create server emoji dir: {}", e.getMessage());
        }
        reload();
    }

    public void reload()
    {
        allEmoji.clear();
        emojiByShortcode.clear();
        if (emojiRoot == null || !Files.isDirectory(emojiRoot)) return;

        File[] uuidDirs = emojiRoot.toFile().listFiles(File::isDirectory);
        if (uuidDirs == null) return;

        for (File uuidDir : uuidDirs)
        {
            UUID uuid;
            try
            {
                uuid = UUID.fromString(uuidDir.getName());
            }
            catch (IllegalArgumentException e)
            {
                continue;
            }

            File[] imageFiles = uuidDir.listFiles((dir, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".png") || lower.endsWith(".gif");
            });
            if (imageFiles == null) continue;

            for (File img : imageFiles)
            {
                String name = img.getName();

                String shortcode = name.substring(0, name.length() - 4).toLowerCase()
                        .replaceAll("[^a-z0-9/._-]", "_");
                try
                {
                    byte[] data = Files.readAllBytes(img.toPath());
                    ServerEmoji emoji = new ServerEmoji(uuid, shortcode, data);
                    allEmoji.add(emoji);
                    emojiByShortcode.put(shortcode, emoji);
                }
                catch (IOException e)
                {
                    DiversifiedChatBar.LOGGER.error("Failed to read emoji {}: {}", img.getName(), e.getMessage());
                }
            }
        }
        DiversifiedChatBar.LOGGER.info("Loaded {} server emoji from {} player(s)", allEmoji.size(), uuidDirs.length);
    }

    public boolean addEmoji(UUID owner, String shortcode, byte[] imageData)
    {
        try
        {
            Files.createDirectories(emojiRoot.resolve(owner.toString()));

            boolean isGif = EmojiManager.isGifData(imageData);
            String ext = isGif ? ".gif" : ".png";
            Path dest = emojiRoot.resolve(owner.toString()).resolve(shortcode + ext);
            int counter = 1;
            while (Files.exists(dest))
            {
                dest = emojiRoot.resolve(owner.toString()).resolve(shortcode + "_" + counter + ext);
                counter++;
            }
            Files.write(dest, imageData);


            String cleanShortcode = shortcode.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
            ServerEmoji old = emojiByShortcode.remove(cleanShortcode);
            if (old != null) allEmoji.remove(old);
            ServerEmoji emoji = new ServerEmoji(owner, cleanShortcode, imageData);
            allEmoji.add(emoji);
            emojiByShortcode.put(cleanShortcode, emoji);
            return true;
        }
        catch (IOException e)
        {
            DiversifiedChatBar.LOGGER.error("Failed to save emoji: {}", e.getMessage());
            return false;
        }
    }

    public boolean removeEmoji(String shortcode)
    {
        String clean = shortcode.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
        ServerEmoji emoji = emojiByShortcode.remove(clean);
        if (emoji == null) return false;

        Path pngFile = emojiRoot.resolve(emoji.owner().toString()).resolve(clean + ".png");
        Path gifFile = emojiRoot.resolve(emoji.owner().toString()).resolve(clean + ".gif");
        try
        {
            Files.deleteIfExists(pngFile);
            Files.deleteIfExists(gifFile);
        }
        catch (IOException e)
        {
            DiversifiedChatBar.LOGGER.error("Failed to delete emoji file: {}", e.getMessage());
        }
        allEmoji.remove(emoji);
        return true;
    }

    public List<ServerEmoji> getAllEmoji()
    {
        return Collections.unmodifiableList(allEmoji);
    }

    public record ServerEmoji(UUID owner, String shortcode, byte[] pngData) {}
}
