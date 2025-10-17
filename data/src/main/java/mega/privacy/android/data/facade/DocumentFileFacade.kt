package mega.privacy.android.data.facade

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import mega.privacy.android.data.facade.DocumentFileFacade.Companion.DOWNLOADS_FOLDER_AUTHORITY
import mega.privacy.android.data.facade.DocumentFileFacade.Companion.EXTERNAL_STORAGE_AUTHORITY
import mega.privacy.android.data.facade.DocumentFileFacade.Companion.PRIMARY
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.wrapper.DocumentFileWrapper
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Default implementation of [DocumentFileWrapper]
 *
 * @property context [ApplicationContext]
 */
class DocumentFileFacade @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceGateway: DeviceGateway,
) : DocumentFileWrapper {

    override val DocumentFile.isTreeDocumentFile: Boolean
        get() = DocumentsContract.isTreeUri(uri)

    override val DocumentFile.isExternalStorageDocument: Boolean
        get() = uri.authority == EXTERNAL_STORAGE_AUTHORITY

    override val DocumentFile.isDownloadsDocument: Boolean
        get() = uri.authority == DOWNLOADS_FOLDER_AUTHORITY

    override val DocumentFile.isMediaDocument: Boolean
        get() = uri.authority == MEDIA_FOLDER_AUTHORITY

    override val DocumentFile.isInPrimaryStorage: Boolean
        get() = isTreeDocumentFile && storageId == PRIMARY
                || isRawFile && uri.path.orEmpty().startsWith(externalStoragePath)

    override val DocumentFile.isInSdCardStorage: Boolean
        get() = isTreeDocumentFile && storageId != PRIMARY
                || isRawFile && uri.path.orEmpty().startsWith("/storage/$storageId")

    override val DocumentFile.isRawFile: Boolean
        get() = uri.scheme == ContentResolver.SCHEME_FILE

    override val DocumentFile.id: String
        get() = getDocumentId(this)

    override val DocumentFile.storageId: String
        get() = getStorageId(context)

    private val externalStoragePath: String
        get() = Environment.getExternalStorageDirectory().absolutePath

    override fun fromTreeUri(uri: Uri): DocumentFile? =
        DocumentFile.fromTreeUri(context, uri)

    override fun fromSingleUri(uri: Uri): DocumentFile? =
        DocumentFile.fromSingleUri(context, uri)

    override fun getDocumentId(documentFile: DocumentFile): String =
        DocumentsContract.getDocumentId(documentFile.uri)

    override fun fromUri(uri: Uri): DocumentFile? {
        val file = getFile(uri)

        return when {
            file?.exists() == true -> DocumentFile.fromFile(file)
            DocumentsContract.isTreeUri(uri) -> fromTreeUri(uri).takeIf { it?.exists() == true }
            else -> fromSingleUri(uri).takeIf { it?.exists() == true }
        }
    }

    /**
     * Get the [File] from the given [Uri].
     * Takes into account different Operating Systems and Manufacturers.
     */
    private fun getFile(uri: Uri) = runCatching {
        when {
            isMIUIGalleryRawUri(uri) -> uri.path?.removePrefix(MIUI_RAW_PREFIX_PATH)
                ?.let { path -> File(path) }

            isSamsungDeviceWithAndroidLessThanQ() -> uri.getColumnInfoString(MediaStore.MediaColumns.DATA)
                ?.let { path -> File(path) }

            uri.scheme == "file" -> uri.path
                ?.let { path -> File(path) }

            else -> null
        }
    }.onFailure { Timber.e(it, "Error getting File from Uri: $uri") }.getOrNull()

    override fun getAbsolutePathFromContentUri(uri: Uri): String? =
        fromUri(uri)?.getAbsolutePath()

    override fun fromFile(file: File): DocumentFile =
        DocumentFile.fromFile(file)

    override fun getSdDocumentFile(
        folderUri: Uri,
        subFolders: List<String>,
        fileName: String,
        mimeType: String,
    ): DocumentFile? {
        var folderDocument = fromUri(folderUri)

        subFolders.forEach { folder ->
            folderDocument =
                folderDocument?.findFile(folder) ?: folderDocument?.createDirectory(folder)
        }
        folderDocument?.findFile(fileName)?.delete()

        return folderDocument?.createFile(mimeType, fileName)
    }

    override suspend fun getDocumentFile(uriString: String) =
        resolveDocumentFile(uriString) { folderTree ->
            if (folderTree.isEmpty()) null else folderTree.last()
        }

    override suspend fun getDocumentFile(uriString: String, fileName: String) =
        resolveDocumentFile(uriString) { fileName }

    private suspend fun resolveDocumentFile(
        uriString: String,
        fileNameProvider: (List<String>) -> String?,
    ): DocumentFile? {
        val uri = uriString.toUri()
        val file = uri.takeIf { uri.scheme == "file" }?.path?.let { File(it) }

        return when {
            file?.exists() == true -> DocumentFile.fromFile(file)


            DocumentsContract.isTreeUri(uri) -> fromTreeUri(uri)?.let { documentFile ->
                val folderTree = getFolderTreeFromPickedContentUri(uriString)

                findDocumentFile(
                    parent = documentFile,
                    folderTree = folderTree,
                    fileName = fileNameProvider(folderTree),
                )
            }

            else -> fromSingleUri(uri)
        }
    }

    override suspend fun getDocumentFileForSyncContentUri(uriString: String): DocumentFile? {
        val uri = uriString.toUri()
        return when {
            DocumentsContract.isTreeUri(uri) -> fromTreeUri(uri)?.let { documentFile ->
                val folderTree = getSyncFolderTreeFromPickedContentUri(uriString)
                if (folderTree.isEmpty()) {
                    return documentFile
                }

                findDocumentFileByFolderTree(documentFile, folderTree)
            }

            else -> fromSingleUri(uri)
        }
    }

    private fun getSyncFolderTreeFromPickedContentUri(uriString: String): List<String> {
        val documentPath = uriString.substringAfter("/document/")
        if (documentPath == uriString) {
            // No "/document/" found in URI
            return emptyList()
        }
        val pathComponents =
            documentPath.split("/").drop(1) // Drop the first component which is the storage ID
        return pathComponents
    }

    fun findDocumentFileByFolderTree(rootDir: DocumentFile, segments: List<String>): DocumentFile? {
        // Path should be like "A/B/c.txt or A/B/C"
        var currentFile: DocumentFile? = rootDir

        for (segment in segments.dropLast(1)) {
            currentFile = currentFile?.findFile(segment)?.takeIf { it.isDirectory } ?: return null
        }

        return currentFile?.findFile(segments.lastOrNull() ?: return null)
    }

    /**
     * Extracts the folder tree from the picked URI string.
     */
    private fun getFolderTreeFromPickedContentUri(uriString: String): List<String> =
        uriString.removePrefix("content://").let {
            val folderTree = it.substringAfterLast("//")

            if (it == folderTree) {
                emptyList()
            } else {
                folderTree.split(File.separator)
            }
        }

    /**
     * Finds a [DocumentFile] in the given [parent] directory, [folderTree] and [fileName].
     */
    private suspend fun findDocumentFile(
        parent: DocumentFile,
        folderTree: List<String>,
        fileName: String?,
    ): DocumentFile? = coroutineScope {
        when {
            folderTree.isNotEmpty() -> parent.findFile(folderTree.first())?.let { documentFile ->
                folderTree.drop(1).let { newFolderTree ->
                    if (newFolderTree.isEmpty() && documentFile.name == fileName) {
                        documentFile
                    } else {
                        ensureActive()
                        findDocumentFile(documentFile, newFolderTree, fileName)
                    }
                }
            }

            fileName == null -> parent

            else -> parent.findFile(fileName)
        }
    }

    private fun DocumentFile.getStorageId(context: Context): String =
        uri.path.orEmpty().let { path ->
            when {
                isRawFile -> File(path).getStorageId(context)
                isExternalStorageDocument -> path.substringBefore(':', "").substringAfterLast('/')
                isDownloadsDocument -> PRIMARY
                else -> ""
            }
        }

    private fun File.getBasePath(context: Context): String {
        val externalStoragePath = externalStoragePath
        if (path.startsWith(externalStoragePath)) {
            return path.substringAfter(externalStoragePath, "").trimFileSeparator()
        }
        val dataDir = context.dataDir.path
        if (path.startsWith(dataDir)) {
            return path.substringAfter(dataDir, "").trimFileSeparator()
        }
        val storageId = getStorageId(context)
        return path.substringAfter("/storage/$storageId", "").trimFileSeparator()
    }

    /**
     * ID of this storage. For external storage, it will return [PRIMARY],
     * otherwise it is a SD Card and will return integers like `6881-2249`.
     */
    private fun File.getStorageId(context: Context) = when {
        path.startsWith(externalStoragePath) -> PRIMARY
        path.startsWith(context.dataDir.path) -> DATA
        else -> path.substringAfter("/storage/", "").substringBefore('/')
    }


    /**
     * Returns a string having leading and trailing characters from the '/' removed.
     */
    private fun String.trimFileSeparator() = trim('/')

    /**
     * @return File path without storage ID. Returns empty `String` if:
     * * It is the root path
     * * It is not a raw file and the authority is neither [EXTERNAL_STORAGE_AUTHORITY] nor [DOWNLOADS_FOLDER_AUTHORITY]
     * * The authority is [DOWNLOADS_FOLDER_AUTHORITY], but [isTreeDocumentFile] returns `false`
     */
    private fun DocumentFile.getBasePath(context: Context): String {
        val path = uri.path.orEmpty()

        return when {
            isRawFile -> File(path).getBasePath(context)

            isExternalStorageDocument && path.contains("/document/$storageId:") -> {
                path.substringAfterLast("/document/$storageId:", "").trimFileSeparator()
            }

            isDownloadsDocument -> {
                // content://com.android.providers.downloads.documents/tree/raw:/storage/emulated/0/Download/Denai/document/raw:/storage/emulated/0/Download/Denai
                // content://com.android.providers.downloads.documents/tree/downloads/document/raw:/storage/emulated/0/Download/Denai
                when {
                    // API 26 - 27 => content://com.android.providers.downloads.documents/document/22
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.P && path.matches(Regex("/document/\\d+")) -> {
                        val fileName = uri.getNameFromDownloadsDocument() ?: return ""
                        "${Environment.DIRECTORY_DOWNLOADS}/$fileName"
                    }

                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && path.matches(Regex("(.*?)/ms[f,d]:\\d+(.*?)")) -> {
                        if (isTreeDocumentFile) {
                            val parentTree = mutableListOf(name.orEmpty())
                            var parent = this
                            while (parent.parentFile?.also { parent = it } != null) {
                                parentTree.add(parent.name.orEmpty())
                            }
                            parentTree.reversed().joinToString("/")
                        } else {
                            // we can't use msf/msd ID as MediaFile ID to fetch relative path, so just return empty String
                            ""
                        }
                    }

                    else -> path.substringAfterLast(externalStoragePath, "")
                        .trimFileSeparator()
                }
            }

            else -> ""
        }
    }

    private fun Uri.getNameFromDownloadsDocument() =
        toRawFile()?.name ?: getColumnInfoString(MediaStore.MediaColumns.DISPLAY_NAME)

    private fun Uri.toRawFile() =
        if (scheme == ContentResolver.SCHEME_FILE) path?.let { File(it) } else null

    private fun Uri.getColumnInfoString(column: String): String? {
        context.contentResolver.query(this, arrayOf(column), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(column)
                if (columnIndex != -1) {
                    return cursor.getString(columnIndex)
                }
            }
        }
        return null
    }


    /**
     * * For file in SD Card => `/storage/6881-2249/Music/song.mp3`
     * * For file in external storage => `/storage/emulated/0/Music/song.mp3`
     *
     * If you want to remember file locations in database or preference, please use this function.
     *
     * @return File's actual path. Returns empty `String` if:
     * * It is not a raw file and the authority is neither [EXTERNAL_STORAGE_AUTHORITY] nor [DOWNLOADS_FOLDER_AUTHORITY]
     * * The authority is [DOWNLOADS_FOLDER_AUTHORITY], but [isTreeDocumentFile] returns `false`
     *
     * @see File.getAbsolutePath
     * @see getSimplePath
     */
    private fun DocumentFile.getAbsolutePath(): String {
        val path = uri.path.orEmpty()

        return when {
            isRawFile -> path

            isExternalStorageDocument && path.contains("/document/$storageId:") -> {
                val basePath =
                    path.substringAfterLast("/document/$storageId:", "").trimFileSeparator()

                if (storageId == PRIMARY) {
                    "${externalStoragePath}/$basePath".trimEnd('/')
                } else {
                    "/storage/$storageId/$basePath".trimEnd('/')
                }
            }

            uri.toString()
                .let { it == DOWNLOADS_TREE_URI || it == "${DOWNLOADS_TREE_URI}/document/downloads" } ->
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath

            isDownloadsDocument -> {
                when {
                    // API 26 - 27 => content://com.android.providers.downloads.documents/document/22
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.P && path.matches(Regex("/document/\\d+")) -> {
                        val fileName = uri.getNameFromDownloadsDocument() ?: return ""
                        File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            fileName
                        ).absolutePath
                    }

                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && path.matches(Regex("(.*?)/ms[f,d]:\\d+(.*?)")) -> {
                        if (isTreeDocumentFile) {
                            val parentTree = mutableListOf(name.orEmpty())
                            var parent = this
                            while (parent.parentFile?.also { parent = it } != null) {
                                parentTree.add(parent.name.orEmpty())
                            }
                            "${externalStoragePath}/${
                                parentTree.reversed().joinToString("/")
                            }".trimEnd('/')
                        } else {
                            // we can't use msf/msd ID as MediaFile ID to fetch relative path, so just return empty String
                            ""
                        }
                    }

                    else -> path.substringAfterLast("/document/raw:", "").trimEnd('/')
                }
            }

            !isTreeDocumentFile -> {
                ""
            }

            isInPrimaryStorage -> {
                "${externalStoragePath}/${getBasePath(context)}".trimEnd('/')
            }

            else -> {
                "/storage/$storageId/${getBasePath(context)}".trimEnd('/')
            }
        }
    }

    override fun isMIUIGalleryRawUri(uri: Uri) =
        uri.authority == MIUI_GALLERY_AUTHORITY
                && uri.path?.startsWith(MIUI_RAW_PREFIX_PATH) == true

    override fun isSamsungDeviceWithAndroidLessThanQ() =
        deviceGateway.getSdkVersionInt() < Build.VERSION_CODES.Q
                && deviceGateway.getManufacturerName().equals("Samsung", true)

    companion object {
        /**
         * External storage authority.
         */
        const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

        /**
         * File picker for each API version gives the following URIs:
         * API 26 - 27 => content://com.android.providers.downloads.documents/document/22
         * API 28 - 29 => content://com.android.providers.downloads.documents/document/raw%3A%2Fstorage%2Femulated%2F0%2FDownload%2Fscreenshot.jpeg
         * API 30+     => content://com.android.providers.downloads.documents/document/msf%3A42
         */
        const val DOWNLOADS_FOLDER_AUTHORITY = "com.android.providers.downloads.documents"

        /**
         * Only available on API 26 to 29.
         */
        const val DOWNLOADS_TREE_URI = "content://$DOWNLOADS_FOLDER_AUTHORITY/tree/downloads"

        /**
         * Media authority.
         */
        const val MEDIA_FOLDER_AUTHORITY = "com.android.providers.media.documents"

        /**
         * For files under [Environment.getExternalStorageDirectory]
         */
        const val PRIMARY = "primary"

        /**
         * For files under [Context.getFilesDir] or [Context.getDataDir].
         * It is not really a storage ID, and can't be used in file tree URI.
         */
        const val DATA = "data"

        /**
         * MIUI’s Gallery app authority.
         */
        const val MIUI_GALLERY_AUTHORITY = "com.miui.gallery.open"

        /**
         * MIUI’s raw file path prefix in Uris.
         */
        const val MIUI_RAW_PREFIX_PATH = "/raw/"
    }
}
