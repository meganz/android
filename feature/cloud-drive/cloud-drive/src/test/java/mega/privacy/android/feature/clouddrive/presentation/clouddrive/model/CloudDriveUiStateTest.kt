package mega.privacy.android.feature.clouddrive.presentation.clouddrive.model

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class CloudDriveUiStateTest {

    @Test
    fun `test that visibleItemsCount returns total count when all items are visible`() {
        val items = createTestItems(
            selected = listOf(false, false, false),
            sensitive = listOf(false, false, false)
        )

        val state = CloudDriveUiState(
            items = items,
            showHiddenNodes = true,
            isHiddenNodesEnabled = true
        )

        assertThat(state.visibleItemsCount).isEqualTo(3)
    }

    @Test
    fun `test that visibleItemsCount returns total count when hidden nodes disabled`() {
        val items = createTestItems(
            selected = listOf(false, false, false),
            sensitive = listOf(true, false, true)
        )

        val state = CloudDriveUiState(
            items = items,
            showHiddenNodes = false,
            isHiddenNodesEnabled = false
        )

        assertThat(state.visibleItemsCount).isEqualTo(3)
    }

    @Test
    fun `test that visibleItemsCount filters out sensitive items when hidden nodes enabled`() {
        val items = createTestItems(
            selected = listOf(false, false, false),
            sensitive = listOf(true, false, true)
        )

        val state = CloudDriveUiState(
            items = items,
            showHiddenNodes = false,
            isHiddenNodesEnabled = true
        )

        assertThat(state.visibleItemsCount).isEqualTo(1)
    }

    @Test
    fun `test that selectedItemsCount returns correct count when all items are visible`() {
        val items = createTestItems(
            selected = listOf(true, false, true),
            sensitive = listOf(false, false, false)
        )

        val state = CloudDriveUiState(
            items = items,
            showHiddenNodes = true,
            isHiddenNodesEnabled = true
        )

        assertThat(state.selectedItemsCount).isEqualTo(2)
    }

    @Test
    fun `test that selectedItemsCount returns zero when all selected items are sensitive`() {
        val items = createTestItems(
            selected = listOf(true, false, true),
            sensitive = listOf(true, false, true)
        )

        val state = CloudDriveUiState(
            items = items,
            showHiddenNodes = false,
            isHiddenNodesEnabled = true
        )

        assertThat(state.selectedItemsCount).isEqualTo(0)
    }

    @Test
    fun `test that selectedItemsCount returns correct count with mixed sensitive and selected items`() {
        val items = createTestItems(
            selected = listOf(true, true, false, true),
            sensitive = listOf(false, true, false, true)
        )

        val state = CloudDriveUiState(
            items = items,
            showHiddenNodes = false,
            isHiddenNodesEnabled = true
        )

        assertThat(state.selectedItemsCount).isEqualTo(1)
    }

    @Test
    fun `test that isInSelectionMode returns false when no items are selected`() {
        val items = createTestItems(
            selected = listOf(false, false, false),
            sensitive = listOf(false, false, false)
        )

        val state = CloudDriveUiState(
            items = items,
            showHiddenNodes = false,
            isHiddenNodesEnabled = true
        )

        assertThat(state.isInSelectionMode).isFalse()
    }

    @Test
    fun `test that isInSelectionMode returns true when visible items are selected`() {
        val items = createTestItems(
            selected = listOf(true, false, false),
            sensitive = listOf(false, false, false)
        )

        val state = CloudDriveUiState(
            items = items,
            showHiddenNodes = false,
            isHiddenNodesEnabled = true
        )

        assertThat(state.isInSelectionMode).isTrue()
    }

    @Test
    fun `test that isInSelectionMode returns false when only sensitive items are selected`() {
        val items = createTestItems(
            selected = listOf(true, false, false),
            sensitive = listOf(true, false, false)
        )

        val state = CloudDriveUiState(
            items = items,
            showHiddenNodes = false,
            isHiddenNodesEnabled = true
        )

        assertThat(state.isInSelectionMode).isFalse()
    }

    @Test
    fun `test that isEmpty returns true when no visible items and not loading`() {
        val items = createTestItems(
            selected = listOf(),
            sensitive = listOf()
        )

        val state = CloudDriveUiState(
            items = items,
            isLoading = false,
            showHiddenNodes = false,
            isHiddenNodesEnabled = true
        )

        assertThat(state.isEmpty).isTrue()
    }

    @Test
    fun `test that isEmpty returns false when no visible items but loading`() {
        val items = createTestItems(
            selected = listOf(),
            sensitive = listOf()
        )

        val state = CloudDriveUiState(
            items = items,
            isLoading = true,
            showHiddenNodes = false,
            isHiddenNodesEnabled = true
        )

        assertThat(state.isEmpty).isFalse()
    }

    @Test
    fun `test that isEmpty returns false when has visible items`() {
        val items = createTestItems(
            selected = listOf(false),
            sensitive = listOf(false)
        )

        val state = CloudDriveUiState(
            items = items,
            isLoading = false,
            showHiddenNodes = false,
            isHiddenNodesEnabled = true
        )

        assertThat(state.isEmpty).isFalse()
    }

    @Test
    fun `test that selectedNodeIds returns correct node ids for selected items`() {
        val items = createTestItems(
            selected = listOf(true, false, true),
            sensitive = listOf(false, false, false)
        )

        val state = CloudDriveUiState(items = items)

        assertThat(state.selectedNodeIds).isEqualTo(listOf(NodeId(1L), NodeId(3L)))
    }

    @Test
    fun `test that selectedNodeIds returns empty list when no items are selected`() {
        val items = createTestItems(
            selected = listOf(false, false, false),
            sensitive = listOf(false, false, false)
        )

        val state = CloudDriveUiState(items = items)

        assertThat(state.selectedNodeIds).isEmpty()
    }

    @Test
    fun `test that selectedNodeIds includes sensitive items when they are selected`() {
        val items = createTestItems(
            selected = listOf(true, true, false),
            sensitive = listOf(true, false, false)
        )

        val state = CloudDriveUiState(items = items)

        assertThat(state.selectedNodeIds).isEqualTo(listOf(NodeId(1L), NodeId(2L)))
    }

    @Test
    fun `test that complex scenario with mixed states works correctly`() {
        val items = createTestItems(
            selected = listOf(true, false, true, false, true),
            sensitive = listOf(false, true, false, true, false)
        )

        val state = CloudDriveUiState(
            items = items,
            isLoading = false,
            showHiddenNodes = false,
            isHiddenNodesEnabled = true
        )

        // Only non-sensitive items are visible (items 0, 2, 4)
        assertThat(state.visibleItemsCount).isEqualTo(3)
        // Only selected non-sensitive items (items 0, 2, 4 are selected and non-sensitive)
        assertThat(state.selectedItemsCount).isEqualTo(3)
        assertThat(state.isInSelectionMode).isTrue()
        assertThat(state.isEmpty).isFalse()
        assertThat(state.selectedNodeIds).isEqualTo(listOf(NodeId(1L), NodeId(3L), NodeId(5L)))
    }

    @Test
    fun `test that edge case with empty items list works correctly`() {
        val state = CloudDriveUiState(
            items = emptyList(),
            isLoading = false,
            showHiddenNodes = false,
            isHiddenNodesEnabled = true
        )

        assertThat(state.visibleItemsCount).isEqualTo(0)
        assertThat(state.selectedItemsCount).isEqualTo(0)
        assertThat(state.isInSelectionMode).isFalse()
        assertThat(state.isEmpty).isTrue()
        assertThat(state.selectedNodeIds).isEmpty()
    }


    @Test
    fun `test that isRootCloudDrive is true when current folder handle is -1L`() {
        val state = CloudDriveUiState(
            currentFolderId = NodeId(-1L),
        )

        assertThat(state.isRootCloudDrive).isTrue()
    }

    private fun createTestItems(
        selected: List<Boolean>,
        sensitive: List<Boolean>,
    ): List<NodeUiItem<TypedNode>> {
        require(selected.size == sensitive.size) { "Lists must have same size" }

        return selected.zip(sensitive).mapIndexed { index, (isSelected, isSensitive) ->
            val node = mock<TypedFolderNode> {
                on { id } doReturn NodeId((index + 1).toLong())
                on { name } doReturn "Item ${index + 1}"
            }

            NodeUiItem(
                node = node,
                isSelected = isSelected,
                isSensitive = isSensitive
            )
        }
    }
}