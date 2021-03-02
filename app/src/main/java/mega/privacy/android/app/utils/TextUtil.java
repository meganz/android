package mega.privacy.android.app.utils;

import android.content.Context;
import android.text.Spanned;
import androidx.core.text.HtmlCompat;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;

import static mega.privacy.android.app.utils.Constants.STRING_SEPARATOR;
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
     * @param context Current Context object, to get a resource(for example, color) should not use application context, need to pass it from the caller.
     * @param textToShow   The message text
     * @param isOwnMessage If it is a sent or received message
     * @return The formatted text
     */
    public static Spanned replaceFormatChatMessages(Context context, String textToShow, boolean isOwnMessage) {
        try {
            textToShow = textToShow.replace("[A]", "<font color=\'"
                    + ColorUtils.getColorHexString(context, R.color.grey_900_grey_100)
                    + "\'>");
            textToShow = textToShow.replace("[/A]", "</font>");
            if (isOwnMessage) {
                textToShow = textToShow.replace("[B]", "<font color=\'"
                        + ColorUtils.getColorHexString(context, R.color.grey_500_grey_400)
                        + "\'>");
            } else {
                textToShow = textToShow.replace("[B]", "<font color=\'"
                        + ColorUtils.getThemeColorHexString(context, R.attr.colorSecondary)
                        + "\'>");
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

    /**
     * If the string received is not null, neither empty, adds a STRING_SEPARATOR at the end.
     *
     * @param text Initial text without separator.
     * @return Text with separator.
     */
    public static String addStringSeparator(String text) {
        return isTextEmpty(text) ? text : text + STRING_SEPARATOR;
    }
}
