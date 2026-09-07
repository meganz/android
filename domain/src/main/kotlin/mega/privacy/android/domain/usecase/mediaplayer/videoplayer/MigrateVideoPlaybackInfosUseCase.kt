package mega.privacy.android.domain.usecase.mediaplayer.videoplayer

import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * Carries the video playback positions held in the legacy DataStore record over to the shared
 * playback info store, once per install.
 *
 * Room's own migrations cannot reach DataStore, so the copy has to happen here. The completion
 * flag is set even when the legacy record is missing or unreadable: bad data must not make the
 * migration retry on every cold start, and a lost playback position is not worth that cost.
 *
 * The legacy record is deliberately left in place. Writes still go to both stores while the read
 * source is behind a flag, so clearing it here would make a rollback resume from nothing.
 *
 * Carried-over positions are held to the same bar as ones written today, so that the store never
 * gains a row that [PersistVideoPlaybackInfoUseCase] would refuse to write and never updates.
 */
class MigrateVideoPlaybackInfosUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() {
        if (mediaPlayerRepository.isVideoPlaybackInfoMigrated()) return

        mediaPlayerRepository.getLegacyVideoPlaybackInfos()
            ?.filter { it.isPlaybackPositionWorthKeeping() }
            ?.takeIf { it.isNotEmpty() }
            ?.let { mediaPlayerRepository.insertMigratedVideoPlaybackInfos(it) }

        mediaPlayerRepository.setVideoPlaybackInfoMigrated()
    }
}
