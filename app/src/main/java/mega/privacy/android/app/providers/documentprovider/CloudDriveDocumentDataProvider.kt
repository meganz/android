package mega.privacy.android.app.providers.documentprovider

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.provider.DocumentsContract.Document
import androidx.annotation.VisibleForTesting
import androidx.core.content.getSystemService
import dagger.Lazy as DaggerLazy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import mega.privacy.android.app.providers.documentprovider.model.CloudDriveDocumentProviderUiState
import mega.privacy.android.app.providers.documentprovider.model.CloudDriveDocumentRow
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.pitag.PitagTarget
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.MonitorPasscodeLockPreferenceUseCase
import mega.privacy.android.domain.usecase.account.MonitorUserCredentialsUseCase
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.domain.usecase.login.GetAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.node.CreateFolderNodeUseCase
import mega.privacy.android.domain.usecase.node.GetChildNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.GetNodesByIdInChunkUseCase
import mega.privacy.android.domain.usecase.node.GetOpenableLocalFileForCloudDriveSafUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.RenameNodeUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.StartUploadUseCase
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class CloudDriveDocumentDataProvider @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val getRootNodeIdUseCase: DaggerLazy<GetRootNodeIdUseCase>,
    private val getNodesByIdInChunkUseCase: DaggerLazy<GetNodesByIdInChunkUseCase>,
    private val getNodeByHandleUseCase: DaggerLazy<GetNodeByHandleUseCase>,
    private val backgroundFastLoginUseCase: DaggerLazy<BackgroundFastLoginUseCase>,
    private val monitorNodeUpdatesUseCase: DaggerLazy<MonitorNodeUpdatesUseCase>,
    private val monitorUserCredentialsUseCase: DaggerLazy<MonitorUserCredentialsUseCase>,
    private val getAccountCredentialsUseCase: DaggerLazy<GetAccountCredentialsUseCase>,
    private val monitorHiddenNodesEnabledUseCase: DaggerLazy<MonitorHiddenNodesEnabledUseCase>,
    private val monitorShowHiddenItemsUseCase: DaggerLazy<MonitorShowHiddenItemsUseCase>,
    private val cloudDriveDocumentRowMapper: DaggerLazy<CloudDriveDocumentRowMapper>,
    private val addNodeType: DaggerLazy<AddNodeType>,
    private val documentIdToNodeIdMapper: DaggerLazy<DocumentIdToNodeIdMapper>,
    private val monitorPasscodeLockPreferenceUseCase: DaggerLazy<MonitorPasscodeLockPreferenceUseCase>,
    private val getOpenableLocalFileForCloudDriveSafUseCase: DaggerLazy<GetOpenableLocalFileForCloudDriveSafUseCase>,
    private val createFolderNodeUseCase: DaggerLazy<CreateFolderNodeUseCase>,
    private val renameNodeUseCase: DaggerLazy<RenameNodeUseCase>,
    private val getChildNodeUseCase: DaggerLazy<GetChildNodeUseCase>,
    private val startUploadUseCase: DaggerLazy<StartUploadUseCase>,
    private val getCacheFileUseCase: DaggerLazy<GetCacheFileUseCase>,
) {

    private val pendingCreates = ConcurrentHashMap<String, PendingCreate>()
    private val pendingFinalizeSignals = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    /** Lets nested SAF createDocument calls resolve a placeholder parent to the real folder NodeId. */
    private val pendingFolderRealNodeId = ConcurrentHashMap<String, NodeId>()
    private val pendingFolderSignals = ConcurrentHashMap<String, CompletableDeferred<NodeId>>()

    private val connectivityState = MutableStateFlow(true)

    @VisibleForTesting
    fun updateConnectivity(connected: Boolean) {
        connectivityState.value = connected
    }

    /** Call once from the content provider's [android.content.ContentProvider.onCreate]. */
    fun monitorConnectivity(context: Context) {
        applicationScope.launch {
            monitorConnectivityFlow(context)
                .catch {
                    Timber.e(it, "CloudDriveDocumentDataProvider monitorConnectivity")
                }
                .collect { connected ->
                    connectivityState.value = connected
                }
        }
    }

    private fun monitorConnectivityFlow(context: Context) = callbackFlow {
        val connectivityManager = context.getSystemService<ConnectivityManager>()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                trySend(
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                )
            }
        }
        connectivityManager?.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager?.unregisterNetworkCallback(callback) }
    }

    private sealed interface SessionState {
        data object NotLoggedIn : SessionState
        data class PasscodeLockEnabled(val accountName: String) : SessionState
        data class Offline(val accountName: String) : SessionState
        data class Ready(
            val accountName: String,
            val rootNodeId: NodeId?,
        ) : SessionState
    }

    @OptIn(FlowPreview::class)
    val state: StateFlow<CloudDriveDocumentProviderUiState> by lazy {
        monitorSessionState()
            .flatMapLatest { sessionState -> sessionStateToUiState(sessionState) }
            .catch { e ->
                Timber.e(e, "CloudDriveDocumentDataProvider state")
                emit(CloudDriveDocumentProviderUiState.NotLoggedIn)
            }.asUiStateFlow(
                scope = applicationScope,
                initialValue = CloudDriveDocumentProviderUiState.Initialising
            )
    }

    private fun monitorSessionState(): Flow<SessionState> =
        combine(
            monitorPasscodeLockPreferenceUseCase.get()().catch {
                Timber.e(it)
                emit(false)
            },
            connectivityState,
            monitorUserCredentialsUseCase.get()().onStart { emit(getAccountCredentialsUseCase.get()()) }
                .catch {
                    Timber.e(it)
                    emit(null)
                }
                .distinctUntilChangedBy { it?.email },
        ) { isPasscodeLockEnabled, isConnected, credentials ->
            Timber.d("CloudDriveDocumentDataProvider isPasscodeLockEnabled=$isPasscodeLockEnabled isConnected=$isConnected credentials=$credentials")
            val accountName = credentials?.email ?: ""
            when {
                credentials == null -> SessionState.NotLoggedIn
                isPasscodeLockEnabled -> SessionState.PasscodeLockEnabled(accountName)
                !isConnected -> SessionState.Offline(accountName)
                else -> SessionState.Ready(accountName, null)
            }
        }.flatMapLatest { sessionState ->
            if (sessionState is SessionState.Ready && sessionState.rootNodeId == null) {
                getRootNodeFlow().map { rootNodeId ->
                    SessionState.Ready(sessionState.accountName, rootNodeId)
                }
            } else {
                flowOf(sessionState)
            }
        }

    private fun sessionStateToUiState(
        sessionState: SessionState,
    ): Flow<CloudDriveDocumentProviderUiState> = when (sessionState) {
        SessionState.NotLoggedIn ->
            flowOf(CloudDriveDocumentProviderUiState.NotLoggedIn)

        is SessionState.PasscodeLockEnabled ->
            flowOf(CloudDriveDocumentProviderUiState.PasscodeLockEnabled(sessionState.accountName))

        is SessionState.Offline ->
            flowOf(CloudDriveDocumentProviderUiState.Offline(sessionState.accountName))

        is SessionState.Ready -> {
            val rootNodeId = sessionState.rootNodeId
            if (rootNodeId == null) {
                flowOf(
                    CloudDriveDocumentProviderUiState.RootNodeNotLoaded(
                        sessionState.accountName
                    )
                )
            } else {
                getDataFlows(
                    sessionState.accountName,
                    "$CLOUD_DRIVE_ROOT_ID:${rootNodeId.longValue}"
                )
            }
        }
    }

    private val refreshRootNodeChannel =
        Channel<Unit>(1, onBufferOverflow = BufferOverflow.DROP_LATEST)

    private fun getRootNodeFlow() = refreshRootNodeChannel.receiveAsFlow().map {
        getRootNodeWithFastLoginIfNeeded()
    }.onStart {
        emit(getRootNodeWithFastLoginIfNeeded())
    }.catch {
        emit(null)
    }.onEach {
        Timber.d("CloudDriveDocumentDataProvider getRootNodeFlow rootNode=$it")
    }

    private suspend fun getRootNodeWithFastLoginIfNeeded(): NodeId? =
        getRootNodeIdUseCase.get()() ?: run {
            Timber.d("CloudDriveDocumentDataProvider getRootNodeUseCase returned null, attempting fast login")
            backgroundFastLoginUseCase.get()().let {
                getRootNodeIdUseCase.get()()
            }
        }

    private sealed interface DocumentDataRequest {
        val documentId: String

        data class Children(override val documentId: String) : DocumentDataRequest
        data class Document(override val documentId: String) : DocumentDataRequest
    }

    private val requestFlow: MutableStateFlow<DocumentDataRequest> =
        MutableStateFlow(DocumentDataRequest.Document(CLOUD_DRIVE_ROOT_ID))

    private fun getDataFlows(accountName: String, rootNodeDocumentId: String) =
        monitorNodeUpdatesUseCase.get()().catch {
            Timber.e(
                it,
                "CloudDriveDocumentDataProvider monitorNodeUpdates"
            )
        }.map { }.onStart { emit(Unit) }.flatMapLatest {
            requestFlow.map { request ->
                request.resolveRootId(rootNodeDocumentId)
            }.flatMapLatest { (request, documentId, notificationString) ->
                when (request) {
                    is DocumentDataRequest.Children ->
                        getChildrenFlow(
                            accountName = accountName,
                            parentDocumentId = documentId,
                            notificationString = notificationString,
                        )

                    is DocumentDataRequest.Document ->
                        getDocumentFlow(
                            accountName = accountName,
                            documentName = documentId,
                            notificationString = notificationString,
                        )
                }
            }
        }

    private fun DocumentDataRequest.resolveRootId(
        rootNodeDocumentId: String,
    ): Triple<DocumentDataRequest, String, String?> =
        if (documentId == CLOUD_DRIVE_ROOT_ID) {
            Triple(this, rootNodeDocumentId, CLOUD_DRIVE_ROOT_ID)
        } else {
            Triple(this, documentId, null)
        }


    private fun getChildrenFlow(
        accountName: String,
        parentDocumentId: String,
        notificationString: String? = null,
    ): Flow<CloudDriveDocumentProviderUiState> {
        val effectiveId = notificationString ?: parentDocumentId
        val parentId = documentIdToNodeIdMapper.get()(parentDocumentId, CLOUD_DRIVE_ROOT_ID)
            ?: return flowOf(
                CloudDriveDocumentProviderUiState.FileNotFound(
                    accountName = accountName,
                    documentId = effectiveId,
                )
            )
        return flow {
            val nodesFlow = getNodesByIdInChunkUseCase.get()(parentId).runningFold<
                    Pair<List<TypedNode>, Boolean>,
                    Pair<List<TypedNode>, Boolean>
                    >(Pair(listOf(), true)) { acc, newValue ->
                Pair(acc.first + newValue.first, newValue.second)
            }.drop(1) // skip initial value
            emitAll(
                combine(
                    nodesFlow,
                    hiddenNodesFilterFlow(),
                ) { (childNodes, hasMore), (isHiddenNodesEnabled, showHiddenItems) ->
                    val filteredNodes = filterNodesByHiddenSettings(
                        childNodes,
                        isHiddenNodesEnabled,
                        showHiddenItems,
                    )
                    CloudDriveDocumentProviderUiState.ChildData(
                        accountName = accountName,
                        parentId = effectiveId,
                        children = filteredNodes.map {
                            cloudDriveDocumentRowMapper.get()(it, CLOUD_DRIVE_ROOT_ID)
                        },
                        hasMore = hasMore,
                    )
                }
            )
        }
    }

    private fun hiddenNodesFilterFlow(): Flow<Pair<Boolean, Boolean>> =
        combine(
            monitorHiddenNodesEnabledUseCase.get()().catch {
                Timber.e(
                    it,
                    "CloudDriveDocumentDataProvider monitorHiddenNodesEnabled"
                )
            },
            monitorShowHiddenItemsUseCase.get()().catch {
                Timber.e(
                    it,
                    "CloudDriveDocumentDataProvider monitorShowHiddenItems"
                )
            },
            ::Pair
        )

    private fun filterNodesByHiddenSettings(
        nodes: List<TypedNode>,
        isHiddenNodesEnabled: Boolean,
        showHiddenItems: Boolean,
    ): List<TypedNode> {
        val showAll = showHiddenItems || !isHiddenNodesEnabled
        return if (showAll) {
            nodes
        } else {
            nodes.filterNot { it.isMarkedSensitive || it.isSensitiveInherited }
        }
    }

    private fun getDocumentFlow(
        accountName: String,
        documentName: String,
        notificationString: String? = null,
    ): Flow<CloudDriveDocumentProviderUiState> {
        val effectiveId = notificationString ?: documentName
        return flow {
            val typedNode = runCatching {
                val nodeId = documentIdToNodeIdMapper.get()(
                    documentName, CLOUD_DRIVE_ROOT_ID
                ) ?: return@runCatching null
                getNodeByHandleUseCase.get()(nodeId.longValue)?.let { node -> addNodeType.get()(node) }
            }.getOrNull()

            if (typedNode == null) {
                emit(
                    CloudDriveDocumentProviderUiState.FileNotFound(
                        accountName = accountName,
                        documentId = effectiveId,
                    )
                )
            } else {
                emitAll(
                    hiddenNodesFilterFlow().map { (isHiddenNodesEnabled, showHiddenItems) ->
                        val shouldHide = filterNodesByHiddenSettings(
                            listOf(typedNode),
                            isHiddenNodesEnabled,
                            showHiddenItems
                        ).isEmpty()
                        if (shouldHide) {
                            CloudDriveDocumentProviderUiState.FileNotFound(
                                accountName = accountName,
                                documentId = effectiveId,
                            )
                        } else {
                            val document =
                                cloudDriveDocumentRowMapper.get()(typedNode, CLOUD_DRIVE_ROOT_ID)
                            val finalDocument =
                                notificationString?.let { document.copy(documentId = it) }
                                    ?: document
                            CloudDriveDocumentProviderUiState.DocumentData(
                                accountName = accountName,
                                documentId = effectiveId,
                                document = finalDocument,
                            )
                        }
                    }
                )
            }
        }
    }

    fun loadDocumentInBackground(documentId: String) {
        applicationScope.launch {
            requestFlow.emit(DocumentDataRequest.Document(documentId))
        }
    }

    fun loadChildrenInBackground(parentDocumentId: String) {
        applicationScope.launch {
            requestFlow.emit(DocumentDataRequest.Children(parentDocumentId))
        }
    }

    fun refreshRootNode() {
        applicationScope.launch {
            refreshRootNodeChannel.send(Unit)
        }
    }

    suspend fun openDocumentFile(documentId: String): File {
        try {
            val nodeId = documentIdToNodeIdMapper.get()(documentId, CLOUD_DRIVE_ROOT_ID)
                ?: throw FileNotFoundException("Invalid document id: $documentId")
            val untypedNode = getNodeByHandleUseCase.get()(nodeId.longValue)
                ?: throw FileNotFoundException("Node not found: $documentId")
            val fileNode = addNodeType.get()(untypedNode) as? TypedFileNode
                ?: throw FileNotFoundException("Document is not a file: $documentId")
            return getOpenableLocalFileForCloudDriveSafUseCase.get()(fileNode)
        } catch (e: FileNotFoundException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            throw FileNotFoundException("Unable to open document: $documentId")
        }
    }

    /** Registers a placeholder for a folder; the SDK call is deferred to [completeFolderCreation]. */
    suspend fun registerPendingFolder(parentDocumentId: String, name: String): String =
        registerPendingCreate(parentDocumentId, name, Document.MIME_TYPE_DIR)

    /** Materialises the folder via the SDK and publishes its real [NodeId] so nested child creates can resolve. */
    suspend fun completeFolderCreation(pendingId: String) {
        val pending = pendingCreates[pendingId]
            ?: throw IllegalStateException("No pending entry for $pendingId")
        require(pending.mimeType == Document.MIME_TYPE_DIR) { "Not a folder pending: $pendingId" }
        // runCatching also catches the NPE Kotlin emits when a mocked value-class return is unboxed.
        val newNodeId = runCatching {
            createFolderNodeUseCase.get()(pending.displayName, pending.parentNodeId)
        }.getOrElse { e ->
            pendingFolderSignals.remove(pendingId)?.completeExceptionally(e)
            pendingCreates.remove(pendingId)
            throw e
        }
        // Publish before the state.first wait so nested child creates suspended on the signal resume immediately.
        pendingFolderRealNodeId[pendingId] = newNodeId
        pendingFolderSignals.remove(pendingId)?.complete(newNodeId)
        // Wait for monitorNodeUpdates to land in state, otherwise notifyChildDocumentsChanged races SAF's re-query.
        val parentDocumentId = parentNodeIdToDocumentId(pending.parentNodeId)
        withTimeoutOrNull(STATE_REFRESH_TIMEOUT_MS) {
            state.first {
                it is CloudDriveDocumentProviderUiState.ChildData &&
                        it.parentId == parentDocumentId &&
                        it.children.any { row -> row.displayName == pending.displayName }
            }
        }
    }

    private suspend fun parentNodeIdToDocumentId(parentNodeId: NodeId): String {
        val rootNodeId = getRootNodeIdUseCase.get()()
        return if (parentNodeId == rootNodeId) {
            CLOUD_DRIVE_ROOT_ID
        } else {
            "$CLOUD_DRIVE_ROOT_ID:${parentNodeId.longValue}"
        }
    }

    /** Registers a placeholder for a file; the upload is enqueued in [onWriteScratchClosed]. */
    suspend fun registerPendingFile(
        parentDocumentId: String,
        displayName: String,
        mimeType: String,
    ): String = registerPendingCreate(parentDocumentId, displayName, mimeType)

    private suspend fun registerPendingCreate(
        parentDocumentId: String,
        displayName: String,
        mimeType: String,
    ): String {
        assertSessionReadyForWrite()
        val parentNodeId = resolveParentNodeId(parentDocumentId)
            ?: throw FileNotFoundException("Invalid parent: $parentDocumentId")
        if (getChildNodeUseCase.get()(parentNodeId, displayName) != null) {
            throw FileNotFoundException("Name already exists: $displayName")
        }
        val pendingId = "$PENDING_PREFIX:${UUID.randomUUID()}"
        pendingCreates[pendingId] = PendingCreate(
            parentNodeId = parentNodeId,
            parentDocumentId = parentDocumentId,
            displayName = displayName,
            mimeType = mimeType,
        )
        return pendingId
    }

    /** Returns true if [documentId] is a pending placeholder. */
    fun isPendingDocumentId(documentId: String): Boolean =
        documentId.startsWith("$PENDING_PREFIX:")

    /** Synthetic rows merged into [parentDocumentId]'s listing; resolved folders are excluded to avoid duplicates with the real node. */
    fun getPendingChildrenForParent(parentDocumentId: String): List<CloudDriveDocumentRow> =
        pendingCreates.entries
            .filter { (id, pending) ->
                pending.parentDocumentId == parentDocumentId &&
                        !pendingFolderRealNodeId.containsKey(id)
            }
            .mapNotNull { (pendingId, _) -> getPendingDocumentRow(pendingId) }

    /** Row shown for a placeholder during the gap between createDocument and openDocument. */
    fun getPendingDocumentRow(documentId: String): CloudDriveDocumentRow? {
        val pending = pendingCreates[documentId] ?: return null
        // Folder placeholders need FLAG_DIR_SUPPORTS_CREATE so SAF treats them as upload destinations during recursive copy.
        val flags = if (pending.mimeType == Document.MIME_TYPE_DIR) {
            Document.FLAG_DIR_SUPPORTS_CREATE
        } else {
            Document.FLAG_SUPPORTS_WRITE
        }
        return CloudDriveDocumentRow(
            documentId = documentId,
            displayName = pending.displayName,
            mimeType = pending.mimeType,
            size = 0L,
            lastModified = System.currentTimeMillis(),
            flags = flags,
        )
    }

    /** Cache file the SAF caller writes to; bytes are uploaded once the PFD closes. */
    fun prepareWriteScratchFile(documentId: String): File {
        assertSessionReadyForWrite()
        val pending = pendingCreates[documentId]
            ?: throw FileNotFoundException("Unknown pending document: $documentId")
        val uuid = documentId.removePrefix("$PENDING_PREFIX:")
        val cacheFile = getCacheFileUseCase.get()(
            SAF_UPLOADS_CACHE_FOLDER,
            "${uuid}_${pending.displayName}",
        ) ?: throw FileNotFoundException("Cannot create scratch file for $documentId")
        cacheFile.parentFile?.mkdirs()
        if (!cacheFile.exists()) cacheFile.createNewFile()
        return cacheFile
    }

    /** Enqueues the upload once the write PFD has closed; placeholder lives until the real node lands or the watcher times out. */
    fun onWriteScratchClosed(documentId: String, file: File, err: IOException?) {
        val pending = pendingCreates[documentId] ?: run {
            Timber.w("CloudDriveDocumentDataProvider: no pending entry for $documentId")
            runCatching { file.delete() }
            return
        }
        if (err != null) {
            Timber.e(err, "CloudDriveDocumentDataProvider write failed for $documentId")
            runCatching { file.delete() }
            finalizePendingFile(documentId)
            return
        }
        applicationScope.launch {
            try {
                runCatching {
                    withTimeoutOrNull(UPLOAD_COMPLETION_TIMEOUT_MS) {
                        startUploadUseCase.get()(
                            localPath = file.absolutePath,
                            parentNodeId = pending.parentNodeId,
                            fileName = pending.displayName,
                            modificationTime = file.lastModified() / 1000,
                            appData = null,
                            isSourceTemporary = true,
                            shouldStartFirst = false,
                            pitagTrigger = PitagTrigger.NotApplicable,
                            pitagTarget = PitagTarget.CloudDrive,
                        ).collect { event ->
                            if (event is TransferEvent.TransferFinishEvent) {
                                event.error?.let {
                                    Timber.e(
                                        it,
                                        "CloudDriveDocumentDataProvider upload error $documentId"
                                    )
                                }
                            }
                        }
                    }
                    val parentDocumentId = parentNodeIdToDocumentId(pending.parentNodeId)
                    withTimeoutOrNull(STATE_REFRESH_TIMEOUT_MS) {
                        state.first {
                            it is CloudDriveDocumentProviderUiState.ChildData &&
                                    it.parentId == parentDocumentId &&
                                    it.children.any { row -> row.displayName == pending.displayName }
                        }
                    }
                }.onFailure {
                    Timber.e(it, "CloudDriveDocumentDataProvider upload failed $documentId")
                }
            } finally {
                finalizePendingFile(documentId)
            }
        }
    }

    /** Suspends until the upload for [pendingId] has finalized (success, failure, or timeout). */
    suspend fun awaitFileFinalized(pendingId: String) {
        if (!pendingCreates.containsKey(pendingId)) return
        val signal = pendingFinalizeSignals.computeIfAbsent(pendingId) { CompletableDeferred() }
        // Re-check after registering the signal in case finalization raced with us.
        if (!pendingCreates.containsKey(pendingId)) {
            pendingFinalizeSignals.remove(pendingId)?.complete(Unit)
        }
        signal.await()
    }

    private fun finalizePendingFile(pendingId: String) {
        pendingCreates.remove(pendingId)
        pendingFinalizeSignals.remove(pendingId)?.complete(Unit)
    }

    /** Renames via the SDK and returns the parent document id so the caller can notify the listing. */
    suspend fun renameDocument(documentId: String, newName: String): String {
        assertSessionReadyForWrite()
        val nodeId = documentIdToNodeIdMapper.get()(documentId, CLOUD_DRIVE_ROOT_ID)
            ?: throw FileNotFoundException("Invalid document id: $documentId")
        val parentDocumentId = resolveParentDocumentId(nodeId)
            ?: throw FileNotFoundException("Cannot resolve parent for $documentId")
        renameNodeUseCase.get()(nodeId.longValue, newName)
        // Block until state holds DocumentData(newName); SAF's DocumentInfo.fromUri reads
        // queryDocument synchronously after this returns and a stale cursor surfaces as
        // "Failed to rename document" in DocumentsUI even though the cloud rename succeeded.
        requestFlow.emit(DocumentDataRequest.Document(documentId))
        withTimeoutOrNull(STATE_REFRESH_TIMEOUT_MS) {
            state.first {
                it is CloudDriveDocumentProviderUiState.DocumentData &&
                        it.documentId == documentId &&
                        it.document.displayName == newName
            }
        }
        return parentDocumentId
    }

    private suspend fun resolveParentDocumentId(nodeId: NodeId): String? {
        val node = getNodeByHandleUseCase.get()(nodeId.longValue) ?: return null
        return parentNodeIdToDocumentId(node.parentId)
    }

    private suspend fun resolveParentNodeId(parentDocumentId: String): NodeId? {
        if (parentDocumentId == CLOUD_DRIVE_ROOT_ID) {
            return getRootNodeIdUseCase.get()()
        }
        if (parentDocumentId.startsWith("$PENDING_PREFIX:")) {
            return resolvePendingFolderParent(parentDocumentId)
        }
        return documentIdToNodeIdMapper.get()(parentDocumentId, CLOUD_DRIVE_ROOT_ID)
    }

    // Suspends until completeFolderCreation publishes the real NodeId; null result becomes
    // `FileNotFoundException("Invalid parent")` upstream so SAF aborts the subtree.
    private suspend fun resolvePendingFolderParent(parentDocumentId: String): NodeId? {
        pendingFolderRealNodeId[parentDocumentId]?.let { return it }
        val pending = pendingCreates[parentDocumentId] ?: return null
        if (pending.mimeType != Document.MIME_TYPE_DIR) return null
        val signal = pendingFolderSignals.computeIfAbsent(parentDocumentId) { CompletableDeferred() }
        // Re-check after registering the signal in case completion raced with us.
        pendingFolderRealNodeId[parentDocumentId]?.let { return it }
        return runCatching {
            withTimeoutOrNull(FOLDER_CREATE_TIMEOUT_MS) { signal.await() }
        }.getOrNull()
    }

    private fun assertSessionReadyForWrite() {
        when (state.value) {
            CloudDriveDocumentProviderUiState.NotLoggedIn ->
                throw FileNotFoundException("Not logged in")

            is CloudDriveDocumentProviderUiState.PasscodeLockEnabled ->
                throw FileNotFoundException("Passcode lock enabled")

            else -> Unit
        }
    }

    companion object {
        /** Document id for the Cloud Drive root. */
        const val CLOUD_DRIVE_ROOT_ID = "mega_cloud_drive_root"

        /** Document id prefix for in-flight create placeholders. */
        const val PENDING_PREFIX = "mega_cloud_drive_pending"

        /** Cache subfolder for SAF write scratch files until upload completes. */
        const val SAF_UPLOADS_CACHE_FOLDER = "saf_uploads"

        // Cap how long write ops block waiting for monitorNodeUpdates to propagate to state.
        private const val STATE_REFRESH_TIMEOUT_MS = 5_000L

        // Cap how long a nested child create blocks awaiting its placeholder folder's NodeId;
        // a single SDK round-trip, so this only fires if the network or SDK is wedged.
        private const val FOLDER_CREATE_TIMEOUT_MS = 30_000L

        // Cap how long the upload watcher waits for the real node to surface; on timeout we
        // still drop the placeholder so the listing refreshes with whatever the SDK has.
        private const val UPLOAD_COMPLETION_TIMEOUT_MS = 30 * 60 * 1000L
    }
}
