package mega.privacy.android.app.providers.documentprovider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
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
import mega.privacy.android.domain.usecase.account.MonitorUserCredentialsUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.GetNodesByIdInChunkUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
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
    private val cloudDriveDocumentRowMapper: CloudDriveDocumentRowMapper,
    private val addNodeType: AddNodeType,
    private val documentIdToNodeIdMapper: DocumentIdToNodeIdMapper,
) {

    val state: StateFlow<CloudDriveDocumentProviderUiState> by lazy {
        monitorUserCredentialsUseCase().flatMapLatest { credentials ->
            flow {
                if (credentials == null) {
                    emit(CloudDriveDocumentProviderUiState.NotLoggedIn)
                } else {
                    val accountName = credentials.email ?: ""
                    emitAll(getRootNodeFlow().mapLatest { if (it?.longValue == -1L) null else it }
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
        }.catch { Timber.e(it, "CloudDriveDocumentDataProvider state") }.asUiStateFlow(
            scope = applicationScope,
            initialValue = CloudDriveDocumentProviderUiState.LoadingRoot
        )
    }

    private val refreshRootNodeChannel =
        Channel<Unit>(1, onBufferOverflow = BufferOverflow.DROP_LATEST)

    private fun getRootNodeFlow() = flow {
        val rootNode = runCatching {
            getRootNodeWithFastLoginIfNeeded()
        }.getOrNull()
        if (rootNode == null) {
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
                                request = request,
                                rootNodeDocumentId = rootNodeDocumentId
                            )
                        }

                        is DocumentDataRequest.Document -> {
                            collectDocumentFlow(
                                accountName = accountName,
                                request = request,
                                rootNodeDocumentId = rootNodeDocumentId
                            )
                        }

                        DocumentDataRequest.Root -> {
                            emit(
                                CloudDriveDocumentProviderUiState.Root(
                                    accountName = accountName,
                                    rootNodeDocumentId = rootNodeDocumentId,
                                )
                            )
                        }
                    }
                }
            }
        }


    private suspend fun FlowCollector<CloudDriveDocumentProviderUiState>.collectChildrenFlow(
        accountName: String,
        request: DocumentDataRequest.Children,
        rootNodeDocumentId: String,
    ) {
        getChildDataFlow(request, accountName, rootNodeDocumentId).let { childDataFlow ->
            if (childDataFlow != null) {
                emitAll(childDataFlow)
            } else {
                emit(
                    CloudDriveDocumentProviderUiState.FileNotFound(
                        accountName = accountName,
                        documentId = request.parentName,
                        rootNodeDocumentId = rootNodeDocumentId,
                    )
                )
            }
        }
    }

    private suspend fun getChildDataFlow(
        request: DocumentDataRequest.Children,
        accountName: String,
        rootNodeDocumentId: String,
    ): Flow<CloudDriveDocumentProviderUiState>? = runCatching {
        val parentId = documentIdToNodeIdMapper(
            request.parentName, CLOUD_DRIVE_ROOT_ID
        ) ?: return@runCatching null
        getNodesByIdInChunkUseCase(parentId).runningFold<Pair<List<TypedNode>, Boolean>, Pair<List<TypedNode>, Boolean>>(
            Pair(listOf(), true)
        ) { acc, newValue ->
            Pair(acc.first + newValue.first, newValue.second)
        }
            .mapLatest<Pair<List<TypedNode>, Boolean>, CloudDriveDocumentProviderUiState> { (childNodes, hasMore) ->
                CloudDriveDocumentProviderUiState.ChildData(
                    accountName = accountName,
                    parentId = request.parentName,
                    children = childNodes.map {
                        cloudDriveDocumentRowMapper(
                            it, CLOUD_DRIVE_ROOT_ID
                        )
                    },
                    hasMore = hasMore,
                    rootNodeDocumentId = rootNodeDocumentId,
                )
            }.onStart {
                emit(
                    CloudDriveDocumentProviderUiState.LoadingChildren(
                        accountName = accountName,
                        currentParentDocumentId = request.parentName,
                        rootNodeDocumentId = rootNodeDocumentId,
                    )
                )
            }
    }.getOrNull()

    private suspend fun FlowCollector<CloudDriveDocumentProviderUiState>.collectDocumentFlow(
        accountName: String,
        request: DocumentDataRequest.Document,
        rootNodeDocumentId: String,
    ) {
        emit(
            CloudDriveDocumentProviderUiState.LoadingDocument(
                accountName = accountName,
                currentDocumentId = request.documentName,
                rootNodeDocumentId = rootNodeDocumentId
            )
        )
        val document = runCatching {
            val documentId = documentIdToNodeIdMapper(
                request.documentName, CLOUD_DRIVE_ROOT_ID
            ) ?: return@runCatching null
            getNodeByHandleUseCase(documentId.longValue)?.let {
                addNodeType(it)
            }?.let {
                cloudDriveDocumentRowMapper(it, CLOUD_DRIVE_ROOT_ID)
            }
        }.getOrNull()

        if (document != null) {
            emit(
                CloudDriveDocumentProviderUiState.DocumentData(
                    accountName = accountName,
                    documentId = request.documentName,
                    document = document,
                    rootNodeDocumentId = rootNodeDocumentId,
                )
            )
        } else {
            emit(
                CloudDriveDocumentProviderUiState.FileNotFound(
                    accountName = accountName,
                    documentId = request.documentName,
                    rootNodeDocumentId = rootNodeDocumentId,
                )
            )
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


    companion object {
        const val CLOUD_DRIVE_ROOT_ID = "mega_cloud_drive_root"
    }
}
