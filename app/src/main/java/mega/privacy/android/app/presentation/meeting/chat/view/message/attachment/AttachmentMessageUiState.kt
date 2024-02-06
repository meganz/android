package mega.privacy.android.app.presentation.meeting.chat.view.message.attachment

import mega.privacy.android.domain.entity.Progress

/**
 * Ui state for attachment messages
 * @property fileTypeResId the icon resource id, null if there is no icon
 * @property fileName
 * @property fileSize in bytes
 * @property previewUri uri string to file preview
 * @property duration String representation of the duration of the file in case it's playable, null otherwise
 * @property loadProgress load progress, null if it's not loading
 */
data class AttachmentMessageUiState(
    val fileTypeResId: Int? = null,
    val fileName: String = "",
    val fileSize: String = "",
    val previewUri: String? = null,
    val duration: String? = null,
    val loadProgress: Progress? = null,
)