package mega.privacy.android.data.gateway

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.domain.qualifier.IoDispatcher
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Cache Gateway implementation
 *
 * @property context
 * @property ioDispatcher
 */
internal class CacheGatewayImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CacheGateway {

    companion object {
        private const val CHAT_TEMPORARY_FOLDER = "chatTempMEGA"
    }

    override suspend fun getOrCreateCacheFolder(folderName: String): File? =
        withContext(ioDispatcher) {
            File(context.cacheDir, folderName).takeIf { it.exists() || it.mkdir() }
        }

    override suspend fun getOrCreateChatCacheFolder(): File? = withContext(ioDispatcher) {
        File(context.filesDir, CHAT_TEMPORARY_FOLDER).takeIf { it.exists() || it.mkdir() }
    }

    override suspend fun getCacheFile(folderName: String, fileName: String): File? =
        withContext(ioDispatcher) {
            return@withContext getOrCreateCacheFolder(folderName)?.takeIf { it.exists() }?.let {
                File(it, fileName)
            }
        }

    override suspend fun clearCacheDirectory() {
        withContext(ioDispatcher) {
            try {
                val dir = context.cacheDir
                dir.list()?.forEach {
                    deleteDir(File(dir, it))
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    override suspend fun getThumbnailCacheFolder(): File? =
        getOrCreateCacheFolder(CacheFolderConstant.THUMBNAIL_FOLDER)

    override suspend fun getPreviewCacheFolder(): File? =
        getOrCreateCacheFolder(CacheFolderConstant.PREVIEW_FOLDER)

    override suspend fun getFullSizeCacheFolder(): File? =
        getOrCreateCacheFolder(CacheFolderConstant.TEMPORARY_FOLDER)

    /**
     * @param dir [File] indicates current directory or file
     * @return Boolean true if the delete operation is successful otherwise false
     */
    private fun deleteDir(dir: File): Boolean {
        return if (dir.isDirectory) {
            dir.deleteRecursively()
        } else if (dir.isFile) {
            dir.delete()
        } else {
            false
        }
    }
}