package mega.privacy.android.app.components.textFormatter;

import java.util.ArrayList;

public class TextFormatterUtils {
    public static final char NEW_LINE = '\n';
    public static final char SPACE = ' ';
    public static final char BOLD_FLAG = '*';
    public static final char STRIKE_FLAG = '~';
    public static final char ITALIC_FLAG = '_';
    public static final char MONOSPACE_FLAG = '`';
    public static final int INVALID_INDEX = -1;

    public static String getText(ArrayList<Character> characters) {
        char[] chars = new char[characters.size()];

        for (int i = 0; i < chars.length; ++i) {
            chars[i] = characters.get(i);
        }

        return new String(chars);
    }

    public static boolean hasFlagSameLine(CharSequence sequence, char flag, int fromIndex) {
        for (int i = fromIndex; i < sequence.length(); ++i) {
            char c = sequence.charAt(i);
            if (c == NEW_LINE) {
                return false;
            }
            if (c == flag) {
                return i != fromIndex && sequence.charAt(i - 1) != SPACE;
            } else if (c == SPACE && sequence.charAt(i - 1) == flag) {
                return false;
            }
        }

        return false;
    }

    public static class Flag {
        int start;
        int end;
        char flag;

        public Flag(int start, int end, char flag) {
            this.start = start;
            this.end = end;
            this.flag = flag;
        }
    }
}
