package mega.privacy.android.domain.entity

/**
 * Data class for videos that need to be uploaded
 * @property originalPath           The original video path
 * @property newPath                The new video path
 * @property pendingMessageId       The pending message ID
 * @property id                     The record ID
 * @property compressionPercentage  Overall video compression progress
 * @property currentDuration        The video read size
 * @property totalDuration          The video size
 */
data class VideoAttachment(
    val originalPath: String,
    val newPath: String,
    val pendingMessageId: Long?,
    val id: Int?,
) {
    var compressionPercentage: Int = 0
    var currentDuration: Long = 0
    var totalDuration: Long = 0
}
