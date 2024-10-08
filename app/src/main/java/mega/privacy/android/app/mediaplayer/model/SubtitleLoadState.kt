package mega.privacy.android.app.mediaplayer.model

import mega.privacy.android.domain.entity.AccountType

/**
 * The subtitle file list load state
 */
sealed class SubtitleLoadState {
    /**
     * Loading state
     */
    data object Loading : SubtitleLoadState()

    /**
     * Subtitle file list is empty
     */
    data object Empty : SubtitleLoadState()

    /**
     * Get favourite list success
     *
     * @property items subtitle file list
     * @property accountType
     * @property isBusinessAccountExpired
     */
    data class Success(
        val items: List<SubtitleFileInfoItem>,
        val accountType: AccountType?,
        val isBusinessAccountExpired: Boolean,
    ) : SubtitleLoadState()
}