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
import mega.privacy.android.data.extensions.getThumbnailFileName
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Default implementation of [CacheFolderGateway]
 *
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
        private const val OLD_TEMPORARY_PIC_DIR = "MEGA/MEGA AppTemp"
        private const val OLD_PROFILE_PID_DIR = "MEGA/MEGA Profile Images"
        private const val OLD_ADVANCES_DEVICES_DIR = "MEGA/MEGA Temp"
        private const val OLD_CHAT_TEMPORARY_DIR = "MEGA/MEGA Temp/Chat"
    }

    override fun getCacheFolder(folderName: String): File? =
        runBlocking { getCacheFolderAsync(folderName) }

    private suspend fun getCacheFolderAsync(folderName: String) = withContext(ioDispatcher) {
        val cache =
            if (folderName == CHAT_TEMPORARY_FOLDER) context.filesDir else context.cacheDir
        File(cache, folderName).takeIf { it.exists() || it.mkdir() }
    }

    override fun clearPublicCache() {
        appScope.launch(ioDispatcher) {
            try {
                fileGateway.deleteFolderAndSubFolders(context.externalCacheDir)
            } catch (e: IOException) {
                Timber.e("IOException deleting external cache", e)
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
            Timber.e(e)
            Timber.e("Exception deleting private cache", e)
        }
        clearPublicCache()
    }

    override fun deleteCacheFolderIfEmpty(folderName: String) {
        appScope.launch(ioDispatcher) {
            getCacheFolderAsync(folderName)?.apply {
                if (fileGateway.isFileAvailable(this) && !this.list().isNullOrEmpty()) {
                    this.delete()
                }
            }
        }
    }

    override fun removeOldTempFolder(folderName: String) {
        appScope.launch(ioDispatcher) {
            getOldTempFolder(folderName).takeIf { it.exists() }?.let {
                try {
                    fileGateway.deleteFolderAndSubFolders(it)
                } catch (e: IOException) {
                    Timber.e(e, "Exception deleting ${it.name} directory")
                }
            }
        }
    }

    override fun getOldTempFolder(folderName: String): File {
        val externalStorageVolumes: Array<out File> =
            ContextCompat.getExternalFilesDirs(context, null)
        return File(externalStorageVolumes[0].absolutePath + File.separator + folderName)
    }

    override fun buildAvatarFile(fileName: String?) =
        getCacheFile(AVATAR_FOLDER, fileName)

    override suspend fun buildDefaultDownloadDir(): File = fileGateway.buildDefaultDownloadDir()

    override fun getThumbnailCacheFilePath(megaNode: MegaNode): String? =
        getCacheFolder(CacheFolderConstant.THUMBNAIL_FOLDER)?.let { thumbnail ->
            "$thumbnail${File.separator}${megaNode.getThumbnailFileName()}"
        }?.takeUnless { megaNode.isFolder }

    override fun getPreviewCacheFilePath(megaNode: MegaNode): String? =
        getCacheFolder(CacheFolderConstant.PREVIEW_FOLDER)?.let { thumbnail ->
            "$thumbnail${File.separator}${megaNode.getThumbnailFileName()}"
        }?.takeUnless { megaNode.isFolder }

    override suspend fun removeOldTempFolders() {
        appScope.launch(ioDispatcher) {
            removeOldTempFolder(OLD_TEMPORARY_PIC_DIR)
            removeOldTempFolder(OLD_PROFILE_PID_DIR)
            removeOldTempFolder(OLD_ADVANCES_DEVICES_DIR)
            removeOldTempFolder(OLD_CHAT_TEMPORARY_DIR)
        }
    }

    override suspend fun clearAppData() {
        Timber.d("clearAppData")
        try {
            fileGateway.deleteFolderAndSubFolders(context.filesDir)
        } catch (e: IOException) {
            Timber.e(e)
            Timber.e("Exception deleting private cache", e)
        }
    }

    override val cacheDir: File
        get() = context.cacheDir
}
