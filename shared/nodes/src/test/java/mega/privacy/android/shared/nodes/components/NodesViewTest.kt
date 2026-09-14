package mega.privacy.android.shared.nodes.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.shared.nodes.components.previewdata.previewFolderNodeUiItem
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.selection.NodeSelectionState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class NodesViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val items = (1L..3L).map { previewFolderNodeUiItem(id = it) }

    private fun setContent(
        selectionState: NodeSelectionState,
        isListView: Boolean = true,
    ) {
        composeTestRule.setContent {
            NodesView(
                items = items,
                onMenuClicked = {},
                onItemClicked = {},
                onLongClicked = {},
                sortConfiguration = NodeSortConfiguration.default,
                isListView = isListView,
                onSortOrderClick = {},
                onChangeViewTypeClicked = {},
                nodeSelectionState = selectionState,
                inSelectionMode = selectionState.isInSelectionMode,
            )
        }
    }

    private fun rows() = composeTestRule.onAllNodes(isSelectable())

    @Test
    fun `test that NodesView marks only the node in selection state as selected`() {
        setContent(NodeSelectionState(initialSelectedIds = setOf(NodeId(2L))))

        rows()[0].assertIsNotSelected()
        rows()[1].assertIsSelected()
        rows()[2].assertIsNotSelected()
    }

    @Test
    fun `test that NodesView updates selected rows when selection state changes after composition`() {
        val selectionState = NodeSelectionState(initialSelectedIds = setOf(NodeId(2L)))
        setContent(selectionState)

        composeTestRule.runOnIdle {
            selectionState.toggleSelection(NodeId(1L))
            selectionState.toggleSelection(NodeId(2L))
        }

        rows()[0].assertIsSelected()
        rows()[1].assertIsNotSelected()
        rows()[2].assertIsNotSelected()
    }

    @Test
    fun `test that NodesView marks all rows selected when selection state selects all`() {
        val selectionState = NodeSelectionState(initialSelectedIds = setOf(NodeId(1L)))
        setContent(selectionState)

        composeTestRule.runOnIdle {
            selectionState.selectAll(items.map { it.id }.toSet())
        }

        rows()[0].assertIsSelected()
        rows()[1].assertIsSelected()
        rows()[2].assertIsSelected()
    }

    @Test
    fun `test that NodesView in grid mode marks only the node in selection state as selected`() {
        setContent(
            selectionState = NodeSelectionState(initialSelectedIds = setOf(NodeId(3L))),
            isListView = false,
        )

        rows()[0].assertIsNotSelected()
        rows()[1].assertIsNotSelected()
        rows()[2].assertIsSelected()
    }

    @Test
    fun `test that NodesView exposes no selection semantics when not in selection mode`() {
        setContent(NodeSelectionState())

        rows().assertCountEquals(0)
    }
}
