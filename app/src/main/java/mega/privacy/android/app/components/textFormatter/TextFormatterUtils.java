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

    /**
     * Create a String from an array of characters
     *
     * @param characters The array of characters.
     * @return The final String.
     */
    public static String getText(ArrayList<Character> characters) {
        char[] chars = new char[characters.size()];

        for (int i = 0; i < chars.length; ++i) {
            chars[i] = characters.get(i);
        }

        return new String(chars);
    }

    /**
     * Method to see if that special character is found again on that same line.
     *
     * @param sequence  The CharSequence to be controlled.
     * @param flag      The element that indicates what type of formatting will be carried out.
     * @param fromIndex The position from which you start checking.
     * @return True, if you find the same formatting element on the same line. False, otherwise.
     */
    public static boolean hasFlagSameLine(CharSequence sequence, char flag, int fromIndex) {
        for (int i = fromIndex; i < sequence.length(); ++i) {
            char c = sequence.charAt(i);
            if (c == NEW_LINE) {
                return false;
            }
            if (c == flag) {
                return i != fromIndex;
            }
            if (c == SPACE) {
                continue;
            }
        }

        return false;
    }

    /**
     * Method to see if the combination "```" is found again in that text.
     *
     * @param sequence  The CharSequence to be controlled.
     * @param flag      The special character that is repeated three times.
     * @param fromIndex The position from which you start checking.
     * @return True, if you find the same formatting element. False, otherwise.
     */
    public static boolean hasMultiMonospaceSameLine(CharSequence sequence, char flag, int fromIndex) {
        for (int i = fromIndex; i < sequence.length(); ++i) {
            char c = sequence.charAt(i);
            if (c == NEW_LINE) {
                return false;
            }
            if (c == flag) {
                if (i != fromIndex && sequence.length() > 6 && sequence.length() > i + 2 &&
                        sequence.charAt(i + 1) == MONOSPACE_FLAG && sequence.charAt(i + 2) == MONOSPACE_FLAG) {
                    if (sequence.length() > i + 3 && sequence.charAt(i + 3) == MONOSPACE_FLAG) {
                        continue;
                    }

                    return true;
                }

                return false;
            }

            if (c == SPACE) {
                continue;
            }
        }

        return false;
    }

    /**
     * Class with the necessary elements to make a sentence formatted. Start, where it begins. End, where it ends. Flag, the formatting element
     */
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
