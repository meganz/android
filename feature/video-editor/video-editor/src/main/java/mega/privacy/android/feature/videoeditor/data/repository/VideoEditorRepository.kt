package mega.privacy.android.feature.videoeditor.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.feature.videoeditor.data.gateway.VideoMetadataGateway
import mega.privacy.android.feature.videoeditor.domain.entity.VideoMetadata
import javax.inject.Inject

/**
 * Repository for the video editor feature. Currently exposes source metadata,
 * reading it via [VideoMetadataGateway] on the IO dispatcher.
 */
class VideoEditorRepository @Inject constructor(
    private val videoEditorGateway: VideoMetadataGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun getVideoMetadata(uriString: String): VideoMetadata =
        withContext(ioDispatcher) { videoEditorGateway.getVideoMetadata(uriString) }
}
