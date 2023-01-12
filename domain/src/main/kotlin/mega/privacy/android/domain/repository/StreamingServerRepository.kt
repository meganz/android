package mega.privacy.android.domain.repository

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
}