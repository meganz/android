package mega.privacy.android.app.presentation.node.label

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.node.model.mapper.NodeLabelResourceMapper
import mega.privacy.android.app.presentation.search.navigation.changeLabelBottomSheetRouteNodeIdArg
import mega.privacy.android.data.mapper.node.label.NodeLabelMapper
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.UpdateNodeLabelUseCase
import mega.privacy.android.domain.usecase.node.GetNodeLabelListUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for change label
 * @property changeLabelUseCase [ChangeLabelState]
 * @property getNodeLabelListUseCase [GetNodeLabelListUseCase]
 * @property nodeLabelMapper [NodeLabelMapper]
 * @property nodeLabelResourceMapper [NodeLabelResourceMapper]
 */
@HiltViewModel
class ChangeLabelBottomSheetViewModel @Inject constructor(
    private val changeLabelUseCase: UpdateNodeLabelUseCase,
    private val getNodeLabelListUseCase: GetNodeLabelListUseCase,
    private val nodeLabelMapper: NodeLabelMapper,
    private val nodeLabelResourceMapper: NodeLabelResourceMapper,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    stateHandle: SavedStateHandle,
) : ViewModel() {

    private val nodeId = stateHandle.get<Long>(changeLabelBottomSheetRouteNodeIdArg)
    private val _state = MutableStateFlow(ChangeLabelState())


    /**
     * Public UI state
     */
    val state: StateFlow<ChangeLabelState> = _state

    /**
     * get label info for a node
     * @param node [Node]
     */
    private fun loadLabelInfo(node: Node) {
        runCatching {
            getNodeLabelListUseCase()
        }.onFailure {
            Timber.e(it)
        }.onSuccess { labelList ->
            val labels = labelList.map {
                nodeLabelResourceMapper(it, nodeLabelMapper(node.label))
            }
            _state.update {
                it.copy(labelList = labels)
            }
        }
    }


    init {
        viewModelScope.launch {
            nodeId?.let {
                runCatching {
                    getNodeByIdUseCase(NodeId(it))
                }.onSuccess {
                    it?.let {
                        loadLabelInfo(node = it)
                    }
                }.onFailure {
                    Timber.e(it, "Error getting node by id")
                }
            }
        }
    }

    /**
     * When label change clicked
     */
    fun onLabelSelected(labelColor: NodeLabel?) {
        nodeId?.let {
            viewModelScope.launch {
                runCatching {
                    changeLabelUseCase(nodeId = NodeId(it), label = labelColor)
                }.onFailure {
                    Timber.e("Error changing label $it")
                }
            }
        }
    }
}