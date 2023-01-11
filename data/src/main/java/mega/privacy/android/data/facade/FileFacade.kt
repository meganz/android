package mega.privacy.android.data.facade

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns.DATA
import android.provider.MediaStore.MediaColumns.DATE_MODIFIED
import android.provider.MediaStore.MediaColumns.DISPLAY_NAME
import android.provider.MediaStore.MediaColumns.SIZE
import android.provider.MediaStore.VOLUME_EXTERNAL
import android.provider.MediaStore.VOLUME_INTERNAL
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.data.gateway.FileGateway
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject

/**
 * Intent extra data for node handle
 */
const val INTENT_EXTRA_NODE_HANDLE = "NODE_HANDLE"

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
    override fun deleteFolderAndSubFolders(f: File?) {
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
                        FileProvider.getUriForFile(
                            context,
                            "mega.privacy.android.app.providers.fileprovider",
                            fileToDelete
                        )
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

    @Deprecated("File already exposes exists() function", ReplaceWith("file?.exists() == true"))
    override suspend fun isFileAvailable(file: File?): Boolean = file?.exists() == true

    override suspend fun buildDefaultDownloadDir(): File =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            ?.let { downloadsDir ->
                File(downloadsDir, DOWNLOAD_DIR)
            } ?: context.filesDir

    override suspend fun getLocalFile(
        fileName: String,
        fileSize: Long,
        lastModifiedDate: Long,
    ) = kotlin.runCatching {
        val selectionArgs = arrayOf(
            fileName,
            fileSize.toString(),
            lastModifiedDate.toString()
        )
        getExternalFile(selectionArgs) ?: getInternalFile(selectionArgs)
    }.getOrNull()

    private fun getExternalFile(
        selectionArgs: Array<String>,
    ) = getExternalCursor(selectionArgs)?.let { cursor ->
        getFileFromCursor(cursor).also { cursor.close() }
    }

    private fun getInternalFile(
        selectionArgs: Array<String>,
    ) = getInternalCursor(selectionArgs)?.let { cursor ->
        getFileFromCursor(cursor).also { cursor.close() }
    }

    private fun getExternalCursor(
        selectionArgs: Array<String>,
    ) = getNonEmptyCursor(
        MediaStore.Files.getContentUri(VOLUME_EXTERNAL),
        selectionArgs,
    )

    private fun getInternalCursor(
        selectionArgs: Array<String>,
    ) = getNonEmptyCursor(
        MediaStore.Files.getContentUri(VOLUME_INTERNAL),
        selectionArgs,
    )

    private fun getNonEmptyCursor(
        uri: Uri,
        selectionArgs: Array<String>,
    ): Cursor? {
        val cursor = context.contentResolver.query(
            /* uri = */
            uri,
            /* projection = */
            arrayOf(DATA),
            /* selection = */
            "$DISPLAY_NAME = ? AND $SIZE = ? AND $DATE_MODIFIED = ?",
            /* selectionArgs = */
            selectionArgs,
            /* sortOrder = */
            null,
        ) ?: return null

        return if (cursor.moveToFirst()) {
            cursor
        } else {
            cursor.close()
            null
        }
    }

    private fun getFileFromCursor(cursor: Cursor): File? {
        val path = cursor.getString(cursor.getColumnIndexOrThrow(DATA))
        return File(path).takeIf { it.exists() }
    }

    override suspend fun getOfflineFilesRootPath() =
        context.filesDir.absolutePath + File.separator + OFFLINE_DIR

    override suspend fun getOfflineFilesInboxRootPath() =
        context.filesDir.absolutePath + File.separator + OFFLINE_DIR + File.separator + "in"

    companion object {
        private const val DOWNLOAD_DIR = "MEGA Downloads"
        private const val OFFLINE_DIR = "MEGA Offline"
    }
}
