package mega.privacy.android.domain.repository

import java.net.URL

/**
 * Http connection repository
 */
interface HttpConnectionRepository {
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
}