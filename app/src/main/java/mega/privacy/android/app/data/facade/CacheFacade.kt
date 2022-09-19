package mega.privacy.android.app.data.facade

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.gateway.CacheGateway
import mega.privacy.android.app.di.IoDispatcher
import java.io.File
import javax.inject.Inject

/**
 * Cache folder implementation
 *
 * @property context
 * @property ioDispatcher
 */
class CacheFacade @Inject constructor(
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
}