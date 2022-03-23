package mega.privacy.android.app.utils;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Spanned;
import androidx.core.text.HtmlCompat;

import mega.privacy.android.app.BaseActivity;
import mega.privacy.android.app.R;

import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.Constants.COPIED_TEXT_LABEL;
import static mega.privacy.android.app.utils.Constants.STRING_SEPARATOR;
import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.Constants.EMAIL_ADDRESS;
import static mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString;
import static mega.privacy.android.app.utils.StringResourcesUtils.getString;

public class TextUtil {

    private static final String COPIED = "Copied Text";

    public static boolean isTextEmpty(String string) {
        return string == null || string.isEmpty() || string.trim().isEmpty();
    }

    public static boolean isTextEmpty(StringBuilder string) {
        if (string == null)
            return true;

        return isTextEmpty(string.toString());
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
        String colorStart = ColorUtils.getColorHexString(context, R.color.grey_900_grey_100);
        String colorEnd = isOwnMessage ?
                ColorUtils.getColorHexString(context, R.color.grey_500_grey_400) :
                ColorUtils.getThemeColorHexString(context, R.attr.colorSecondary);
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
     * Formats a String of notification screen.
     *
     * @param context Current Context object, to get a resource(for example, color)
     *                should not use application context, need to pass it from the caller.
     * @param text    The text to format.
     * @return The string formatted.
     */
    public static Spanned replaceFormatNotificationText(Context context, String text) {
        try {
            text = text.replace("[A]", "<font color='"
                    + ColorUtils.getColorHexString(context, R.color.grey_900_grey_100)
                    + "'>");
            text = text.replace("[/A]", "</font>");
            text = text.replace("[B]", "<font color='"
                    + ColorUtils.getColorHexString(context, R.color.grey_500_grey_400)
                    + "'>");

            text = text.replace("[/B]", "</font>");
        } catch (Exception e) {
            logWarning("Error replacing text. ", e);
        }

        return HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY);
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
     * @param context     Current Context object, to get a resource(for example, color)
     *                    should not use application context, need to pass it from the caller.
     * @param textToShow The text to format.
     * @return The string formatted.
     */
    public static String formatEmptyScreenText(Context context, String textToShow) {
        String colorStart = ColorUtils.getColorHexString(context, R.color.grey_900_grey_100);
        String colorEnd =  ColorUtils.getColorHexString(context, R.color.grey_300_grey_600);
        return replaceFormatText(textToShow, colorStart, colorEnd).toString();
    }

    public static Spanned formatEmptyRecentChatsScreenText(Context context, String textToShow) {
        String colorStart = ColorUtils.getColorHexString(context, R.color.grey_300_grey_600);
        String colorEnd =  ColorUtils.getColorHexString(context, R.color.grey_900_grey_100);
        return replaceFormatText(textToShow, colorStart, colorEnd);
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
     * Gets the string to show as file info details with the next format: "size · date".
     *
     * @param size The file size.
     * @param date The file modification date.
     * @return The string so show as file info details.
     */
    public static String getFileInfo(String size, String date) {
        return String.format("%s · %s", size, date);
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

    /**
     * Copies some content to the ClipBoard.
     *
     * @param activity   Activity from which the content has to be copied.
     * @param textToCopy Content to copy.
     */
    public static void copyToClipboard(Activity activity, String textToCopy) {
        ClipboardManager clipManager =
                (ClipboardManager) activity.getSystemService(BaseActivity.CLIPBOARD_SERVICE);

        ClipData clip = ClipData.newPlainText(COPIED_TEXT_LABEL, textToCopy);
        clipManager.setPrimaryClip(clip);
    }
}
