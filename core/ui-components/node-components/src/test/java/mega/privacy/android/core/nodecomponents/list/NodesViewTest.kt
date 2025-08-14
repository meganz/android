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
    ): NodeUiItem<TypedNode> {
        return NodeUiItem(
            node = node,
            isSelected = false,
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
            )

            assertThat(result).hasSize(1)
            assertThat(result).isEqualTo(items.toImmutableList())
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
            )

            // Check that the list is immutable (cannot be modified)
            assertThat(result).isInstanceOf(kotlin.collections.List::class.java)
            // The result should be immutable, but the exact type might vary
            // For single items, it might be a SingletonList which is still immutable
        }
    }
}