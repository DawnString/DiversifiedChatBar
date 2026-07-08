package cn.dawnstring.diversifiedchatbar.client;

import cn.dawnstring.diversifiedchatbar.emoji.Emoji;
import cn.dawnstring.diversifiedchatbar.emoji.EmojiManager;
import cn.dawnstring.diversifiedchatbar.DiversifiedChatBar;
import cn.dawnstring.diversifiedchatbar.config.DCBConfig;
import cn.dawnstring.diversifiedchatbar.network.EmojiPayloads;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class EmojiManageScreen extends Screen
{
    private static final int CELL_SIZE = 72;
    private static final int GRID_COLS = 4;
    private static final int PADDING = 10;
    private static final int HEADER_HEIGHT = 40;

    private int scrollOffset = 0;
    private List<Emoji> emojis;
    private Emoji deleteHoverEmoji = null;
    private static final int SIZE_BTN_W = 16;

    public EmojiManageScreen()
    {
        super(Component.translatable("diversifiedchatbar.screen.emojiManager.title"));
    }

    @Override
    protected void init()
    {
        super.init();
        emojis = EmojiManager.getInstance().getAllEmojis();

        addRenderableWidget(Button.builder(
                Component.translatable("diversifiedchatbar.screen.emojiManager.config"),
                btn -> minecraft.setScreen(new EmojiConfigScreen(this))
        ).bounds(width / 2 - 150, height - 30, 60, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("diversifiedchatbar.screen.emojiManager.refresh"), btn -> {
            EmojiManager.getInstance().reload();
            emojis = EmojiManager.getInstance().getAllEmojis();
        }).bounds(width / 2 - 30, height - 30, 70, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("diversifiedchatbar.screen.emojiManager.back"), btn -> onClose())
                .bounds(width / 2 + 50, height - 30, 70, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawString(font, title, 10, 10, 0xFFFFFF, false);
        Path folder = EmojiManager.getInstance().getEmojiFolderPath();
        guiGraphics.drawString(font, Component.translatable("diversifiedchatbar.screen.emojiManager.folder", folder != null ? folder.toString() : "N/A"), 10, 24, 0x888888, false);

        if (emojis == null || emojis.isEmpty())
        {
            guiGraphics.drawString(font, Component.translatable("diversifiedchatbar.screen.emojiManager.empty"), 10, 60, 0x888888, false);
            guiGraphics.drawString(font, Component.translatable("diversifiedchatbar.screen.emojiManager.dragHint"), 10, 72, 0x666666, false);
            return;
        }

        int startY = HEADER_HEIGHT;
        int gridWidth = GRID_COLS * (CELL_SIZE + PADDING);
        int startX = (width - gridWidth) / 2;

        deleteHoverEmoji = null;
        EmojiManager emojiManager = EmojiManager.getInstance();


        for (int i = 0; i < emojis.size(); i++)
        {
            Emoji emoji = emojis.get(i);
            int col = i % GRID_COLS;
            int row = i / GRID_COLS;
            int x = startX + col * (CELL_SIZE + PADDING);
            int y = startY + row * (CELL_SIZE + PADDING) - scrollOffset;

            if (y < HEADER_HEIGHT - CELL_SIZE || y > height - 50) continue;

            if (emoji.isAnimated())
            {
                emojiManager.updateAnimatedTexture(emoji);
            }

            guiGraphics.fill(x, y, x + CELL_SIZE, y + CELL_SIZE, 0x33FFFFFF);

            if (mouseX >= x && mouseX <= x + CELL_SIZE && mouseY >= y && mouseY <= y + CELL_SIZE)
            {
                guiGraphics.fill(x, y, x + CELL_SIZE, y + CELL_SIZE, 0x44FFFFFF);
            }

            int renderSize = Math.min(CELL_SIZE - 16, Math.min(emoji.width(), emoji.height()));
            int ox = (CELL_SIZE - renderSize) / 2;
            int oy = (CELL_SIZE - renderSize) / 2 - 6;

            RenderSystem.setShaderTexture(0, emoji.textureLocation());
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            var pose = guiGraphics.pose().last().pose();
            int ex1 = x + ox;
            int ey1 = y + oy;
            int ex2 = ex1 + renderSize;
            int ey2 = ey1 + renderSize;
            var tesselator = Tesselator.getInstance();
            var builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            builder.addVertex(pose, ex1, ey2, 0).setUv(0, 1);
            builder.addVertex(pose, ex2, ey2, 0).setUv(1, 1);
            builder.addVertex(pose, ex2, ey1, 0).setUv(1, 0);
            builder.addVertex(pose, ex1, ey1, 0).setUv(0, 0);
            BufferUploader.drawWithShader(builder.build());

            guiGraphics.drawString(font, ":" + emoji.shortcode() + ":", x + 2, y + CELL_SIZE - 12, 0xCCCCCC, false);

            if (mouseX >= x && mouseX <= x + CELL_SIZE && mouseY >= y && mouseY <= y + CELL_SIZE)
            {
                guiGraphics.drawString(font, Component.translatable("diversifiedchatbar.screen.emojiManager.delete"), x + CELL_SIZE - 20, y + 2, 0xFF4444, false);
                deleteHoverEmoji = emoji;
            }
        }

        String hint = Component.translatable("diversifiedchatbar.screen.emojiManager.dragHint").getString();
        int hintWidth = font.width(hint);
        guiGraphics.drawString(font, hint, (width - hintWidth) / 2, height - 50, 0x666666, false);


        diversifiedchatbar$renderSizeControl(guiGraphics);
    }

    private void diversifiedchatbar$renderSizeControl(GuiGraphics guiGraphics)
    {
        double scale = DCBConfig.getEffectiveChatScale();
        String label = Component.translatable("diversifiedchatbar.screen.emojiManager.size", String.format("%.1f", scale)).getString();
        int lblW = font.width(label);
        int lblX = width - lblW - 10;
        guiGraphics.drawString(font, label, lblX, 10, 0xCCCCCC, false);

        int btnY = 8;
        int btnH = 14;
        int bx = lblX - 4 - SIZE_BTN_W;


        int decColor = 0xFF444444;
        guiGraphics.fill(bx, btnY, bx + SIZE_BTN_W, btnY + btnH, decColor);
        guiGraphics.drawString(font, "-", bx + 4, btnY + 1, 0xFFFFFF, false);


        int incX = lblX + lblW + 4;
        guiGraphics.fill(incX, btnY, incX + SIZE_BTN_W, btnY + btnH, decColor);
        guiGraphics.drawString(font, "+", incX + 3, btnY + 1, 0xFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0 && deleteHoverEmoji != null)
        {
            EmojiManager.getInstance().removeEmoji(deleteHoverEmoji);
            emojis = EmojiManager.getInstance().getAllEmojis();
            return true;
        }

        if (button == 0)
        {
            int mx = (int) mouseX;
            int my = (int) mouseY;

            double scale = DCBConfig.getEffectiveChatScale();
            String label = Component.translatable("diversifiedchatbar.screen.emojiManager.size", String.format("%.1f", scale)).getString();
            int lblW = font.width(label);
            int lblX = width - lblW - 10;
            int btnY = 8;
            int btnH = 14;


            int decX = lblX - 4 - SIZE_BTN_W;
            if (mx >= decX && mx <= decX + SIZE_BTN_W && my >= btnY && my <= btnY + btnH)
            {
                double newVal = Math.max(0.25, scale - 0.25);
                DCBConfig.runtimeChatScaleOverride = (newVal < 0.5 && Math.abs(newVal - 0.5) > 0.01) ? 0.0 : newVal;
                if (DCBConfig.runtimeChatScaleOverride < 0.01) DCBConfig.runtimeChatScaleOverride = 0.0;
                return true;
            }


            int incX = lblX + lblW + 4;
            if (mx >= incX && mx <= incX + SIZE_BTN_W && my >= btnY && my <= btnY + btnH)
            {
                double newVal = Math.min(3.0, scale + 0.25);
                DCBConfig.runtimeChatScaleOverride = newVal;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        if (emojis == null) return false;
        int totalRows = (int) Math.ceil((double) emojis.size() / GRID_COLS);
        int maxScroll = Math.max(0, totalRows * (CELL_SIZE + PADDING) - (height - 100));
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (scrollY * 40)));
        return true;
    }

    @Override
    public void onFilesDrop(List<Path> paths)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null && !mc.isLocalServer())
        {

            int idx = 0;
            for (Path path : paths)
            {
                String name = path.getFileName().toString().toLowerCase();
                if (!(name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif"))) continue;
                try
                {
                    String shortcode = java.time.LocalDateTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("'emoji_'yyyyMMdd_HHmmss"));
                    if (idx > 0) shortcode += "_" + idx;

                    byte[] imageData;
                    if (name.endsWith(".gif") || name.endsWith(".png"))
                    {

                        imageData = Files.readAllBytes(path);
                    }
                    else
                    {

                        BufferedImage bi = ImageIO.read(path.toFile());
                        if (bi == null) continue;
                        BufferedImage argb = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
                        argb.getGraphics().drawImage(bi, 0, 0, null);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(argb, "PNG", baos);
                        imageData = baos.toByteArray();
                    }

                    PacketDistributor.sendToServer(new EmojiPayloads.EmojiUploadPayload(shortcode, imageData));
                    DiversifiedChatBar.LOGGER.info("Uploaded emoji {} to server", shortcode);
                    idx++;
                }
                catch (IOException e)
                {
                    DiversifiedChatBar.LOGGER.error("Failed to upload emoji: {}", e.getMessage());
                }
            }
        }
        else
        {

            for (Path path : paths)
            {
                String name = path.getFileName().toString().toLowerCase();
                if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif"))
                {
                    EmojiManager.getInstance().addEmojiFromSource(path, null);
                }
            }
            emojis = EmojiManager.getInstance().getAllEmojis();
        }
    }
}
