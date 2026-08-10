package mega.privacy.android.app.utils

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.text.Spanned
import androidx.core.text.HtmlCompat
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.Constants.COPIED_TEXT_LABEL
import mega.privacy.android.app.utils.Constants.EMAIL_ADDRESS
import timber.log.Timber

object TextUtil {

    /**
     * Method to remove the format placeholders.
     * 
     * @param text The string to be processed.
     * @return The processed string.
     */
    fun removeFormatPlaceholder(text: String): String {
        var text = text
        try {
            text = text.replace("[A]", "")
            text = text.replace("[/A]", "")
            text = text.replace("[B]", "")
            text = text.replace("[/B]", "")
            text = text.replace("[C]", "")
            text = text.replace("[/C]", "")
        } catch (e: Exception) {
            Timber.w(e, "Error replacing text. ")
        }
        return text
    }

    /**
     * Add the appropriate format in the call ended chat messages.
     * 
     * @param textToShow The message text
     * @return The formatted text
     */
    fun replaceFormatCallEndedMessage(textToShow: String): Spanned {
        var textToShow = textToShow
        try {
            textToShow = textToShow.replace("[A]", "")
            textToShow = textToShow.replace("[/A]", "")
            textToShow = textToShow.replace("[B]", "<font face=\'sans-serif-medium\'>")
            textToShow = textToShow.replace("[/B]", "</font>")
            textToShow = textToShow.replace("[C]", "")
            textToShow = textToShow.replace("[/C]", "")
        } catch (e: Exception) {
            Timber.e(e.getStackTrace().toString())
        }

        return HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    /**
     * Add appropriate formatting to text on empty screens with chosen colours.
     * 
     * @param textToShow The message text
     * @param colorStart Color
     * @param colorEnd   Color
     * @return The formatted text
     */
    fun replaceFormatText(textToShow: String, colorStart: String?, colorEnd: String?): Spanned {
        var textToShow = textToShow
        try {
            textToShow = textToShow.replace("[A]", "<font color=$colorStart>")
            textToShow = textToShow.replace("[/A]", "</font>")
            textToShow = textToShow.replace("[B]", "<font color=$colorEnd>")
            textToShow = textToShow.replace("[/B]", "</font>")
        } catch (e: Exception) {
            Timber.e(e.stackTrace.toString())
        }

        return HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    /**
     * Check email validity
     * 
     * @param str Email
     * @return Boolean
     */
    @Deprecated("<p> Use {@link IsEmailValidUseCase} instead.")
    fun isEmail(str: String?): Boolean {
        return !str.isNullOrBlank() && EMAIL_ADDRESS.matcher(str).matches()
    }

    /**
     * Gets the latest position of a file name before the .extension in order to set the cursor
     * or select the entire file name.
     * 
     * @param isFile True if is file, false otherwise.
     * @param text   Current text of the input view.
     * @return The latest position of a file name before the .extension.
     */
    fun getCursorPositionOfName(isFile: Boolean, text: String?): Int {
        if (text.isNullOrBlank()) {
            return 0
        }

        if (isFile) {
            val s: Array<String?> =
                text!!.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (s != null) {
                val numParts = s.size
                var lastSelectedPos = 0

                if (numParts > 1) {
                    for (i in 0..<(numParts - 1)) {
                        lastSelectedPos += s[i]!!.length
                        lastSelectedPos++
                    }

                    //The last point should not be selected)
                    lastSelectedPos--
                    return lastSelectedPos
                }
            }
        }

        return text!!.length
    }

    /**
     * Formats a String of an empty screen.
     * 
     * @param context    Current Context object, to get a resource(for example, color)
     * should not use application context, need to pass it from the caller.
     * @param textToShow The text to format.
     * @return The string formatted.
     */
    fun formatEmptyScreenText(context: Context, textToShow: String): Spanned {
        val colorStart = getColorHexString(context, R.color.grey_900_grey_100)
        val colorEnd = getColorHexString(context, R.color.grey_900_grey_100)
        return replaceFormatText(textToShow, colorStart, colorEnd)
    }

    /**
     * Gets the string to show as content of a folder.
     * 
     * @param numFolders The number of folders the folder contains.
     * @param numFiles   The number of files the folder contains.
     * @return The string so show as content of the folder.
     */
    @JvmStatic
    fun getFolderInfo(numFolders: Int, numFiles: Int, context: Context): String {
        if (numFolders == 0 && numFiles == 0) {
            return context.getString(mega.privacy.android.shared.resources.R.string.empty_file_browser_folder)
        } else if (numFolders == 0 && numFiles > 0) {
            return context.getResources().getQuantityString(
                mega.privacy.android.shared.resources.R.plurals.num_of_files_with_parameter,
                numFiles,
                numFiles
            )
        } else if (numFiles == 0 && numFolders > 0) {
            return context.getResources().getQuantityString(
                mega.privacy.android.shared.resources.R.plurals.num_of_folders_with_parameter,
                numFolders,
                numFolders
            )
        } else {
            return context.getResources().getQuantityString(
                mega.privacy.android.shared.resources.R.plurals.num_of_folders_and_num_of_files,
                numFolders,
                numFolders
            ) + context.getResources().getQuantityString(
                mega.privacy.android.shared.resources.R.plurals.num_of_files_with_parameter,
                numFiles,
                numFiles
            )
        }
    }

    /**
     * Gets the string to show as file info details with the next format: "size · date".
     * 
     * @param size The file size.
     * @param date The file modification date.
     * @return The string so show as file info details.
     */
    @JvmStatic
    fun getFileInfo(size: String, date: String?): String {
        return String.format("%s · %s", size, date)
    }

    /**
     * Copies some content to the ClipBoard.
     * 
     * @param activity   Activity from which the content has to be copied.
     * @param textToCopy Content to copy.
     */
    fun copyToClipboard(activity: Activity, textToCopy: String?) {
        val clipManager =
            activity.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

        val clip = ClipData.newPlainText(COPIED_TEXT_LABEL, textToCopy)
        clipManager.setPrimaryClip(clip)
    }
}
