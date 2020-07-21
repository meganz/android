package mega.privacy.android.app.utils;

import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.Constants.EMAIL_ADDRESS;

public class TextUtil {

    public static boolean isTextEmpty(String string) {
        return string == null || string.isEmpty() || string.trim().isEmpty();
    }

    /**
     * Method for the treatment of plurals in the strings
     *
     * @param text The string to update.
     * @return The updated string.
     */
    public static String getStringPlural(String text) {
        try {
            text = text.replace("[A]", "");
            text = text.replace("[/A]", "");
            text = text.replace("[B]", "");
            text = text.replace("[/B]", "");
            text = text.replace("[C]", "");
            text = text.replace("[/C]", "");
        } catch (Exception e) {
            logWarning("Error replacing text.");
        }
        return text;
    }

    public static boolean isEmail(String str) {
        return EMAIL_ADDRESS.matcher(str).matches();
    }
}
