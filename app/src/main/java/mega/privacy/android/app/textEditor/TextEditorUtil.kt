package mega.privacy.android.app.textEditor

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.textEditor.TextEditorViewModel.Companion.EDIT_MODE
import mega.privacy.android.app.utils.Constants.APP_DATA_INDICATOR

/**
 * Util class related to [TextEditorActivity].
 */
object TextEditorUtil {

    /**
     * Gets the result string to show after trying to upload a text file from [TextEditorActivity].
     *
     * @param appData   String containing the data of the upload transfer for giving context.
     * @param isSuccess True if the upload finished with success, false otherwise.
     * @return Result string.
     */
    @JvmStatic
    fun getCreationOrEditorText(appData: String, isSuccess: Boolean, context: Context): String {
        val appDataParts = appData.split(APP_DATA_INDICATOR).toTypedArray()
        val isEditMode = appDataParts[1] == EDIT_MODE
        val isCloudFile = appDataParts[2].toBoolean()

        return context.getString(
            when {
                isSuccess && isEditMode -> R.string.file_updated
                isEditMode -> R.string.file_update_failed
                isSuccess && isCloudFile -> R.string.text_editor_creation_success
                isCloudFile -> R.string.text_editor_creation_error
                isSuccess -> R.string.file_created
                else -> R.string.file_creation_failed
            }
        )
    }
}