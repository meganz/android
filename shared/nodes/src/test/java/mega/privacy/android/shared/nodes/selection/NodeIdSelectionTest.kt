package mega.privacy.android.shared.nodes.selection

import androidx.compose.runtime.mutableStateOf
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodesLoadingState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeIdSelectionTest {

    private val selectableIds = mutableStateOf<Set<NodeId>>(emptySet())
    private val loadingState = mutableStateOf<NodesLoadingState>(NodesLoadingState.FullyLoaded)

    @BeforeEach
    fun setUp() {
        selectableIds.value = setOf(NodeId(1L), NodeId(2L), NodeId(3L))
        loadingState.value = NodesLoadingState.FullyLoaded
    }

    private fun underTest(state: NodeSelectionState = NodeSelectionState()) =
        NodeIdSelection(state, selectableIds, loadingState)

    @Test
    fun `test that selectedNodeIds and selectedCount include ids outside the selectable set`() {
        val selection = underTest(NodeSelectionState(initialSelectedIds = setOf(NodeId(2L), NodeId(99L))))

        assertThat(selection.selectedNodeIds).containsExactly(NodeId(2L), NodeId(99L))
        assertThat(selection.selectedCount).isEqualTo(2)
    }

    @Test
    fun `test that isAllSelected is true only when every selectable id is selected`() {
        val selection = underTest(NodeSelectionState(initialSelectedIds = setOf(NodeId(1L), NodeId(2L))))
        assertThat(selection.isAllSelected).isFalse()

        selection.state.toggleSelection(NodeId(3L))
        assertThat(selection.isAllSelected).isTrue()
    }

    @Test
    fun `test that isAllSelected ignores selected ids outside the selectable set`() {
        val selection = underTest(NodeSelectionState(initialSelectedIds = setOf(NodeId(1L), NodeId(99L))))

        assertThat(selection.isAllSelected).isFalse()
    }

    @Test
    fun `test that isAllSelected is false when nothing is selectable`() {
        selectableIds.value = emptySet()

        assertThat(underTest(NodeSelectionState(initialSelectedIds = setOf(NodeId(99L)))).isAllSelected).isFalse()
    }

    @Test
    fun `test that selectAll selects the selectable ids and completes when fully loaded`() {
        val selection = underTest()

        selection.selectAll()

        assertThat(selection.selectedNodeIds).containsExactly(NodeId(1L), NodeId(2L), NodeId(3L))
        assertThat(selection.isSelecting).isFalse()
    }

    @Test
    fun `test that selectAll keeps selecting while more pages are loading`() {
        loadingState.value = NodesLoadingState.PartiallyLoaded
        val selection = underTest()

        selection.selectAll()

        assertThat(selection.isSelecting).isTrue()
        assertThat(selection.isInSelectionMode).isTrue()
    }
}
