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
        private const val VOICE_CLIP_FOLDER = "voiceClipsMEGA"
    }

    override suspend fun getOrCreateCacheFolder(folderName: String): File? =
        withContext(ioDispatcher) {
            File(context.cacheDir, folderName).takeIf { it.exists() || it.mkdir() }
        }

    override suspend fun getOrCreateChatCacheFolder(): File? = withContext(ioDispatcher) {
        File(
            context.filesDir,
            CacheFolderConstant.CHAT_TEMPORARY_FOLDER
        ).takeIf { it.exists() || it.mkdir() }
    }

    override suspend fun getCacheFile(folderName: String, fileName: String): File? {
        val formattedName = if (folderName == CacheFolderConstant.THUMBNAIL_FOLDER) {
            // Thumbnail file name does not contain extension, because it can be either JPG or PNG
            fileName.substringBeforeLast(".")
        } else {
            fileName
        }

        return getOrCreateCacheFolder(folderName)
            ?.takeIf { it.exists() }
            ?.let { folder -> File(folder, formattedName) }
    }

    override suspend fun clearCacheDirectory() {
        try {
            val dir = context.cacheDir
            dir.list()?.forEach {
                deleteDir(File(dir, it))
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override suspend fun getThumbnailCacheFolder(): File? =
        getOrCreateCacheFolder(CacheFolderConstant.THUMBNAIL_FOLDER)

    override suspend fun getPreviewCacheFolder(): File? =
        getOrCreateCacheFolder(CacheFolderConstant.PREVIEW_FOLDER)

    override suspend fun getFullSizeCacheFolder(): File? =
        getOrCreateCacheFolder(CacheFolderConstant.TEMPORARY_FOLDER)

    override suspend fun getCameraUploadsCacheFolder(): File? =
        getOrCreateCacheFolder(CacheFolderConstant.CAMERA_UPLOADS_CACHE_FOLDER)

    override suspend fun buildAvatarFile(fileName: String?) =
        fileName?.let { getCacheFile(CacheFolderConstant.AVATAR_FOLDER, it) }

    override suspend fun clearAppData(excludeFileNames: Set<String>) {
        try {
            val dir = context.filesDir
            dir.list()?.asSequence()
                ?.filter { !excludeFileNames.contains(it) }
                ?.forEach {
                    deleteDir(File(dir, it))
                }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override suspend fun clearSdkCache() {
        try {
            val dir = context.dataDir
            dir.listFiles()?.forEach {
                it.delete()
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override suspend fun getVoiceClipFile(name: String): File? =
        getCacheFile(VOICE_CLIP_FOLDER, name)?.let {
            if (it.exists()) it else null
        }

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