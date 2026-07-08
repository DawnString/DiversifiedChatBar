package cn.dawnstring.diversifiedchatbar;

import cn.dawnstring.diversifiedchatbar.config.DCBConfig;
import cn.dawnstring.diversifiedchatbar.emoji.EmojiManager;
import cn.dawnstring.diversifiedchatbar.network.ClientPayloadHandler;
import cn.dawnstring.diversifiedchatbar.network.EmojiPayloads;
import cn.dawnstring.diversifiedchatbar.network.ServerPayloadHandler;
import cn.dawnstring.diversifiedchatbar.server.ServerEmojiManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

@Mod(DiversifiedChatBar.MODID)
public class DiversifiedChatBar
{
    public static final String MODID = "diversifiedchatbar";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    private static Path runtimeConfigPath;

    public static void saveRuntimeConfig(int lines, double fraction, double scale)
    {
        if (runtimeConfigPath == null) return;

        Properties props = new Properties();
        props.setProperty("chatLines", String.valueOf(lines));
        props.setProperty("screenFraction", String.valueOf(fraction));
        props.setProperty("chatScale", String.valueOf(scale));

        try (OutputStream out = Files.newOutputStream(runtimeConfigPath))
        {
            props.store(out, "Runtime overrides - loaded at startup");
            LOGGER.info("Runtime config saved: lines={}, fraction={}, scale={}", lines, fraction, scale);
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to save runtime config", e);
        }
    }

    public static void loadRuntimeConfig(ModConfig config)
    {
        Path dir = config.getFullPath().getParent();

        if (dir == null) return;

        runtimeConfigPath = dir.resolve("diversifiedchatbar-runtime.properties");

        if (!Files.exists(runtimeConfigPath)) return;

        Properties props = new Properties();

        try (InputStream in = Files.newInputStream(runtimeConfigPath))
        {
            props.load(in);

            if (props.containsKey("chatLines"))
                DCBConfig.runtimeChatLinesOverride = Integer.parseInt(props.getProperty("chatLines"));

            if (props.containsKey("screenFraction"))
                DCBConfig.runtimeScreenFractionOverride = Double.parseDouble(props.getProperty("screenFraction"));

            if (props.containsKey("chatScale"))
                DCBConfig.runtimeChatScaleOverride = Double.parseDouble(props.getProperty("chatScale"));

            LOGGER.info("Runtime config loaded: lines={}, fraction={}, scale={}",
                    DCBConfig.runtimeChatLinesOverride, DCBConfig.runtimeScreenFractionOverride, DCBConfig.runtimeChatScaleOverride);
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to load runtime config", e);
        }
    }

    public DiversifiedChatBar(ModContainer modContainer)
    {
        modContainer.registerConfig(ModConfig.Type.COMMON, DCBConfig.SPEC);

        var bus = modContainer.getEventBus();

        bus.addListener((ModConfigEvent event) ->
        {
            if (event.getConfig().getSpec() == DCBConfig.SPEC)
            {
                loadRuntimeConfig(event.getConfig());
            }
        });

        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            bus.register(ClientHandler.class);
        }

        bus.register(NetworkHandler.class);

        NeoForge.EVENT_BUS.register(ServerHandler.class);
    }

    public static class ClientHandler
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            event.enqueueWork(() ->
            {
                EmojiManager.getInstance().load(new DCBConfig(DCBConfig.SPEC));
            });
        }
    }

    public static class NetworkHandler
    {
        @SubscribeEvent
        public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event)
        {
            PayloadRegistrar registrar = event.registrar(DiversifiedChatBar.MODID);

            if (FMLEnvironment.dist == Dist.CLIENT)
            {
                registrar.playToClient(
                        EmojiPayloads.EmojiSyncPayload.TYPE,
                        EmojiPayloads.EmojiSyncPayload.STREAM_CODEC,
                        ClientPayloadHandler::handleSync
                );
            }

            registrar.playToServer(
                    EmojiPayloads.EmojiUploadPayload.TYPE,
                    EmojiPayloads.EmojiUploadPayload.STREAM_CODEC,
                    ServerPayloadHandler::handleUpload
            );
            registrar.playToServer(
                    EmojiPayloads.EmojiDeletePayload.TYPE,
                    EmojiPayloads.EmojiDeletePayload.STREAM_CODEC,
                    ServerPayloadHandler::handleDelete
            );
        }
    }

    public static class ServerHandler
    {
        @SubscribeEvent
        public static void onServerStarted(ServerStartedEvent event)
        {
            ServerEmojiManager.getInstance().init(event.getServer());
        }
    }
}
