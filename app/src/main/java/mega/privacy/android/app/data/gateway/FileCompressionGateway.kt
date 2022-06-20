package mega.privacy.android.app.data.gateway

import java.io.File

/**
 * File compression gateway
 *
 */
interface FileCompressionGateway {
    /**
     * Zip folder
     *
     * @param sourceFolder
     * @param zipFile
     */
    suspend fun zipFolder(sourceFolder: File, zipFile: File)
}