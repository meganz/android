package mega.privacy.android.domain.usecase.logout

import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.videosection.ClearRecentlyWatchedVideosUseCase
import javax.inject.Inject

/**
 * Clear audio and video playback data logout task
 */
class ClearPlaybackDataLogoutTask @Inject constructor(
    private val clearRecentlyWatchedVideosUseCase: ClearRecentlyWatchedVideosUseCase,
    private val mediaPlayerRepository: MediaPlayerRepository,
) : LogoutTask {

    /**
     * Invoke
     *
     * Every cleanup runs even when an earlier one fails: a skipped step would leave the previous
     * account's playback positions on disk, which is exactly what this task exists to prevent.
     * Failures are aggregated and rethrown so the caller still reports them.
     */
    override suspend fun onLogoutSuccess() {
        val failures = listOf(
            runCatching { clearRecentlyWatchedVideosUseCase() },
            runCatching { mediaPlayerRepository.clearAllPlaybackInfos() },
            runCatching { mediaPlayerRepository.clearPlaybackInformation() },
        ).mapNotNull { it.exceptionOrNull() }

        failures.firstOrNull()?.let { failure ->
            failures.drop(1).forEach(failure::addSuppressed)
            throw failure
        }
    }
}
