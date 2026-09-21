package mega.privacy.android.shared.nodes.selection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodesLoadingState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RememberNodeIdSelectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val ids = mutableStateOf(setOf(NodeId(1L), NodeId(2L)))
    private val loadingState = mutableStateOf<NodesLoadingState>(NodesLoadingState.PartiallyLoaded)
    private lateinit var selection: NodeIdSelection

    private fun setContent() {
        composeTestRule.setContent {
            val currentIds by ids
            val currentLoadingState by loadingState
            selection = rememberNodeIdSelection(currentIds, currentLoadingState)
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `test that selectAll extends to ids that arrive later when started during partial load`() {
        setContent()
        composeTestRule.runOnIdle { selection.selectAll() }

        composeTestRule.runOnIdle {
            ids.value = setOf(NodeId(1L), NodeId(2L), NodeId(3L), NodeId(4L))
            loadingState.value = NodesLoadingState.FullyLoaded
        }

        composeTestRule.runOnIdle {
            assertThat(selection.selectedNodeIds).containsExactly(NodeId(1L), NodeId(2L), NodeId(3L), NodeId(4L))
            assertThat(selection.isSelecting).isFalse()
            assertThat(selection.isAllSelected).isTrue()
        }
    }

    @Test
    fun `test that rememberNodeIdSelection does not grow a manual selection when ids arrive later`() {
        setContent()
        composeTestRule.runOnIdle { selection.state.toggleSelection(NodeId(1L)) }

        composeTestRule.runOnIdle {
            ids.value = setOf(NodeId(1L), NodeId(2L), NodeId(3L), NodeId(4L))
            loadingState.value = NodesLoadingState.FullyLoaded
        }

        composeTestRule.runOnIdle {
            assertThat(selection.selectedNodeIds).containsExactly(NodeId(1L))
        }
    }

    @Test
    fun `test that rememberNodeIdSelection returns the same instance across recompositions`() {
        setContent()
        val first = selection

        composeTestRule.runOnIdle { ids.value = ids.value + NodeId(3L) }

        composeTestRule.runOnIdle { assertThat(selection).isSameInstanceAs(first) }
    }
}
