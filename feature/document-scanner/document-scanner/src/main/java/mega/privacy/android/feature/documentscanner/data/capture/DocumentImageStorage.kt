package mega.privacy.android.feature.documentscanner.data.capture

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.WorkerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * Writes scanned-page bitmaps to app-private storage as JPEGs and returns their
 * file URIs. Kept in the data layer since it touches the filesystem directly.
 */
internal class DocumentImageStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Compresses [bitmap] to a JPEG named [fileName] under the scans directory
     * and returns its `file://` URI string. Performs blocking file I/O — call
     * from a background thread.
     */
    @WorkerThread
    fun saveJpeg(bitmap: Bitmap, fileName: String, quality: Int = JPEG_QUALITY): String {
        val dir = File(context.filesDir, SCANS_DIR).apply { mkdirs() }
        val file = File(dir, fileName)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
        return Uri.fromFile(file).toString()
    }

    /**
     * Deletes the file backing a `file://` URI previously returned by [saveJpeg].
     * A missing file or a non-file URI is a no-op. Blocking I/O — call off the main thread.
     */
    @WorkerThread
    fun delete(uriString: String) {
        Uri.parse(uriString).path?.let { File(it).delete() }
    }

    /**
     * Removes the entire scans directory. Blocking I/O — call off the main thread.
     */
    @WorkerThread
    fun deleteAll() {
        File(context.filesDir, SCANS_DIR).deleteRecursively()
    }

    private companion object {
        const val SCANS_DIR = "document_scans"
        const val JPEG_QUALITY = 90
    }
}
