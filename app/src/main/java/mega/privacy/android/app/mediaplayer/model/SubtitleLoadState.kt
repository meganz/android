package mega.privacy.android.app.mediaplayer.model

/**
 * The subtitle file list load state
 */
sealed class SubtitleLoadState {
    /**
     * Loading state
     */
    object Loading : SubtitleLoadState()

    /**
     * Subtitle file list is empty
     */
    object Empty : SubtitleLoadState()

    /**
     * Get favourite list success
     *
     * @property items subtitle file list
     */
    data class Success(val items: List<SubtitleFileInfoItem>) : SubtitleLoadState()
}