package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.CacheRepository
import java.io.File
import javax.inject.Inject

/**
 * Implementation of [CacheRepository]
 */
internal class CacheRepositoryImpl @Inject constructor(
    private val cacheFolderGateway: CacheFolderGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CacheRepository {
    override suspend fun getCacheSize(): Long = withContext(ioDispatcher) {
        cacheFolderGateway.getCacheSize()
    }

    override suspend fun clearCache() = withContext(ioDispatcher) {
        cacheFolderGateway.clearCache()
    }

    override fun getCacheFile(folderName: String, fileName: String): File? =
        cacheFolderGateway.getCacheFile(folderName, fileName)

    override suspend fun getCacheFolder(folderName: String): File? = withContext(ioDispatcher) {
        cacheFolderGateway.getCacheFolderAsync(folderName)
    }

    override suspend fun getPreviewFile(fileName: String): File? =
        cacheFolderGateway.getPreviewFile(fileName)

    override suspend fun getPreviewDownloadPathForNode(): String =
        cacheFolderGateway.getPreviewDownloadPathForNode()

    override fun isFileInCacheDirectory(file: File) =
        cacheFolderGateway.isFileInCacheDirectory(file)

    override fun getCacheFolderNameForUpload(isForChat: Boolean) =
        if (isForChat) {
            CacheFolderConstant.CHAT_TEMPORARY_FOLDER
        } else {
            CacheFolderConstant.TEMPORARY_FOLDER
        }
}