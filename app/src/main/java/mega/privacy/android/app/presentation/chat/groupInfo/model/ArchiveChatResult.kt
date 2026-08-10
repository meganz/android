package mega.privacy.android.app.presentation.chat.groupInfo.model

/**
 * Result of an archive/unarchive chat operation.
 *
 * @property success    True if the operation succeeded, false otherwise.
 * @property isArchive  True if the operation was an archive, false if it was an unarchive.
 * @property chatTitle  The formatted chat title, used for the feedback message.
 */
data class ArchiveChatResult(
    val success: Boolean,
    val isArchive: Boolean,
    val chatTitle: String,
)
