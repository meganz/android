package mega.privacy.android.app.components.twemoji;

import com.google.code.regexp.Pattern;

abstract class AbstractEmoji {
    static final Pattern shortCodePattern = Pattern.compile(":(\\w+):");

    static final Pattern htmlEntityPattern = Pattern.compile("&#\\w+;");

    static final Pattern htmlSurrogateEntityPattern = Pattern.compile("(?<H>&#\\w+;)(?<L>&#\\w+;)");

    static final Pattern htmlSurrogateEntityPattern2 = Pattern.compile("&#\\w+;&#\\w+;&#\\w+;&#\\w+;");

    static final Pattern shortCodeOrHtmlEntityPattern = Pattern.compile(":\\w+:|(?<H1>&#\\w+;)(?<H2>&#\\w+;)(?<L1>&#\\w+;)(?<L2>&#\\w+;)|(?<H>&#\\w+;)(?<L>&#\\w+;)|&#\\w+;");


    static String htmlifyHelper(String text, boolean isHex, boolean isSurrogate) {

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < text.length(); i++) {
            int ch = text.codePointAt(i);

            if (ch <= 128) {
                sb.appendCodePoint(ch);
            } else if (ch > 128 && (ch < 159 || (ch >= 55296 && ch <= 57343))) {
                continue;
            } else {
                if (isHex) {
                    sb.append("&#x" + Integer.toHexString(ch) + ";");
                } else {
                    if (isSurrogate) {
                        double H = Math.floor((ch - 0x10000) / 0x400) + 0xD800;
                        double L = ((ch - 0x10000) % 0x400) + 0xDC00;
                        sb.append("&#" + String.format("%.0f", H) + ";&#" + String.format("%.0f", L) + ";");
                    } else {
                        sb.append("&#" + ch + ";");
                    }
                }
            }

        }

        return sb.toString();
    }
}
