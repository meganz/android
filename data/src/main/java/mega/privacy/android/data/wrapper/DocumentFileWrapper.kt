package mega.privacy.android.data.wrapper

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * Wrapper class for static functions concerning [androidx.documentfile.provider.DocumentFile]
 */
interface DocumentFileWrapper {

    /**
     * If the given DocumentFile represents a Document tree.
     */
    val DocumentFile.isTreeDocumentFile: Boolean

    /**
     * If the given DocumentFile represents a file in local storage.
     */
    val DocumentFile.isExternalStorageDocument: Boolean

    /**
     * If the given DocumentFile represents a file in local storage, Downloads folder.
     */
    val DocumentFile.isDownloadsDocument: Boolean

    /**
     * If the given DocumentFile represents a media file.
     */
    val DocumentFile.isMediaDocument: Boolean

    /**
     * If the given DocumentFile represents a file in local storage.
     */
    val DocumentFile.isInPrimaryStorage: Boolean

    /**
     * If the given DocumentFile represents a file in SD card storage.
     */
    val DocumentFile.isInSdCardStorage: Boolean

    /**
     * If the given DocumentFile represents a file created with [File]
     */
    val DocumentFile.isRawFile: Boolean

    /**
     * DocumentFile id
     */
    val DocumentFile.id: String

    /**
     * DocumentFile storage id
     */
    val DocumentFile.storageId: String

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
     * Creates a [DocumentFile] representing the document at the given [uri]
     */
    fun fromSingleUri(uri: Uri): DocumentFile?

    /**
     * Gets the [DocumentFile] id.
     *
     * @see android.provider.DocumentsContract.getDocumentId
     * @param documentFile the [DocumentFile] to get the id from
     * @return the id of the [DocumentFile]
     */
    fun getDocumentId(documentFile: DocumentFile): String

    /**
     * Creates a [DocumentFile] representing the document at the given [uri]
     *
     * @see androidx.documentfile.provider.DocumentFile.fromUri
     * @param uri the URI
     *
     * @return A potentially nullable [DocumentFile]
     */
    fun fromUri(uri: Uri): DocumentFile?

    /**
     * Gets the absolute path of the given [uri]
     */
    fun getAbsolutePathFromContentUri(uri: Uri): String?

    /**
     * Creates a [DocumentFile] representing the given [file]
     */
    fun fromFile(file: File): DocumentFile

    /**
     * Creates a [DocumentFile] representing the document tree rooted at the given [uri]
     */
    fun getSdDocumentFile(
        folderUri: Uri,
        subFolders: List<String>,
        fileName: String,
        mimeType: String,
    ): DocumentFile?

    /**
     * Get the document file for the given uri string.
     *
     * Note that, for downloads, we have this uri but it is not the real DocumentFile uri,
     * so we need to find the real DocumentFile uri by using the given uriString, getting
     * the tree uri DocumentFile and then.
     *
     * @param uriString the string representation of the uri
     * @return the [DocumentFile] of the document file if found, null otherwise
     */
    suspend fun getDocumentFile(uriString: String): DocumentFile?

    /**
     * Get the document file for the given uri string and file name.
     *
     * Note that, for downloads, we have this uri but it is not the real DocumentFile uri,
     * so we need to find the real DocumentFile uri by using the given uriString, getting
     * the tree uri DocumentFile and then, finding the child by the file name.
     *
     * @param uriString the string representation of the uri
     * @param fileName the name of the file to find
     * @return the [DocumentFile] of the document file if found, null otherwise
     */
    suspend fun getDocumentFile(uriString: String, fileName: String): DocumentFile?

    /**
     * Get the document file for the given uri string for sync content uri.
     * e.g  "content://com.android.externalstorage.documents/tree/primary:Recordings/document/primary:Recordings/A/B/c.txt"
     */
    suspend fun getDocumentFileForSyncContentUri(uriString: String): DocumentFile?

    /**
     * MIUI’s Uris are not standard content Uris. MIUI’s Gallery app exposes a “raw” file URI for files stored on device storage.
     * MIUI’s “raw” URI doesn’t actually point to a document SAF can handle, it’s more like a “shortcut” to a file path,
     * that is why we need to handle them separately.
     * E.g.: content://com.miui.gallery.open/raw/%2Fstorage%2Femulated%2F0%2Fscreen-recording-1738156692736.mp4
     *  and its path /raw/storage/emulated/0/screen-recording-1738156692736.mp4
     */
    fun isMIUIGalleryRawUri(uri: Uri): Boolean

    /**
     * Check if the device is a Samsung device with Android version less than Q.
     * Samsung devices with Android version less than Q have an issue that causes
     * DocumentFile to not work properly. It is required to use File API instead.
     */
    fun isSamsungDeviceWithAndroidLessThanQ(): Boolean
}
