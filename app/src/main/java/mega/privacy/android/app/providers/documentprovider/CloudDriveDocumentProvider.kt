package mega.privacy.android.app.providers.documentprovider

import android.app.AuthenticationRequiredException
import android.app.PendingIntent
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.HandlerThread
import android.os.OperationCanceledException
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.BuildConfig
import mega.privacy.android.app.R
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.providers.documentprovider.CloudDriveDocumentDataProvider.Companion.CLOUD_DRIVE_ROOT_ID
import mega.privacy.android.app.providers.documentprovider.model.CloudDriveDocumentProviderUiState
import mega.privacy.android.app.providers.documentprovider.model.CloudDriveDocumentRow
import mega.privacy.android.app.providers.documentprovider.model.HasCredentials
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.Executors


/**
 * Document provider that exposes the user's MEGA Cloud Drive via the Storage Access Framework.
 * Data and use cases are delegated to [CloudDriveDocumentDataProvider], which is injected via the dependency container.
 */
class CloudDriveDocumentProvider : DocumentsProvider() {

    companion object {

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

        private val DEFAULT_DOCUMENT_PROJECTION: Array<String> = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE
        )

        private const val LOGIN_PENDING_INTENT_REQUEST_CODE = 1001

        /** Delay before notifying SAF so it has time to register its ContentObserver on the cursor's notification URI. */
        private const val NOTIFY_DELAY_MS = 100L

        /** Daemon dispatcher so open work is not on the viewer thread. */
        private val openDocumentDispatcher: CoroutineDispatcher =
            Executors.newCachedThreadPool { r ->
                Thread(r, "CloudDriveDocOpen").apply { isDaemon = true }
            }.asCoroutineDispatcher()

        /** Background handler used to deliver ParcelFileDescriptor close callbacks for write opens. */
        private val writeCloseHandler: Handler by lazy {
            HandlerThread("CloudDriveDocWriteClose").apply {
                isDaemon = true
                start()
            }.let { Handler(it.looper) }
        }
    }

    private val dependencyContainer: CloudDriveDocumentProviderEntryPoint by lazy {
        EntryPointAccessors.fromApplication(
            context!!.applicationContext,
            CloudDriveDocumentProviderEntryPoint::class.java
        )
    }

    private val dataProvider: CloudDriveDocumentDataProvider by lazy {
        dependencyContainer.cloudDriveDocumentDataProvider()
    }

    private val applicationScope: CoroutineScope by lazy {
        dependencyContainer.applicationScope()
    }

    private val authority = BuildConfig.CLOUD_DRIVE_DOCUMENT_PROVIDER_AUTHORITY

    private var rootNotifyJob: Job? = null
    private var documentNotifyJob: Job? = null
    private var childDocumentsNotifyJob: Job? = null

    override fun onCreate(): Boolean {
        Timber.d("CloudDriveDocumentProvider onCreate called")
        context?.let { dataProvider.monitorConnectivity(it) }
        return true
    }

    override fun queryRoots(projection: Array<String>?): Cursor {
        val summary = when (val state = dataProvider.state.value) {
            is HasCredentials -> state.accountName
            CloudDriveDocumentProviderUiState.NotLoggedIn -> getLoginToMEGAString()

            else -> getLoadingString()
        }
        val result =
            getMatrixCursor(resolveRootProjection(projection), withLoadingInfo = false).apply {
                addRootRow(summary)
            }

        setNotificationUriForRoot(result)
        listenForRootChanges()
        return result
    }

    private fun listenForRootChanges() {
        rootNotifyJob?.cancel()
        val wasLoggedIn = dataProvider.state.value is HasCredentials
        val wasOffline = dataProvider.state.value is CloudDriveDocumentProviderUiState.Offline
        rootNotifyJob = applicationScope.launch {
            dataProvider.state.first { state ->
                val isLoggedIn = state is HasCredentials
                val isOffline = state is CloudDriveDocumentProviderUiState.Offline
                isLoggedIn != wasLoggedIn || isOffline != wasOffline
            }
            delay(NOTIFY_DELAY_MS)
            notifyRootChanged(CLOUD_DRIVE_ROOT_ID)
            notifyDocumentChanged(CLOUD_DRIVE_ROOT_ID)
        }
    }

    private fun MatrixCursor.addRootRow(summary: String) {
        newRow().apply {
            add(Root.COLUMN_ROOT_ID, CLOUD_DRIVE_ROOT_ID)
            add(
                Root.COLUMN_TITLE,
                getAppNameString()
            )
            add(Root.COLUMN_SUMMARY, summary)
            add(Root.COLUMN_DOCUMENT_ID, CLOUD_DRIVE_ROOT_ID)
            add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
            add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE)
        }
    }

    override fun queryDocument(documentId: String?, projection: Array<String>?): Cursor {
        if (documentId.isNullOrEmpty()) {
            throw FileNotFoundException("Invalid document id: $documentId")
        }

        if (documentId == CLOUD_DRIVE_ROOT_ID) {
            return documentCursorForRootDocument(projection)
        }

        if (dataProvider.isPendingDocumentId(documentId)) {
            val pendingRow = dataProvider.getPendingDocumentRow(documentId)
                ?: throw FileNotFoundException("Pending document not found: $documentId")
            return documentCursor(row = pendingRow, projection = projection)
        }

        val result = documentQueryCursor(documentId, projection)
        setNotificationUriForDocument(documentId, result)
        if (result.isLoading) {
            listenForDocumentChanges(documentId)
        }
        return result
    }

    private fun listenForDocumentChanges(documentId: String) {
        documentNotifyJob?.cancel()
        documentNotifyJob = applicationScope.launch {
            dataProvider.state.first { state ->
                when (state) {
                    is CloudDriveDocumentProviderUiState.DocumentData ->
                        state.documentId == documentId

                    is CloudDriveDocumentProviderUiState.FileNotFound ->
                        state.documentId == documentId

                    is CloudDriveDocumentProviderUiState.PasscodeLockEnabled,
                    is CloudDriveDocumentProviderUiState.Offline,
                    CloudDriveDocumentProviderUiState.NotLoggedIn,
                        -> true

                    else -> false
                }
            }
            delay(NOTIFY_DELAY_MS)
            notifyDocumentChanged(documentId)
        }
    }

    private fun documentCursorForRootDocument(projection: Array<String>?): MatrixCursor {
        val row = CloudDriveDocumentRow(
            documentId = CLOUD_DRIVE_ROOT_ID,
            displayName = getAppNameString(),
            mimeType = Document.MIME_TYPE_DIR,
            size = 0L,
            lastModified = 0L,
            flags = Document.FLAG_DIR_SUPPORTS_CREATE,
        )
        return documentCursor(row = row, projection = projection)
    }

    private fun documentQueryCursor(
        documentId: String,
        projection: Array<String>?,
    ): MatrixCursor = when (val state = dataProvider.state.value) {
        is CloudDriveDocumentProviderUiState.DocumentData -> {
            if (documentId == state.documentId) {
                documentCursor(row = state.document, projection = projection)
            } else {
                loadDocumentAsync(documentId, projection)
            }
        }

        is CloudDriveDocumentProviderUiState.LoadingDocument -> {
            if (state.currentDocumentId != documentId) {
                dataProvider.loadDocumentInBackground(documentId)
            }
            getMatrixCursor(resolveDocumentProjection(projection), withLoadingInfo = true)
        }

        is CloudDriveDocumentProviderUiState.PasscodeLockEnabled -> {
            getErrorCursor(
                resolveDocumentProjection(projection),
                getPasscodeLockEnabledString()
            )
        }

        is CloudDriveDocumentProviderUiState.Offline -> {
            getErrorCursor(
                resolveDocumentProjection(projection),
                getOfflineString()
            )
        }

        CloudDriveDocumentProviderUiState.Initialising -> loadDocumentAsync(
            documentId,
            projection
        )

        CloudDriveDocumentProviderUiState.NotLoggedIn ->
            throwAuthenticationRequired()

        is CloudDriveDocumentProviderUiState.ChildData -> loadDocumentAsync(
            documentId,
            projection
        )

        is CloudDriveDocumentProviderUiState.LoadingChildren -> loadDocumentAsync(
            documentId,
            projection
        )

        is CloudDriveDocumentProviderUiState.FileNotFound -> throw FileNotFoundException("Node not found: $documentId")

        is CloudDriveDocumentProviderUiState.RootNodeNotLoaded -> {
            dataProvider.refreshRootNode()
            loadDocumentAsync(documentId, projection)
        }
    }

    private fun loadDocumentAsync(
        documentId: String,
        projection: Array<String>?,
    ): MatrixCursor {
        dataProvider.loadDocumentInBackground(documentId)
        return getMatrixCursor(resolveDocumentProjection(projection), withLoadingInfo = true)
    }

    /** Read or write open; not the root. @throws FileNotFoundException, AuthenticationRequiredException, OperationCanceledException */
    override fun openDocument(
        documentId: String?,
        mode: String?,
        signal: CancellationSignal?,
    ): ParcelFileDescriptor {
        if (documentId.isNullOrEmpty()) {
            throw FileNotFoundException("Invalid document id: $documentId")
        }
        if (documentId == CLOUD_DRIVE_ROOT_ID) {
            throw FileNotFoundException("Cannot open root as document")
        }

        val isWrite = dataProvider.isPendingDocumentId(documentId)

        when (dataProvider.state.value) {
            is CloudDriveDocumentProviderUiState.NotLoggedIn -> throwAuthenticationRequired()
            is CloudDriveDocumentProviderUiState.PasscodeLockEnabled ->
                throw FileNotFoundException(getPasscodeLockEnabledString())

            is CloudDriveDocumentProviderUiState.Offline ->
                if (!isWrite) throw FileNotFoundException(getOfflineString())

            is CloudDriveDocumentProviderUiState.RootNodeNotLoaded ->
                dataProvider.refreshRootNode()

            else -> Unit
        }

        if (isWrite) {
            val parcelMode = ParcelFileDescriptor.parseMode(mode ?: "w")
            val scratchFile = resolveScratchFile(documentId, signal)
            return ParcelFileDescriptor.open(
                scratchFile,
                parcelMode,
                writeCloseHandler,
            ) { err ->
                dataProvider.onWriteScratchClosed(documentId, scratchFile, err)
            }
        }

        val file = resolveLocalFile(documentId, signal)
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    private fun resolveScratchFile(documentId: String, signal: CancellationSignal?): File = try {
        runBlocking(openDocumentDispatcher) {
            val job = coroutineContext[Job]
            signal?.setOnCancelListener { job?.cancel() }
            try {
                dataProvider.prepareWriteScratchFile(documentId)
            } finally {
                signal?.setOnCancelListener(null)
            }
        }
    } catch (e: CancellationException) {
        throw OperationCanceledException(e.message)
    } catch (e: FileNotFoundException) {
        throw e
    } catch (e: Throwable) {
        throw FileNotFoundException("Failed to prepare scratch file: ${e.message}").also {
            it.initCause(e)
        }
    }

    override fun createDocument(
        parentDocumentId: String,
        mimeType: String,
        displayName: String,
    ): String {
        Timber.d("CloudDriveDocumentProvider createDocument parent=$parentDocumentId mime=$mimeType name=$displayName")
        val pendingId = try {
            runBlocking(openDocumentDispatcher) {
                if (mimeType == Document.MIME_TYPE_DIR) {
                    dataProvider.registerPendingFolder(parentDocumentId, displayName)
                } else {
                    dataProvider.registerPendingFile(parentDocumentId, displayName, mimeType)
                }
            }
        } catch (e: FileNotFoundException) {
            throw e
        } catch (e: AuthenticationRequiredException) {
            throw e
        } catch (e: Throwable) {
            throw FileNotFoundException("Failed to create document: ${e.message}").also {
                it.initCause(e)
            }
        }
        if (mimeType == Document.MIME_TYPE_DIR) {
            applicationScope.launch {
                runCatching { dataProvider.completeFolderCreation(pendingId) }
                    .onFailure {
                        Timber.e(
                            it,
                            "CloudDriveDocumentProvider completeFolderCreation failed for $pendingId"
                        )
                    }
                notifyDocumentChanged(pendingId)
                notifyChildDocumentsChanged(parentDocumentId)
            }
        } else {
            // Surface the placeholder in the listing immediately, then wait for the data provider
            // to finalize the pending entry (upload completed/failed/timed out) and re-notify so
            // SAF re-queries with the real node in place of the placeholder.
            notifyChildDocumentsChanged(parentDocumentId)
            applicationScope.launch {
                dataProvider.awaitFileFinalized(pendingId)
                notifyChildDocumentsChanged(parentDocumentId)
            }
        }
        return pendingId
    }

    override fun renameDocument(documentId: String, displayName: String): String? {
        Timber.d("CloudDriveDocumentProvider renameDocument documentId=$documentId name=$displayName")
        return try {
            val parentDocumentId = runBlocking(openDocumentDispatcher) {
                dataProvider.renameDocument(documentId, displayName)
            }
            notifyChildDocumentsChanged(parentDocumentId)
            // DocumentsUI re-queries via DocumentInfo.fromUri using the URI returned here. Returning
            // null surfaces a null URI, which DocumentInfo.fromUri throws NPE on; that is caught
            // upstream and shown as "Failed to rename" even though the rename succeeded. Returning
            // the (unchanged) document id keeps the URI valid so the rename completes cleanly.
            documentId
        } catch (e: FileNotFoundException) {
            throw e
        } catch (e: AuthenticationRequiredException) {
            throw e
        } catch (e: Throwable) {
            Timber.e(e, "CloudDriveDocumentProvider renameDocument failed for $documentId")
            throw FileNotFoundException("Failed to rename document: ${e.message}").also {
                it.initCause(e)
            }
        }
    }

    private fun resolveLocalFile(documentId: String, signal: CancellationSignal?): File = try {
        runBlocking(openDocumentDispatcher) {
            val job = coroutineContext[Job]
            signal?.setOnCancelListener { job?.cancel() }
            try {
                dataProvider.openDocumentFile(documentId)
            } finally {
                signal?.setOnCancelListener(null)
            }
        }
    } catch (e: CancellationException) {
        throw OperationCanceledException(e.message)
    } catch (e: FileNotFoundException) {
        throw e
    } catch (e: AuthenticationRequiredException) {
        throw e
    } catch (e: Throwable) {
        throw FileNotFoundException("Failed to open document: ${e.message}").also {
            it.initCause(e)
        }
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<String>?,
        sortOrder: String?,
    ): Cursor {
        if (parentDocumentId.isEmpty()) {
            throw FileNotFoundException("Invalid parent document id: $parentDocumentId")
        }

        val result = childDocumentsCursor(parentDocumentId, projection)
        setNotificationUriForChildDocuments(parentDocumentId, result)
        if (result.isLoading) {
            listenForChildDocumentChanges(parentDocumentId)
        }
        return result
    }

    private fun listenForChildDocumentChanges(parentDocumentId: String) {
        childDocumentsNotifyJob?.cancel()
        childDocumentsNotifyJob = applicationScope.launch {
            dataProvider.state.first { state ->
                when (state) {
                    is CloudDriveDocumentProviderUiState.ChildData ->
                        state.parentId == parentDocumentId

                    is CloudDriveDocumentProviderUiState.FileNotFound,
                    is CloudDriveDocumentProviderUiState.PasscodeLockEnabled,
                    is CloudDriveDocumentProviderUiState.Offline,
                    CloudDriveDocumentProviderUiState.NotLoggedIn,
                        -> true

                    else -> false
                }
            }
            delay(NOTIFY_DELAY_MS)
            notifyChildDocumentsChanged(parentDocumentId)
        }
    }

    private fun childDocumentsCursor(
        parentDocumentId: String,
        projection: Array<String>?,
    ): MatrixCursor = when (val state = dataProvider.state.value) {
        is CloudDriveDocumentProviderUiState.PasscodeLockEnabled ->
            getErrorCursor(
                resolveDocumentProjection(projection),
                getPasscodeLockEnabledString()
            )

        is CloudDriveDocumentProviderUiState.Offline ->
            getErrorCursor(
                resolveDocumentProjection(projection),
                getOfflineString()
            )

        is CloudDriveDocumentProviderUiState.ChildData -> {
            if (parentDocumentId == state.parentId) {
                documentCursor(
                    rows = state.children + dataProvider.getPendingChildrenForParent(parentDocumentId),
                    projection = projection,
                    isLoading = state.hasMore
                )
            } else {
                loadChildrenAsync(parentDocumentId, projection)
            }
        }

        is CloudDriveDocumentProviderUiState.DocumentData -> loadChildrenAsync(
            parentDocumentId,
            projection
        )

        is CloudDriveDocumentProviderUiState.LoadingChildren -> {
            if (parentDocumentId != state.currentParentDocumentId) {
                dataProvider.loadChildrenInBackground(parentDocumentId)
            }
            getMatrixCursor(resolveDocumentProjection(projection), withLoadingInfo = true)
        }

        is CloudDriveDocumentProviderUiState.LoadingDocument -> loadChildrenAsync(
            parentDocumentId,
            projection
        )

        CloudDriveDocumentProviderUiState.Initialising -> loadChildrenAsync(
            parentDocumentId,
            projection
        )

        CloudDriveDocumentProviderUiState.NotLoggedIn ->
            throwAuthenticationRequired()

        is CloudDriveDocumentProviderUiState.FileNotFound ->
            throw FileNotFoundException("Invalid parent document id: $parentDocumentId")

        is CloudDriveDocumentProviderUiState.RootNodeNotLoaded -> {
            dataProvider.refreshRootNode()
            loadChildrenAsync(parentDocumentId, projection)
        }
    }

    private fun loadChildrenAsync(
        parentId: String,
        projection: Array<String>?,
    ): MatrixCursor {
        dataProvider.loadChildrenInBackground(parentId)
        return getMatrixCursor(resolveDocumentProjection(projection), withLoadingInfo = true)
    }

    private fun createLoginPendingIntent(): PendingIntent {
        val appContext = requireNotNull(context).applicationContext

        val loginIntent = Intent(appContext, MegaActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("open_from_document_provider", true)
        }

        return PendingIntent.getActivity(
            appContext,
            LOGIN_PENDING_INTENT_REQUEST_CODE,
            loginIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun throwAuthenticationRequired(): Nothing {
        throw AuthenticationRequiredException(
            IllegalStateException(getLoginToMEGAString()),
            createLoginPendingIntent()
        )
    }

    private val MatrixCursor.isLoading: Boolean
        get() = extras?.getBoolean(DocumentsContract.EXTRA_LOADING) == true

    private fun getMatrixCursor(
        projection: Array<String>,
        withLoadingInfo: Boolean,
    ): MatrixCursor {
        if (!withLoadingInfo) {
            return MatrixCursor(projection)
        } else {
            val loadingMessage = getLoadingString()
            return object : MatrixCursor(projection) {
                override fun getExtras(): Bundle {
                    return Bundle().apply {
                        putBoolean(DocumentsContract.EXTRA_LOADING, true)
                        putString(DocumentsContract.EXTRA_INFO, loadingMessage)
                    }
                }
            }
        }
    }

    private fun getErrorCursor(projection: Array<String>, errorMessage: String): MatrixCursor {
        return object : MatrixCursor(projection) {
            override fun getExtras(): Bundle = Bundle().apply {
                putString(DocumentsContract.EXTRA_ERROR, errorMessage)
            }
        }
    }

    private fun getLoadingString() =
        context?.getString(sharedR.string.photos_loading_indicator_text) ?: "Loading"

    private fun getAppNameString() = context?.getString(R.string.app_name) ?: "MEGA"
    private fun getLoginToMEGAString() =
        context?.getString(R.string.login_to_mega) ?: "Log in to MEGA"

    private fun getPasscodeLockEnabledString() =
        context?.getString(sharedR.string.saf_passcode_lock_enabled_message)
            ?: "To browse your MEGA files, disable your passcode in the MEGA app. Go to Settings, then tap Security."

    private fun getOfflineString() = context?.getString(sharedR.string.saf_no_internet_message)
        ?: "Connect to the internet to browse your MEGA files. Files saved for offline are available in the MEGA app."

    private fun notifyDocumentChanged(documentId: String) {
        context?.let {
            val documentUri = DocumentsContract.buildDocumentUri(authority, documentId)
            it.contentResolver.notifyChange(documentUri, null)
        }
    }

    private fun notifyChildDocumentsChanged(parentDocumentId: String) {
        context?.let {
            val childDocumentsUri =
                DocumentsContract.buildChildDocumentsUri(authority, parentDocumentId)
            it.contentResolver.notifyChange(childDocumentsUri, null)
        }
    }

    private fun setNotificationUriForRoot(result: MatrixCursor) {
        context?.let {
            result.setNotificationUri(
                it.contentResolver,
                DocumentsContract.buildRootsUri(authority)
            )
        }
    }

    private fun notifyRootChanged(rootDocumentId: String? = null) {
        context?.let { context ->
            val rootsUri = DocumentsContract.buildRootsUri(authority)
            context.contentResolver?.notifyChange(rootsUri, null)
            rootDocumentId?.let {
                val rootChildrenUri =
                    DocumentsContract.buildChildDocumentsUri(authority, rootDocumentId)
                context.contentResolver?.notifyChange(rootChildrenUri, null)
            }
        }
    }

    private fun setNotificationUriForDocument(documentId: String, result: MatrixCursor) {
        context?.let {
            result.setNotificationUri(
                it.contentResolver,
                DocumentsContract.buildDocumentUri(authority, documentId)
            )
        }
    }

    private fun setNotificationUriForChildDocuments(
        parentDocumentId: String,
        result: MatrixCursor,
    ) {
        context?.let {
            result.setNotificationUri(
                it.contentResolver,
                DocumentsContract.buildChildDocumentsUri(authority, parentDocumentId)
            )
        }
    }

    private fun resolveDocumentProjection(projection: Array<String>?) =
        projection ?: DEFAULT_DOCUMENT_PROJECTION

    private fun resolveRootProjection(projection: Array<String>?) =
        projection ?: DEFAULT_ROOT_PROJECTION

    private fun documentCursor(
        row: CloudDriveDocumentRow,
        projection: Array<String>?,
        isLoading: Boolean = false,
    ) = documentCursor(
        rows = listOf(row),
        isLoading = isLoading,
        projection = projection
    )

    private fun documentCursor(
        rows: List<CloudDriveDocumentRow>,
        projection: Array<String>?,
        isLoading: Boolean = false,
    ) = getMatrixCursor(resolveDocumentProjection(projection), isLoading)
        .apply {
            rows.forEach { row ->
                newRow().apply {
                    add(Document.COLUMN_DOCUMENT_ID, row.documentId)
                    add(Document.COLUMN_DISPLAY_NAME, row.displayName)
                    add(Document.COLUMN_SIZE, row.size)
                    add(Document.COLUMN_MIME_TYPE, row.mimeType)
                    add(Document.COLUMN_LAST_MODIFIED, row.lastModified)
                    add(Document.COLUMN_FLAGS, row.flags)
                }
            }
        }

}

/**
 * Entry point for Cloud Drive Document Provider dependencies.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface CloudDriveDocumentProviderEntryPoint {
    fun cloudDriveDocumentDataProvider(): CloudDriveDocumentDataProvider

    @ApplicationScope
    fun applicationScope(): CoroutineScope
}