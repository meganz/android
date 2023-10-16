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
}