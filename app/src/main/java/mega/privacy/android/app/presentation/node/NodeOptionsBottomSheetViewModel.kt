package mega.privacy.android.app.presentation.node

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
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.Backups
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.CloudDrive
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.IncomingShares
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.Links
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.OutgoingShares
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.RubbishBin
import mega.privacy.android.app.presentation.extensions.isOutShare
import mega.privacy.android.app.presentation.node.model.NodeBottomSheetState
import mega.privacy.android.app.presentation.node.model.mapper.NodeAccessPermissionIconMapper
import mega.privacy.android.app.presentation.node.model.mapper.NodeBottomSheetActionMapper
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.NodeBottomSheetMenuItem
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromEmailUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.domain.usecase.shares.DefaultGetContactItemFromInShareFolder
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.domain.usecase.shares.GetOutShareByNodeIdUseCase
import mega.android.core.ui.model.menu.MenuActionWithIcon
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
    private val nodeAccessPermissionIconMapper: NodeAccessPermissionIconMapper,
    private val getContactItemFromInShareFolder: DefaultGetContactItemFromInShareFolder,
    private val getOutShareByNodeIdUseCase: GetOutShareByNodeIdUseCase,
    private val getContactFromEmailUseCase: GetContactFromEmailUseCase,
    @CloudDrive private val cloudDriveBottomSheetOptions: Lazy<Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>>,
    @RubbishBin private val rubbishBinBottomSheetOptions: Lazy<Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>>,
    @IncomingShares private val incomingSharesBottomSheetOptions: Lazy<Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>>,
    @OutgoingShares private val outgoingSharesBottomSheetOptions: Lazy<Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>>,
    @Links private val linksBottomSheetOptions: Lazy<Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>>,
    @Backups private val backupsBottomSheetOptions: Lazy<Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>>,
) : ViewModel() {

    private val _state = MutableStateFlow(NodeBottomSheetState())

    /**
     * public UI State
     */
    val state: StateFlow<NodeBottomSheetState> = _state

    init {
        viewModelScope.launch {
            monitorConnectivityUseCase().collect { isConnected ->
                _state.update {
                    it.copy(isOnline = isConnected)
                }
            }
        }
    }

    /**
     * Get bottom sheet options
     *
     * @param nodeId [TypedNode]
     * @return state
     */
    fun getBottomSheetOptions(nodeId: Long, nodeSourceType: NodeSourceType) {
        viewModelScope.launch {
            val bottomSheetOptions = getOptionsForSourceType(nodeSourceType)

            _state.update {
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
                    isConnected = state.value.isOnline
                ).groupBy { it.group }
                    .toSortedMap()
                    .mapValues { (_, list) ->
                        list.sortedBy { it.orderInGroup }.toImmutableList()
                    }
                    .values.toImmutableList()
                val accessPermissionIcon =
                    getAccessPermissionIcon(permission ?: AccessPermission.UNKNOWN, typedNode)
                _state.update {
                    it.copy(
                        name = typedNode.name,
                        actions = bottomSheetItems,
                        node = typedNode,
                        error = if (bottomSheetItems.isEmpty()) triggered(Exception("No actions available")) else consumed(),
                        accessPermissionIcon = accessPermissionIcon,
                    )
                }
                getShareInfo(typedNode)
            } ?: run {
                _state.update {
                    it.copy(error = triggered(Exception("Node is null")))
                }
            }
        }
    }

    private fun getShareInfo(typedNode: TypedNode) {
        if (typedNode.isIncomingShare && typedNode is TypedFolderNode) {
            viewModelScope.launch {
                runCatching {
                    getContactItemFromInShareFolder(typedNode, true)
                }.onSuccess { contact ->
                    _state.update {
                        it.copy(
                            shareInfo = contact?.contactData?.fullName ?: contact?.email
                        )
                    }
                }.onFailure {
                    Timber.e(it)
                }
            }
        }
        if (typedNode.isOutShare()) {
            viewModelScope.launch {
                runCatching {
                    getOutShareByNodeIdUseCase(typedNode.id)
                }.onSuccess { outShares ->
                    if (outShares.size == 1) {
                        outShares.first().user?.let { getShareUserInfo(it) }
                    } else {
                        _state.update {
                            it.copy(outgoingShares = outShares)
                        }
                    }
                }.onFailure {
                    Timber.e(it)
                }
            }
        }
    }

    private fun getShareUserInfo(user: String) = viewModelScope.launch {
        runCatching {
            getContactFromEmailUseCase(user, true)
        }.onSuccess { contact ->
            _state.update {
                it.copy(
                    shareInfo = contact?.contactData?.fullName ?: contact?.email
                )
            }
        }.onFailure {
            Timber.e(it)
        }
    }


    /**
     * Get access permission icon
     * Access permission icon is only shown for incoming shares
     *
     * @return icon
     */
    private fun getAccessPermissionIcon(accessPermission: AccessPermission, node: TypedNode): Int? =
        nodeAccessPermissionIconMapper(accessPermission).takeIf { node.isIncomingShare }

    /**
     * When error consumed
     */
    fun onConsumeErrorState() {
        _state.update { it.copy(error = consumed()) }
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
