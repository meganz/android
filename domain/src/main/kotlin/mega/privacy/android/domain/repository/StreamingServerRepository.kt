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
}