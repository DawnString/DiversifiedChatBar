package cn.dawnstring.diversifiedchatbar.network;

import cn.dawnstring.diversifiedchatbar.DiversifiedChatBar;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EmojiPayloads
{

    
    public record EmojiSyncPayload(List<EmojiEntry> entries) implements CustomPacketPayload
{
        public static final CustomPacketPayload.Type<EmojiSyncPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DiversifiedChatBar.MODID, "emoji_sync"));
        public static final StreamCodec<FriendlyByteBuf, EmojiSyncPayload> STREAM_CODEC =
                CustomPacketPayload.codec(EmojiSyncPayload::write, EmojiSyncPayload::new);

        private EmojiSyncPayload(FriendlyByteBuf buf)
{
            this(readEntries(buf));
        }

        private static List<EmojiEntry> readEntries(FriendlyByteBuf buf)
{
            int count = buf.readVarInt();
            List<EmojiEntry> list = new ArrayList<>(count);
            for (int i = 0; i < count; i++)
{
                list.add(new EmojiEntry(buf.readUUID(), buf.readUtf(), buf.readByteArray()));
            }
            return list;
        }

        private void write(FriendlyByteBuf buf)
{
            buf.writeVarInt(entries.size());
            for (EmojiEntry entry : entries)
{
                buf.writeUUID(entry.owner());
                buf.writeUtf(entry.shortcode());
                buf.writeByteArray(entry.imageData());
            }
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
{
            return TYPE;
        }
    }

    public record EmojiEntry(UUID owner, String shortcode, byte[] imageData) {}

    
    public record EmojiUploadPayload(String shortcode, byte[] imageData) implements CustomPacketPayload
{
        public static final CustomPacketPayload.Type<EmojiUploadPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DiversifiedChatBar.MODID, "emoji_upload"));
        public static final StreamCodec<FriendlyByteBuf, EmojiUploadPayload> STREAM_CODEC =
                CustomPacketPayload.codec(EmojiUploadPayload::write, EmojiUploadPayload::new);

        private EmojiUploadPayload(FriendlyByteBuf buf)
{
            this(buf.readUtf(), buf.readByteArray());
        }

        private void write(FriendlyByteBuf buf)
{
            buf.writeUtf(shortcode);
            buf.writeByteArray(imageData);
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
{
            return TYPE;
        }
    }

    
    public record EmojiDeletePayload(String shortcode) implements CustomPacketPayload
{
        public static final CustomPacketPayload.Type<EmojiDeletePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DiversifiedChatBar.MODID, "emoji_delete"));
        public static final StreamCodec<FriendlyByteBuf, EmojiDeletePayload> STREAM_CODEC =
                CustomPacketPayload.codec(EmojiDeletePayload::write, EmojiDeletePayload::new);

        private EmojiDeletePayload(FriendlyByteBuf buf)
{
            this(buf.readUtf());
        }

        private void write(FriendlyByteBuf buf)
{
            buf.writeUtf(shortcode);
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
{
            return TYPE;
        }
    }
}
