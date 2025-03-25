package mega.privacy.android.app.providers

import mega.privacy.android.shared.resources.R as SharedR
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.BuildConfig
import mega.privacy.android.app.initializer.DependencyContainer
import mega.privacy.android.domain.usecase.login.GetAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.login.MonitorLogoutUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineDocumentProviderRootFolderUseCase
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException


/**
 * OfflineDocumentProvider that exposes a custom document tree for the Mega app.
 */
class OfflineDocumentProvider : DocumentsProvider() {

    companion object {
        private const val OFFLINE_ROOT_ID = "mega_offline_root"

        // Use these as the default columns to return information about a root if no specific
        // columns are requested in a query.
        private val DEFAULT_ROOT_PROJECTION: Array<String> = arrayOf(
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_MIME_TYPES,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_AVAILABLE_BYTES
        )

        // Use these as the default columns to return information about a document if no specific
        // columns are requested in a query.
        private val DEFAULT_DOCUMENT_PROJECTION: Array<String> = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE
        )
    }

    private var rootFolder: File? = null
    private val dependencyContainer: DependencyContainer = DependencyContainer
    private val applicationScope: CoroutineScope by lazy { dependencyContainer.applicationScope }
    private val getOfflineDocumentProviderRootFolderUseCase: GetOfflineDocumentProviderRootFolderUseCase by lazy { dependencyContainer.getOfflineDocumentProviderRootFolderUseCase }
    private val monitorLogoutUseCase: MonitorLogoutUseCase by lazy { dependencyContainer.monitorLogoutUseCase }
    private val getAccountCredentialsUseCase: GetAccountCredentialsUseCase by lazy { dependencyContainer.getAccountCredentialsUseCase }
    private var logoutJob: Job? = null

    /**
     * onCreate is called when the Android system initializes the document provider.
     * This happens when a client (such as a file picker, a system UI, or another app) attempts to access the provider for the first time in a process lifecycle.
     */
    override fun onCreate(): Boolean {
        Timber.d("OfflineDocumentProvider onCreate called")
        return true
    }

    private fun initRootFolder() {
        runBlocking {
            rootFolder = runCatching {
                getOfflineDocumentProviderRootFolderUseCase()
            }.getOrNull()
        }
    }

    /**
     * Notify the Android system and registered observers that the data associated with the specified rootsUri has changed
     */
    private fun notifyChange() {
        val rootsUri: Uri =
            DocumentsContract.buildRootsUri(BuildConfig.OFFLINE_DOCUMENT_PROVIDER_AUTHORITY)
        context?.contentResolver?.notifyChange(rootsUri, null)
    }


    /**
     * Returns the available roots (top-level directories or entry points) that the document provider exposes
     */
    override fun queryRoots(projection: Array<String>?): Cursor {
        Timber.d("OfflineDocumentProvider queryRoots called")

        initProvider()

        val result = MatrixCursor(
            resolveRootProjection(projection)
        )

        val accountCredentials = runBlocking {
            runCatching { getAccountCredentialsUseCase() }.getOrNull()
        }
        if (accountCredentials == null) {
            Timber.d("OfflineDocumentProvider queryRoots User is not logged in, return queryRoots")
            return result
        }

        if (rootFolder == null) {
            Timber.d("OfflineDocumentProvider queryRoots root not initialized as no offline files saved, return queryRoots")
            return result
        }

        Timber.d("OfflineDocumentProvider queryRoots User is logged in, root is initialized")
        rootFolder?.let {
            result.newRow().apply {
                add(Root.COLUMN_ROOT_ID, OFFLINE_ROOT_ID)
                add(
                    Root.COLUMN_TITLE,
                    context?.getString(SharedR.string.general_mega_offline)
                )
                add(Root.COLUMN_SUMMARY, accountCredentials.email)
                add(Root.COLUMN_DOCUMENT_ID, getDocIdForFile(rootFolder ?: return@apply))
                add(Root.COLUMN_AVAILABLE_BYTES, (rootFolder ?: return@apply).freeSpace)
                add(Root.COLUMN_MIME_TYPES, getChildMimeTypes())
                add(Root.COLUMN_ICON, mega.privacy.android.app.R.drawable.logo_loading_ic)
            }
        }
        return result
    }

    private fun initProvider() {
        monitorLogout()
        initRootFolder()
    }

    /**
     * Monitor logout so as to update the document provider root when the user logs out
     * When user logs out, no need to expose the root folder
     */
    private fun monitorLogout() {
        if (logoutJob?.isActive == true) {
            return
        }
        logoutJob = applicationScope.launch {
            monitorLogoutUseCase().collect { isLoggedOut ->
                Timber.d("OfflineDocumentProvider monitorLogout isLoggedOut: $isLoggedOut")
                if (isLoggedOut) {
                    notifyChange()
                }
            }
        }
    }

    private fun getChildMimeTypes(): String {
        val mimeTypes: MutableSet<String> = HashSet()
        mimeTypes.add("image/*")
        mimeTypes.add("text/*")
        mimeTypes.add("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        val mimeTypesString = StringBuilder()
        for (mimeType in mimeTypes) {
            mimeTypesString.append(mimeType).append("\n")
        }

        return mimeTypesString.toString()
    }


    /**
     * Used to retrieve metadata about a specific document (or file) identified by a documentId
     * It allows the system or client applications to query information about individual documents managed by the document provider.
     */
    override fun queryDocument(documentId: String?, projection: Array<String>?): Cursor {
        Timber.d("OfflineDocumentProvider queryDocument called")
        initProvider()
        return MatrixCursor(resolveDocumentProjection(projection)).apply {
            includeFile(this, documentId, null)
        }
    }

    /**
     * Used to retrieve metadata about the immediate child documents (files or folders) of a specific parent document (directory).
     * This method is essential for navigating through the hierarchical structure of a document tree.
     */
    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<String>?,
        sortOrder: String?,
    ): Cursor {
        Timber.d("OfflineDocumentProvider queryChildDocuments called")
        initProvider()
        return MatrixCursor(resolveDocumentProjection(projection)).apply {
            val parent: File = getFileForDocId(parentDocumentId)
            parent.listFiles()
                ?.forEach { file ->
                    includeFile(this, null, file)
                }
        }
    }

    private fun resolveDocumentProjection(projection: Array<String>?): Array<String> {
        return projection ?: DEFAULT_DOCUMENT_PROJECTION
    }

    private fun resolveRootProjection(projection: Array<String>?): Array<String> {
        return projection ?: DEFAULT_ROOT_PROJECTION
    }


    /**
     * Used to handle a request for opening a specific document (file) for reading or writing.
     * It provides the system or client app with an input or output stream to access the contents of the document.
     */
    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?,
    ): ParcelFileDescriptor {
        Timber.d("OfflineDocumentProvider openDocument called")

        val file: File = getFileForDocId(documentId)
        val accessMode: Int = ParcelFileDescriptor.parseMode(mode)

        val isWrite: Boolean = mode.contains("w")
        return if (isWrite) {
            val handler = Handler(context!!.mainLooper)
            try {
                ParcelFileDescriptor.open(file, accessMode, handler) {
                    Timber.d("OfflineDocumentProvider A file with id $documentId has been closed! Time to update the server.")
                }
            } catch (e: IOException) {
                throw FileNotFoundException(
                    "Failed to open document with id $documentId and mode $mode"
                )
            }
        } else {
            ParcelFileDescriptor.open(file, accessMode)
        }
    }


    @Throws(FileNotFoundException::class)
    private fun includeFile(result: MatrixCursor, documentId: String?, file: File?) {
        Timber.d("OfflineDocumentProvider includeFile called with documentId: $documentId | file: $file")

        val docId = documentId ?: getDocIdForFile(file ?: return)
        Timber.d("OfflineDocumentProvider includeFile docId: $docId")

        val actualFile = file ?: getFileForDocId(docId)
        Timber.d("OfflineDocumentProvider includeFile actualFile: $actualFile | actualFile.isDirectory: ${actualFile.isDirectory} | actualFile.isFile: ${actualFile.isFile} | actualFile.canWrite(): ${actualFile.canWrite()}")

        val flags = when {
            actualFile.isDirectory && actualFile.canWrite() -> {
                Document.FLAG_DIR_SUPPORTS_CREATE or
                        Document.FLAG_SUPPORTS_DELETE or
                        Document.FLAG_SUPPORTS_RENAME or
                        Document.FLAG_SUPPORTS_REMOVE or
                        Document.FLAG_SUPPORTS_MOVE or
                        Document.FLAG_SUPPORTS_COPY
            }

            actualFile.isFile && actualFile.canWrite() -> {
                Document.FLAG_SUPPORTS_WRITE or
                        Document.FLAG_SUPPORTS_DELETE or
                        Document.FLAG_SUPPORTS_RENAME or
                        Document.FLAG_SUPPORTS_REMOVE or
                        Document.FLAG_SUPPORTS_MOVE or
                        Document.FLAG_SUPPORTS_COPY
            }

            else -> 0
        }
        Timber.d("OfflineDocumentProvider includeFile flags: $flags")

        result.newRow().apply {
            add(Document.COLUMN_DOCUMENT_ID, docId)
            add(Document.COLUMN_DISPLAY_NAME, actualFile.name)
            add(Document.COLUMN_SIZE, actualFile.length())
            add(Document.COLUMN_MIME_TYPE, getTypeForFile(actualFile))
            add(Document.COLUMN_LAST_MODIFIED, actualFile.lastModified())
            add(Document.COLUMN_FLAGS, 0)
            add(Document.COLUMN_ICON, mega.privacy.android.core.R.drawable.ic_mega) // Custom icon
        }
    }

    private fun getTypeForFile(file: File): String {
        return if (file.isDirectory) {
            Document.MIME_TYPE_DIR
        } else {
            getTypeForName(file.name)
        }
    }

    private fun getTypeForName(name: String): String {
        val lastDot = name.lastIndexOf('.')
        if (lastDot >= 0) {
            val extension = name.substring(lastDot + 1)
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (mime != null) {
                return mime
            }
        }
        return "application/octet-stream"
    }


    private fun getDocIdForFile(file: File): String {
        val path = file.absolutePath
        val rootPath = rootFolder?.absolutePath ?: ""
        return if (path == rootPath) OFFLINE_ROOT_ID else "$OFFLINE_ROOT_ID:" + path.substring(
            rootPath.length + 1
        )
    }

    private fun getFileForDocId(docId: String): File {
        if (docId == OFFLINE_ROOT_ID && rootFolder != null) {
            return rootFolder as File
        }

        val splitIndex = docId.indexOf(':', 1)
        if (splitIndex < 0) {
            throw IllegalArgumentException("Invalid document ID: $docId")
        }

        val path = docId.substring(splitIndex + 1)
        val target = File(rootFolder, path)
        if (!target.exists()) {
            throw IllegalArgumentException("File not found: $docId")
        }

        return target
    }
}

