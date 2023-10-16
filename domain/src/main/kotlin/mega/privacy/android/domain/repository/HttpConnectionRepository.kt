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
}