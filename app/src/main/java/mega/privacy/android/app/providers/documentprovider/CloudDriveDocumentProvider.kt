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
import mega.privacy.android.app.providers.documentprovider.model.ChildrenSlot
import mega.privacy.android.app.providers.documentprovider.model.CloudDriveSessionState
import mega.privacy.android.app.providers.documentprovider.model.CloudDriveDocumentRow
import mega.privacy.android.app.providers.documentprovider.model.DocumentSlot
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
        val summary = when (val session = dataProvider.sessionState.value) {
            is CloudDriveSessionState.Ready -> session.accountName
            is CloudDriveSessionState.PasscodeLockEnabled -> session.accountName
            is CloudDriveSessionState.Offline -> session.accountName
            is CloudDriveSessionState.RootNodeNotLoaded -> session.accountName
            CloudDriveSessionState.NotLoggedIn -> getLoginToMEGAString()
            CloudDriveSessionState.Initialising -> getLoadingString()
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
        val before = dataProvider.sessionState.value
        val wasLoggedIn = before.isLoggedIn
        val wasOffline = before is CloudDriveSessionState.Offline
        rootNotifyJob = applicationScope.launch {
            dataProvider.sessionState.first { current ->
                val isLoggedIn = current.isLoggedIn
                val isOffline = current is CloudDriveSessionState.Offline
                isLoggedIn != wasLoggedIn || isOffline != wasOffline
            }
            delay(NOTIFY_DELAY_MS)
            notifyRootChanged(CLOUD_DRIVE_ROOT_ID)
            notifyDocumentChanged(CLOUD_DRIVE_ROOT_ID)
        }
    }

    private val CloudDriveSessionState.isLoggedIn: Boolean
        get() = this is CloudDriveSessionState.Ready ||
                this is CloudDriveSessionState.Offline ||
                this is CloudDriveSessionState.PasscodeLockEnabled ||
                this is CloudDriveSessionState.RootNodeNotLoaded

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
            add(Root.COLUMN_FLAGS, 0)
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

    private fun documentQueryCursor(
        documentId: String,
        projection: Array<String>?,
    ): MatrixCursor {
        // Session-level cases first — they don't depend on the document slot.
        when (val session = dataProvider.sessionState.value) {
            CloudDriveSessionState.NotLoggedIn -> throwAuthenticationRequired()
            is CloudDriveSessionState.PasscodeLockEnabled ->
                return getErrorCursor(
                    resolveDocumentProjection(projection),
                    getPasscodeLockEnabledString(),
                )

            is CloudDriveSessionState.Offline ->
                return getErrorCursor(
                    resolveDocumentProjection(projection),
                    getOfflineString(),
                )

            is CloudDriveSessionState.RootNodeNotLoaded -> {
                dataProvider.refreshRootNode()
                return loadDocumentAsync(documentId, projection)
            }

            CloudDriveSessionState.Initialising,
            is CloudDriveSessionState.Ready,
                -> Unit
        }

        return when (val slot = dataProvider.documentState.value) {
            is DocumentSlot.Loaded ->
                if (slot.documentId == documentId) {
                    documentCursor(row = slot.row, projection = projection)
                } else {
                    serveDocumentFromCacheOrLoad(documentId, projection)
                }

            is DocumentSlot.Loading -> serveDocumentFromCacheOrLoad(documentId, projection)

            is DocumentSlot.NotFound ->
                if (slot.documentId == documentId) {
                    throw FileNotFoundException("Node not found: $documentId")
                } else {
                    serveDocumentFromCacheOrLoad(documentId, projection)
                }

            DocumentSlot.Idle -> serveDocumentFromCacheOrLoad(documentId, projection)
        }
    }

    private fun serveDocumentFromCacheOrLoad(
        documentId: String,
        projection: Array<String>?,
    ): MatrixCursor {
        val cachedRow = dataProvider.findCachedChildRow(documentId)
            ?: return loadDocumentAsync(documentId, projection)
        // Cache hit: still kick off documentState load so it's warm for subsequent queries.
        dataProvider.loadDocumentInBackground(documentId)
        return documentCursor(row = cachedRow, projection = projection)
    }

    private fun listenForDocumentChanges(documentId: String) {
        documentNotifyJob?.cancel()
        documentNotifyJob = applicationScope.launch {
            dataProvider.documentState.first { slot ->
                when (slot) {
                    is DocumentSlot.Loaded -> slot.documentId == documentId
                    is DocumentSlot.NotFound -> slot.documentId == documentId
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
            flags = 0,
        )
        return documentCursor(row = row, projection = projection)
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

        when (dataProvider.sessionState.value) {
            CloudDriveSessionState.NotLoggedIn -> throwAuthenticationRequired()
            is CloudDriveSessionState.PasscodeLockEnabled ->
                throw FileNotFoundException(getPasscodeLockEnabledString())

            is CloudDriveSessionState.Offline ->
                if (!isWrite) throw FileNotFoundException(getOfflineString())

            is CloudDriveSessionState.RootNodeNotLoaded ->
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
        if (needsChildDocumentsRefresh(parentDocumentId)) {
            listenForChildDocumentChanges(parentDocumentId)
        }
        return result
    }

    private fun needsChildDocumentsRefresh(parentDocumentId: String): Boolean =
        when (val slot = dataProvider.childrenState.value) {
            is ChildrenSlot.Loaded ->
                slot.parentDocumentId != parentDocumentId || slot.hasMore
            is ChildrenSlot.NotFound ->
                slot.parentDocumentId != parentDocumentId
            else -> true
        }

    private fun childDocumentsCursor(
        parentDocumentId: String,
        projection: Array<String>?,
    ): MatrixCursor {
        // Session-level cases first.
        when (val session = dataProvider.sessionState.value) {
            CloudDriveSessionState.NotLoggedIn -> throwAuthenticationRequired()
            is CloudDriveSessionState.PasscodeLockEnabled ->
                return getErrorCursor(
                    resolveDocumentProjection(projection),
                    getPasscodeLockEnabledString(),
                )

            is CloudDriveSessionState.Offline ->
                return getErrorCursor(
                    resolveDocumentProjection(projection),
                    getOfflineString(),
                )

            is CloudDriveSessionState.RootNodeNotLoaded -> {
                dataProvider.refreshRootNode()
                return loadChildrenAsync(parentDocumentId, projection)
            }

            CloudDriveSessionState.Initialising,
            is CloudDriveSessionState.Ready,
                -> Unit
        }

        // Fast path for the folder Info screen: serve a non-loading empty cursor so
        if (dataProvider.findCachedChildRow(parentDocumentId) != null) {
            val current = dataProvider.childrenState.value
            val alreadyHandled = current is ChildrenSlot.Loaded &&
                    current.parentDocumentId == parentDocumentId
            if (!alreadyHandled) {
                dataProvider.loadChildrenInBackground(parentDocumentId)
                return getMatrixCursor(
                    resolveDocumentProjection(projection),
                    withLoadingInfo = false,
                )
            }
        }

        return when (val slot = dataProvider.childrenState.value) {
            is ChildrenSlot.Loaded ->
                if (slot.parentDocumentId == parentDocumentId) {
                    documentCursor(
                        rows = slot.children + dataProvider.getPendingChildrenForParent(
                            parentDocumentId
                        ),
                        projection = projection,
                        isLoading = slot.hasMore,
                    )
                } else {
                    loadChildrenAsync(parentDocumentId, projection)
                }

            is ChildrenSlot.Loading ->
                if (slot.parentDocumentId != parentDocumentId) {
                    dataProvider.loadChildrenInBackground(parentDocumentId)
                    getMatrixCursor(resolveDocumentProjection(projection), withLoadingInfo = true)
                } else {
                    getMatrixCursor(resolveDocumentProjection(projection), withLoadingInfo = true)
                }

            is ChildrenSlot.NotFound ->
                if (slot.parentDocumentId == parentDocumentId) {
                    throw FileNotFoundException("Invalid parent document id: $parentDocumentId")
                } else {
                    loadChildrenAsync(parentDocumentId, projection)
                }

            ChildrenSlot.Idle -> loadChildrenAsync(parentDocumentId, projection)
        }
    }

    private fun listenForChildDocumentChanges(parentDocumentId: String) {
        childDocumentsNotifyJob?.cancel()
        childDocumentsNotifyJob = applicationScope.launch {
            dataProvider.childrenState.first { slot ->
                when (slot) {
                    is ChildrenSlot.Loaded -> slot.parentDocumentId == parentDocumentId
                    is ChildrenSlot.NotFound -> slot.parentDocumentId == parentDocumentId
                    else -> false
                }
            }
            delay(NOTIFY_DELAY_MS)
            notifyChildDocumentsChanged(parentDocumentId)
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
