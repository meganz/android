package mega.privacy.android.app.modalbottomsheet.nodelabel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.UpdateNodeLabelUseCase
import javax.inject.Inject

/**
 * UI State for NodeLabelBottomSheetDialogFragment
 */
data class NodeLabelUiState(
    val isLoading: Boolean = false,
    val node: TypedNode? = null,
    val nodes: List<TypedNode> = emptyList(),
    val isMultipleSelection: Boolean = false,
    val hasError: Boolean = false,
    val shouldDismiss: Boolean = false,
) {
    val hasNodes: Boolean get() = if (isMultipleSelection) nodes.isNotEmpty() else node != null
}


/**
 * ViewModel for managing node labeling operations in the NodeLabelBottomSheetDialogFragment.
 * Handles both single and multiple node labeling operations using a single UI state.
 */
@HiltViewModel
class NodeLabelBottomSheetDialogFragmentViewModelV2 @Inject constructor(
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val updateNodeLabelUseCase: UpdateNodeLabelUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NodeLabelUiState())

    /**
     * UI State that contains all the necessary data for the UI
     */
    val uiState: StateFlow<NodeLabelUiState> = _uiState.asStateFlow()

    /**
     * Helper method to set loading state
     */
    private fun setLoadingState() {
        _uiState.value = _uiState.value.copy(isLoading = true, hasError = false)
    }

    /**
     * Helper method to set success state
     */
    private fun setSuccessState() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            shouldDismiss = true
        )
    }

    /**
     * Helper method to set error state
     */
    private fun setErrorState() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            hasError = true,
            shouldDismiss = true
        )
    }

    /**
     * Loads a single node by handle
     *
     * @param nodeHandle The handle of the node to load
     */
    fun loadNode(nodeHandle: Long) {
        viewModelScope.launch {
            try {
                setLoadingState()

                val node = getNodeByIdUseCase(NodeId(nodeHandle))

                if (node != null) {
                    _uiState.value = _uiState.value.copy(
                        node = node,
                        isMultipleSelection = false,
                        isLoading = false
                    )
                } else {
                    // Node not found - show general error
                    setErrorState()
                }
            } catch (_: Exception) {
                // General error - show general error
                setErrorState()
            }
        }
    }

    /**
     * Loads multiple nodes by handles
     *
     * @param nodeHandles Array of node handles to load
     */
    fun loadNodes(nodeHandles: LongArray) {
        viewModelScope.launch {
            try {
                setLoadingState()

                // Load all nodes in parallel for better performance
                val nodes = nodeHandles.map { handle ->
                    async {
                        getNodeByIdUseCase(NodeId(handle))
                    }
                }.awaitAll().filterNotNull()

                _uiState.value = _uiState.value.copy(
                    nodes = nodes,
                    isMultipleSelection = true,
                    isLoading = false
                )
            } catch (_: Exception) {
                // General error - show general error
                setErrorState()
            }
        }
    }

    /**
     * Updates a single node's label
     *
     * @param nodeHandle The handle of the node to update
     * @param label The new label to set, or null to remove the label
     */
    fun updateNodeLabel(
        nodeHandle: Long,
        label: NodeLabel?,
    ) {
        viewModelScope.launch {
            try {
                setLoadingState()

                updateNodeLabelUseCase(NodeId(nodeHandle), label)

                setSuccessState()
            } catch (_: Exception) {
                // General error - show general error
                setErrorState()
            }
        }
    }

    /**
     * Updates multiple nodes' labels
     *
     * @param nodeHandles List of node handles to update
     * @param label The new label to set, or null to remove the label
     */
    fun updateMultipleNodeLabels(
        nodeHandles: List<Long>,
        label: NodeLabel?,
    ) {
        viewModelScope.launch {
            try {
                setLoadingState()

                // Process all node label updates in parallel for better performance
                nodeHandles.map { handle ->
                    async {
                        updateNodeLabelUseCase(NodeId(handle), label)
                    }
                }.awaitAll()

                setSuccessState()
            } catch (_: Exception) {
                // General error - show general error
                setErrorState()
            }
        }
    }

    /**
     * Gets the uniform label if all nodes in the list have the same label
     *
     * @param nodes List of TypedNode objects to check
     * @return The uniform label if all nodes have the same label, otherwise null
     */
    fun getUniformLabel(nodes: List<TypedNode>): NodeLabel? {
        if (nodes.isEmpty()) return null

        val firstLabel = getNodeLabelFromTypedNode(nodes.first()) ?: return null

        return if (nodes.all { getNodeLabelFromTypedNode(it) == firstLabel }) {
            firstLabel
        } else {
            null
        }
    }

    /**
     * Converts TypedNode label to NodeLabel enum
     *
     * @param typedNode The TypedNode to get label from
     * @return The corresponding NodeLabel enum, or null if unknown
     */
    private fun getNodeLabelFromTypedNode(typedNode: TypedNode): NodeLabel? {
        return typedNode.nodeLabel
    }

    /**
     * Checks if a node has any label
     *
     * @param node The TypedNode to check
     * @return true if the node has a label, false otherwise
     */
    fun hasLabel(node: TypedNode): Boolean =
        node.nodeLabel != null
}
