package mega.privacy.android.app.providers.documentprovider

import android.content.Context
import android.net.ConnectivityManager
import androidx.annotation.VisibleForTesting
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeId
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
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

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

    @OptIn(FlowPreview::class)
    val state: StateFlow<CloudDriveDocumentProviderUiState> by lazy {
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
            Triple(isPasscodeLockEnabled, isConnected, credentials)
        }.flatMapLatest { (isPasscodeLockEnabled, isConnected, credentials) ->
            flow {
                if (credentials == null) {
                    emit(CloudDriveDocumentProviderUiState.NotLoggedIn)
                } else if (isPasscodeLockEnabled) {
                    val accountName = credentials.email ?: ""
                    emit(CloudDriveDocumentProviderUiState.PasscodeLockEnabled(accountName))
                } else if (!isConnected) {
                    val accountName = credentials.email ?: ""
                    emit(CloudDriveDocumentProviderUiState.Offline(accountName))
                } else {
                    val accountName = credentials.email ?: ""
                    emitAll(
                        getRootNodeFlow()
                            .flatMapLatest { rootNodeId ->
                                if (rootNodeId == null) {
                                    flowOf(
                                        CloudDriveDocumentProviderUiState.RootNodeNotLoaded(
                                            accountName
                                        )
                                    )
                                } else {
                                    getDataFlows(
                                        accountName,
                                        "$CLOUD_DRIVE_ROOT_ID:${rootNodeId.longValue}"
                                    )
                                }
                            })
                }
            }
        }.catch { e ->
            Timber.e(e, "CloudDriveDocumentDataProvider state")
            emit(CloudDriveDocumentProviderUiState.NotLoggedIn)
        }.asUiStateFlow(
            scope = applicationScope,
            initialValue = CloudDriveDocumentProviderUiState.Initialising
        )
    }

    private val refreshRootNodeChannel =
        Channel<Unit>(1, onBufferOverflow = BufferOverflow.DROP_LATEST)

    private fun getRootNodeFlow() = flow {
        val rootNode = runCatching {
            getRootNodeWithFastLoginIfNeeded()
        }.getOrNull()
        if (rootNode == null) {
            emit(null)
            emitAll(refreshRootNodeChannel.receiveAsFlow().mapLatest {
                getRootNodeWithFastLoginIfNeeded()
            })
        } else {
            emit(rootNode)
        }
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
        data object Root : DocumentDataRequest
        data object RootChildren : DocumentDataRequest
        data class Children(val parentName: String) : DocumentDataRequest
        data class Document(val documentName: String) : DocumentDataRequest
    }

    private val requestFlow: MutableStateFlow<DocumentDataRequest> =
        MutableStateFlow(DocumentDataRequest.Root)

    private fun getDataFlows(accountName: String, rootNodeDocumentId: String) =
        monitorNodeUpdatesUseCase().catch {
            Timber.e(
                it,
                "CloudDriveDocumentDataProvider monitorNodeUpdates"
            )
        }.mapLatest { Unit }.onStart { emit(Unit) }.flatMapLatest {
            requestFlow.flatMapLatest { request ->
                flow {
                    when (request) {
                        is DocumentDataRequest.Children -> {
                            collectChildrenFlow(
                                accountName = accountName,
                                parentDocumentId = request.parentName,
                            )
                        }

                        is DocumentDataRequest.Document -> {
                            collectDocumentFlow(
                                accountName = accountName,
                                documentName = request.documentName,
                            )
                        }

                        DocumentDataRequest.Root -> {
                            collectDocumentFlow(
                                accountName = accountName,
                                documentName = rootNodeDocumentId,
                                notificationString = CLOUD_DRIVE_ROOT_ID
                            )
                        }

                        DocumentDataRequest.RootChildren -> {
                            collectChildrenFlow(
                                accountName = accountName,
                                parentDocumentId = rootNodeDocumentId,
                                notificationString = CLOUD_DRIVE_ROOT_ID
                            )
                        }
                    }
                }
            }
        }


    private suspend fun FlowCollector<CloudDriveDocumentProviderUiState>.collectChildrenFlow(
        accountName: String,
        parentDocumentId: String,
        notificationString: String? = null,
    ) {
        getChildDataFlow(parentDocumentId, accountName, notificationString).let { childDataFlow ->
            if (childDataFlow != null) {
                emitAll(childDataFlow)
            } else {
                emit(
                    CloudDriveDocumentProviderUiState.FileNotFound(
                        accountName = accountName,
                        documentId = notificationString ?: parentDocumentId,
                    )
                )
            }
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

    private suspend fun getChildDataFlow(
        parentDocumentId: String,
        accountName: String,
        notificationString: String? = null,
    ): Flow<CloudDriveDocumentProviderUiState>? = runCatching {
        val parentId = documentIdToNodeIdMapper(
            parentDocumentId, CLOUD_DRIVE_ROOT_ID
        ) ?: return@runCatching null
        val nodesFlow = getNodesByIdInChunkUseCase(parentId).runningFold<
                Pair<List<TypedNode>, Boolean>,
                Pair<List<TypedNode>, Boolean>
                >(Pair(listOf(), true)) { acc, newValue ->
            Pair(acc.first + newValue.first, newValue.second)
        }
        flow<CloudDriveDocumentProviderUiState> {
            emit(
                CloudDriveDocumentProviderUiState.LoadingChildren(
                    accountName = accountName,
                    currentParentDocumentId = notificationString ?: parentDocumentId,
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
                        parentId = notificationString ?: parentDocumentId,
                        children = filteredNodes.map {
                            cloudDriveDocumentRowMapper(it, CLOUD_DRIVE_ROOT_ID)
                        },
                        hasMore = hasMore,
                    )
                }
            )
        }
    }.getOrNull()

    private suspend fun FlowCollector<CloudDriveDocumentProviderUiState>.collectDocumentFlow(
        accountName: String,
        documentName: String,
        notificationString: String? = null,
    ) {
        emit(
            CloudDriveDocumentProviderUiState.LoadingDocument(
                accountName = accountName,
                currentDocumentId = notificationString ?: documentName,
            )
        )
        val typedNode = runCatching {
            val nodeId = documentIdToNodeIdMapper(
                documentName, CLOUD_DRIVE_ROOT_ID
            ) ?: return@runCatching null
            getNodeByHandleUseCase(nodeId.longValue)?.let { addNodeType(it) }
        }.getOrNull()

        val (isHiddenNodesEnabled, showHiddenItems) = hiddenNodesFilterFlow().first()
        val shouldHideDocument = typedNode != null &&
                filterNodesByHiddenSettings(
                    listOf(typedNode),
                    isHiddenNodesEnabled,
                    showHiddenItems
                ).isEmpty()

        if (typedNode != null && !shouldHideDocument) {
            val document = cloudDriveDocumentRowMapper(typedNode, CLOUD_DRIVE_ROOT_ID)
            val finalDocument =
                notificationString?.let { document.copy(documentId = it) } ?: document
            emit(
                CloudDriveDocumentProviderUiState.DocumentData(
                    accountName = accountName,
                    documentId = notificationString ?: documentName,
                    document = finalDocument,
                )
            )
        } else {
            emit(
                CloudDriveDocumentProviderUiState.FileNotFound(
                    accountName = accountName,
                    documentId = notificationString ?: documentName,
                )
            )
        }
    }

    fun loadDocumentInBackground(documentId: String) {
        applicationScope.launch {
            val request = if (documentId == CLOUD_DRIVE_ROOT_ID) {
                DocumentDataRequest.Root
            } else {
                DocumentDataRequest.Document(documentId)
            }
            requestFlow.emit(request)
        }
    }

    fun loadChildrenInBackground(parentDocumentId: String) {
        applicationScope.launch {
            val request = if (parentDocumentId == CLOUD_DRIVE_ROOT_ID) {
                DocumentDataRequest.RootChildren
            } else {
                DocumentDataRequest.Children(parentDocumentId)
            }
            requestFlow.emit(request)
        }
    }

    fun refreshRootNode() {
        applicationScope.launch {
            refreshRootNodeChannel.send(Unit)
        }
    }


    companion object {
        const val CLOUD_DRIVE_ROOT_ID = "mega_cloud_drive_root"
    }
}
