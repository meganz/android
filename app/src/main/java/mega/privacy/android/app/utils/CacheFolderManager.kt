package mega.privacy.android.app.utils

import android.content.Context
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.di.EntryPointsModule
import mega.privacy.android.data.gateway.CacheFolderGateway
import java.io.File

object CacheFolderManager {

    private const val OLD_TEMPORARY_PIC_DIR = "MEGA/MEGA AppTemp"
    private const val OLD_PROFILE_PID_DIR = "MEGA/MEGA Profile Images"
    private const val OLD_ADVANCES_DEVICES_DIR = "MEGA/MEGA Temp"
    private const val OLD_CHAT_TEMPORARY_DIR = "MEGA/MEGA Temp/Chat"
    const val THUMBNAIL_FOLDER = "thumbnailsMEGA"
    const val PREVIEW_FOLDER = "previewsMEGA"
    const val AVATAR_FOLDER = "avatarsMEGA"
    private const val QR_FOLDER = "qrMEGA"
    const val VOICE_CLIP_FOLDER = "voiceClipsMEGA"
    const val TEMPORARY_FOLDER = "tempMEGA"
    const val CHAT_TEMPORARY_FOLDER = "chatTempMEGA"

    val cacheFolderGateway: CacheFolderGateway by lazy {
        EntryPointAccessors.fromApplication(MegaApplication.getInstance(),
            EntryPointsModule.CacheFolderManagerEntryPoint::class.java).cacheFolderGateway
    }

    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun getCacheFolder(context: Context, folderName: String): File? {
        return cacheFolderGateway.getCacheFolder(folderName)
    }

    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun createCacheFolders(context: Context) {
        cacheFolderGateway.apply {
            createCacheFolder(THUMBNAIL_FOLDER)
            createCacheFolder(PREVIEW_FOLDER)
            createCacheFolder(AVATAR_FOLDER)
            createCacheFolder(QR_FOLDER)
            createCacheFolder(VOICE_CLIP_FOLDER)
        }
    }

    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun clearPublicCache(context: Context) {
        cacheFolderGateway.clearPublicCache()
    }

    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun createCacheFolder(context: Context, name: String) {
        cacheFolderGateway.createCacheFolder(name)
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

    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun deleteCacheFolderIfEmpty(context: Context, folderName: String) {
        cacheFolderGateway.deleteCacheFolderIfEmpty(folderName)
    }

    @JvmStatic
    fun removeOldTempFolders(context: Context?) {
        removeOldTempFolder(context, OLD_TEMPORARY_PIC_DIR)
        removeOldTempFolder(context, OLD_PROFILE_PID_DIR)
        removeOldTempFolder(context, OLD_ADVANCES_DEVICES_DIR)
        removeOldTempFolder(context, OLD_CHAT_TEMPORARY_DIR)
        val oldOfflineFolder = getOldTempFolder(OfflineUtils.OLD_OFFLINE_DIR)
        if (FileUtil.isFileAvailable(oldOfflineFolder)) {
            OfflineUtils.moveOfflineFiles(context)
        }
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