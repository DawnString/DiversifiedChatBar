package cn.dawnstring.diversifiedchatbar.network;

import cn.dawnstring.diversifiedchatbar.DiversifiedChatBar;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public class SyncHandlerBridge
{
    public static void handleSync(EmojiPayloads.EmojiSyncPayload payload, IPayloadContext context)
{
        
        try
{
            Class.forName("cn.dawnstring.diversifiedchatbar.network.ClientPayloadHandler")
                    .getMethod("handleSync", EmojiPayloads.EmojiSyncPayload.class, IPayloadContext.class)
                    .invoke(null, payload, context);
        }
catch (Exception e)
{
            DiversifiedChatBar.LOGGER.error("Failed to delegate sync payload", e);
        }
    }
}
