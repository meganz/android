package mega.privacy.android.app.presentation.node.label

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.node.model.mapper.NodeLabelResourceMapper
import mega.privacy.android.data.mapper.node.label.NodeLabelMapper
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.UpdateNodeLabelUseCase
import mega.privacy.android.domain.usecase.node.GetNodeLabelListUseCase
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
) : ViewModel() {

    private val _state = MutableStateFlow(ChangeLabelState())

    /**
     * Public UI state
     */
    val state: StateFlow<ChangeLabelState> = _state

    /**
     * get label info for a node
     * @param node [Node]
     */
    fun getLabelInfo(node: Node) {
        val labelList = getNodeLabelListUseCase().map {
            nodeLabelResourceMapper(it, nodeLabelMapper(node.label))
        }
        _state.update {
            it.copy(labelList = labelList)
        }
    }

    /**
     * When label change clicked
     */
    fun onLabelSelected(nodeId: NodeId, labelColor: NodeLabel?) {
        viewModelScope.launch {
            changeLabelUseCase(nodeId = nodeId, label = labelColor)
        }
    }
}