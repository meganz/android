package mega.privacy.android.app.utils;

import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.Constants.EMAIL_ADDRESS;

public class TextUtil {

    public static boolean isTextEmpty(String string) {
        return string == null || string.isEmpty() || string.trim().isEmpty();
    }

    /**
     * Method to remove the format placeholders.
     *
     * @param text The string to be processed.
     * @return The processed string.
     */
    public static String removeFormatPlaceholder(String text) {
        try {
            text = text.replace("[A]", "");
            text = text.replace("[/A]", "");
            text = text.replace("[B]", "");
            text = text.replace("[/B]", "");
            text = text.replace("[C]", "");
            text = text.replace("[/C]", "");
        } catch (Exception e) {
            logWarning("Error replacing text. ", e);
        }
        return text;
    }

    public static boolean isEmail(String str) {
        return !isTextEmpty(str) && EMAIL_ADDRESS.matcher(str).matches();
    }

    /**
     * Gets the latest position of a file name before the .extension in order to set the cursor
     * or select the entire file name.
     *
     * @param isFile True if is file, false otherwise.
     * @param text   Current text of the input view.
     * @return The latest position of a file name before the .extension.
     */
    public static int getCursorPositionOfName(boolean isFile, String text) {
        if (isTextEmpty(text)) {
            return 0;
        }

        if (isFile) {
            String[] s = text.split("\\.");
            if (s != null) {
                int numParts = s.length;
                int lastSelectedPos = 0;

                if (numParts > 1) {
                    for (int i = 0; i < (numParts - 1); i++) {
                        lastSelectedPos += s[i].length();
                        lastSelectedPos++;
                    }

                    //The last point should not be selected)
                    lastSelectedPos--;
                    return lastSelectedPos;
                }
            }
        }

        return text.length();
    }

    /**
     * Formats a String of an empty screen.
     *
     * @param emptyString The text to format.
     * @return The string formatted.
     */
    public static String formatEmptyScreenText(String emptyString) {
        try {
            emptyString = emptyString.replace("[A]", "<font color='#000000'>");
            emptyString = emptyString.replace("[/A]", "</font>");
            emptyString = emptyString.replace("[B]", "<font color='#7a7a7a'>");
            emptyString = emptyString.replace("[/B]", "</font>");
        } catch (Exception e) {
            logWarning("Exception formatting string", e);
        }

        return emptyString;
    }
}
