package mega.privacy.android.shared.nodes.selection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.shared.nodes.components.previewdata.previewFolderNodeUiItem
import mega.privacy.android.shared.nodes.model.TypedNodeItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RememberNodeSelectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val items = mutableStateOf<List<TypedNodeItem<TypedNode>>>(
        (1L..2L).map { previewFolderNodeUiItem(id = it) }
    )
    private val loadingState = mutableStateOf<NodesLoadingState>(NodesLoadingState.PartiallyLoaded)
    private lateinit var selection: NodeSelection<TypedNode>

    private fun setContent() {
        composeTestRule.setContent {
            val currentItems by items
            val currentLoadingState by loadingState
            selection = rememberNodeSelection(currentItems, currentLoadingState)
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `test that selectAll extends to items that arrive later when started during partial load`() {
        setContent()
        composeTestRule.runOnIdle { selection.selectAll() }

        composeTestRule.runOnIdle {
            items.value = (1L..4L).map { previewFolderNodeUiItem(id = it) }
            loadingState.value = NodesLoadingState.FullyLoaded
        }

        composeTestRule.runOnIdle {
            assertThat(selection.state.selectedNodeIds).containsExactly(NodeId(1L), NodeId(2L), NodeId(3L), NodeId(4L))
            assertThat(selection.isSelecting).isFalse()
            assertThat(selection.isAllSelected).isTrue()
        }
    }

    @Test
    fun `test that rememberNodeSelection does not grow a manual selection when items arrive later`() {
        setContent()
        composeTestRule.runOnIdle { selection.toggle(NodeId(1L)) }

        composeTestRule.runOnIdle {
            items.value = (1L..4L).map { previewFolderNodeUiItem(id = it) }
            loadingState.value = NodesLoadingState.FullyLoaded
        }

        composeTestRule.runOnIdle {
            assertThat(selection.state.selectedNodeIds).containsExactly(NodeId(1L))
        }
    }

    @Test
    fun `test that rememberNodeSelection returns the same instance across recompositions`() {
        setContent()
        val first = selection

        composeTestRule.runOnIdle { items.value = items.value + previewFolderNodeUiItem(id = 3L) }

        composeTestRule.runOnIdle { assertThat(selection).isSameInstanceAs(first) }
    }
}
