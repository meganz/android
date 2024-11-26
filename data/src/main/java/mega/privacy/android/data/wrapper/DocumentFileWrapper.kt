package mega.privacy.android.data.wrapper

import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/**
 * Wrapper class for static functions concerning [androidx.documentfile.provider.DocumentFile]
 */
interface DocumentFileWrapper {

    /**
     * Creates a [DocumentFile] representing the document tree rooted at the given [uri]
     *
     * @see androidx.documentfile.provider.DocumentFile.fromTreeUri
     * @param uri the tree URI
     *
     * @return A potentially nullable [DocumentFile]
     */
    fun fromTreeUri(uri: Uri): DocumentFile?

    /**
     * Gets the [DocumentFile] id.
     *
     * @see android.provider.DocumentsContract.getDocumentId
     * @param documentFile the [DocumentFile] to get the id from
     * @return the id of the [DocumentFile]
     */
    fun getDocumentId(documentFile: DocumentFile): String
}