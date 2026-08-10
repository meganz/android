package mega.privacy.android.core.nodecomponents.sheet.options

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.android.core.ui.model.SnackbarAttributes
import mega.privacy.android.core.nodecomponents.mapper.NodeBottomSheetActionMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeBottomSheetState
import mega.privacy.android.core.nodecomponents.mapper.OfflineTypedNodeMapper
import mega.privacy.android.core.nodecomponents.mapper.ZipFileTypedNodeMapper
import mega.privacy.android.core.nodecomponents.menu.menuitem.InfoBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.registry.NodeMenuProviderRegistry
import mega.privacy.android.core.nodecomponents.model.NodeActionModeMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailUriRequest
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.MonitorNodeUpdatesById
import mega.privacy.android.domain.usecase.file.GetFileByPathUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeFromSerializedDataUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.GetPublicNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.IsNodeDeletedFromBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import mega.privacy.android.domain.usecase.node.publiclink.MapTypedNodeToPublicLinkUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineFileInformationByIdUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.shared.nodes.mapper.NodeUiItemMapper
import mega.privacy.android.shared.nodes.model.NodeUiItem
import timber.log.Timber

/**
 * Node options bottom sheet view model
 *
 * @property nodeBottomSheetActionMapper
 * @property getNodeAccessPermission
 * @property isNodeInRubbishBinUseCase
 * @property isNodeInBackupsUseCase
 * @property monitorConnectivityUseCase
 * @property getNodeByIdUseCase
 * @property getPublicNodeByIdUseCase
 */
@HiltViewModel(assistedFactory = NodeOptionsBottomSheetViewModel.Factory::class)
class NodeOptionsBottomSheetViewModel @AssistedInject constructor(
    private val nodeBottomSheetActionMapper: NodeBottomSheetActionMapper,
    private val getNodeAccessPermission: GetNodeAccessPermission,
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase,
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase,
    private val isNodeDeletedFromBackupsUseCase: IsNodeDeletedFromBackupsUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getPublicNodeByIdUseCase: GetPublicNodeByIdUseCase,
    private val getPublicNodeUseCase: GetPublicNodeUseCase,
    private val mapTypedNodeToPublicLinkUseCase: MapTypedNodeToPublicLinkUseCase,
    private val nodeUiItemMapper: NodeUiItemMapper,
    private val offlineTypedNodeMapper: OfflineTypedNodeMapper,
    private val getOfflineFileInformationByIdUseCase: GetOfflineFileInformationByIdUseCase,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    private val monitorNodeUpdatesById: MonitorNodeUpdatesById,
    private val snackbarEventQueue: SnackbarEventQueue,
    private val nodeMenuProviderRegistry: NodeMenuProviderRegistry,
    private val getFileByPathUseCase: GetFileByPathUseCase,
    private val zipFileTypedNodeMapper: ZipFileTypedNodeMapper,
    private val getPublicNodeFromSerializedDataUseCase: GetPublicNodeFromSerializedDataUseCase,
    private val getChatFileUseCase: GetChatFileUseCase,
    @Assisted private val nodeId: Long,
    @Assisted private val nodeSourceType: NodeSourceType,
    @Assisted private val partiallyExpand: Boolean,
    @Assisted("chatId") private val chatId: Long?,
    @Assisted("msgId") private val msgId: Long?,
    @Assisted("publicLinkUrl") private val publicLinkUrl: String?,
    @Assisted("localFilePath") private val localFilePath: String?,
    @Assisted("serializedData") private val serializedData: String?,
) : ViewModel() {

    private data class PrimaryNodeResult(
        val node: TypedNode?,
        val isChatNodeFromOthers: Boolean = false,
    )

    private var offlineMonitorJob: Job? = null

    internal val uiState: StateFlow<NodeBottomSheetState>
        field = MutableStateFlow(
            NodeBottomSheetState(
                nodeId = nodeId,
                nodeSourceType = nodeSourceType,
                partiallyExpand = partiallyExpand,
            )
        )

    init {
        viewModelScope.launch {
            monitorConnectivityUseCase().collect { isConnected ->
                uiState.update {
                    it.copy(isOnline = isConnected)
                }
            }
        }
        getBottomSheetOptions()
        if (nodeSourceType == NodeSourceType.OFFLINE) {
            monitorOfflineNodeAvailability(nodeId)
        }
        if (publicLinkUrl.isNullOrBlank()
            && nodeSourceType != NodeSourceType.FOLDER_LINK
            && nodeSourceType != NodeSourceType.FILE_LINK
            && nodeSourceType != NodeSourceType.MEDIA_PLAYER_ZIP_FILE
        ) {
            viewModelScope.launch {
                monitorNodeUpdatesById(NodeId(nodeId))
                    .catch { Timber.e(it) }
                    .collectLatest { getBottomSheetOptions() }
            }
        }
    }

    suspend fun showSnackbar(attributes: SnackbarAttributes) =
        snackbarEventQueue.queueMessage(attributes)

    private fun getBottomSheetOptions() {
        viewModelScope.launch {
            val nodeId = NodeId(nodeId)
            val options = nodeMenuProviderRegistry.getBottomSheetOptions(nodeSourceType)
            val deferredNode = async { loadPrimaryNode(nodeId) }
            val deferredPermission = async {
                runCatching { getNodeAccessPermission(nodeId) }.getOrNull()
            }
            val isInRubbish = runCatching { isNodeInRubbishBinUseCase(nodeId) }.getOrDefault(false)
            val deferredInBackups = async { loadIsInBackups(nodeId, isInRubbish) }
            val (primaryNode, isChatNodeFromOthers) = deferredNode.await()
            val effectiveNode =
                primaryNode ?: loadOfflineFallbackNode(nodeId) ?: loadZipFileNode()

            if (effectiveNode == null) {
                uiState.update { it.copy(error = triggered(Exception("Node is null"))) }
                return@launch
            }

            val bottomSheetItems = buildBottomSheetItems(
                options = options,
                node = effectiveNode,
                isInRubbish = isInRubbish,
                permission = deferredPermission.await(),
                isInBackUps = deferredInBackups.await(),
                isChatNodeFromOthers = isChatNodeFromOthers,
            )
            val nodeUiItem = nodeUiItemMapper(listOf(effectiveNode))
                .firstOrNull()
                ?.withPublicLinkPreview(effectiveNode)

            uiState.update {
                it.copy(
                    actions = bottomSheetItems,
                    node = nodeUiItem,
                    error = if (bottomSheetItems.isEmpty()) {
                        triggered(Exception("No actions available"))
                    } else {
                        consumed()
                    },
                )
            }
        }
    }

    /**
     * Lookup branches in priority order:
     *  1. [NodeSourceType.FILE_LINK] — fetch via [getPublicNodeUseCase] and
     *     wrap as a `PublicLinkFile` so click handlers can route through
     *     `CopyPublicNodeUseCase`. Falls back to [getNodeByIdUseCase] when no
     *     [publicLinkUrl] is provided (the file may still be in the user's
     *     account).
     *  2. [NodeSourceType.FOLDER_LINK] — fetch by id via [getPublicNodeByIdUseCase].
     *     Falls back to [getPublicNodeFromSerializedDataUseCase] when the id lookup
     *     returns null.
     *  3. [NodeSourceType.CHAT] — try [getNodeByIdUseCase] first (own nodes sent to
     *     chat). If that returns null, fall back to [GetChatFileUseCase] using
     *     [chatId]/[msgId] (nodes received from others); in that case
     *     [PrimaryNodeResult.isChatNodeFromOthers] is set to true so the caller can
     *     suppress actions that require node ownership (e.g. Info).
     *  4. Otherwise fetch their own nodes by [getNodeByIdUseCase].
     */
    private suspend fun loadPrimaryNode(nodeId: NodeId): PrimaryNodeResult = runCatching {
        when (nodeSourceType) {
            NodeSourceType.FILE_LINK -> PrimaryNodeResult(
                node = if (!publicLinkUrl.isNullOrBlank()) {
                    mapTypedNodeToPublicLinkUseCase(getPublicNodeUseCase(publicLinkUrl))
                } else {
                    getNodeByIdUseCase(nodeId)
                }
            )

            NodeSourceType.FOLDER_LINK -> PrimaryNodeResult(
                node = getPublicNodeByIdUseCase(nodeId) ?: run {
                    Timber.d("getPublicNodeByIdUseCase returned null for nodeId=$nodeId, trying serializedData fallback")
                    serializedData?.let { getPublicNodeFromSerializedDataUseCase(it) }
                }
            )

            NodeSourceType.CHAT -> {
                val node = getNodeByIdUseCase(nodeId)
                if (node != null) {
                    PrimaryNodeResult(node = node)
                } else {
                    val chatFile = if (chatId != null && msgId != null)
                        getChatFileUseCase(chatId, msgId)
                    else null
                    PrimaryNodeResult(
                        node = chatFile,
                        isChatNodeFromOthers = chatFile != null,
                    )
                }
            }

            else -> PrimaryNodeResult(node = getNodeByIdUseCase(nodeId))
        }
    }.getOrNull() ?: PrimaryNodeResult(null)

    private suspend fun loadZipFileNode(): TypedNode? {
        if (nodeSourceType != NodeSourceType.MEDIA_PLAYER_ZIP_FILE) return null
        return localFilePath?.let { path ->
            runCatching {
                val file = getFileByPathUseCase(path) ?: return null
                zipFileTypedNodeMapper(file)
            }.onFailure {
                Timber.e(it, "Failed to load zip file information for nodeId=$nodeId")
            }.getOrNull()
        }
    }

    private suspend fun loadOfflineFallbackNode(nodeId: NodeId): TypedNode? {
        if (nodeSourceType != NodeSourceType.OFFLINE) return null
        return runCatching { getOfflineFileInformationByIdUseCase(nodeId) }
            .onFailure {
                Timber.e(it, "Failed to load offline file information for nodeId=$nodeId")
            }
            .getOrNull()
            ?.let(offlineTypedNodeMapper::invoke)
    }

    private suspend fun loadIsInBackups(nodeId: NodeId, isInRubbish: Boolean): Boolean =
        runCatching {
            if (isInRubbish) {
                isNodeDeletedFromBackupsUseCase(nodeId)
            } else {
                isNodeInBackupsUseCase(nodeId.longValue)
            }
        }.getOrDefault(false)

    private suspend fun buildBottomSheetItems(
        options: Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<*>>,
        node: TypedNode,
        isInRubbish: Boolean,
        permission: AccessPermission?,
        isInBackUps: Boolean,
        isChatNodeFromOthers: Boolean = false,
    ): List<List<NodeActionModeMenuItem>> {
        val effectiveOptions = if (isChatNodeFromOthers) {
            options.filterNot { it is InfoBottomSheetMenuItem }.toSet()
        } else {
            options
        }
        return nodeBottomSheetActionMapper(
            toolbarOptions = effectiveOptions,
            selectedNode = node,
            isNodeInRubbish = isInRubbish,
            accessPermission = permission,
            isInBackUps = isInBackUps,
            isConnected = uiState.value.isOnline,
            nodeSourceType = nodeSourceType,
        )
            .groupBy { it.group }
            .toSortedMap()
            .mapValues { (_, list) -> list.sortedBy { it.orderInGroup } }
            .values
            .toList()
    }

    private fun NodeUiItem<TypedNode>.withPublicLinkPreview(
        source: TypedNode,
    ): NodeUiItem<TypedNode> {
        if (publicLinkUrl.isNullOrBlank()) return this
        val previewPath = (source as? TypedFileNode)?.previewPath ?: return this
        return copy(thumbnailData = ThumbnailUriRequest(UriPath(previewPath)))
    }

    /**
     * Monitor offline node availability and dismiss the bottom sheet when the node is removed.
     */
    private fun monitorOfflineNodeAvailability(nodeId: Long) {
        offlineMonitorJob?.cancel()
        offlineMonitorJob = viewModelScope.launch {
            monitorOfflineNodeUpdatesUseCase()
                .catch { Timber.e(it) }
                .collect { offlineList ->
                    val handle = nodeId.toString()
                    val stillAvailable = offlineList.any { it.handle == handle }
                    if (!stillAvailable) {
                        uiState.update {
                            it.copy(error = triggered(Exception("Offline node removed")))
                        }
                    }
                }
        }
    }

    /**
     * When error consumed
     */
    fun onConsumeErrorState() {
        uiState.update { it.copy(error = consumed()) }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            nodeId: Long,
            nodeSourceType: NodeSourceType,
            partiallyExpand: Boolean,
            @Assisted("chatId") chatId: Long?,
            @Assisted("msgId") msgId: Long?,
            @Assisted("publicLinkUrl") publicLinkUrl: String?,
            @Assisted("localFilePath") localFilePath: String?,
            @Assisted("serializedData") serializedData: String?,
        ): NodeOptionsBottomSheetViewModel
    }
}
