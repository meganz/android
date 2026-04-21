package mega.privacy.android.app.providers.documentprovider

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.annotation.VisibleForTesting
import androidx.core.content.getSystemService
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch
import mega.privacy.android.app.providers.documentprovider.model.CloudDriveDocumentProviderUiState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.MonitorPasscodeLockPreferenceUseCase
import mega.privacy.android.domain.usecase.account.MonitorUserCredentialsUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.domain.usecase.login.GetAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.GetNodesByIdInChunkUseCase
import mega.privacy.android.domain.usecase.node.GetOpenableLocalFileForCloudDriveSafUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Data provider for the Cloud Drive document provider.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class CloudDriveDocumentDataProvider @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase,
    private val getNodesByIdInChunkUseCase: GetNodesByIdInChunkUseCase,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val backgroundFastLoginUseCase: BackgroundFastLoginUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val monitorUserCredentialsUseCase: MonitorUserCredentialsUseCase,
    private val getAccountCredentialsUseCase: GetAccountCredentialsUseCase,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val cloudDriveDocumentRowMapper: CloudDriveDocumentRowMapper,
    private val addNodeType: AddNodeType,
    private val documentIdToNodeIdMapper: DocumentIdToNodeIdMapper,
    private val monitorPasscodeLockPreferenceUseCase: MonitorPasscodeLockPreferenceUseCase,
    private val getOpenableLocalFileForCloudDriveSafUseCase: GetOpenableLocalFileForCloudDriveSafUseCase,
) {

    /**
     * Connectivity state. Updated by [monitorConnectivity] or [updateConnectivity] (e.g. for tests).
     */
    private val connectivityState = MutableStateFlow(true)

    /**
     * Updates connectivity state. For use in tests only; production code uses [monitorConnectivity].
     */
    @VisibleForTesting
    fun updateConnectivity(connected: Boolean) {
        connectivityState.value = connected
    }

    /**
     * Starts monitoring network connectivity and updates [connectivityState].
     * Call once from the content provider's [android.content.ContentProvider.onCreate]
     */
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
            monitorPasscodeLockPreferenceUseCase().catch {
                Timber.e(it)
                emit(false)
            },
            connectivityState,
            monitorUserCredentialsUseCase().onStart { emit(getAccountCredentialsUseCase()) }
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
        getRootNodeIdUseCase() ?: run {
            Timber.d("CloudDriveDocumentDataProvider getRootNodeUseCase returned null, attempting fast login")
            backgroundFastLoginUseCase().let {
                getRootNodeIdUseCase()
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
        monitorNodeUpdatesUseCase().catch {
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
        val parentId = documentIdToNodeIdMapper(parentDocumentId, CLOUD_DRIVE_ROOT_ID)
            ?: return flowOf(
                CloudDriveDocumentProviderUiState.FileNotFound(
                    accountName = accountName,
                    documentId = effectiveId,
                )
            )
        return flow {
            val nodesFlow = getNodesByIdInChunkUseCase(parentId).runningFold<
                    Pair<List<TypedNode>, Boolean>,
                    Pair<List<TypedNode>, Boolean>
                    >(Pair(listOf(), true)) { acc, newValue ->
                Pair(acc.first + newValue.first, newValue.second)
            }
            emit(
                CloudDriveDocumentProviderUiState.LoadingChildren(
                    accountName = accountName,
                    currentParentDocumentId = effectiveId,
                )
            )
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
                            cloudDriveDocumentRowMapper(it, CLOUD_DRIVE_ROOT_ID)
                        },
                        hasMore = hasMore,
                    )
                }
            )
        }
    }

    private fun hiddenNodesFilterFlow(): Flow<Pair<Boolean, Boolean>> =
        combine(
            monitorHiddenNodesEnabledUseCase().catch {
                Timber.e(
                    it,
                    "CloudDriveDocumentDataProvider monitorHiddenNodesEnabled"
                )
            },
            monitorShowHiddenItemsUseCase().catch {
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
            emit(
                CloudDriveDocumentProviderUiState.LoadingDocument(
                    accountName = accountName,
                    currentDocumentId = effectiveId,
                )
            )
            val typedNode = runCatching {
                val nodeId = documentIdToNodeIdMapper(
                    documentName, CLOUD_DRIVE_ROOT_ID
                ) ?: return@runCatching null
                getNodeByHandleUseCase(nodeId.longValue)?.let { addNodeType(it) }
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
                                cloudDriveDocumentRowMapper(typedNode, CLOUD_DRIVE_ROOT_ID)
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

    /** Local [File] for SAF [documentId]. @throws FileNotFoundException */
    suspend fun openDocumentFile(documentId: String): File {
        try {
            val nodeId = documentIdToNodeIdMapper(documentId, CLOUD_DRIVE_ROOT_ID)
                ?: throw FileNotFoundException("Invalid document id: $documentId")
            val untypedNode = getNodeByHandleUseCase(nodeId.longValue)
                ?: throw FileNotFoundException("Node not found: $documentId")
            val fileNode = addNodeType(untypedNode) as? TypedFileNode
                ?: throw FileNotFoundException("Document is not a file: $documentId")
            return getOpenableLocalFileForCloudDriveSafUseCase(fileNode)
        } catch (e: FileNotFoundException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            throw FileNotFoundException("Unable to open document: $documentId")
        }
    }

    companion object {
        const val CLOUD_DRIVE_ROOT_ID = "mega_cloud_drive_root"
    }
}
