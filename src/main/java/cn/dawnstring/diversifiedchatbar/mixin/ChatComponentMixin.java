package cn.dawnstring.diversifiedchatbar.mixin;

import cn.dawnstring.diversifiedchatbar.config.DCBConfig;
import cn.dawnstring.diversifiedchatbar.emoji.Emoji;
import cn.dawnstring.diversifiedchatbar.emoji.EmojiManager;
import cn.dawnstring.diversifiedchatbar.emoji.EmojiParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public class ChatComponentMixin
{

    @Unique
    private int diversifiedchatbar$extraHeightBelow;
    @Unique
    private int diversifiedchatbar$pendingBgX1;
    @Unique
    private int diversifiedchatbar$pendingBgY1;
    @Unique
    private int diversifiedchatbar$pendingBgX2;
    @Unique
    private int diversifiedchatbar$pendingBgY2;
    @Unique
    private int diversifiedchatbar$pendingBgColor;
    @Unique
    private boolean diversifiedchatbar$hasPendingBg;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(CallbackInfo ci)
{
        diversifiedchatbar$extraHeightBelow = 0;
        diversifiedchatbar$hasPendingBg = false;
    }

    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V")
    )
    private void onFill(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color)
{
        int height = y2 - y1;
        Font f = Minecraft.getInstance().font;
        int expectedLineH = (f != null ? f.lineHeight : 9) + 2;
        if (Math.abs(height - expectedLineH) <= 2)
{
            diversifiedchatbar$pendingBgX1 = x1;
            diversifiedchatbar$pendingBgY1 = y1;
            diversifiedchatbar$pendingBgX2 = x2;
            diversifiedchatbar$pendingBgY2 = y2;
            diversifiedchatbar$pendingBgColor = color;
            diversifiedchatbar$hasPendingBg = true;
            return;
        }
        guiGraphics.fill(x1, y1, x2, y2, color);
    }

    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I")
    )
    private int onDrawString(GuiGraphics guiGraphics, Font font, FormattedCharSequence seq, int x, int y, int color)
{
        int adjustedY = y - diversifiedchatbar$extraHeightBelow;

        int alphaInt = (color >>> 24) & 0xFF;
        float alpha = Math.max(0.0F, Math.min(1.0F, alphaInt / 255.0F));

        String text = diversifiedchatbar$extractString(seq);
        List<EmojiParser.EmojiMatch> matches = EmojiParser.parseEmojis(text);
        if (matches.isEmpty())
{
            if (diversifiedchatbar$hasPendingBg)
{
                int adjY1 = diversifiedchatbar$pendingBgY1 - diversifiedchatbar$extraHeightBelow;
                int adjY2 = diversifiedchatbar$pendingBgY2 - diversifiedchatbar$extraHeightBelow;
                guiGraphics.fill(diversifiedchatbar$pendingBgX1, adjY1, diversifiedchatbar$pendingBgX2, adjY2, diversifiedchatbar$pendingBgColor);
                diversifiedchatbar$hasPendingBg = false;
            }
            return guiGraphics.drawString(font, seq, x, adjustedY, color);
        }

        int lineHeight = font.lineHeight + 1;
        int guiHeight = guiGraphics.guiHeight();
        boolean isChatScreen = Minecraft.getInstance().screen instanceof ChatScreen;
        double chatScale = DCBConfig.getEffectiveChatScale();

        int maxSize;
        if (isChatScreen)
{
            double fraction = DCBConfig.getEffectiveScreenFraction();
            maxSize = (int) Math.round(Math.min(guiHeight * fraction, 256) * chatScale);
        }
else
{
            int chatLines = DCBConfig.getEffectiveChatLines();
            maxSize = (int) Math.round(chatLines * lineHeight * chatScale);
        }

        int actualRenderH = 0;
        for (EmojiParser.EmojiMatch match : matches)
{
            if (match.emoji() != null)
{
                int h = Math.min(match.emoji().height(), maxSize);
                if (h > actualRenderH) actualRenderH = h;
            }
        }
        if (actualRenderH <= 0) actualRenderH = maxSize;

        int neededLines = Math.max(1, (int) Math.ceil((double) actualRenderH / lineHeight));
        int extraSpace = Math.max(actualRenderH, (neededLines - 1) * lineHeight) + lineHeight / 2;
        int prevExtraHeightBelow = diversifiedchatbar$extraHeightBelow;

        
        if (diversifiedchatbar$hasPendingBg)
{
            int bgTop = adjustedY - actualRenderH;
            int bgBottom = diversifiedchatbar$pendingBgY2 - prevExtraHeightBelow;
            guiGraphics.fill(diversifiedchatbar$pendingBgX1, bgTop, diversifiedchatbar$pendingBgX2, bgBottom, diversifiedchatbar$pendingBgColor);
            diversifiedchatbar$hasPendingBg = false;
        }

        diversifiedchatbar$extraHeightBelow = prevExtraHeightBelow + extraSpace;

        boolean pureEmoji = diversifiedchatbar$isPureEmojiLine(text, matches);
        if (!pureEmoji)
{
            FormattedCharSequence cleanSeq = diversifiedchatbar$skipShortcodes(seq, matches, text);
            guiGraphics.drawString(font, cleanSeq, x, adjustedY, color);
        }

        
        for (EmojiParser.EmojiMatch match : matches)
{
            Emoji emoji = match.emoji();
            if (emoji != null)
{
                int offsetX = font.width(text.substring(0, match.startIndex()));
                int ex = x + offsetX;
                int renderH = Math.min(emoji.height(), maxSize);
                int renderW = renderH * emoji.width() / Math.max(emoji.height(), 1);
                int renderY = (diversifiedchatbar$pendingBgY2 - prevExtraHeightBelow) - renderH;

                EmojiManager.getInstance().updateAnimatedTexture(emoji);

                guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
                guiGraphics.blit(
                        emoji.textureLocation(),
                        ex, renderY,
                        0, 0,
                        renderW, renderH,
                        renderW, renderH
                );
                guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }

        return 0;
    }

    @Unique
    private static boolean diversifiedchatbar$isPureEmojiLine(String text, List<EmojiParser.EmojiMatch> matches)
{
        StringBuilder clean = new StringBuilder();
        int lastEnd = 0;
        for (EmojiParser.EmojiMatch match : matches)
{
            clean.append(text, lastEnd, match.startIndex());
            lastEnd = match.endIndex();
        }
        clean.append(text.substring(lastEnd));
        return clean.toString().trim().isEmpty();
    }

    @Unique
    private static String diversifiedchatbar$extractString(FormattedCharSequence seq)
{
        StringBuilder sb = new StringBuilder();
        seq.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return sb.toString();
    }

    @Unique
    private static FormattedCharSequence diversifiedchatbar$skipShortcodes(FormattedCharSequence original, List<EmojiParser.EmojiMatch> matches, String text)
{
        boolean[] skip = new boolean[text.length()];
        for (EmojiParser.EmojiMatch m : matches)
{
            for (int i = m.startIndex(); i < m.endIndex() && i < text.length(); i++)
{
                skip[i] = true;
            }
        }
        return (sink) -> {
            int[] pos = {0};
            return original.accept((idx, style, codePoint) -> {
                int curPos = pos[0];
                pos[0] = curPos + Character.charCount(codePoint);
                if (curPos < skip.length && skip[curPos])
{
                    return true;
                }
                return sink.accept(idx, style, codePoint);
            });
        };
    }
}
