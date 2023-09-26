package mega.privacy.android.app.textEditor

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.transfer.TransferAppData

/**
 * Util class related to [TextEditorActivity].
 */
object TextEditorUtil {

    /**
     * Gets the result string to show after trying to upload a text file from [TextEditorActivity].
     *
     * @param textFileUpload   data class containing the data of the upload transfer for giving context.
     * @param isSuccess True if the upload finished with success, false otherwise.
     * @return Result string.
     */
    @JvmStatic
    fun getCreationOrEditorText(
        textFileUpload: TransferAppData.TextFileUpload,
        isSuccess: Boolean,
        context: Context,
    ): String {
        val isEditMode = textFileUpload.mode == TransferAppData.TextFileUpload.Mode.Edit
        val isCloudFile = textFileUpload.fromHomePage

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