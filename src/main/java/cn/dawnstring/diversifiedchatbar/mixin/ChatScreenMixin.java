package cn.dawnstring.diversifiedchatbar.mixin;

import cn.dawnstring.diversifiedchatbar.client.EmojiManageScreen;
import cn.dawnstring.diversifiedchatbar.emoji.Emoji;
import cn.dawnstring.diversifiedchatbar.emoji.EmojiManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ChatScreen.class)
public class ChatScreenMixin
{
    @Shadow
    protected EditBox input;

    @Unique
    private boolean diversifiedchatbar$showPicker = false;

    @Unique
    private int diversifiedchatbar$pickerPage = 0;

    @Unique
    private static final int PICKER_COLS = 2;
    @Unique
    private static final int PICKER_ROWS = 3;
    @Unique
    private static final int EMOJI_CELL_SIZE = 30;
    @Unique
    private static final int PICKER_PADDING = 5;
    @Unique
    private static final int BUTTON_SIZE = 18;
    @Unique
    private static final int EMOJIS_PER_PAGE = PICKER_COLS * PICKER_ROWS;

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci)
    {
        EmojiManager manager = EmojiManager.getInstance();
        if (!manager.isLoaded()) return;

        diversifiedchatbar$renderToggleButton(guiGraphics);
        diversifiedchatbar$renderSettingsButton(guiGraphics);

        if (!diversifiedchatbar$showPicker) return;

        List<Emoji> emojis = manager.getAllEmojis();
        if (emojis.isEmpty()) return;

        int guiWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int totalPages = Math.max(1, (int) Math.ceil((double) emojis.size() / EMOJIS_PER_PAGE));
        if (diversifiedchatbar$pickerPage >= totalPages)
        {
            diversifiedchatbar$pickerPage = totalPages - 1;
        }

        
        int panelWidth = PICKER_COLS * (EMOJI_CELL_SIZE + PICKER_PADDING) + PICKER_PADDING;
        int panelHeight = PICKER_ROWS * (EMOJI_CELL_SIZE + PICKER_PADDING) + PICKER_PADDING + 14;
        int panelX = input.getX() + input.getWidth() - panelWidth - 4;
        int panelY = input.getY() - panelHeight - 4;

        
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF111111);

        int startIndex = diversifiedchatbar$pickerPage * EMOJIS_PER_PAGE;
        int endIndex = Math.min(startIndex + EMOJIS_PER_PAGE, emojis.size());

        for (int i = startIndex; i < endIndex; i++)
        {
            int indexInPage = i - startIndex;
            int col = indexInPage % PICKER_COLS;
            int row = indexInPage / PICKER_COLS;
            int ex = panelX + PICKER_PADDING + col * (EMOJI_CELL_SIZE + PICKER_PADDING);
            int ey = panelY + PICKER_PADDING + row * (EMOJI_CELL_SIZE + PICKER_PADDING);

            Emoji emoji = emojis.get(i);
            int renderSize = Math.min(EMOJI_CELL_SIZE - 6, Math.min(emoji.width(), emoji.height()));
            int ox = (EMOJI_CELL_SIZE - renderSize) / 2;
            int oy = (EMOJI_CELL_SIZE - renderSize) / 2;

            if (mouseX >= ex && mouseX <= ex + EMOJI_CELL_SIZE && mouseY >= ey && mouseY <= ey + EMOJI_CELL_SIZE)
            {
                guiGraphics.fill(ex, ey, ex + EMOJI_CELL_SIZE, ey + EMOJI_CELL_SIZE, 0x44FFFFFF);
                guiGraphics.drawString(Minecraft.getInstance().font, ":" + emoji.shortcode() + ":", ex, ey + EMOJI_CELL_SIZE + 2, 0xCCCCCC, false);
            }

            
            int tex1 = ex + ox;
            int tey1 = ey + oy;
            guiGraphics.blit(emoji.textureLocation(), tex1, tey1, renderSize, renderSize,
                    0, 0, emoji.width(), emoji.height(), emoji.width(), emoji.height());
        }

        Font font = Minecraft.getInstance().font;
        String pageText = (diversifiedchatbar$pickerPage + 1) + "/" + totalPages;
        int pageTextWidth = font.width(pageText);
        guiGraphics.drawString(font, pageText, panelX + (panelWidth - pageTextWidth) / 2, panelY + panelHeight - 12, 0x888888, false);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir)
    {
        if (button != 0) return;

        if (diversifiedchatbar$isOverToggleButton((int) mouseX, (int) mouseY))
        {
            diversifiedchatbar$showPicker = !diversifiedchatbar$showPicker;
            diversifiedchatbar$pickerPage = 0;
            cir.setReturnValue(true);
            return;
        }

        if (diversifiedchatbar$isOverSettingsButton((int) mouseX, (int) mouseY))
        {
            Minecraft.getInstance().setScreen(new EmojiManageScreen());
            cir.setReturnValue(true);
            return;
        }

        if (!diversifiedchatbar$showPicker) return;

        EmojiManager manager = EmojiManager.getInstance();
        if (!manager.isLoaded()) return;

        List<Emoji> emojis = manager.getAllEmojis();
        if (emojis.isEmpty()) return;

        int guiWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int totalPages = Math.max(1, (int) Math.ceil((double) emojis.size() / EMOJIS_PER_PAGE));

        int panelWidth = PICKER_COLS * (EMOJI_CELL_SIZE + PICKER_PADDING) + PICKER_PADDING;
        int panelHeight = PICKER_ROWS * (EMOJI_CELL_SIZE + PICKER_PADDING) + PICKER_PADDING + 14;
        int panelX = input.getX() + input.getWidth() - panelWidth - 4;
        int panelY = input.getY() - panelHeight - 4;

        int startIndex = diversifiedchatbar$pickerPage * EMOJIS_PER_PAGE;
        int endIndex = Math.min(startIndex + EMOJIS_PER_PAGE, emojis.size());

        for (int i = startIndex; i < endIndex; i++)
        {
            int indexInPage = i - startIndex;
            int col = indexInPage % PICKER_COLS;
            int row = indexInPage / PICKER_COLS;
            int ex = panelX + PICKER_PADDING + col * (EMOJI_CELL_SIZE + PICKER_PADDING);
            int ey = panelY + PICKER_PADDING + row * (EMOJI_CELL_SIZE + PICKER_PADDING);

            if (mouseX >= ex && mouseX <= ex + EMOJI_CELL_SIZE && mouseY >= ey && mouseY <= ey + EMOJI_CELL_SIZE)
            {
                Emoji emoji = emojis.get(i);
                String message = ":" + emoji.shortcode() + ":";
                ChatScreen self = (ChatScreen) (Object) this;
                self.handleChatInput(message, true);
                diversifiedchatbar$showPicker = false;
                cir.setReturnValue(true);
                return;
            }
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir)
    {
        if (!diversifiedchatbar$showPicker) return;

        EmojiManager manager = EmojiManager.getInstance();
        if (!manager.isLoaded()) return;

        List<Emoji> emojis = manager.getAllEmojis();
        if (emojis.isEmpty()) return;

        int totalPages = Math.max(1, (int) Math.ceil((double) emojis.size() / EMOJIS_PER_PAGE));

        int guiWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int panelWidth = PICKER_COLS * (EMOJI_CELL_SIZE + PICKER_PADDING) + PICKER_PADDING;
        int panelHeight = PICKER_ROWS * (EMOJI_CELL_SIZE + PICKER_PADDING) + PICKER_PADDING + 14;
        int panelX = input.getX() + input.getWidth() - panelWidth - 4;
        int panelY = input.getY() - panelHeight - 4;

        if (mouseX >= panelX && mouseX <= panelX + panelWidth && mouseY >= panelY && mouseY <= panelY + panelHeight)
        {
            if (scrollY < 0)
            {
                diversifiedchatbar$pickerPage = Math.min(diversifiedchatbar$pickerPage + 1, totalPages - 1);
            }
            else if (scrollY > 0)
            {
                diversifiedchatbar$pickerPage = Math.max(diversifiedchatbar$pickerPage - 1, 0);
            }
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void diversifiedchatbar$renderToggleButton(GuiGraphics guiGraphics)
    {
        Font font = Minecraft.getInstance().font;
        int btnX = input.getX() + input.getWidth() - BUTTON_SIZE - 24;
        int btnY = input.getY() + (input.getHeight() - BUTTON_SIZE) / 2;

        int bgColor = diversifiedchatbar$showPicker ? 0xFF444444 : 0xFF222222;
        guiGraphics.fill(btnX, btnY, btnX + BUTTON_SIZE, btnY + BUTTON_SIZE, bgColor);
        guiGraphics.drawString(font, "EM", btnX + 1, btnY + 1, 0xFFFFFF, false);
    }

    @Unique
    private boolean diversifiedchatbar$isOverToggleButton(int mouseX, int mouseY)
    {
        int btnX = input.getX() + input.getWidth() - BUTTON_SIZE - 24;
        int btnY = input.getY() + (input.getHeight() - BUTTON_SIZE) / 2;
        return mouseX >= btnX && mouseX <= btnX + BUTTON_SIZE && mouseY >= btnY && mouseY <= btnY + BUTTON_SIZE;
    }

    @Unique
    private void diversifiedchatbar$renderSettingsButton(GuiGraphics guiGraphics)
    {
        Font font = Minecraft.getInstance().font;
        int btnX = input.getX() + input.getWidth() - BUTTON_SIZE - 2;
        int btnY = input.getY() + (input.getHeight() - BUTTON_SIZE) / 2;

        guiGraphics.fill(btnX, btnY, btnX + BUTTON_SIZE, btnY + BUTTON_SIZE, 0xFF333333);
        guiGraphics.drawString(font, "CF", btnX + 1, btnY + 1, 0xAAAAAA, false);
    }

    @Unique
    private boolean diversifiedchatbar$isOverSettingsButton(int mouseX, int mouseY)
    {
        int btnX = input.getX() + input.getWidth() - BUTTON_SIZE - 2;
        int btnY = input.getY() + (input.getHeight() - BUTTON_SIZE) / 2;
        return mouseX >= btnX && mouseX <= btnX + BUTTON_SIZE && mouseY >= btnY && mouseY <= btnY + BUTTON_SIZE;
    }
}
