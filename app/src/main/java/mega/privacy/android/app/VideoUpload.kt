package mega.privacy.android.app

/**
 * Data class for videos that need to be uploaded
 * @property originalPath           The original video path
 * @property newPath                The new video path
 * @property size                   The video size
 * @property pendingMessageId       The pending message ID
 * @property compressionPercentage  Overall video compression progress
 * @property readSize               The video read size
 */
data class VideoUpload(
    val originalPath: String,
    val newPath: String,
    val size: Long,
    val pendingMessageId: Long,
) {
    var compressionPercentage: Int = 0
    var readSize: Long = 0
}
