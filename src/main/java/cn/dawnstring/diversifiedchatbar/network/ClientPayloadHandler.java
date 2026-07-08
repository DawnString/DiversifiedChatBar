package cn.dawnstring.diversifiedchatbar.network;

import cn.dawnstring.diversifiedchatbar.DiversifiedChatBar;
import cn.dawnstring.diversifiedchatbar.emoji.Emoji;
import cn.dawnstring.diversifiedchatbar.emoji.EmojiManager;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ClientPayloadHandler
{

    public static void handleSync(EmojiPayloads.EmojiSyncPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            EmojiManager manager = EmojiManager.getInstance();


            manager.clearServerEmoji();

            for (EmojiPayloads.EmojiEntry entry : payload.entries())
            {
                try
                {
                    byte[] data = entry.imageData();
                    boolean isGif = EmojiManager.isGifData(data);

                    if (isGif)
                    {

                        manager.loadServerGif(entry.owner(), entry.shortcode(), data);
                    }
                    else
                    {
                        try (var in = new ByteArrayInputStream(data))
                        {
                            NativeImage image = NativeImage.read(in);
                            NativeImage processed = EmojiManager.compressIfNeededStatic(image);
                            int w = processed.getWidth();
                            int h = processed.getHeight();

                            String texPath = "server_emoji/" + entry.owner().toString() + "_" + entry.shortcode();
                            ResourceLocation texLoc = ResourceLocation.fromNamespaceAndPath(DiversifiedChatBar.MODID, texPath);

                            DynamicTexture tex = new DynamicTexture(processed);
                            mc.getTextureManager().register(texLoc, tex);

                            Emoji emoji = new Emoji(
                                    entry.shortcode(), texLoc, w, h,
                                    null, Emoji.Source.SERVER, entry.owner(), false
                            );
                            manager.addServerEmoji(emoji);
                        }
                    }
                }
                catch (IOException e)
                {
                    DiversifiedChatBar.LOGGER.error("Failed to process server emoji {}: {}", entry.shortcode(), e.getMessage());
                }
            }
            DiversifiedChatBar.LOGGER.info("Received {} server emoji", payload.entries().size());
        });
    }
}
