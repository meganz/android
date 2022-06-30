package mega.privacy.android.app.data.facade

import android.content.Context
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.data.gateway.CacheFolderGateway
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.Util
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.ThreadPoolExecutor
import javax.inject.Inject

class CacheFolderFacade @Inject constructor(
    @ApplicationContext private val context: Context,
    private val megaThreadPoolExecutor: ThreadPoolExecutor,
) : CacheFolderGateway {

    companion object {
        private const val CHAT_TEMPORARY_FOLDER = "chatTempMEGA"
    }

    override fun getCacheFolder(folderName: String): File? {
        Timber.d("Create cache folder: $folderName")
        return megaThreadPoolExecutor.submit(
            Callable {
                val cache =
                    if (folderName == CHAT_TEMPORARY_FOLDER) context.filesDir else context.cacheDir
                File(cache, folderName).takeIf { it.exists() || it.mkdir() }
            }
        ).get()
    }

    override fun clearPublicCache() {
        megaThreadPoolExecutor.execute {
            context.externalCacheDir.let { file ->
                try {
                    FileUtil.deleteFolderAndSubfolders(context, file)
                } catch (e: IOException) {
                    Timber.e("IOException deleting external cache", e)
                }
            }
        }
    }

    override fun createCacheFolder(name: String) {
        megaThreadPoolExecutor.execute {
            getCacheFolder(name)?.takeIf {
                it.exists()
            }?.let { nonNullFile ->
                Timber.d("${nonNullFile.name} folder created: ${nonNullFile.absolutePath}")
            } ?: run {
                Timber.d("Create $name file failed")
            }
        }
    }

    override fun getCacheFile(folderName: String, fileName: String?): File? {
        return megaThreadPoolExecutor.submit(
            Callable {
                getCacheFolder(folderName)?.takeIf { it.exists() }?.let {
                    fileName?.let { nonNullFileName ->
                        File(it, nonNullFileName)
                    }
                }
            }
        ).get()
    }

    override fun getCacheSize(): String {
        Timber.d("getCacheSize")
        val cacheIntDir = context.cacheDir
        val cacheExtDir = context.externalCacheDir
        cacheIntDir?.let {
            Timber.d("Path to check internal: ${it.absolutePath}")
        }
        FileUtil.getDirSize(cacheIntDir).plus(FileUtil.getDirSize(cacheExtDir))
            .let { totalCacheSize ->
                return Util.getSizeString(totalCacheSize)
            }
    }

    override fun clearCache() {
        Timber.d("clearCache")
        val cacheIntDir = context.cacheDir
        try {
            FileUtil.deleteFolderAndSubfolders(context, cacheIntDir)
        } catch (e: IOException) {
            e.printStackTrace()
            Timber.e("Exception deleting private cache", e)
        }
        clearPublicCache()
    }

    override fun deleteCacheFolderIfEmpty(folderName: String) {
        getCacheFolder(folderName)?.apply {
            if (FileUtil.isFileAvailable(this) && !this.list().isNullOrEmpty()) {
                this.delete()
            }
        }
    }

    override fun removeOldTempFolder(folderName: String) {
        megaThreadPoolExecutor.execute {
            getOldTempFolder(folderName).takeIf { it.exists() }?.let {
                try {
                    FileUtil.deleteFolderAndSubfolders(context, it)
                } catch (e: IOException) {
                    Timber.e(e, "Exception deleting ${it.name} directory")
                    e.printStackTrace()
                }
            }
        }
    }

    override fun getOldTempFolder(folderName: String): File {
        val externalStorageVolumes: Array<out File> =
            ContextCompat.getExternalFilesDirs(context, null)
        return File(externalStorageVolumes[0].absolutePath + File.separator + folderName)
    }
}