package mega.privacy.android.app.providers.documentprovider

import android.app.AuthenticationRequiredException
import android.app.PendingIntent
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import android.os.CancellationSignal
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
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import mega.privacy.android.app.BuildConfig
import mega.privacy.android.app.R
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.providers.documentprovider.CloudDriveDocumentDataProvider.Companion.CLOUD_DRIVE_ROOT_ID
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


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

        private const val OPEN_POLL_MS = 250L

        /** Daemon dispatcher so open work is not on the viewer thread. */
        private val openDocumentDispatcher: CoroutineDispatcher =
            Executors.newCachedThreadPool { r ->
                Thread(r, "CloudDriveDocOpen").apply { isDaemon = true }
            }.asCoroutineDispatcher()
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

    override fun onCreate(): Boolean {
        Timber.d("CloudDriveDocumentProvider onCreate called")
        context?.let { dataProvider.monitorConnectivity(it) }
        applicationScope.launch {
            var wasLoggedIn: Boolean? = null
            var wasOffline: Boolean? = null
            dataProvider.state.collect { state ->
                Timber.d("CloudDriveDocumentProvider onCreate state=$state")
                val isLoggedIn = state is HasCredentials
                val isOffline = state is CloudDriveDocumentProviderUiState.Offline
                if ((wasLoggedIn != null && wasLoggedIn != isLoggedIn) || (wasOffline != null && wasOffline != isOffline)) {
                    notifyRootChanged(CLOUD_DRIVE_ROOT_ID)
                    notifyDocumentChanged(CLOUD_DRIVE_ROOT_ID)
                }
                wasLoggedIn = isLoggedIn
                wasOffline = isOffline
                when (state) {
                    CloudDriveDocumentProviderUiState.Initialising -> {}

                    CloudDriveDocumentProviderUiState.NotLoggedIn -> {}

                    is CloudDriveDocumentProviderUiState.LoadingDocument -> {}

                    is CloudDriveDocumentProviderUiState.LoadingChildren -> {}

                    is CloudDriveDocumentProviderUiState.DocumentData -> notifyDocumentChanged(state.documentId)

                    is CloudDriveDocumentProviderUiState.ChildData -> notifyChildDocumentsChanged(
                        state.parentId
                    )

                    is CloudDriveDocumentProviderUiState.FileNotFound -> notifyDocumentChanged(state.documentId)

                    is CloudDriveDocumentProviderUiState.PasscodeLockEnabled -> {
                        notifyDocumentChanged(CLOUD_DRIVE_ROOT_ID)
                        notifyChildDocumentsChanged(CLOUD_DRIVE_ROOT_ID)
                    }

                    is CloudDriveDocumentProviderUiState.Offline -> {}

                    is CloudDriveDocumentProviderUiState.RootNodeNotLoaded -> {}
                }
            }
        }
        return true
    }

    override fun queryRoots(projection: Array<String>?): Cursor {
        Timber.d("CloudDriveDocumentProvider queryRoots projection=$projection,")
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
        return result
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
            add(Root.COLUMN_FLAGS, 0)
        }
    }

    override fun queryDocument(documentId: String?, projection: Array<String>?): Cursor {
        Timber.d("CloudDriveDocumentProvider queryDocument documentId=$documentId")

        if (documentId.isNullOrEmpty()) {
            throw FileNotFoundException("Invalid document id: $documentId")
        }

        if (documentId == CLOUD_DRIVE_ROOT_ID) {
            return documentCursorForRootDocument(projection)
        }

        val result = when (val state = dataProvider.state.value) {
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
        setNotificationUriForDocument(documentId, result)
        return result
    }

    private fun documentCursorForRootDocument(projection: Array<String>?): MatrixCursor {
        val row = CloudDriveDocumentRow(
            documentId = CLOUD_DRIVE_ROOT_ID,
            displayName = getAppNameString(),
            mimeType = Document.MIME_TYPE_DIR,
            size = 0L,
            lastModified = 0L,
            flags = 0
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

    /** Read-only open; not the root. @throws FileNotFoundException, AuthenticationRequiredException, OperationCanceledException */
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
        if (mode != null && mode != "r") {
            throw FileNotFoundException("Write mode not supported")
        }

        when (dataProvider.state.value) {
            is CloudDriveDocumentProviderUiState.NotLoggedIn -> throwAuthenticationRequired()
            is CloudDriveDocumentProviderUiState.PasscodeLockEnabled ->
                throw FileNotFoundException(getPasscodeLockEnabledString())

            is CloudDriveDocumentProviderUiState.Offline ->
                throw FileNotFoundException(getOfflineString())

            else -> Unit
        }

        if (signal?.isCanceled == true) {
            throw OperationCanceledException("Cancelled before resolve")
        }

        val file = openLocalFileOffViewerThread(documentId, signal)
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    private fun openLocalFileOffViewerThread(
        documentId: String,
        signal: CancellationSignal?,
    ): File {
        // Launch on the dispatcher (not the Binder thread) so the Binder thread stays alive
        // and responsive while polling the future — required for correct ParcelFileDescriptor handoff.
        val future = CompletableFuture<File>()
        applicationScope.launch(openDocumentDispatcher) {
            try {
                future.complete(dataProvider.openDocumentFile(documentId))
            } catch (e: CancellationException) {
                future.cancel(true)
                throw e
            } catch (e: Throwable) {
                future.completeExceptionally(e)
            }
        }
        return awaitOpenFutureResult(future, signal)
    }

    /** [Future.get] with short timeout to poll [signal]. */
    private fun <T> awaitOpenFutureResult(future: Future<T>, signal: CancellationSignal?): T {
        var restoreInterrupt = false
        try {
            while (true) {
                if (signal?.isCanceled == true) {
                    future.cancel(true)
                    throw OperationCanceledException("Cancelled while resolving document")
                }
                try {
                    return future.get(OPEN_POLL_MS, TimeUnit.MILLISECONDS)
                } catch (e: ExecutionException) {
                    mapOpenFailure(e.cause ?: e)
                } catch (_: TimeoutException) {
                } catch (_: InterruptedException) {
                    restoreInterrupt = true
                } catch (e: java.util.concurrent.CancellationException) {
                    throw OperationCanceledException(e.message)
                }
            }
        } finally {
            if (restoreInterrupt) {
                Thread.currentThread().interrupt()
            }
        }
    }

    /**
     * Rethrows the async failure from [Future.get] with types SAF callers expect.
     */
    private fun mapOpenFailure(t: Throwable): Nothing {
        when (t) {
            is CancellationException -> throw OperationCanceledException(t.message)
            is OperationCanceledException -> throw t
            is FileNotFoundException -> throw t
            is AuthenticationRequiredException -> throw t
            else -> throw FileNotFoundException("Failed to open document: ${t.message}")
                .also { it.initCause(t) }
        }
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<String>?,
        sortOrder: String?,
    ): Cursor {
        Timber.d("CloudDriveDocumentProvider queryChildDocuments parent=$parentDocumentId sortOrder=$sortOrder")
        if (parentDocumentId.isEmpty()) {
            throw FileNotFoundException("Invalid parent document id: $parentDocumentId")
        }

        val result = when (val state = dataProvider.state.value) {
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
                        rows = state.children,
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
        setNotificationUriForChildDocuments(parentDocumentId, result)
        return result
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
        Timber.d("CloudDriveDocumentProvider notifyRootChanged rootDocumentId=$rootDocumentId")
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