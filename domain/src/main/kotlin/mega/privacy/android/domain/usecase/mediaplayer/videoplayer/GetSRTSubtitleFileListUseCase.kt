package mega.privacy.android.domain.usecase.mediaplayer.videoplayer

import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for get SRT subtitle file info list
 */
class GetSRTSubtitleFileListUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
) {
    /**
     * Get SRT subtitle file info list
     *
     * @return SRT subtitle file info list
     */
    suspend operator fun invoke(): List<SubtitleFileInfo> =
        mediaPlayerRepository.getSubtitleFileInfoList("*.srt")
}