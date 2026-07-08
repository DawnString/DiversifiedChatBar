package cn.dawnstring.diversifiedchatbar.emoji;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmojiParser
{
    private static final Pattern SHORTCODE_PATTERN = Pattern.compile(":([a-zA-Z0-9_]+):");

    public static class EmojiMatch
    {
        private final String shortcode;
        private final int startIndex;
        private final int endIndex;
        private final Emoji emoji;

        public EmojiMatch(String shortcode, int startIndex, int endIndex, Emoji emoji)
        {
            this.shortcode = shortcode;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.emoji = emoji;
        }

        public String shortcode() { return shortcode; }
        public int startIndex() { return startIndex; }
        public int endIndex() { return endIndex; }
        public Emoji emoji() { return emoji; }
    }

    public static List<EmojiMatch> parseEmojis(String text)
    {
        List<EmojiMatch> matches = new ArrayList<>();
        if (text == null || text.isEmpty()) return matches;

        EmojiManager manager = EmojiManager.getInstance();
        if (!manager.isLoaded()) return matches;

        Matcher matcher = SHORTCODE_PATTERN.matcher(text);
        while (matcher.find())
        {
            String shortcode = matcher.group(1);
            Emoji emoji = manager.getEmoji(shortcode);
            if (emoji != null)
            {
                matches.add(new EmojiMatch(
                        shortcode, matcher.start(), matcher.end(), emoji));
            }
        }
        return matches;
    }

    public static String replaceWithSpaces(String text)
    {
        EmojiManager manager = EmojiManager.getInstance();
        if (!manager.isLoaded()) return text;

        Matcher matcher = SHORTCODE_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find())
        {
            String shortcode = matcher.group(1);
            Emoji emoji = manager.getEmoji(shortcode);
            if (emoji != null)
            {
                int len = matcher.end() - matcher.start();
                matcher.appendReplacement(sb, " ".repeat(len));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
