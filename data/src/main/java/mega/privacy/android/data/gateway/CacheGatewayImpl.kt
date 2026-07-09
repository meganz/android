package mega.privacy.android.data.gateway

import android.content.Context
import androidx.annotation.VisibleForTesting
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.domain.qualifier.IoDispatcher
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Cache Gateway implementation
 *
 * @property context
 * @property ioDispatcher
 * @property deviceGateway
 */
internal class CacheGatewayImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val deviceGateway: DeviceGateway,
) : CacheGateway {

    /**
     * Cache folder paths to avoid multiple IO operations
     */
    private val pathCache = ConcurrentHashMap<String, String>()

    private val pathCacheRefreshedAtNanos =
        AtomicLong(deviceGateway.nanoTime - REVALIDATE_INTERVAL.inWholeNanoseconds)

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
            clearPathCache()
            val dir = context.cacheDir
            dir.list()?.forEach {
                deleteDir(File(dir, it))
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override suspend fun getThumbnailCacheFolderPath(): String? =
        getCachedFolderPath(CacheFolderConstant.THUMBNAIL_FOLDER)

    override suspend fun getPreviewCacheFolderPath(): String? =
        getCachedFolderPath(CacheFolderConstant.PREVIEW_FOLDER)

    override suspend fun getFullSizeCacheFolderPath(): String? =
        getCachedFolderPath(CacheFolderConstant.TEMPORARY_FOLDER)

    private suspend fun getCachedFolderPath(folderName: String): String? {
        val refreshedAtNanos = pathCacheRefreshedAtNanos.get()
        val nowNanos = deviceGateway.nanoTime
        // CAS so that concurrent lookups trigger at most one refresh per interval
        if ((nowNanos - refreshedAtNanos).nanoseconds >= REVALIDATE_INTERVAL &&
            pathCacheRefreshedAtNanos.compareAndSet(refreshedAtNanos, nowNanos)
        ) {
            pathCache.clear()
        }
        return pathCache[folderName]
            ?: getOrCreateCacheFolder(folderName)?.path?.also { pathCache[folderName] = it }
    }

    override suspend fun clearPathCache() {
        pathCache.clear()
        pathCacheRefreshedAtNanos.set(deviceGateway.nanoTime)
    }

    override suspend fun getCameraUploadsCacheFolder(): File? =
        getOrCreateCacheFolder(CacheFolderConstant.CAMERA_UPLOADS_CACHE_FOLDER)

    override suspend fun buildAvatarFile(fileName: String?) =
        fileName?.let { getCacheFile(CacheFolderConstant.AVATAR_FOLDER, it) }

    override suspend fun clearAppData(excludeFileNames: Set<String>) {
        try {
            clearPathCache()
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

    companion object {
        private const val VOICE_CLIP_FOLDER = "voiceClipsMEGA"

        /**
         * How long cached paths are trusted before being dropped and resolved from disk again.
         * The system or the user can delete the cache directory at any time without notifying
         * the app, so a cached path may go stale; a bounded refresh keeps the worst case to
         * one interval.
         */
        @VisibleForTesting
        internal val REVALIDATE_INTERVAL = 10.seconds
    }
}
