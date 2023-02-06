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
     * flow to monitor states of video compression
     */
    val state: Flow<VideoCompressionState>

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
    suspend fun stop()

    /**
     * start compression
     */
    suspend fun start()

    /**
     * add list to queue
     */
    suspend fun addItems(videoAttachments: List<VideoAttachment>)
}
