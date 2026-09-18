package mega.privacy.android.core.nodecomponents.components.selectionmode

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.triggered
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionIntent
import mega.privacy.android.core.nodecomponents.model.NodeActionState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.shared.nodes.components.previewdata.previewFolderNodeUiItem
import mega.privacy.android.shared.nodes.selection.NodeSelection
import mega.privacy.android.shared.nodes.selection.rememberNodeSelection
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NodeSelectionActionsEffectTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val items = (1L..3L).map { previewFolderNodeUiItem(id = it) }
    private val actionState = mutableStateOf(NodeActionState())
    private val intents = mutableListOf<NodeOptionsActionIntent>()
    private lateinit var selection: NodeSelection<TypedNode>

    private fun setContent() {
        composeTestRule.setContent {
            val uiState by actionState
            selection = rememberNodeSelection(items)
            NodeSelectionActionsEffect(
                selection = selection,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                uiState = uiState,
                onIntent = intents::add,
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `test that a SelectedNodesChanged intent carries the selected nodes`() {
        setContent()

        composeTestRule.runOnIdle { selection.toggle(NodeId(2L)) }
        composeTestRule.waitForIdle()

        assertThat(intents.last()).isEqualTo(
            NodeOptionsActionIntent.SelectedNodesChanged(
                nodes = setOf(items[1].node),
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            )
        )
    }

    @Test
    fun `test that the selection is cleared and the event consumed when an action is triggered`() {
        setContent()
        composeTestRule.runOnIdle { selection.toggle(NodeId(1L)) }

        composeTestRule.runOnIdle {
            actionState.value = actionState.value.copy(actionTriggeredEvent = triggered)
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { assertThat(selection.isInSelectionMode).isFalse() }
        assertThat(intents).contains(NodeOptionsActionIntent.ActionTriggeredConsumed)
    }

    @Test
    fun `test that the selection is cleared and the event consumed when the options are dismissed`() {
        setContent()
        composeTestRule.runOnIdle { selection.toggle(NodeId(1L)) }

        composeTestRule.runOnIdle {
            actionState.value = actionState.value.copy(dismissEvent = triggered)
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { assertThat(selection.isInSelectionMode).isFalse() }
        assertThat(intents).contains(NodeOptionsActionIntent.DismissConsumed)
    }

    @Test
    fun `test that consumed events neither clear the selection nor emit consume intents`() {
        setContent()
        composeTestRule.runOnIdle { selection.toggle(NodeId(1L)) }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { assertThat(selection.isInSelectionMode).isTrue() }
        assertThat(intents).doesNotContain(NodeOptionsActionIntent.ActionTriggeredConsumed)
        assertThat(intents).doesNotContain(NodeOptionsActionIntent.DismissConsumed)
    }
}
