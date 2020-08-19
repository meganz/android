package mega.privacy.android.app.components.textFormatter;

import java.util.ArrayList;

public class textFormatterUtils {
    public static final char NEW_LINE = '\n';
    public static final char SPACE = ' ';
    public static final char BOLD_FLAG = '*';
    public static final char STRIKE_FLAG = '~';
    public static final char ITALIC_FLAG = '_';
    public static final String MONOSPACE_FLAG = "```";
    public static final int INVALID_INDEX = -1;

    public textFormatterUtils() {
    }

    public static boolean isFlagged(CharSequence text, int index) {
        if (index > -1 && index < text.length()) {
            char c = text.charAt(index);
            return c == SPACE || c == NEW_LINE || c == BOLD_FLAG || c == ITALIC_FLAG || c == STRIKE_FLAG;
        } else {
            return true;
        }
    }

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
                return i != fromIndex;
            }else if(c == SPACE){
                int previous = sequence.charAt(i-1);
                if(previous!=-1 && previous == flag){
                    return false;
                }

                if(i == sequence.length()-2 && sequence.charAt(sequence.length()-1) == flag){
                    return false;
                }
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
