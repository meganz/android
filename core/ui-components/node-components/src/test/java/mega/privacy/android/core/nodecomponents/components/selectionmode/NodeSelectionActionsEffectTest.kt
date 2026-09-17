package mega.privacy.android.core.nodecomponents.components.selectionmode

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
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
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class NodeSelectionActionsEffectTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val items = (1L..3L).map { previewFolderNodeUiItem(id = it) }
    private val actionState = MutableStateFlow(NodeActionState())
    private val viewModel = mock<NodeOptionsActionViewModel> {
        on { uiState }.thenReturn(actionState)
    }
    private lateinit var selection: NodeSelection<TypedNode>

    private fun setContent() {
        composeTestRule.setContent {
            selection = rememberNodeSelection(items)
            NodeSelectionActionsEffect(
                selection = selection,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                viewModel = viewModel,
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `test that available actions are refreshed with the selected nodes`() {
        setContent()

        composeTestRule.runOnIdle { selection.toggle(NodeId(2L)) }
        composeTestRule.waitForIdle()

        verify(viewModel).updateSelectionModeAvailableActions(
            eq(setOf(items[1].node)),
            eq(NodeSourceType.CLOUD_DRIVE),
        )
    }

    @Test
    fun `test that the selection is cleared when an action is triggered`() {
        setContent()
        composeTestRule.runOnIdle { selection.toggle(NodeId(1L)) }

        actionState.update { it.copy(actionTriggeredEvent = triggered) }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { assertThat(selection.isInSelectionMode).isFalse() }
        verify(viewModel).resetActionTriggered()
    }

    @Test
    fun `test that the selection is cleared when the options are dismissed`() {
        setContent()
        composeTestRule.runOnIdle { selection.toggle(NodeId(1L)) }

        actionState.update { it.copy(dismissEvent = triggered) }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { assertThat(selection.isInSelectionMode).isFalse() }
        verify(viewModel).resetDismiss()
    }

    @Test
    fun `test that a consumed event does not clear the selection`() {
        setContent()
        composeTestRule.runOnIdle { selection.toggle(NodeId(1L)) }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { assertThat(selection.isInSelectionMode).isTrue() }
        verify(viewModel, never()).resetActionTriggered()
        verify(viewModel, never()).resetDismiss()
        verify(viewModel, atLeastOnce()).updateSelectionModeAvailableActions(any(), any())
    }
}
