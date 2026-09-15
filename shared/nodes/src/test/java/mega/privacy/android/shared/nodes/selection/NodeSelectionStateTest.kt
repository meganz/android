package mega.privacy.android.shared.nodes.selection

import androidx.compose.runtime.saveable.SaverScope
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodesLoadingState
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeSelectionStateTest {

    private val saverScope = SaverScope { true }

    private fun saveAndRestore(state: NodeSelectionState): NodeSelectionState? =
        with(NodeSelectionState.Saver) { saverScope.save(state)?.let { restore(it) } }

    @Test
    fun `test that Saver saves and restores selectedNodeIds`() {
        val ids = setOf(NodeId(1L), NodeId(2L), NodeId(3L))
        val state = NodeSelectionState(initialSelectedIds = ids)

        val restored = saveAndRestore(state)

        assertThat(restored?.selectedNodeIds).isEqualTo(ids)
    }

    @Test
    fun `test that Saver saves and restores selectAllAwaitingMoreItems as true`() {
        val state = NodeSelectionState(selectAllInProgress = true)

        val restored = saveAndRestore(state)

        assertThat(restored?.selectAllAwaitingMoreItems).isTrue()
    }

    @Test
    fun `test that Saver restores selectAllAwaitingMoreItems as false when not selecting`() {
        val state = NodeSelectionState(selectAllInProgress = false)

        val restored = saveAndRestore(state)

        assertThat(restored?.selectAllAwaitingMoreItems).isFalse()
    }

    @Test
    fun `test that selectAll without loading state resets selectAllAwaitingMoreItems`() {
        val state = NodeSelectionState(selectAllInProgress = true)
        state.selectAll(setOf(NodeId(1L), NodeId(2L)))
        assertThat(state.selectedNodeIds).containsExactly(NodeId(1L), NodeId(2L))
        assertThat(state.selectAllAwaitingMoreItems).isFalse()
    }

    @Test
    fun `test that selectAll with FullyLoaded resets selectAllAwaitingMoreItems`() {
        val state = NodeSelectionState(selectAllInProgress = true)
        state.selectAll(setOf(NodeId(1L), NodeId(2L)), NodesLoadingState.FullyLoaded)
        assertThat(state.selectedNodeIds).containsExactly(NodeId(1L), NodeId(2L))
        assertThat(state.selectAllAwaitingMoreItems).isFalse()
    }

    @Test
    fun `test that selectAll with PartiallyLoaded keeps selectAllAwaitingMoreItems true`() {
        val state = NodeSelectionState()
        state.selectAll(setOf(NodeId(1L)), NodesLoadingState.PartiallyLoaded)
        assertThat(state.selectedNodeIds).containsExactly(NodeId(1L))
        assertThat(state.selectAllAwaitingMoreItems).isTrue()
    }

    @Test
    fun `test that selectAll with Loading keeps selectAllAwaitingMoreItems true`() {
        val state = NodeSelectionState()
        state.selectAll(setOf(NodeId(1L)), NodesLoadingState.Loading)
        assertThat(state.selectedNodeIds).containsExactly(NodeId(1L))
        assertThat(state.selectAllAwaitingMoreItems).isTrue()
    }

    @Test
    fun `test that selectAll with Failed resets selectAllAwaitingMoreItems`() {
        val state = NodeSelectionState(selectAllInProgress = true)
        state.selectAll(setOf(NodeId(1L)), NodesLoadingState.Failed)
        assertThat(state.selectedNodeIds).containsExactly(NodeId(1L))
        assertThat(state.selectAllAwaitingMoreItems).isFalse()
    }

    @Test
    fun `test that toggleSelection adds an unselected id and removes a selected one`() {
        val state = NodeSelectionState(initialSelectedIds = setOf(NodeId(1L)))

        state.toggleSelection(NodeId(2L))
        assertThat(state.selectedNodeIds).containsExactly(NodeId(1L), NodeId(2L))

        state.toggleSelection(NodeId(1L))
        assertThat(state.selectedNodeIds).containsExactly(NodeId(2L))
    }

    @Test
    fun `test that toggleSelection exits selection mode when the last id is removed`() {
        val state = NodeSelectionState(
            initialSelectedIds = setOf(NodeId(1L)),
            selectAllInProgress = true,
        )

        state.toggleSelection(NodeId(1L))

        assertThat(state.selectedNodeIds).isEmpty()
        assertThat(state.selectAllAwaitingMoreItems).isFalse()
        assertThat(state.isInSelectionMode).isFalse()
    }

    @Test
    fun `test that toggleSelection exposes a new selectedNodeIds instance on every change`() {
        val state = NodeSelectionState(initialSelectedIds = setOf(NodeId(1L)))
        val before = state.selectedNodeIds

        state.toggleSelection(NodeId(2L))

        assertThat(state.selectedNodeIds).isNotSameInstanceAs(before)
        assertThat(before).containsExactly(NodeId(1L))
    }

    @Test
    fun `test that deselectAll clears selection and selectAllAwaitingMoreItems`() {
        val state = NodeSelectionState(
            initialSelectedIds = setOf(NodeId(1L), NodeId(2L)),
            selectAllInProgress = true,
        )

        state.deselectAll()

        assertThat(state.selectedNodeIds).isEmpty()
        assertThat(state.selectAllAwaitingMoreItems).isFalse()
    }
}
