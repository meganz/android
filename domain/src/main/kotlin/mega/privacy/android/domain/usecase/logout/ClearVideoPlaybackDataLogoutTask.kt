package mega.privacy.android.domain.usecase.logout

import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.videosection.ClearRecentlyWatchedVideosUseCase
import javax.inject.Inject

/**
 * Clear video playback data logout task
 */
class ClearVideoPlaybackDataLogoutTask @Inject constructor(
    private val clearRecentlyWatchedVideosUseCase: ClearRecentlyWatchedVideosUseCase,
    private val mediaPlayerRepository: MediaPlayerRepository,
) : LogoutTask {

    /**
     * Invoke
     */
    override suspend fun invoke() {
        clearRecentlyWatchedVideosUseCase()
        mediaPlayerRepository.clearPlaybackInformation()
    }
}