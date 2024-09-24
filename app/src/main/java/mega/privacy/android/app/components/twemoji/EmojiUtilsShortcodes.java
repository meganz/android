package mega.privacy.android.app.components.twemoji;

import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;

import org.hamcrest.Matchers;

public class EmojiUtilsShortcodes extends AbstractEmoji {

    private static final String HIGH_SURROGATE = "H";
    private static final String HIGH_SURROGATE_FIRST = "H1";
    private static final String HIGH_SURROGATE_SECOND = "H2";
    private static final String LOW_SURROGATE = "L";
    private static final String LOW_SURROGATE_FIRST = "L1";
    private static final String LOW_SURROGATE_SECOND = "L2";

    /**
     * Get emoji by unicode, short code, decimal html entity or hexadecimal html
     * entity
     *
     * @param code unicode, short code, decimal html entity or hexadecimal html
     * @return Emoji
     */
    public static EmojiShortcodes getEmoji(String code) {
        Matcher m = shortCodePattern.matcher(code);
        if (m.find()) {
            code = m.group(1);
        }
        if ((EmojiManagerShortcodes.emojiData != null) && (EmojiManagerShortcodes.emojiData.size() > 0)) {
            for (EmojiShortcodes emoji : EmojiManagerShortcodes.emojiData) {
                if (Matchers.equalTo(code).matches(emoji.getEmoji()) || Matchers.equalToIgnoringCase(code).matches(emoji.getEmoji()) ||
                        Matchers.equalToIgnoringCase(code).matches(emoji.getHexHtml()) || Matchers.equalToIgnoringCase(code).matches(emoji.getDecimalHtml()) ||
                        Matchers.equalToIgnoringCase(code).matches(emoji.getDecimalSurrogateHtml()) || Matchers.equalToIgnoringCase(code).matches(emoji.getHexHtmlShort()) ||
                        Matchers.equalToIgnoringCase(code).matches(emoji.getDecimalHtmlShort()) || Matchers.hasItem(code).matches(emoji.getAliases())) {
                    return emoji;
                }
            }
        }
        return null;
    }

    /**
     * Converts emoji short codes or html entities in string with emojis
     *
     * @param text String to emojify
     * @return emojified String
     */
    public static String emojify(String text) {
        return emojify(text, 0);

    }

    private static String emojify(String text, int startIndex) {
        // remove previous handle since we won't be handling emoticons
        // emotions should be processed in second go.
        // this will avoid conflicts with shortcodes. For Example: :p:p should
        // not be processed as shortcode, but as emoticon
        text = processStringWithRegex(text, shortCodeOrHtmlEntityPattern, startIndex, true);

        return text;
    }

    /**
     * Common method used for processing the string to replace with emojis
     *
     * @param text
     * @param pattern
     * @return
     */
    private static String processStringWithRegex(String text, Pattern pattern, int startIndex, boolean recurseEmojify) {
        //System.out.println(text);
        if(text == null) return null;
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        int resetIndex = 0;

        if (startIndex > 0) {
            matcher.region(startIndex, text.length());
        }

        while (matcher.find()) {
            String emojiCode = matcher.group();
            EmojiShortcodes emoji = getEmoji(emojiCode);
            // replace matched tokens with emojis

            if (emoji != null) {
                matcher.appendReplacement(sb, emoji.getEmoji());
            } else {
                if (htmlSurrogateEntityPattern2.matcher(emojiCode).matches()) {
                    String highSurrogate1 = matcher.group(HIGH_SURROGATE_FIRST);
                    String highSurrogate2 = matcher.group(HIGH_SURROGATE_SECOND);
                    String lowSurrogate1 = matcher.group(LOW_SURROGATE_FIRST);
                    String lowSurrogate2 = matcher.group(LOW_SURROGATE_SECOND);
                    matcher.appendReplacement(sb, processStringWithRegex(highSurrogate1 + highSurrogate2, shortCodeOrHtmlEntityPattern, 0, false));

                    //basically this handles &#junk1;&#10084;&#65039;&#junk2; scenario
                    //verifies if &#junk1;&#10084; or &#junk1; are valid emojis via recursion
                    //if not move past &#junk1; and reset the cursor to &#10084;
                    if (sb.toString().endsWith(highSurrogate2)) {
                        resetIndex = sb.length() - highSurrogate2.length();
                    } else {
                        resetIndex = sb.length();
                    }
                    sb.append(lowSurrogate1);
                    sb.append(lowSurrogate2);
                    break;
                } else if (htmlSurrogateEntityPattern.matcher(emojiCode).matches()) {
                    //could be individual html entities assumed as surrogate pair
                    String highSurrogate = matcher.group(HIGH_SURROGATE);
                    String lowSurrogate = matcher.group(LOW_SURROGATE);
                    matcher.appendReplacement(sb, processStringWithRegex(highSurrogate, htmlEntityPattern, 0, true));
                    resetIndex = sb.length();
                    sb.append(lowSurrogate);
                    break;
                } else {
                    matcher.appendReplacement(sb, emojiCode);
                }
            }

        }

        matcher.appendTail(sb);

        //do not recurse emojify when coming here through htmlSurrogateEntityPattern2..so we get a chance to check if the tail
        //is part of a surrogate entity
        if (recurseEmojify && resetIndex > 0) {
            return emojify(sb.toString(), resetIndex);
        }
        return sb.toString();
    }

    /**
     * Converts emojis, hex, decimal htmls, emoticons in a string to short codes
     *
     * @param text String to shortcodify
     * @return shortcodified string
     */
    public static String shortCodify(String text) {
        String emojifiedText = emojify(text);

        // the emojis
        // could not find an ideal way to identify emojis in the passed string
        // characters like <3 has multiple characters, but doesn't have
        // surrogate pairs
        // so at this point, we iterate through all the emojis and replace with
        // short codes
        for (EmojiShortcodes emoji : EmojiManagerShortcodes.emojiData)
            emojifiedText = emojifiedText.replace(emoji.getEmoji(), ":" + emoji.getAliases().get(0) + ":");
        return emojifiedText;
    }

}
