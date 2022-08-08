package mega.privacy.android.app.utils.wrapper

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/**
 * Get document file wrapper
 */
interface GetDocumentFileWrapper {
    /**
     * Get document file from uri and context
     */
    fun getDocumentFileFromTreeUri(context: Context, uri: Uri): DocumentFile?
}
