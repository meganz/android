package mega.privacy.android.app.utils;

import android.text.Spanned;

import androidx.core.text.HtmlCompat;

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

    /**
     * Add the appropriate format in the  chat management messages.
     *
     * @param textToShow   The message text
     * @param isOwnMessage If it is a sent or received message
     * @return The formatted text
     */
    public static Spanned replaceFormatChatMessages(String textToShow, boolean isOwnMessage) {
        try {
            textToShow = textToShow.replace("[A]", "<font color='#060000'>");
            textToShow = textToShow.replace("[/A]", "</font>");
            if (isOwnMessage) {
                textToShow = textToShow.replace("[B]", "<font color='#868686'>");
            } else {
                textToShow = textToShow.replace("[B]", "<font color='#00BFA5'>");
            }
            textToShow = textToShow.replace("[/B]", "</font>");
        } catch (Exception e) {
            logWarning("Error replacing text. ", e);
        }

        return HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY);
    }

    public static boolean isEmail(String str) {
        return !isTextEmpty(str) && EMAIL_ADDRESS.matcher(str).matches();
    }
}
