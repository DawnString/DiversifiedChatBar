package cn.dawnstring.diversifiedchatbar.client;

import cn.dawnstring.diversifiedchatbar.DiversifiedChatBar;
import cn.dawnstring.diversifiedchatbar.config.DCBConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class EmojiConfigScreen extends Screen
{
    private static final int BTN_W = 16;
    private static final int BTN_H = 14;

    private final Screen parent;

    
    private int curLines;
    private double curFraction;
    private double curScale;

    protected EmojiConfigScreen(Screen parent)
    {
        super(Component.translatable("diversifiedchatbar.screen.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init()
    {
        super.init();
        curLines = DCBConfig.runtimeChatLinesOverride > 0
                ? DCBConfig.runtimeChatLinesOverride : DCBConfig.EMOJI_CHAT_LINES.get();
        curFraction = DCBConfig.runtimeScreenFractionOverride > 0.0
                ? DCBConfig.runtimeScreenFractionOverride : DCBConfig.EMOJI_SCREEN_FRACTION.get();
        curScale = DCBConfig.runtimeChatScaleOverride > 0.0
                ? DCBConfig.runtimeChatScaleOverride : DCBConfig.EMOJI_CHAT_SCALE.get();

        addRenderableWidget(Button.builder(
                Component.translatable("diversifiedchatbar.screen.config.done"),
                btn -> onClose()
        ).bounds(width / 2 - 40, height - 30, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawString(font, title, 10, 10, 0xFFFFFF, false);

        int y = 40;
        y = drawSetting(guiGraphics, "diversifiedchatbar.screen.config.lines", curLines, 2, 12, 1, y);
        y = drawSettingDecimal(guiGraphics, "diversifiedchatbar.screen.config.fraction", curFraction, 0.10, 0.50, 0.05, y);
        drawSettingDecimal(guiGraphics, "diversifiedchatbar.screen.config.scale", curScale, 0.25, 3.0, 0.25, y);
    }

    private int drawSetting(GuiGraphics gui, String labelKey, int value, int min, int max, int step, int y)
    {
        String label = Component.translatable(labelKey).getString() + ": ";
        gui.drawString(font, label, 20, y + 2, 0xCCCCCC, false);
        int lw = font.width(label);
        int x = 20 + lw;

        
        gui.fill(x, y, x + BTN_W, y + BTN_H, 0xFF444444);
        gui.drawString(font, "-", x + 4, y + 1, 0xFFFFFF, false);
        x += BTN_W + 2;

        
        String valStr = String.valueOf(value);
        gui.drawString(font, valStr, x, y + 2, 0xFFFFFF, false);
        x += font.width(valStr) + 2;

        
        gui.fill(x, y, x + BTN_W, y + BTN_H, 0xFF444444);
        gui.drawString(font, "+", x + 4, y + 1, 0xFFFFFF, false);

        return y + 24;
    }

    private int drawSettingDecimal(GuiGraphics gui, String labelKey, double value, double min, double max, double step, int y)
    {
        String label = Component.translatable(labelKey).getString() + ": ";
        gui.drawString(font, label, 20, y + 2, 0xCCCCCC, false);
        int lw = font.width(label);
        int x = 20 + lw;

        
        gui.fill(x, y, x + BTN_W, y + BTN_H, 0xFF444444);
        gui.drawString(font, "-", x + 4, y + 1, 0xFFFFFF, false);
        x += BTN_W + 2;

        
        String valStr = String.format("%.2f", value);
        gui.drawString(font, valStr, x, y + 2, 0xFFFFFF, false);
        x += font.width(valStr) + 2;

        
        gui.fill(x, y, x + BTN_W, y + BTN_H, 0xFF444444);
        gui.drawString(font, "+", x + 4, y + 1, 0xFFFFFF, false);

        return y + 24;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0)
        {
            int mx = (int) mouseX;
            int my = (int) mouseY;

            int y = 40;
            if (clickSetting(mx, my, y, 2, 12, 1, v -> curLines = v))
            {
                applyOverrides();
                return true;
            }
            y += 24;
            if (clickSettingDecimal(mx, my, y,
                    "diversifiedchatbar.screen.config.fraction", curFraction,
                    0.10, 0.50, 0.05, v -> curFraction = v))
                    {
                applyOverrides();
                return true;
            }
            y += 24;
            if (clickSettingDecimal(mx, my, y,
                    "diversifiedchatbar.screen.config.scale", curScale,
                    0.25, 3.0, 0.25, v -> curScale = v))
                    {
                applyOverrides();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickSetting(int mx, int my, int y, int min, int max, int step, java.util.function.IntConsumer setter)
    {
        int x = 20 + font.width(Component.translatable("diversifiedchatbar.screen.config.lines").getString() + ": ");
        
        if (mx >= x && mx <= x + BTN_W && my >= y && my <= y + BTN_H)
        {
            setter.accept(Math.max(min, curLines - step));
            return true;
        }
        x += BTN_W + 2 + font.width(String.valueOf(curLines)) + 2;
        
        if (mx >= x && mx <= x + BTN_W && my >= y && my <= y + BTN_H)
        {
            setter.accept(Math.min(max, curLines + step));
            return true;
        }
        return false;
    }

    private boolean clickSettingDecimal(int mx, int my, int y, String labelKey, double curValue, double min, double max, double step, java.util.function.DoubleConsumer setter)
    {
        String label = Component.translatable(labelKey).getString() + ": ";
        int x = 20 + font.width(label);

        
        if (mx >= x && mx <= x + BTN_W && my >= y && my <= y + BTN_H)
        {
            double newVal = Math.max(min, curValue - step);
            newVal = Math.round(newVal / step) * step;
            newVal = Math.min(max, newVal);
            setter.accept(newVal);
            return true;
        }
        x += BTN_W + 2 + font.width(String.format("%.2f", curValue)) + 2;
        
        if (mx >= x && mx <= x + BTN_W && my >= y && my <= y + BTN_H)
        {
            double newVal = Math.min(max, curValue + step);
            newVal = Math.round(newVal / step) * step;
            newVal = Math.max(min, newVal);
            setter.accept(newVal);
            return true;
        }
        return false;
    }

    private void applyOverrides()
    {
        DCBConfig.runtimeChatLinesOverride = curLines;
        DCBConfig.runtimeScreenFractionOverride = curFraction;
        DCBConfig.runtimeChatScaleOverride = curScale;
    }

    private void saveToConfigFile()
    {
        DiversifiedChatBar.saveRuntimeConfig(curLines, curFraction, curScale);
    }

    @Override
    public void onClose()
    {
        applyOverrides();
        saveToConfigFile();
        DiversifiedChatBar.LOGGER.info("Emoji config saved: lines={}, fraction={}, scale={}", curLines, curFraction, curScale);
        minecraft.setScreen(parent);
    }
}
