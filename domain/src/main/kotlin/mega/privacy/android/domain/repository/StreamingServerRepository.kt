package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.node.Node

/**
 * Streaming server repository
 */
interface StreamingServerRepository {
    /**
     * Start server
     *
     */
    suspend fun startServer()

    /**
     * Stop server
     *
     */
    suspend fun stopServer()

    /**
     * Set max buffer size
     *
     * @param bufferSize
     */
    suspend fun setMaxBufferSize(bufferSize: Int)

    /**
     * Get file streaming uri for a node
     *
     * @param node
     * @return local url string if found
     */
    suspend fun getFileStreamingUri(node: Node): String?

    /**
     * Get file streaming uri for a node handle
     *
     * @param nodeHandle node handle
     * @return local url string if found
     */
    suspend fun getFileStreamingUri(nodeHandle: Long): String?

    /**
     * Get file streaming uri for a folder link node, resolved and authorised through the folder
     * API but served by the main API.
     *
     * @param nodeHandle node handle
     * @return local url string if found
     */
    suspend fun getFolderLinkFileStreamingUri(nodeHandle: Long): String?

    /**
     * Get file streaming uri for a folder link node, resolved and served entirely through the
     * folder API.
     *
     * @param nodeHandle node handle
     * @return local url string if found
     */
    suspend fun getFolderLinkFileStreamingUriFromFolderApi(nodeHandle: Long): String?

    /**
     * Whether the most recent streaming request was for audio or video.
     *
     * The SDK over quota event carries no node handle, so this correlates by recency: with several
     * streams in flight it reflects the last one requested, not necessarily the one that hit the
     * quota.
     *
     * @return true when the last streaming request was for media, false when it was for other
     * content or nothing has been streamed yet.
     */
    fun isLastStreamedContentMedia(): Boolean
}