package mega.privacy.android.data.gateway

import java.net.URL

/**
 * Http connection gateway
 */
interface HttpConnectionGateway {
    /**
     * Get bytes of data from given url
     */
    suspend fun getDataBytesFromUrl(url: URL): ByteArray?

    /**
     * Get at most [maxBytes] bytes from the start of the given url using an HTTP Range request.
     *
     * @return the read bytes, or null if the connection failed.
     */
    suspend fun getDataBytesFromUrl(url: URL, maxBytes: Int): ByteArray?

    /**
     * Get the full content of the given url, refusing content larger than [maxBytes] instead of
     * truncating it. Oversized content is rejected before it is fully buffered.
     *
     * @return the content, or null if the connection failed.
     * @throws mega.privacy.android.domain.exception.DataTooLargeException when the content is
     * larger than [maxBytes]
     */
    suspend fun getDataBytesFromUrlWithinLimit(url: URL, maxBytes: Int): ByteArray?
}