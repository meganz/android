package mega.privacy.android.domain.entity.texteditor

/**
 * Result of a text editor save operation.
 * When [UploadRequired], the ViewModel should trigger the transfer event to start the upload.
 */
sealed interface TextEditorSaveResult {

    /**
     * Save wrote content to a temp file; caller should trigger upload with these params.
     */
    data class UploadRequired(
        val tempPath: String,
        val parentHandle: Long,
        val isEditMode: Boolean,
        val fromHome: Boolean,
    ) : TextEditorSaveResult
}
