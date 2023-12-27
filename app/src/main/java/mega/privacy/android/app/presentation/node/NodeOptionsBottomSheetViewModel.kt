package mega.privacy.android.app.presentation.node

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.node.model.NodeBottomSheetState
import mega.privacy.android.app.presentation.node.model.mapper.NodeBottomSheetActionMapper
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.NodeBottomSheetMenuItem
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import javax.inject.Inject

/**
 * Node options bottom sheet view model
 *
 * @property nodeBottomSheetActionMapper
 * @property bottomSheetOptions
 * @property getNodeAccessPermission
 * @property isNodeInRubbish
 * @property isNodeInBackupsUseCase
 */
@HiltViewModel
class NodeOptionsBottomSheetViewModel @Inject constructor(
    private val nodeBottomSheetActionMapper: NodeBottomSheetActionMapper,
    private val bottomSheetOptions: Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>,
    private val getNodeAccessPermission: GetNodeAccessPermission,
    private val isNodeInRubbish: IsNodeInRubbish,
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
) : ViewModel() {

    /**
     * private UI state
     */
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
     * @param nodeUIItem
     * @return state
     */
    fun getBottomSheetOptions(nodeUIItem: NodeUIItem<TypedNode>) = viewModelScope.launch {
        val node = nodeUIItem.node
        val isNodeInRubbish = async { isNodeInRubbish(node.id.longValue) }
        val accessPermission = async { getNodeAccessPermission(node.id) }
        val isInBackUps = async { isNodeInBackupsUseCase(node.id.longValue) }
        val bottomSheetItems = nodeBottomSheetActionMapper(
            toolbarOptions = bottomSheetOptions,
            selectedNode = nodeUIItem.node,
            isNodeInRubbish = isNodeInRubbish.await(),
            accessPermission = accessPermission.await(),
            isInBackUps = isInBackUps.await(),
            isConnected = state.value.isOnline,
        )
        _state.update {
            it.copy(
                name = nodeUIItem.node.name,
                actions = bottomSheetItems
            )
        }
    }
}