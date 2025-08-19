package mega.privacy.android.core.nodecomponents.sheet.changelabel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.mapper.NodeLabelResourceMapper
import mega.privacy.android.core.nodecomponents.model.label.ChangeLabelState
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.UpdateNodeLabelUseCase
import mega.privacy.android.domain.usecase.node.GetNodeLabelListUseCase
import mega.privacy.android.domain.usecase.node.GetNodeLabelUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for change label
 * @property changeLabelUseCase [mega.privacy.android.core.nodecomponents.model.label.ChangeLabelState]
 * @property getNodeLabelListUseCase [mega.privacy.android.domain.usecase.node.GetNodeLabelListUseCase]
 * @property getNodeLabelUseCase [mega.privacy.android.domain.usecase.node.GetNodeLabelUseCase]
 * @property nodeLabelResourceMapper [mega.privacy.android.core.nodecomponents.mapper.NodeLabelResourceMapper]
 */
@HiltViewModel
class ChangeLabelBottomSheetViewModel @Inject constructor(
    private val changeLabelUseCase: UpdateNodeLabelUseCase,
    private val getNodeLabelListUseCase: GetNodeLabelListUseCase,
    private val getNodeLabelUseCase: GetNodeLabelUseCase,
    private val nodeLabelResourceMapper: NodeLabelResourceMapper,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ChangeLabelState())

    /**
     * Public UI state
     */
    val state: StateFlow<ChangeLabelState> = _state

    /**
     * get label info for a node
     * @param nodeId [NodeId]
     */
    suspend fun loadLabelInfo(nodeId: NodeId) {
        getTypedNode(nodeId)?.let {
            getLabelList(it)
        }
    }

    private fun getLabelList(node: TypedNode) {
        runCatching {
            getNodeLabelListUseCase()
        }.onFailure {
            Timber.e(it)
        }.onSuccess { labelList ->
            val labels = labelList.map {
                nodeLabelResourceMapper(
                    it,
                    getNodeLabelUseCase(node.label)
                )
            }
            _state.update {
                it.copy(
                    labelList = labels,
                    nodeId = node.id
                )
            }
        }
    }

    private suspend fun getTypedNode(nodeId: NodeId) = runCatching {
        getNodeByIdUseCase(nodeId)
    }.onFailure {
        Timber.e(it)
    }.getOrNull()

    /**
     * When label change clicked
     */
    fun onLabelSelected(labelColor: NodeLabel?) {
        state.value.nodeId?.let {
            viewModelScope.launch {
                runCatching {
                    changeLabelUseCase(nodeId = it, label = labelColor)
                }.onFailure {
                    Timber.e("Error changing label $it")
                }
            }
        }
    }
}