package mega.privacy.android.data.gateway

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.VideoAttachment
import mega.privacy.android.domain.entity.VideoCompressionState
import mega.privacy.android.domain.entity.VideoQuality


/**
 * Video Compression Gateway
 */
internal interface VideoCompressorGateway {

    /**
     * set video quality
     */
    fun setVideoQuality(videoQuality: VideoQuality)

    /**
     * set output root
     */
    fun setOutputRoot(root: String)

    /**
     * check whether it is running or not
     */
    fun isRunning(): Boolean

    /**
     * stop compression
     */
    fun stop()

    /**
     * start compression
     */
    fun start(): Flow<VideoCompressionState>

    /**
     * add list to queue
     */
    fun addItems(videoAttachments: List<VideoAttachment>)
}
