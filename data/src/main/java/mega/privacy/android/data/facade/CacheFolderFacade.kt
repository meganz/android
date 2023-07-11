package mega.privacy.android.data.facade

import android.content.Context
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Default implementation of [CacheFolderGateway]
 *
 * @property context [Context]
 * @property fileGateway [FileGateway]
 * @property appScope [CoroutineScope]
 * @property ioDispatcher [CoroutineDispatcher]
 */
internal class CacheFolderFacade @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileGateway: FileGateway,
    @ApplicationScope private val appScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CacheFolderGateway {

    companion object {
        private const val CHAT_TEMPORARY_FOLDER = "chatTempMEGA"
        private const val AVATAR_FOLDER = "avatarsMEGA"
        private const val CAMERA_UPLOADS_CACHE_FOLDER = "cu"
    }

    override fun getCacheFolder(folderName: String): File? =
        runBlocking { getCacheFolderAsync(folderName) }

    override suspend fun getCacheFolderAsync(folderName: String) = withContext(ioDispatcher) {
        val cache =
            if (folderName == CHAT_TEMPORARY_FOLDER) context.filesDir else context.cacheDir
        File(cache, folderName).takeIf { it.exists() || it.mkdir() }
    }

    override fun clearPublicCache() {
        appScope.launch(ioDispatcher) {
            try {
                fileGateway.deleteFolderAndSubFolders(context.externalCacheDir)
            } catch (e: Exception) {
                Timber.e("Exception deleting external cache", e)
            }
        }
    }

    override fun createCacheFolder(name: String) {
        appScope.launch(ioDispatcher) {
            getCacheFolderAsync(name)?.takeIf {
                it.exists()
            }?.let { nonNullFile ->
                Timber.d("${nonNullFile.name} folder created: ${nonNullFile.absolutePath}")
            } ?: run {
                Timber.d("Create $name file failed")
            }
        }
    }

    override fun getCacheFile(folderName: String, fileName: String?): File? {
        return getCacheFolder(folderName)?.takeIf { it.exists() }?.let {
            fileName?.let { nonNullFileName ->
                File(it, nonNullFileName)
            }
        }
    }

    override suspend fun getCacheFileAsync(folderName: String, fileName: String?) =
        getCacheFolderAsync(folderName)?.takeIf { it.exists() }?.let {
            fileName?.let { nonNullFileName ->
                File(it, nonNullFileName)
            }
        }

    override fun getCacheSize(): Long = runBlocking {
        Timber.d("getCacheSize")
        val cacheIntDir = context.cacheDir
        val cacheExtDir = context.externalCacheDir
        cacheIntDir?.let {
            Timber.d("Path to check internal: ${it.absolutePath}")
        }
        return@runBlocking fileGateway.getDirSize(cacheIntDir)
            .plus(fileGateway.getDirSize(cacheExtDir))
    }

    override suspend fun clearCache() {
        Timber.d("clearCache")
        try {
            fileGateway.deleteFolderAndSubFolders(context.cacheDir)
        } catch (e: IOException) {
            Timber.e("Exception deleting private cache", e)
        }
        clearPublicCache()
    }

    override fun deleteCacheFolderIfEmpty(folderName: String) {
        appScope.launch(ioDispatcher) {
            getCacheFolderAsync(folderName)?.apply {
                if (fileGateway.isFileAvailable(this) && this.list().isNullOrEmpty()) {
                    this.delete()
                }
            }
        }
    }

    override suspend fun buildDefaultDownloadDir(): File = fileGateway.buildDefaultDownloadDir()

    override suspend fun getCameraUploadsCacheFolder(): File =
        File(cacheDir, CAMERA_UPLOADS_CACHE_FOLDER)

    override val cacheDir: File
        get() = context.cacheDir
}
