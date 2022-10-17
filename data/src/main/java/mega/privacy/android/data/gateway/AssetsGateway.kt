package mega.privacy.android.data.gateway

import java.io.InputStream

/**
 * Assets gateway
 *
 */
interface AssetsGateway {
    /**
     * Open
     * 
     * @param filePath in assets folder
     */
    fun open(filePath: String): InputStream
}