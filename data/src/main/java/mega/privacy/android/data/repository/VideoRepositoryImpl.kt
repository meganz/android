package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.flowOn
import mega.privacy.android.data.gateway.VideoCompressorGateway
import mega.privacy.android.domain.entity.VideoAttachment
import mega.privacy.android.domain.entity.VideoCompressionState
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.VideoRepository

internal class VideoRepositoryImpl(
    private val videoCompressorGateway: VideoCompressorGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : VideoRepository {

    override fun compressVideo(
        root: String,
        filePath: String,
        newFilePath: String,
        quality: VideoQuality,
    ): Flow<VideoCompressionState> =
        videoCompressorGateway.apply {
            setOutputRoot(root)
            setVideoQuality(quality)
            addItems(
                listOf(
                    VideoAttachment(
                        filePath,
                        newFilePath,
                        id = null,
                        pendingMessageId = null,
                    )
                )
            )
        }.start()
            .cancellable()
            .flowOn(ioDispatcher)


}
