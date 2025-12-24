package mega.privacy.mobile.home.presentation.recents.bucket.model

import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecentsBucketUiStateTest {

    private val testNodeSourceType = NodeSourceType.CLOUD_DRIVE

    private fun createMockFileNode(
        name: String = "testFile.txt",
        id: Long = 1L,
    ): TypedFileNode = mock {
        on { it.name }.thenReturn(name)
        on { it.id }.thenReturn(NodeId(id))
        val fileTypeInfo = TextFileTypeInfo("text/plain", "txt")
        on { it.type }.thenReturn(fileTypeInfo)
    }

    private fun createMockNodeUiItem(
        node: TypedFileNode,
        isSelected: Boolean = false,
    ): NodeUiItem<TypedNode> = NodeUiItem(
        node = node,
        isSelected = isSelected,
    )

    @Test
    fun `test that isEmpty returns true when fileCount is zero and not loading`() {
        val state = RecentsBucketUiState(
            fileCount = 0,
            isLoading = false,
            nodeSourceType = testNodeSourceType
        )
        assertThat(state.isEmpty).isTrue()
    }

    @Test
    fun `test that isEmpty returns false when items count is greater than zero`() {
        val state = RecentsBucketUiState(
            fileCount = 1,
            items = listOf(mock(), mock()),
            isLoading = false,
            nodeSourceType = testNodeSourceType
        )
        assertThat(state.isEmpty).isFalse()
    }

    @Test
    fun `test that isEmpty returns false when loading`() {
        val state = RecentsBucketUiState(
            fileCount = 0,
            isLoading = true,
            nodeSourceType = testNodeSourceType
        )
        assertThat(state.isEmpty).isFalse()
    }

    @Test
    fun `test that isEmpty returns false when fileCount is greater than zero and loading`() {
        val state = RecentsBucketUiState(
            fileCount = 3,
            isLoading = true,
            nodeSourceType = testNodeSourceType
        )
        assertThat(state.isEmpty).isFalse()
    }

    @Test
    fun `test that selectedItemsCount returns correct count`() {
        val node1 = createMockFileNode(name = "file1.txt", id = 1L)
        val node2 = createMockFileNode(name = "file2.txt", id = 2L)
        val node3 = createMockFileNode(name = "file3.txt", id = 3L)

        val item1 = createMockNodeUiItem(node1, isSelected = true)
        val item2 = createMockNodeUiItem(node2, isSelected = false)
        val item3 = createMockNodeUiItem(node3, isSelected = true)

        val state = RecentsBucketUiState(
            items = listOf(item1, item2, item3),
            nodeSourceType = testNodeSourceType
        )
        assertThat(state.selectedItemsCount).isEqualTo(2)
    }

    @Test
    fun `test that selectedItemsCount returns zero when no items selected`() {
        val node1 = createMockFileNode(name = "file1.txt", id = 1L)
        val node2 = createMockFileNode(name = "file2.txt", id = 2L)

        val item1 = createMockNodeUiItem(node1, isSelected = false)
        val item2 = createMockNodeUiItem(node2, isSelected = false)

        val state = RecentsBucketUiState(
            items = listOf(item1, item2),
            nodeSourceType = testNodeSourceType
        )
        assertThat(state.selectedItemsCount).isEqualTo(0)
    }

    @Test
    fun `test that selectedItemsCount returns zero when items list is empty`() {
        val state = RecentsBucketUiState(
            items = emptyList(),
            nodeSourceType = testNodeSourceType
        )
        assertThat(state.selectedItemsCount).isEqualTo(0)
    }

    @Test
    fun `test that selectedItemsCount returns total count when all items selected`() {
        val node1 = createMockFileNode(name = "file1.txt", id = 1L)
        val node2 = createMockFileNode(name = "file2.txt", id = 2L)
        val node3 = createMockFileNode(name = "file3.txt", id = 3L)

        val item1 = createMockNodeUiItem(node1, isSelected = true)
        val item2 = createMockNodeUiItem(node2, isSelected = true)
        val item3 = createMockNodeUiItem(node3, isSelected = true)

        val state = RecentsBucketUiState(
            items = listOf(item1, item2, item3),
            nodeSourceType = testNodeSourceType
        )
        assertThat(state.selectedItemsCount).isEqualTo(3)
    }

    @Test
    fun `test that isInSelectionMode returns true when items are selected`() {
        val node1 = createMockFileNode(name = "file1.txt", id = 1L)
        val node2 = createMockFileNode(name = "file2.txt", id = 2L)

        val item1 = createMockNodeUiItem(node1, isSelected = true)
        val item2 = createMockNodeUiItem(node2, isSelected = false)

        val state = RecentsBucketUiState(
            items = listOf(item1, item2),
            nodeSourceType = testNodeSourceType
        )
        assertThat(state.isInSelectionMode).isTrue()
    }

    @Test
    fun `test that isInSelectionMode returns false when no items selected`() {
        val node1 = createMockFileNode(name = "file1.txt", id = 1L)
        val node2 = createMockFileNode(name = "file2.txt", id = 2L)

        val item1 = createMockNodeUiItem(node1, isSelected = false)
        val item2 = createMockNodeUiItem(node2, isSelected = false)

        val state = RecentsBucketUiState(
            items = listOf(item1, item2),
            nodeSourceType = testNodeSourceType
        )
        assertThat(state.isInSelectionMode).isFalse()
    }

    @Test
    fun `test that isInSelectionMode returns false when items list is empty`() {
        val state = RecentsBucketUiState(
            items = emptyList(),
            nodeSourceType = testNodeSourceType
        )
        assertThat(state.isInSelectionMode).isFalse()
    }

    @Test
    fun `test that isInSelectionMode returns true when all items selected`() {
        val node1 = createMockFileNode(name = "file1.txt", id = 1L)
        val node2 = createMockFileNode(name = "file2.txt", id = 2L)

        val item1 = createMockNodeUiItem(node1, isSelected = true)
        val item2 = createMockNodeUiItem(node2, isSelected = true)

        val state = RecentsBucketUiState(
            items = listOf(item1, item2),
            nodeSourceType = testNodeSourceType
        )
        assertThat(state.isInSelectionMode).isTrue()
    }

    @Test
    fun `test that isAllSelected returns true when all items selected`() {
        val node1 = createMockFileNode(name = "file1.txt", id = 1L)
        val node2 = createMockFileNode(name = "file2.txt", id = 2L)

        val item1 = createMockNodeUiItem(node1, isSelected = true)
        val item2 = createMockNodeUiItem(node2, isSelected = true)

        val state = RecentsBucketUiState(
            items = listOf(item1, item2),
            nodeSourceType = testNodeSourceType
        )
        assertThat(state.isAllSelected).isTrue()
    }

    @Test
    fun `test that isAllSelected returns false when not all items selected`() {
        val node1 = createMockFileNode(name = "file1.txt", id = 1L)
        val node2 = createMockFileNode(name = "file2.txt", id = 2L)

        val item1 = createMockNodeUiItem(node1, isSelected = true)
        val item2 = createMockNodeUiItem(node2, isSelected = false)

        val state = RecentsBucketUiState(
            items = listOf(item1, item2),
            nodeSourceType = testNodeSourceType
        )
        assertThat(state.isAllSelected).isFalse()
    }

    @Test
    fun `test that isAllSelected returns true when empty list`() {
        val state = RecentsBucketUiState(
            items = emptyList(),
            nodeSourceType = testNodeSourceType
        )
        assertThat(state.isAllSelected).isTrue()
    }

    @Test
    fun `test that isAllSelected returns false when no items selected`() {
        val node1 = createMockFileNode(name = "file1.txt", id = 1L)
        val node2 = createMockFileNode(name = "file2.txt", id = 2L)

        val item1 = createMockNodeUiItem(node1, isSelected = false)
        val item2 = createMockNodeUiItem(node2, isSelected = false)

        val state = RecentsBucketUiState(
            items = listOf(item1, item2),
            nodeSourceType = testNodeSourceType
        )
        assertThat(state.isAllSelected).isFalse()
    }

    @Test
    fun `test that selectedNodes returns only selected nodes`() {
        val node1 = createMockFileNode(name = "file1.txt", id = 1L)
        val node2 = createMockFileNode(name = "file2.txt", id = 2L)
        val node3 = createMockFileNode(name = "file3.txt", id = 3L)

        val item1 = createMockNodeUiItem(node1, isSelected = true)
        val item2 = createMockNodeUiItem(node2, isSelected = false)
        val item3 = createMockNodeUiItem(node3, isSelected = true)

        val state = RecentsBucketUiState(
            items = listOf(item1, item2, item3),
            nodeSourceType = testNodeSourceType
        )
        assertThat(state.selectedNodes).hasSize(2)
        assertThat(state.selectedNodes).containsExactly(node1, node3)
    }

    @Test
    fun `test that selectedNodes returns empty list when no items selected`() {
        val node1 = createMockFileNode(name = "file1.txt", id = 1L)
        val node2 = createMockFileNode(name = "file2.txt", id = 2L)

        val item1 = createMockNodeUiItem(node1, isSelected = false)
        val item2 = createMockNodeUiItem(node2, isSelected = false)

        val state = RecentsBucketUiState(
            items = listOf(item1, item2),
            nodeSourceType = testNodeSourceType
        )
        assertThat(state.selectedNodes).isEmpty()
    }

    @Test
    fun `test that selectedNodes returns empty list when items list is empty`() {
        val state = RecentsBucketUiState(
            items = emptyList(),
            nodeSourceType = testNodeSourceType
        )
        assertThat(state.selectedNodes).isEmpty()
    }

    @Test
    fun `test that selectedNodes returns all nodes when all items selected`() {
        val node1 = createMockFileNode(name = "file1.txt", id = 1L)
        val node2 = createMockFileNode(name = "file2.txt", id = 2L)
        val node3 = createMockFileNode(name = "file3.txt", id = 3L)

        val item1 = createMockNodeUiItem(node1, isSelected = true)
        val item2 = createMockNodeUiItem(node2, isSelected = true)
        val item3 = createMockNodeUiItem(node3, isSelected = true)

        val state = RecentsBucketUiState(
            items = listOf(item1, item2, item3),
            nodeSourceType = testNodeSourceType
        )
        assertThat(state.selectedNodes).hasSize(3)
        assertThat(state.selectedNodes).containsExactly(node1, node2, node3)
    }

    @Test
    fun `test that selectedNodeIds returns only selected node ids`() {
        val node1 = createMockFileNode(name = "file1.txt", id = 1L)
        val node2 = createMockFileNode(name = "file2.txt", id = 2L)
        val node3 = createMockFileNode(name = "file3.txt", id = 3L)

        val item1 = createMockNodeUiItem(node1, isSelected = true)
        val item2 = createMockNodeUiItem(node2, isSelected = false)
        val item3 = createMockNodeUiItem(node3, isSelected = true)

        val state = RecentsBucketUiState(
            items = listOf(item1, item2, item3),
            nodeSourceType = testNodeSourceType
        )
        assertThat(state.selectedNodeIds).hasSize(2)
        assertThat(state.selectedNodeIds).containsExactly(node1.id, node3.id)
    }

    @Test
    fun `test that selectedNodeIds returns empty list when no items selected`() {
        val node1 = createMockFileNode(name = "file1.txt", id = 1L)
        val node2 = createMockFileNode(name = "file2.txt", id = 2L)

        val item1 = createMockNodeUiItem(node1, isSelected = false)
        val item2 = createMockNodeUiItem(node2, isSelected = false)

        val state = RecentsBucketUiState(
            items = listOf(item1, item2),
            nodeSourceType = testNodeSourceType
        )
        assertThat(state.selectedNodeIds).isEmpty()
    }

    @Test
    fun `test that selectedNodeIds returns empty list when items list is empty`() {
        val state = RecentsBucketUiState(
            items = emptyList(),
            nodeSourceType = testNodeSourceType
        )
        assertThat(state.selectedNodeIds).isEmpty()
    }

    @Test
    fun `test that selectedNodeIds returns all node ids when all items selected`() {
        val node1 = createMockFileNode(name = "file1.txt", id = 1L)
        val node2 = createMockFileNode(name = "file2.txt", id = 2L)
        val node3 = createMockFileNode(name = "file3.txt", id = 3L)

        val item1 = createMockNodeUiItem(node1, isSelected = true)
        val item2 = createMockNodeUiItem(node2, isSelected = true)
        val item3 = createMockNodeUiItem(node3, isSelected = true)

        val state = RecentsBucketUiState(
            items = listOf(item1, item2, item3),
            nodeSourceType = testNodeSourceType
        )
        assertThat(state.selectedNodeIds).hasSize(3)
        assertThat(state.selectedNodeIds).containsExactly(node1.id, node2.id, node3.id)
    }

    @Test
    fun `test that default values are set correctly`() {
        val state = RecentsBucketUiState(
            nodeSourceType = testNodeSourceType
        )

        assertThat(state.items).isEmpty()
        assertThat(state.isMediaBucket).isFalse()
        assertThat(state.isLoading).isTrue()
        assertThat(state.fileCount).isEqualTo(0)
        assertThat(state.timestamp).isEqualTo(0L)
        assertThat(state.parentFolderName).isEqualTo(LocalizedText.Literal(""))
        assertThat(state.parentFolderHandle).isEqualTo(-1L)
        assertThat(state.excludeSensitives).isFalse()
        assertThat(state.navigateBack).isEqualTo(consumed)
    }

    @Test
    fun `test that all properties can be set correctly`() {
        val node1 = createMockFileNode(name = "file1.txt", id = 1L)
        val item1 = createMockNodeUiItem(node1, isSelected = true)
        val parentFolderName = LocalizedText.Literal("Test Folder")

        val state = RecentsBucketUiState(
            items = listOf(item1),
            isMediaBucket = true,
            isLoading = false,
            fileCount = 5,
            timestamp = 1234567890L,
            parentFolderName = parentFolderName,
            parentFolderHandle = 999L,
            nodeSourceType = testNodeSourceType,
            excludeSensitives = true,
            navigateBack = triggered
        )

        assertThat(state.items).hasSize(1)
        assertThat(state.isMediaBucket).isTrue()
        assertThat(state.isLoading).isFalse()
        assertThat(state.fileCount).isEqualTo(5)
        assertThat(state.timestamp).isEqualTo(1234567890L)
        assertThat(state.parentFolderName).isEqualTo(parentFolderName)
        assertThat(state.parentFolderHandle).isEqualTo(999L)
        assertThat(state.excludeSensitives).isTrue()
        assertThat(state.navigateBack).isEqualTo(triggered)
    }
}

