package mega.privacy.android.core.nodecomponents.sheet.options

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.android.core.ui.model.SnackbarAttributes
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.mapper.NodeBottomSheetActionMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeBottomSheetState
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.qualifier.features.Backups
import mega.privacy.android.domain.qualifier.features.CloudDrive
import mega.privacy.android.domain.qualifier.features.IncomingShares
import mega.privacy.android.domain.qualifier.features.Links
import mega.privacy.android.domain.qualifier.features.OutgoingShares
import mega.privacy.android.domain.qualifier.features.RubbishBin
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import timber.log.Timber
import javax.inject.Inject

/**
 * Node options bottom sheet view model
 *
 * @property nodeBottomSheetActionMapper
 * @property getNodeAccessPermission
 * @property isNodeInRubbishBinUseCase
 * @property isNodeInBackupsUseCase
 * @property monitorConnectivityUseCase
 * @property getNodeByIdUseCase
 */
@HiltViewModel
class NodeOptionsBottomSheetViewModel @Inject constructor(
    private val nodeBottomSheetActionMapper: NodeBottomSheetActionMapper,
    private val getNodeAccessPermission: GetNodeAccessPermission,
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase,
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val nodeUiItemMapper: NodeUiItemMapper,
    private val snackbarEventQueue: SnackbarEventQueue,
    @CloudDrive private val cloudDriveBottomSheetOptions: Lazy<Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>>,
    @RubbishBin private val rubbishBinBottomSheetOptions: Lazy<Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>>,
    @IncomingShares private val incomingSharesBottomSheetOptions: Lazy<Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>>,
    @OutgoingShares private val outgoingSharesBottomSheetOptions: Lazy<Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>>,
    @Links private val linksBottomSheetOptions: Lazy<Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>>,
    @Backups private val backupsBottomSheetOptions: Lazy<Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>>,
) : ViewModel() {

    internal val uiState: StateFlow<NodeBottomSheetState>
        field = MutableStateFlow(NodeBottomSheetState())

    init {
        viewModelScope.launch {
            monitorConnectivityUseCase().collect { isConnected ->
                uiState.update {
                    it.copy(isOnline = isConnected)
                }
            }
        }
    }

    suspend fun showSnackbar(attributes: SnackbarAttributes) =
        snackbarEventQueue.queueMessage(attributes)

    /**
     * Get bottom sheet options
     *
     * @param nodeId [mega.privacy.android.domain.entity.node.TypedNode]
     * @return state
     */
    fun getBottomSheetOptions(nodeId: Long, nodeSourceType: NodeSourceType) {
        viewModelScope.launch {
            val bottomSheetOptions = getOptionsForSourceType(nodeSourceType)

            uiState.update {
                it.copy(
                    actions = persistentListOf(),
                    node = null
                )
            }
            val node = async { runCatching { getNodeByIdUseCase(NodeId(nodeId)) }.getOrNull() }
            val isNodeInRubbish =
                async { runCatching { isNodeInRubbishBinUseCase(NodeId(nodeId)) }.getOrDefault(false) }
            val accessPermission =
                async { runCatching { getNodeAccessPermission(NodeId(nodeId)) }.getOrNull() }
            val isInBackUps =
                async { runCatching { isNodeInBackupsUseCase(nodeId) }.getOrDefault(false) }
            val typedNode = node.await()
            val permission = accessPermission.await()
            typedNode?.let {
                val bottomSheetItems = nodeBottomSheetActionMapper(
                    toolbarOptions = bottomSheetOptions,
                    selectedNode = typedNode,
                    isNodeInRubbish = isNodeInRubbish.await(),
                    accessPermission = permission,
                    isInBackUps = isInBackUps.await(),
                    isConnected = uiState.value.isOnline
                ).sortedBy { it.orderInGroup }
                    .toImmutableList()
                val nodeUiItem = nodeUiItemMapper(listOf(typedNode)).firstOrNull()

                uiState.update {
                    it.copy(
                        actions = bottomSheetItems,
                        node = nodeUiItem,
                        error = if (bottomSheetItems.isEmpty()) triggered(Exception("No actions available")) else consumed(),
                    )
                }
            } ?: run {
                uiState.update {
                    it.copy(error = triggered(Exception("Node is null")))
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

    private fun getOptionsForSourceType(nodeSourceType: NodeSourceType): Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<*>> =
        when (nodeSourceType) {
            NodeSourceType.INCOMING_SHARES -> incomingSharesBottomSheetOptions
            NodeSourceType.OUTGOING_SHARES -> outgoingSharesBottomSheetOptions
            NodeSourceType.LINKS -> linksBottomSheetOptions
            NodeSourceType.RUBBISH_BIN -> rubbishBinBottomSheetOptions
            NodeSourceType.BACKUPS -> backupsBottomSheetOptions
            else -> cloudDriveBottomSheetOptions
        }.get()
}