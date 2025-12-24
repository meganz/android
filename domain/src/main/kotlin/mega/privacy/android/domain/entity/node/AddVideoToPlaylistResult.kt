package mega.privacy.android.domain.entity.node

/**
 * Result of adding a video to a playlist.
 *
 * @property message A message indicating the result of the operation.
 * @property isRetry A flag indicating whether the operation can be retried.
 * @property videoHandle The handle of the video that was added to the playlist.
 */
data class AddVideoToPlaylistResult(
    val message: String = "",
    val isRetry: Boolean = false,
    val videoHandle: Long = -1,
)
