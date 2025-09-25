package mega.privacy.android.app.modalbottomsheet.nodelabel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.usecase.UpdateNodeLabelUseCase
import mega.privacy.mobile.analytics.event.LabelAddedMenuItemEvent
import mega.privacy.mobile.analytics.event.LabelRemovedMenuItemEvent
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Sealed class representing the result of loading a single node
 */
sealed class NodeLoadResult {
    data class Success(val node: MegaNode) : NodeLoadResult()
    data class Error(val exception: Throwable) : NodeLoadResult()
    object Loading : NodeLoadResult()
}

/**
 * Sealed class representing the result of loading multiple nodes
 */
sealed class NodesLoadResult {
    data class Success(val nodes: List<MegaNode>) : NodesLoadResult()
    data class Error(val exception: Throwable) : NodesLoadResult()
    object Loading : NodesLoadResult()
}

/**
 * Sealed class representing the result of updating node labels
 */
sealed class LabelUpdateResult {
    object Success : LabelUpdateResult()
    data class Error(val exception: Throwable) : LabelUpdateResult()
    object Loading : LabelUpdateResult()
}

/**
 * ViewModel for managing node labeling operations in the NodeLabelBottomSheetDialogFragment.
 * Handles both single and multiple node labeling operations.
 */
@HiltViewModel
class NodeLabelBottomSheetDialogFragmentViewModel @Inject constructor(
    private val updateNodeLabelUseCase: UpdateNodeLabelUseCase,
) : ViewModel() {

    // LiveData for single node loading
    private val _nodeLoadResult = MutableLiveData<NodeLoadResult>()

    /**
     * LiveData that emits the result of loading a single node.
     *
     * @see NodeLoadResult for possible states (Loading, Success, Error)
     */
    val nodeLoadResult: LiveData<NodeLoadResult> = _nodeLoadResult

    // LiveData for multiple nodes loading
    private val _nodesLoadResult = MutableLiveData<NodesLoadResult>()

    /**
     * LiveData that emits the result of loading multiple nodes.
     *
     * @see NodesLoadResult for possible states (Loading, Success, Error)
     */
    val nodesLoadResult: LiveData<NodesLoadResult> = _nodesLoadResult

    // LiveData for single node label update
    private val _labelUpdateResult = MutableLiveData<LabelUpdateResult>()

    /**
     * LiveData that emits the result of updating a single node's label.
     *
     * @see LabelUpdateResult for possible states (Loading, Success, Error)
     */
    val labelUpdateResult: LiveData<LabelUpdateResult> = _labelUpdateResult

    // LiveData for multiple nodes label update
    private val _multipleLabelsUpdateResult = MutableLiveData<LabelUpdateResult>()

    /**
     * LiveData that emits the result of updating multiple nodes' labels.
     *
     * @see LabelUpdateResult for possible states (Loading, Success, Error)
     */
    val multipleLabelsUpdateResult: LiveData<LabelUpdateResult> = _multipleLabelsUpdateResult


    /**
     * Loads a single node by handle using LiveData (preferred method)
     *
     * @param nodeHandle The handle of the node to load
     * @param megaApi The MegaApi instance to convert UnTypedNode to MegaNode
     * @param dispatcher The dispatcher to use for JNI calls (defaults to IO)
     */
    @JvmOverloads
    fun loadNode(
        nodeHandle: Long,
        megaApi: nz.mega.sdk.MegaApiAndroid,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) {
        _nodeLoadResult.value = NodeLoadResult.Loading

        viewModelScope.launch {
            runCatching {
                withContext(dispatcher) {
                    megaApi.getNodeByHandle(nodeHandle)
                }
            }.onSuccess { node ->
                _nodeLoadResult.value = if (node != null) {
                    NodeLoadResult.Success(node)
                } else {
                    NodeLoadResult.Error(Exception("Node not found for handle: $nodeHandle"))
                }
            }.onFailure { exception ->
                _nodeLoadResult.value = NodeLoadResult.Error(exception)
            }
        }
    }


    /**
     * Loads multiple nodes by handles using LiveData (preferred method)
     *
     * @param nodeHandles Array of node handles to load
     * @param megaApi The MegaApi instance to convert UnTypedNode to MegaNode
     * @param dispatcher The dispatcher to use for JNI calls (defaults to IO)
     */
    @JvmOverloads
    fun loadNodes(
        nodeHandles: LongArray,
        megaApi: nz.mega.sdk.MegaApiAndroid,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) {
        _nodesLoadResult.value = NodesLoadResult.Loading

        viewModelScope.launch {
            runCatching {
                withContext(dispatcher) {
                    nodeHandles.map { nodeHandle ->
                        megaApi.getNodeByHandle(nodeHandle)
                    }.filterNotNull()
                }
            }.onSuccess { nodes ->
                _nodesLoadResult.value = NodesLoadResult.Success(nodes)
            }.onFailure { exception ->
                _nodesLoadResult.value = NodesLoadResult.Error(exception)
            }
        }
    }

    /**
     * Suspending version of updateNodeLabel (internal use and testing)
     *
     * @param nodeId The ID of the node to update
     * @param label The new label to apply, or null to remove the label
     */
    internal suspend fun updateNodeLabelSuspend(nodeId: NodeId, label: NodeLabel?) {
        runCatching {
            updateNodeLabelUseCase(nodeId, label)
        }.onSuccess {
            trackAnalytics(label != null)
        }.onFailure { exception ->
            when (exception) {
                is CancellationException -> {
                    // Don't log cancellation as an error - it's expected when the user navigates away
                    Timber.d("Label update cancelled for node ${nodeId.longValue} - user likely navigated away")
                }

                else -> {
                    Timber.e(exception, "Failed to update node label")
                }
            }
        }
    }


    /**
     * Updates the label for a single node using LiveData (preferred method)
     *
     * @param nodeHandle The handle of the node to update
     * @param label The new label to apply, or null to remove the label
     */
    fun updateNodeLabel(nodeHandle: Long, label: NodeLabel?) {
        _labelUpdateResult.value = LabelUpdateResult.Loading

        viewModelScope.launch {
            runCatching {
                withTimeout(5.seconds) { // 5 second timeout
                    updateNodeLabelSuspend(NodeId(nodeHandle), label)
                }
            }.onSuccess {
                _labelUpdateResult.value = LabelUpdateResult.Success
            }.onFailure { exception ->
                Timber.e(exception, "Failed to update node label for handle: $nodeHandle")
                _labelUpdateResult.value = LabelUpdateResult.Error(exception)
            }
        }
    }

    /**
     * Suspending version of updateMultipleNodeLabels (internal use and testing)
     *
     * @param nodeIds List of node IDs to update
     * @param label The new label to apply, or null to remove the label
     */
    internal suspend fun updateMultipleNodeLabelsSuspend(nodeIds: List<NodeId>, label: NodeLabel?) {
        var successCount = 0
        var failureCount = 0

        nodeIds.forEach { nodeId ->
            runCatching {
                updateNodeLabelUseCase(nodeId, label)
            }.onSuccess {
                successCount++
            }.onFailure { exception ->
                when (exception) {
                    is CancellationException -> {
                        // Don't count cancellation as a failure - it's expected when the user navigates away
                        Timber.d("Label update cancelled for node ${nodeId.longValue} - user likely navigated away")
                    }

                    else -> {
                        failureCount++
                        Timber.e(
                            exception,
                            "Failed to update label for node ${nodeId.longValue}"
                        )
                    }
                }
            }
        }

        // Track analytics if at least one node was updated successfully
        if (successCount > 0) {
            trackAnalytics(label != null)
        }

        // Log summary
        if (failureCount > 0) {
            Timber.w("Label update completed: $successCount successful, $failureCount failed")
        }
    }


    /**
     * Updates the label for multiple nodes using LiveData (preferred method)
     *
     * @param nodeHandles List of node handles to update
     * @param label The new label to apply, or null to remove the label
     */
    fun updateMultipleNodeLabels(nodeHandles: List<Long>, label: NodeLabel?) {
        _multipleLabelsUpdateResult.value = LabelUpdateResult.Loading

        viewModelScope.launch {
            runCatching {
                withTimeout(10.seconds) { // 10 second timeout for multiple operations
                    updateMultipleNodeLabelsSuspend(nodeHandles.map { NodeId(it) }, label)
                }
            }.onSuccess {
                _multipleLabelsUpdateResult.value = LabelUpdateResult.Success
            }.onFailure { exception ->
                Timber.e(
                    exception,
                    "Failed to update multiple node labels for handles: $nodeHandles"
                )
                _multipleLabelsUpdateResult.value = LabelUpdateResult.Error(exception)
            }
        }
    }


    /**
     * Converts MegaNode label integer to NodeLabel enum
     *
     * @param labelInt The integer label from MegaNode
     * @return The corresponding NodeLabel enum, or null if no label
     */
    private fun getNodeLabelFromInt(labelInt: Int): NodeLabel? {
        return when (labelInt) {
            MegaNode.NODE_LBL_RED -> NodeLabel.RED
            MegaNode.NODE_LBL_ORANGE -> NodeLabel.ORANGE
            MegaNode.NODE_LBL_YELLOW -> NodeLabel.YELLOW
            MegaNode.NODE_LBL_GREEN -> NodeLabel.GREEN
            MegaNode.NODE_LBL_BLUE -> NodeLabel.BLUE
            MegaNode.NODE_LBL_PURPLE -> NodeLabel.PURPLE
            MegaNode.NODE_LBL_GREY -> NodeLabel.GREY
            else -> null
        }
    }

    /**
     * Converts NodeLabel enum to MegaNode label integer
     *
     * @param nodeLabel The NodeLabel enum
     * @return The corresponding integer label for MegaNode
     */
    fun getIntFromNodeLabel(nodeLabel: NodeLabel): Int {
        return when (nodeLabel) {
            NodeLabel.RED -> MegaNode.NODE_LBL_RED
            NodeLabel.ORANGE -> MegaNode.NODE_LBL_ORANGE
            NodeLabel.YELLOW -> MegaNode.NODE_LBL_YELLOW
            NodeLabel.GREEN -> MegaNode.NODE_LBL_GREEN
            NodeLabel.BLUE -> MegaNode.NODE_LBL_BLUE
            NodeLabel.PURPLE -> MegaNode.NODE_LBL_PURPLE
            NodeLabel.GREY -> MegaNode.NODE_LBL_GREY
        }
    }

    /**
     * Checks if a node has any label
     *
     * @param node The UnTypedNode to check
     * @return true if the node has a label, false otherwise
     */
    @JvmName("hasLabelUnTypedNode")
    fun hasLabel(node: UnTypedNode): Boolean =
        node.nodeLabel != null

    /**
     * Checks if a node has any label (MegaNode compatibility)
     *
     * @param node The MegaNode to check
     * @return true if the node has a label, false otherwise
     */
    @JvmName("hasLabelMegaNode")
    fun hasLabel(node: MegaNode): Boolean =
        node.label != MegaNode.NODE_LBL_UNKNOWN && node.label > 0

    /**
     * Gets the uniform label if all nodes in the list have the same label
     *
     * @param nodes List of UnTypedNode objects to check
     * @return The uniform label if all nodes have the same label, otherwise null
     */
    @JvmName("getUniformLabelFromUnTypedNodes")
    fun getUniformLabel(nodes: List<UnTypedNode>): NodeLabel? {
        if (nodes.isEmpty()) return null

        val firstLabel = nodes.first().nodeLabel ?: return null

        return if (nodes.all { it.nodeLabel == firstLabel }) {
            firstLabel
        } else {
            null
        }
    }

    /**
     * Gets the uniform label if all nodes in the list have the same label (MegaNode compatibility)
     *
     * @param nodes List of MegaNode objects to check
     * @return The uniform label if all nodes have the same label, otherwise null
     */
    @JvmName("getUniformLabelFromMegaNodes")
    fun getUniformLabel(nodes: List<MegaNode>): NodeLabel? {
        if (nodes.isEmpty()) return null

        val firstLabel = getNodeLabelFromInt(nodes.first().label) ?: return null

        return if (nodes.all { getNodeLabelFromInt(it.label) == firstLabel }) {
            firstLabel
        } else {
            null
        }
    }

    /**
     * Tracks analytics for label operations
     *
     * @param isLabelAdded true if a label was added, false if removed
     */
    private fun trackAnalytics(isLabelAdded: Boolean) {
        Analytics.tracker.trackEvent(
            if (isLabelAdded) {
                LabelAddedMenuItemEvent
            } else {
                LabelRemovedMenuItemEvent
            }
        )
    }
}

