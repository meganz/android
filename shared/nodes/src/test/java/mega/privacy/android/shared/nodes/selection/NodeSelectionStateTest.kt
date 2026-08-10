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

    @Test
    fun `test that Saver saves and restores selectedNodeIds`() {
        val ids = setOf(NodeId(1L), NodeId(2L), NodeId(3L))
        val state = NodeSelectionState(initialSelectedIds = ids)

        val saved = with(NodeSelectionState.Saver) { saverScope.save(state) }
        val restored = saved?.let { NodeSelectionState.Saver.restore(it) }

        assertThat(restored?.selectedNodeIds).isEqualTo(ids)
    }

    @Test
    fun `test that Saver saves and restores selectAllAwaitingMoreItems as true`() {
        val state = NodeSelectionState(selectAllInProgress = true)

        val saved = with(NodeSelectionState.Saver) { saverScope.save(state) }
        val restored = saved?.let { NodeSelectionState.Saver.restore(it) }

        assertThat(restored?.selectAllAwaitingMoreItems).isTrue()
    }

    @Test
    fun `test that Saver restores selectAllAwaitingMoreItems as false when not selecting`() {
        val state = NodeSelectionState(selectAllInProgress = false)

        val saved = with(NodeSelectionState.Saver) { saverScope.save(state) }
        val restored = saved?.let { NodeSelectionState.Saver.restore(it) }

        assertThat(restored?.selectAllAwaitingMoreItems).isFalse()
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
}
