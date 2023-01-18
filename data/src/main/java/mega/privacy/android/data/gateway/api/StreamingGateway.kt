package mega.privacy.android.data.gateway.api

import nz.mega.sdk.MegaNode


/**
 * Gateway for streaming nodes via an http proxy server
 */
interface StreamingGateway {
    /**
     * Returns a URL to a node in the local HTTP proxy server
     *
     * @param node Node to generate the local HTTP link
     * @return URL to the node in the local HTTP proxy server, otherwise NULL
     */
    suspend fun getLocalLink(node: MegaNode): String?

    /**
     * Returns the current port of the server
     *
     * @return 0 if the server is not running. Otherwise the port in which it's listening to
     */
    suspend fun getPort(): Int

    /**
     * Start an HTTP proxy server
     *
     * @return True if the server is ready, false if the initialization failed
     */
    suspend fun startServer(): Boolean

    /**
     * Set the maximum buffer size for the internal buffer
     *
     * @param bufferSize Maximum buffer size (in bytes) or a number <= 0 to use the
     *                   internal default value
     */
    suspend fun setMaxBufferSize(bufferSize: Int)

    /**
     * Stop the HTTP proxy server
     */
    suspend fun stopServer()
}