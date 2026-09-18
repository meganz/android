package mega.privacy.android.shared.nodes.selection

import androidx.compose.runtime.mutableStateOf
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.shared.nodes.components.previewdata.previewFolderNodeUiItem
import mega.privacy.android.shared.nodes.model.TypedNodeItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeSelectionTest {

    private val items = mutableStateOf<List<TypedNodeItem<TypedNode>>>(emptyList())
    private val loadingState = mutableStateOf<NodesLoadingState>(NodesLoadingState.FullyLoaded)

    @BeforeEach
    fun setUp() {
        items.value = (1L..4L).map { previewFolderNodeUiItem(id = it) }
        loadingState.value = NodesLoadingState.FullyLoaded
    }

    private fun underTest(state: NodeSelectionState = NodeSelectionState()) =
        NodeSelection(state, items, loadingState)

    @Test
    fun `test that selectedNodes returns the selected items in item order`() {
        val selection = underTest()

        selection.toggle(NodeId(3L))
        selection.toggle(NodeId(1L))

        assertThat(selection.selectedNodes.map { it.id }).containsExactly(NodeId(1L), NodeId(3L)).inOrder()
    }

    @Test
    fun `test that selectedNodes ignores ids that are not in items while selectedCount counts them`() {
        val selection = underTest(NodeSelectionState(initialSelectedIds = setOf(NodeId(2L), NodeId(99L))))

        assertThat(selection.selectedNodes.map { it.id }).containsExactly(NodeId(2L))
        assertThat(selection.selectedCount).isEqualTo(2)
    }

    @Test
    fun `test that selectedNodes follows a change of items`() {
        val selection = underTest(NodeSelectionState(initialSelectedIds = setOf(NodeId(2L), NodeId(4L))))

        items.value = items.value.filterNot { it.id == NodeId(4L) }

        assertThat(selection.selectedNodes.map { it.id }).containsExactly(NodeId(2L))
    }

    @Test
    fun `test that isAllSelected is false for an empty list`() {
        items.value = emptyList()

        assertThat(underTest().isAllSelected).isFalse()
    }

    @Test
    fun `test that isAllSelected is true only when every item is selected`() {
        val selection = underTest()

        selection.selectAll()
        assertThat(selection.isAllSelected).isTrue()

        selection.toggle(NodeId(2L))
        assertThat(selection.isAllSelected).isFalse()
    }

    @Test
    fun `test that selectAll selects every item and completes when fully loaded`() {
        val selection = underTest()

        selection.selectAll()

        assertThat(selection.state.selectedNodeIds).containsExactly(NodeId(1L), NodeId(2L), NodeId(3L), NodeId(4L))
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

    @Test
    fun `test that deselectAll clears the selection and exits selection mode`() {
        val selection = underTest(NodeSelectionState(initialSelectedIds = setOf(NodeId(1L)), selectAllInProgress = true))

        selection.deselectAll()

        assertThat(selection.selectedNodes).isEmpty()
        assertThat(selection.isInSelectionMode).isFalse()
    }
}
