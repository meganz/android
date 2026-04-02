package mega.privacy.android.feature.clouddrive.presentation.clouddrive.selection

import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.shared.nodes.selection.NodeSelectionState
import org.junit.Test

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
}
