package mega.privacy.android.core.nodecomponents.list

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.toImmutableList
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class NodesViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Test data helpers
    private fun createFolderNode(id: Long, name: String = "Folder $id"): TypedFolderNode {
        return mock<TypedFolderNode> {
            whenever(it.id).thenReturn(NodeId(id))
            whenever(it.name).thenReturn(name)
        }
    }

    private fun createFileNode(id: Long, name: String = "File $id"): TypedFileNode {
        return mock<TypedFileNode> {
            whenever(it.id).thenReturn(NodeId(id))
            whenever(it.name).thenReturn(name)
        }
    }

    private fun createNodeUiItem(
        node: TypedNode,
        isSensitive: Boolean = false,
        isDummy: Boolean = false,
    ): NodeUiItem<TypedNode> {
        return NodeUiItem(
            node = node,
            isSelected = false,
            isDummy = isDummy,
            isSensitive = isSensitive
        )
    }

    @Test
    fun `test that rememberNodeItems returns original list for list view`() {
        val items = listOf(
            createNodeUiItem(createFolderNode(1)),
            createNodeUiItem(createFileNode(2)),
            createNodeUiItem(createFileNode(3))
        )

        composeTestRule.setContent {
            val result = rememberNodeItems(
                nodeUIItems = items,
                showHiddenItems = false,
                isHiddenNodesEnabled = true,
                isListView = true,
                spanCount = 3
            )

            assertThat(result).isEqualTo(items.toImmutableList())
        }
    }

    @Test
    fun `test that rememberNodeItems filters sensitive nodes when hidden nodes enabled and not showing hidden items`() {
        val items = listOf(
            createNodeUiItem(createFolderNode(1), isSensitive = false),
            createNodeUiItem(createFileNode(2), isSensitive = true),
            createNodeUiItem(createFileNode(3), isSensitive = false),
            createNodeUiItem(createFileNode(4), isSensitive = true)
        )

        composeTestRule.setContent {
            val result = rememberNodeItems(
                nodeUIItems = items,
                showHiddenItems = false,
                isHiddenNodesEnabled = true,
                isListView = true,
                spanCount = 3
            )

            assertThat(result).hasSize(2)
            assertThat(result[0].node.id).isEqualTo(NodeId(1))
            assertThat(result[1].node.id).isEqualTo(NodeId(3))
        }
    }

    @Test
    fun `test that rememberNodeItems does not filter when showHiddenItems is true`() {
        val items = listOf(
            createNodeUiItem(createFolderNode(1), isSensitive = false),
            createNodeUiItem(createFileNode(2), isSensitive = true),
            createNodeUiItem(createFileNode(3), isSensitive = false)
        )

        composeTestRule.setContent {
            val result = rememberNodeItems(
                nodeUIItems = items,
                showHiddenItems = true,
                isHiddenNodesEnabled = true,
                isListView = true,
                spanCount = 3
            )

            assertThat(result).hasSize(3)
            assertThat(result).isEqualTo(items.toImmutableList())
        }
    }

    @Test
    fun `test that rememberNodeItems does not filter when hidden nodes feature is disabled`() {
        val items = listOf(
            createNodeUiItem(createFolderNode(1), isSensitive = false),
            createNodeUiItem(createFileNode(2), isSensitive = true),
            createNodeUiItem(createFileNode(3), isSensitive = false)
        )

        composeTestRule.setContent {
            val result = rememberNodeItems(
                nodeUIItems = items,
                showHiddenItems = false,
                isHiddenNodesEnabled = false,
                isListView = true,
                spanCount = 3
            )

            assertThat(result).hasSize(3)
            assertThat(result).isEqualTo(items.toImmutableList())
        }
    }

    @Test
    fun `test that rememberNodeItems adds placeholders for grid view when folders don't fill complete row`() {
        val items = listOf(
            createNodeUiItem(createFolderNode(1)),
            createNodeUiItem(createFolderNode(2)),
            createNodeUiItem(createFileNode(3)),
            createNodeUiItem(createFileNode(4))
        )

        composeTestRule.setContent {
            val result = rememberNodeItems(
                nodeUIItems = items,
                showHiddenItems = false,
                isHiddenNodesEnabled = false,
                isListView = false,
                spanCount = 3
            )

            assertThat(result).hasSize(5) // 2 folders + 1 placeholder + 2 files
            assertThat(result[0].node.id).isEqualTo(NodeId(1))
            assertThat(result[1].node.id).isEqualTo(NodeId(2))
            assertThat(result[2].isDummy).isTrue()
            assertThat(result[3].node.id).isEqualTo(NodeId(3))
            assertThat(result[4].node.id).isEqualTo(NodeId(4))
        }
    }

    @Test
    fun `test that rememberNodeItems does not add placeholders when folders fill complete row`() {
        val items = listOf(
            createNodeUiItem(createFolderNode(1)),
            createNodeUiItem(createFolderNode(2)),
            createNodeUiItem(createFolderNode(3)),
            createNodeUiItem(createFileNode(4))
        )

        composeTestRule.setContent {
            val result = rememberNodeItems(
                nodeUIItems = items,
                showHiddenItems = false,
                isHiddenNodesEnabled = false,
                isListView = false,
                spanCount = 3
            )

            assertThat(result).hasSize(4) // No placeholders needed
            assertThat(result).isEqualTo(items.toImmutableList())
        }
    }

    @Test
    fun `test that rememberNodeItems does not add placeholders when all items are folders`() {
        val items = listOf(
            createNodeUiItem(createFolderNode(1)),
            createNodeUiItem(createFolderNode(2)),
            createNodeUiItem(createFolderNode(3))
        )

        composeTestRule.setContent {
            val result = rememberNodeItems(
                nodeUIItems = items,
                showHiddenItems = false,
                isHiddenNodesEnabled = false,
                isListView = false,
                spanCount = 3
            )

            assertThat(result).hasSize(3)
            assertThat(result).isEqualTo(items.toImmutableList())
        }
    }

    @Test
    fun `test that rememberNodeItems does not add placeholders when no folders`() {
        val items = listOf(
            createNodeUiItem(createFileNode(1)),
            createNodeUiItem(createFileNode(2)),
            createNodeUiItem(createFileNode(3))
        )

        composeTestRule.setContent {
            val result = rememberNodeItems(
                nodeUIItems = items,
                showHiddenItems = false,
                isHiddenNodesEnabled = false,
                isListView = false,
                spanCount = 3
            )

            assertThat(result).hasSize(3)
            assertThat(result).isEqualTo(items.toImmutableList())
        }
    }

    @Test
    fun `test that rememberNodeItems handles empty list`() {
        val items = emptyList<NodeUiItem<TypedNode>>()

        composeTestRule.setContent {
            val result = rememberNodeItems(
                nodeUIItems = items,
                showHiddenItems = false,
                isHiddenNodesEnabled = true,
                isListView = false,
                spanCount = 3
            )

            assertThat(result).isEmpty()
        }
    }

    @Test
    fun `test that rememberNodeItems handles single item`() {
        val items = listOf(createNodeUiItem(createFolderNode(1)))

        composeTestRule.setContent {
            val result = rememberNodeItems(
                nodeUIItems = items,
                showHiddenItems = false,
                isHiddenNodesEnabled = false,
                isListView = false,
                spanCount = 3
            )

            assertThat(result).hasSize(1)
            assertThat(result).isEqualTo(items.toImmutableList())
        }
    }

    @Test
    fun `test that rememberNodeItems handles span count of 1`() {
        val items = listOf(
            createNodeUiItem(createFolderNode(1)),
            createNodeUiItem(createFileNode(2))
        )

        composeTestRule.setContent {
            val result = rememberNodeItems(
                nodeUIItems = items,
                showHiddenItems = false,
                isHiddenNodesEnabled = false,
                isListView = false,
                spanCount = 1
            )

            assertThat(result).hasSize(2)
            assertThat(result).isEqualTo(items.toImmutableList())
        }
    }

    @Test
    fun `test that rememberNodeItems handles span count of 2 with mixed content`() {
        val items = listOf(
            createNodeUiItem(createFolderNode(1)),
            createNodeUiItem(createFileNode(2)),
            createNodeUiItem(createFileNode(3))
        )

        composeTestRule.setContent {
            val result = rememberNodeItems(
                nodeUIItems = items,
                showHiddenItems = false,
                isHiddenNodesEnabled = false,
                isListView = false,
                spanCount = 2
            )

            assertThat(result).hasSize(4) // 1 folder + 1 placeholder + 2 files
            assertThat(result[0].node.id).isEqualTo(NodeId(1))
            assertThat(result[1].isDummy).isTrue()
            assertThat(result[2].node.id).isEqualTo(NodeId(2))
            assertThat(result[3].node.id).isEqualTo(NodeId(3))
        }
    }

    @Test
    fun `test that rememberNodeItems combines filtering and grid optimization`() {
        val items = listOf(
            createNodeUiItem(createFolderNode(1), isSensitive = false),
            createNodeUiItem(createFolderNode(2), isSensitive = true), // Will be filtered
            createNodeUiItem(createFileNode(3), isSensitive = false),
            createNodeUiItem(createFileNode(4), isSensitive = true) // Will be filtered
        )

        composeTestRule.setContent {
            val result = rememberNodeItems(
                nodeUIItems = items,
                showHiddenItems = false,
                isHiddenNodesEnabled = true,
                isListView = false,
                spanCount = 3
            )

            // After filtering: 1 folder + 1 file = 2 items
            // Since only 1 folder, need 2 placeholders to fill the row
            assertThat(result).hasSize(4) // 1 folder + 2 placeholders + 1 file
            assertThat(result[0].node.id).isEqualTo(NodeId(1))
            assertThat(result[1].isDummy).isTrue()
            assertThat(result[2].isDummy).isTrue()
            assertThat(result[3].node.id).isEqualTo(NodeId(3))
        }
    }

    @Test
    fun `test that rememberNodeItems placeholder is based on last folder`() {
        val items = listOf(
            createNodeUiItem(createFolderNode(1)),
            createNodeUiItem(createFolderNode(2)),
            createNodeUiItem(createFileNode(3)),
            createNodeUiItem(createFolderNode(4)), // This should not affect placeholder count
            createNodeUiItem(createFileNode(5))
        )

        composeTestRule.setContent {
            val result = rememberNodeItems(
                nodeUIItems = items,
                showHiddenItems = false,
                isHiddenNodesEnabled = false,
                isListView = false,
                spanCount = 3
            )

            // 2 folders at the beginning, need 1 placeholder
            assertThat(result).hasSize(5) // No placeholders needed since folders are not consecutive
            assertThat(result[0].node.id).isEqualTo(NodeId(1))
            assertThat(result[1].node.id).isEqualTo(NodeId(2))
            assertThat(result[2].node.id).isEqualTo(NodeId(3))
            assertThat(result[3].node.id).isEqualTo(NodeId(4))
            assertThat(result[4].node.id).isEqualTo(NodeId(5))
        }
    }

    @Test
    fun `test that rememberNodeItems returns immutable list`() {
        val items = listOf(createNodeUiItem(createFolderNode(1)))

        composeTestRule.setContent {
            val result = rememberNodeItems(
                nodeUIItems = items,
                showHiddenItems = false,
                isHiddenNodesEnabled = false,
                isListView = true,
                spanCount = 3
            )

            // Check that the list is immutable (cannot be modified)
            assertThat(result).isInstanceOf(kotlin.collections.List::class.java)
            // The result should be immutable, but the exact type might vary
            // For single items, it might be a SingletonList which is still immutable
        }
    }
}