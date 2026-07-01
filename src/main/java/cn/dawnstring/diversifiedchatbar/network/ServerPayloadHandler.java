package cn.dawnstring.diversifiedchatbar.network;

import cn.dawnstring.diversifiedchatbar.DiversifiedChatBar;
import cn.dawnstring.diversifiedchatbar.server.ServerEmojiManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.stream.Collectors;

public class ServerPayloadHandler
{

    public static void handleUpload(EmojiPayloads.EmojiUploadPayload payload, IPayloadContext context)
{
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            MinecraftServer server = player.getServer();
            if (server == null) return;

            boolean ok = ServerEmojiManager.getInstance().addEmoji(
                    player.getUUID(), payload.shortcode(), payload.imageData()
            );
            if (ok)
{
                DiversifiedChatBar.LOGGER.info("Player {} uploaded emoji: {}", player.getName().getString(), payload.shortcode());
                broadcastSync(server);
            }
        });
    }

    public static void handleDelete(EmojiPayloads.EmojiDeletePayload payload, IPayloadContext context)
{
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            MinecraftServer server = player.getServer();
            if (server == null) return;

            boolean ok = ServerEmojiManager.getInstance().removeEmoji(payload.shortcode());
            if (ok)
{
                DiversifiedChatBar.LOGGER.info("Player {} deleted emoji: {}", player.getName().getString(), payload.shortcode());
                broadcastSync(server);
            }
        });
    }

    public static void broadcastSync(MinecraftServer server)
{
        List<EmojiPayloads.EmojiEntry> entries = ServerEmojiManager.getInstance().getAllEmoji().stream()
                .map(e -> new EmojiPayloads.EmojiEntry(e.owner(), e.shortcode(), e.pngData()))
                .collect(Collectors.toList());

        EmojiPayloads.EmojiSyncPayload sync = new EmojiPayloads.EmojiSyncPayload(entries);
        PacketDistributor.sendToAllPlayers(sync);
    }
}
