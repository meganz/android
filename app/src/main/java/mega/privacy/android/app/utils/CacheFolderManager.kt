package mega.privacy.android.app.utils

import android.content.Context
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.di.EntryPointsModule
import mega.privacy.android.data.gateway.CacheFolderGateway
import java.io.File

/**
 * CacheFolder Manager
 */
object CacheFolderManager {
    /**
     * THUMBNAIL_FOLDER
     */
    const val THUMBNAIL_FOLDER = "thumbnailsMEGA"

    /**
     * PREVIEW_FOLDER
     */
    const val PREVIEW_FOLDER = "previewsMEGA"

    /**
     * AVATAR_FOLDER
     */
    const val AVATAR_FOLDER = "avatarsMEGA"

    /**
     * QR_FOLDER
     */
    private const val QR_FOLDER = "qrMEGA"

    /**
     * VOICE_CLIP_FOLDER
     */
    const val VOICE_CLIP_FOLDER = "voiceClipsMEGA"

    /**
     * TEMPORARY_FOLDER
     */
    const val TEMPORARY_FOLDER = "tempMEGA"

    /**
     * CHAT_TEMPORARY_FOLDER
     */
    const val CHAT_TEMPORARY_FOLDER = "chatTempMEGA"

    /**
     * CacheFolder Gateway
     */
    val cacheFolderGateway: CacheFolderGateway by lazy {
        EntryPointAccessors.fromApplication(
            MegaApplication.getInstance(),
            EntryPointsModule.CacheFolderManagerEntryPoint::class.java
        ).cacheFolderGateway
    }

    /**
     * Get Cache Folder given folder Name
     */
    @JvmStatic
    fun getCacheFolder(folderName: String): File? {
        return cacheFolderGateway.getCacheFolder(folderName)
    }

    /**
     * Create Cache Folders
     */
    @JvmStatic
    fun createCacheFolders() {
        cacheFolderGateway.apply {
            createCacheFolder(THUMBNAIL_FOLDER)
            createCacheFolder(PREVIEW_FOLDER)
            createCacheFolder(AVATAR_FOLDER)
            createCacheFolder(QR_FOLDER)
            createCacheFolder(VOICE_CLIP_FOLDER)
        }
    }

    /**
     * Clear External Cache Dir
     */
    @JvmStatic
    fun clearPublicCache() {
        cacheFolderGateway.clearPublicCache()
    }


    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun buildQrFile(context: Context, fileName: String?): File? {
        return cacheFolderGateway.getCacheFile(QR_FOLDER, fileName)
    }

    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun buildPreviewFile(context: Context, fileName: String?): File? {
        return cacheFolderGateway.getCacheFile(PREVIEW_FOLDER, fileName)
    }

    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun buildThumbnailFile(context: Context, fileName: String?): File? {
        return cacheFolderGateway.getCacheFile(THUMBNAIL_FOLDER, fileName)
    }

    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun buildAvatarFile(context: Context, fileName: String?): File? {
        return cacheFolderGateway.getCacheFile(AVATAR_FOLDER, fileName)
    }

    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun buildVoiceClipFile(context: Context, fileName: String?): File? {
        return cacheFolderGateway.getCacheFile(VOICE_CLIP_FOLDER, fileName)
    }

    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun buildTempFile(context: Context, fileName: String?): File? {
        return cacheFolderGateway.getCacheFile(TEMPORARY_FOLDER, fileName)
    }

    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun buildChatTempFile(context: Context, fileName: String?): File? {
        return cacheFolderGateway.getCacheFile(CHAT_TEMPORARY_FOLDER, fileName)
    }

    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun getCacheFile(context: Context, folderName: String, fileName: String?): File? {
        return cacheFolderGateway.getCacheFile(folderName, fileName)
    }

    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun getCacheSize(): Long {
        return cacheFolderGateway.getCacheSize()
    }

    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun clearCache() {
        runBlocking { cacheFolderGateway.clearCache() }
    }


    /**
     *  Deletes the Cache folder if it is empty
     *
     *  @param folderName Name of the folder
     */
    @JvmStatic
    fun deleteCacheFolderIfEmpty(folderName: String) {
        cacheFolderGateway.deleteCacheFolderIfEmpty(folderName)
    }

    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun removeOldTempFolder(context: Context?, folderName: String) {
        cacheFolderGateway.removeOldTempFolder(folderName)
    }

    @JvmStatic
    fun getOldTempFolder(folderName: String): File {
        return cacheFolderGateway.getOldTempFolder(folderName)
    }
}