package mega.privacy.android.app.modalbottomsheet.nodelabel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
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

/**
 * ViewModel for managing node labeling operations in the NodeLabelBottomSheetDialogFragment.
 * Handles both single and multiple node labeling operations.
 */
@HiltViewModel
class NodeLabelBottomSheetDialogFragmentViewModel @Inject constructor(
    private val updateNodeLabelUseCase: UpdateNodeLabelUseCase,
) : ViewModel() {

    /**
     * Loads a single node by handle and converts it to MegaNode with callback (for Java compatibility)
     *
     * @param nodeHandle The handle of the node to load
     * @param megaApi The MegaApi instance to convert UnTypedNode to MegaNode
     * @param callback Callback to receive the MegaNode result
     */
    fun loadMegaNode(
        nodeHandle: Long,
        megaApi: nz.mega.sdk.MegaApiAndroid,
        callback: (MegaNode?) -> Unit,
    ) {
        viewModelScope.launch {
            callback(megaApi.getNodeByHandle(nodeHandle))
        }
    }

    /**
     * Loads multiple nodes by handles and converts them to MegaNodes with callback (for Java compatibility)
     *
     * @param nodeHandles Array of node handles to load
     * @param megaApi The MegaApi instance to convert UnTypedNode to MegaNode
     * @param callback Callback to receive the MegaNode results
     */
    fun loadMegaNodes(
        nodeHandles: LongArray,
        megaApi: nz.mega.sdk.MegaApiAndroid,
        callback: (List<MegaNode>) -> Unit,
    ) {
        viewModelScope.launch {
            val megaNodes = nodeHandles.map { nodeHandle ->
                megaApi.getNodeByHandle(nodeHandle)
            }.filterNotNull()
            callback(megaNodes)
        }
    }

    /**
     * Updates the label for a single node
     *
     * @param nodeId The ID of the node to update
     * @param label The new label to apply, or null to remove the label
     */
    fun updateNodeLabel(nodeId: NodeId, label: NodeLabel?) {
        viewModelScope.launch {
            runCatching {
                updateNodeLabelUseCase(nodeId, label)
            }.onSuccess {
                trackAnalytics(label != null)
            }.onFailure { exception ->
                Timber.e(exception, "Failed to update node label")
            }
        }
    }

    /**
     * Updates the label for a single node (Java-compatible version)
     *
     * @param nodeHandle The handle of the node to update
     * @param label The new label to apply, or null to remove the label
     */
    @JvmName("updateNodeLabelByHandle")
    fun updateNodeLabel(nodeHandle: Long, label: NodeLabel?) {
        updateNodeLabel(NodeId(nodeHandle), label)
    }

    /**
     * Updates the label for multiple nodes
     *
     * @param nodeIds List of node IDs to update
     * @param label The new label to apply, or null to remove the label
     */
    fun updateMultipleNodeLabels(nodeIds: List<NodeId>, label: NodeLabel?) {
        viewModelScope.launch {
            runCatching {
                nodeIds.forEach { nodeId ->
                    updateNodeLabelUseCase(nodeId, label)
                }
            }.onSuccess {
                trackAnalytics(label != null)
            }.onFailure { exception ->
                Timber.e(exception, "Failed to update multiple node labels")
            }
        }
    }

    /**
     * Updates the label for multiple nodes (Java-compatible version)
     *
     * @param nodeHandles List of node handles to update
     * @param label The new label to apply, or null to remove the label
     */
    @JvmName("updateMultipleNodeLabelsByHandles")
    fun updateMultipleNodeLabels(nodeHandles: List<Long>, label: NodeLabel?) {
        updateMultipleNodeLabels(nodeHandles.map { NodeId(it) }, label)
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
        node.label != MegaNode.NODE_LBL_UNKNOWN && node.label > 0

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

        val firstLabel = getNodeLabelFromInt(nodes.first().label) ?: return null

        return if (nodes.all { getNodeLabelFromInt(it.label) == firstLabel }) {
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