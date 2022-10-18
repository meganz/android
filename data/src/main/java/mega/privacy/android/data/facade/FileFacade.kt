package mega.privacy.android.data.facade

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.data.gateway.FileGateway
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject

/**
 * File facade
 *
 * Refer to [mega.privacy.android.app.utils.FileUtil]
 */
class FileFacade @Inject constructor(
    @ApplicationContext private val context: Context,
) : FileGateway {
    override suspend fun getDirSize(dir: File?): Long {
        dir ?: return -1L
        var size = 0L
        val files = dir.listFiles().orEmpty()

        if (files.isNotEmpty()) {
            for (file in files) {
                size += if (file.isFile) {
                    file.length()
                } else {
                    getDirSize(file)
                }
            }
        }
        Timber.d("Dir size: %s", size)
        return size
    }

    @Suppress("DEPRECATION")
    @Throws(IOException::class)
    override suspend fun deleteFolderAndSubFolders(f: File?) {
        f ?: return
        Timber.d("deleteFolderAndSubfolders: %s", f.absolutePath)
        val files = f.takeIf { it.isDirectory }?.listFiles().orEmpty()
        if (files.isNotEmpty()) {
            for (c in files) {
                deleteFolderAndSubFolders(c)
            }
        }
        if (!f.delete()) {
            throw FileNotFoundException("Failed to delete file: $f")
        } else {
            try {
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                    val fileToDelete = File(f.absolutePath)
                    val contentUri = try {
                        FileProvider.getUriForFile(context,
                            "mega.privacy.android.app.providers.fileprovider",
                            fileToDelete)
                    } catch (e: IllegalArgumentException) {
                        Uri.fromFile(fileToDelete)
                    }
                    data = contentUri
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.sendBroadcast(mediaScanIntent)
            } catch (e: Exception) {
                Timber.e(e, "Exception while deleting media scanner file")
            }
        }
    }

    override suspend fun isFileAvailable(file: File?): Boolean = file?.exists() == true

    override suspend fun buildDefaultDownloadDir(): File =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            ?.let { downloadsDir ->
                File(downloadsDir, DOWNLOAD_DIR)
            } ?: context.filesDir

    companion object {
        private const val DOWNLOAD_DIR = "MEGA Downloads"
    }
}