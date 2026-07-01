package cn.dawnstring.diversifiedchatbar.server;

import cn.dawnstring.diversifiedchatbar.DiversifiedChatBar;
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
                    allEmoji.add(new ServerEmoji(uuid, shortcode, data));
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
            
            boolean isGif = isGifData(imageData);
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
            allEmoji.removeIf(e -> e.shortcode().equals(cleanShortcode));
            allEmoji.add(new ServerEmoji(owner, cleanShortcode, imageData));
            return true;
        }
catch (IOException e)
{
            DiversifiedChatBar.LOGGER.error("Failed to save emoji: {}", e.getMessage());
            return false;
        }
    }

    private static boolean isGifData(byte[] data)
{
        if (data.length < 6) return false;
        String magic = new String(data, 0, 6, java.nio.charset.StandardCharsets.US_ASCII);
        return magic.equals("GIF87a") || magic.equals("GIF89a");
    }

    public boolean removeEmoji(String shortcode)
{
        String clean = shortcode.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
        for (ServerEmoji emoji : allEmoji)
{
            if (emoji.shortcode().equals(clean))
{
                
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
        }
        return false;
    }

    public List<ServerEmoji> getAllEmoji()
{
        return Collections.unmodifiableList(allEmoji);
    }

    public record ServerEmoji(UUID owner, String shortcode, byte[] pngData) {}
}
