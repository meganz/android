package mega.privacy.android.app.utils;

import android.text.Spanned;
import androidx.core.text.HtmlCompat;
import mega.privacy.android.app.R;

import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.Constants.EMAIL_ADDRESS;
import static mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString;
import static mega.privacy.android.app.utils.StringResourcesUtils.getString;

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
     * Add the appropriate format in the chat messages.
     *
     * @param textToShow   The message text
     * @param isOwnMessage If it is a sent or received message
     * @return The formatted text
     */
    public static Spanned replaceFormatChatMessages(String textToShow, boolean isOwnMessage) {
        String colorStart = "'#060000'";
        String colorEnd = isOwnMessage ? "'#868686'" : "'#00BFA5'";
        return replaceFormatText(textToShow, colorStart, colorEnd);
    }

    public static Spanned replaceFormatText(String textToShow, String colorStart, String colorEnd) {
        try {
            textToShow = textToShow.replace("[A]", "<font color=" + colorStart + ">");
            textToShow = textToShow.replace("[/A]", "</font>");
            textToShow = textToShow.replace("[B]", "<font color=" + colorEnd + ">");
            textToShow = textToShow.replace("[/B]", "</font>");
        } catch (Exception e) {
            logError(e.getStackTrace().toString());
        }

        return HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY);
    }

    public static boolean isEmail(String str) {
        return !isTextEmpty(str) && EMAIL_ADDRESS.matcher(str).matches();
    }

    /**
     * Gets the string to show as content of a folder.
     *
     * @param numFolders The number of folders the folder contains.
     * @param numFiles   The number of files the folder contains.
     * @return The string so show as content of the folder.
     */
    public static String getFolderInfo(int numFolders, int numFiles) {
        if (numFolders == 0 && numFiles == 0) {
            return getString(R.string.file_browser_empty_folder);
        } else if (numFolders == 0 && numFiles > 0) {
            return getQuantityString(R.plurals.num_files_with_parameter, numFiles, numFiles);
        } else if (numFiles == 0 && numFolders > 0) {
            return getQuantityString(R.plurals.num_folders_with_parameter, numFolders, numFolders);
        } else if (numFolders == 1 && numFiles == 1) {
            return getString(R.string.one_folder_one_file);
        } else if (numFolders == 1 && numFiles > 1) {
            return getString(R.string.one_folder_several_files, numFiles);
        } else {
            return getQuantityString(R.plurals.num_folders_num_files, numFiles, numFolders, numFiles);
        }
    }
}
