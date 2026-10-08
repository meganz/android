package mega.privacy.android.shared.nodes.selection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.shared.nodes.components.previewdata.previewFolderNodeUiItem
import mega.privacy.android.shared.nodes.model.TypedNodeItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RememberPagedNodeSelectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val items = mutableStateOf<List<TypedNodeItem<TypedNode>>>(nodes(1L..2L))
    private val reconcileVersion = mutableIntStateOf(0)
    private lateinit var selection: NodeSelection<TypedNode>

    private fun nodes(ids: LongRange) = ids.map { previewFolderNodeUiItem(id = it) }

    private fun setContent() {
        composeTestRule.setContent {
            val currentItems by items
            val currentVersion by reconcileVersion
            selection = rememberPagedNodeSelection(currentItems, currentVersion)
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `test that selectAll takes the loaded rows and not the rows of a later page`() {
        setContent()
        composeTestRule.runOnIdle { selection.selectAll() }

        composeTestRule.runOnIdle { items.value = nodes(1L..4L) }

        composeTestRule.runOnIdle {
            assertThat(selection.state.selectedNodeIds).containsExactly(NodeId(1L), NodeId(2L))
            assertThat(selection.isAllSelected).isFalse()
            assertThat(selection.isSelecting).isFalse()
        }
    }

    @Test
    fun `test that a re-read drops a selected row that is gone`() {
        setContent()
        composeTestRule.runOnIdle { selection.selectAll() }

        composeTestRule.runOnIdle {
            items.value = listOf(previewFolderNodeUiItem(id = 1L), previewFolderNodeUiItem(id = 9L))
            reconcileVersion.intValue = 1
        }

        composeTestRule.runOnIdle {
            assertThat(selection.state.selectedNodeIds).containsExactly(NodeId(1L))
        }
    }

    @Test
    fun `test that a page without a re-read keeps a selected row that is no longer listed`() {
        setContent()
        composeTestRule.runOnIdle { selection.toggle(NodeId(1L)) }

        composeTestRule.runOnIdle { items.value = nodes(2L..4L) }

        composeTestRule.runOnIdle {
            assertThat(selection.state.selectedNodeIds).containsExactly(NodeId(1L))
        }
    }

    @Test
    fun `test that a re-read with the same rows keeps the selection`() {
        setContent()
        composeTestRule.runOnIdle { selection.selectAll() }

        composeTestRule.runOnIdle {
            items.value = nodes(1L..2L)
            reconcileVersion.intValue = 1
        }

        composeTestRule.runOnIdle {
            assertThat(selection.state.selectedNodeIds).containsExactly(NodeId(1L), NodeId(2L))
            assertThat(selection.isAllSelected).isTrue()
        }
    }
}
