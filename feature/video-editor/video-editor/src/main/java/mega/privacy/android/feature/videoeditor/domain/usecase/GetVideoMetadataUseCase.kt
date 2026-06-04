package mega.privacy.android.feature.videoeditor.domain.usecase

import mega.privacy.android.feature.videoeditor.data.repository.VideoEditorRepository
import mega.privacy.android.feature.videoeditor.domain.entity.VideoMetadata
import javax.inject.Inject

/**
 * Reads the [VideoMetadata] of the source video identified by `uriString`.
 */
class GetVideoMetadataUseCase @Inject constructor(
    private val videoEditorRepository: VideoEditorRepository,
) {
    suspend operator fun invoke(uriString: String): VideoMetadata =
        videoEditorRepository.getVideoMetadata(uriString)
}
