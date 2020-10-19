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
}
