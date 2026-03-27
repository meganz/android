package mega.privacy.android.feature.clouddrive.presentation.clouddrive.selection

import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.shared.account.overquota.OverQuotaStatus
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveUiState
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.computeSelectedItemsCount
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeUiItem
import mega.privacy.android.shared.nodes.selection.NodeSelectionState
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class NodeSelectionStateTest {

    @Test
    fun `test that toggleSelection adds a node when not selected`() {
        val state = NodeSelectionState()
        state.toggleSelection(NodeId(1L))
        assertThat(state.selectedNodeIds).containsExactly(NodeId(1L))
    }

    @Test
    fun `test that toggleSelection removes a node when already selected`() {
        val state = NodeSelectionState(initialSelectedIds = setOf(NodeId(1L)))
        state.toggleSelection(NodeId(1L))
        assertThat(state.selectedNodeIds).isEmpty()
    }

    @Test
    fun `test that toggleSelection resets isSelecting when all deselected`() {
        val state = NodeSelectionState(
            initialSelectedIds = setOf(NodeId(1L)),
            selectAllInProgress = true
        )
        state.toggleSelection(NodeId(1L))
        assertThat(state.selectAllAwaitingMoreItems).isFalse()
    }

    @Test
    fun `test that selectAll sets all node IDs and resets isSelecting`() {
        val state = NodeSelectionState()
        state.selectAll(setOf(NodeId(1L), NodeId(2L), NodeId(3L)), NodesLoadingState.FullyLoaded)
        assertThat(state.selectedNodeIds).containsExactly(NodeId(1L), NodeId(2L), NodeId(3L))
        assertThat(state.selectAllAwaitingMoreItems).isFalse()
    }

    @Test
    fun `test that selectAll during partial load then fully loaded completes selection`() {
        val state = NodeSelectionState()
        // Simulate select-all while partially loaded
        state.selectAll(setOf(NodeId(1L)), NodesLoadingState.PartiallyLoaded)
        assertThat(state.selectAllAwaitingMoreItems).isTrue()

        // Simulate fully loaded with more items
        state.selectAll(setOf(NodeId(1L), NodeId(2L), NodeId(3L)), NodesLoadingState.FullyLoaded)
        assertThat(state.selectedNodeIds).containsExactly(NodeId(1L), NodeId(2L), NodeId(3L))
        assertThat(state.selectAllAwaitingMoreItems).isFalse()
    }

    @Test
    fun `test that deselectAll clears selection and resets isSelecting`() {
        val state = NodeSelectionState(
            initialSelectedIds = setOf(NodeId(1L), NodeId(2L)),
            selectAllInProgress = true
        )
        state.deselectAll()
        assertThat(state.selectedNodeIds).isEmpty()
        assertThat(state.selectAllAwaitingMoreItems).isFalse()
    }

    @Test
    fun `test that computeSelectedItemsCount counts all selected when hidden nodes shown`() {
        val items = listOf(
            createNodeUiItem(NodeId(1L), isSensitive = false),
            createNodeUiItem(NodeId(2L), isSensitive = true),
            createNodeUiItem(NodeId(3L), isSensitive = false),
        )
        val selectedIds = setOf(NodeId(1L), NodeId(2L), NodeId(3L))
        val showHiddenNodes = true
        val isHiddenNodesEnabled = true

        val uiState = createState(items, showHiddenNodes, isHiddenNodesEnabled)

        val count = uiState.computeSelectedItemsCount(
            selectedIds = selectedIds,
        )

        assertThat(count).isEqualTo(3)
    }

    @Test
    fun `test that computeSelectedItemsCount excludes sensitive items when hidden nodes not shown`() {
        val items = listOf(
            createNodeUiItem(NodeId(1L), isSensitive = false),
            createNodeUiItem(NodeId(2L), isSensitive = true),
            createNodeUiItem(NodeId(3L), isSensitive = false),
        )
        val selectedIds = setOf(NodeId(1L), NodeId(2L), NodeId(3L))
        val showHiddenNodes = false
        val isHiddenNodesEnabled = true

        val uiState = createState(items, showHiddenNodes, isHiddenNodesEnabled)
        val count = uiState.computeSelectedItemsCount(
            selectedIds = selectedIds,
        )

        assertThat(count).isEqualTo(2)
    }

    @Test
    fun `test that computeSelectedItemsCount counts all when hidden nodes feature disabled`() {
        val items = listOf(
            createNodeUiItem(NodeId(1L), isSensitive = false),
            createNodeUiItem(NodeId(2L), isSensitive = true),
        )
        val selectedIds = setOf(NodeId(1L), NodeId(2L))
        val showHiddenNodes = false
        val isHiddenNodesEnabled = false

        val uiState = createState(items, showHiddenNodes, isHiddenNodesEnabled)
        val count = uiState.computeSelectedItemsCount(
            selectedIds = selectedIds,
        )

        assertThat(count).isEqualTo(2)
    }

    @Test
    fun `test that computeSelectedItemsCount returns zero when no items selected`() {
        val items = listOf(
            createNodeUiItem(NodeId(1L), isSensitive = false),
        )
        val showHiddenNodes = true
        val isHiddenNodesEnabled = false

        val uiState = createState(items, showHiddenNodes, isHiddenNodesEnabled)
        val count = uiState.computeSelectedItemsCount(
            selectedIds = emptySet(),
        )

        assertThat(count).isEqualTo(0)
    }

    private fun createNodeUiItem(
        nodeId: NodeId,
        isSensitive: Boolean,
    ): NodeUiItem<TypedNode> {
        val typedNode = mock<TypedNode> {
            on { id } doReturn nodeId
        }
        return NodeUiItem(
            node = typedNode,
            isSelected = false,
            isSensitive = isSensitive,
        )
    }

    private fun createState(
        items: List<NodeUiItem<TypedNode>>,
        showHiddenNodes: Boolean,
        isHiddenNodesEnabled: Boolean,
    ): CloudDriveUiState = CloudDriveUiState(
        title = LocalizedText.Literal(""),
        nodesLoadingState = NodesLoadingState.Loading,
        isHiddenNodeSettingsLoading = true,
        currentFolderId = NodeId(-1L),
        isCloudDriveRoot = false,
        items = items,
        currentViewType = ViewType.LIST,
        navigateToFolderEvent = consumed(),
        navigateBack = consumed,
        openedFileNode = null,
        showHiddenNodes = showHiddenNodes,
        isHiddenNodesEnabled = isHiddenNodesEnabled,
        hasMediaItems = false,
        selectedSortOrder = SortOrder.ORDER_DEFAULT_ASC,
        selectedSortConfiguration = NodeSortConfiguration.default,
        overQuotaStatus = OverQuotaStatus(),
        shouldShowWarning = true,
        isContactVerificationOn = false,
        showContactNotVerifiedBanner = false,
        nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        hasWritePermission = false,
        isSearchRevampEnabled = false,
    )
}
