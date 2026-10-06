package mega.privacy.android.shared.nodes.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.shared.nodes.components.previewdata.previewFolderNodeUiItem
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
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

    private fun setHighlightedContent(
        isListView: Boolean,
        listState: LazyListState,
        gridState: LazyGridState,
    ) {
        val manyItems = (1L..60L).map { previewFolderNodeUiItem(id = it) }
            .mapIndexed { index, item -> if (index == 50) item.copy(isHighlighted = true) else item }
        composeTestRule.setContent {
            NodesView(
                modifier = Modifier.fillMaxSize(),
                items = manyItems,
                onMenuClicked = {},
                onItemClicked = {},
                onLongClicked = {},
                sortConfiguration = NodeSortConfiguration.default,
                isListView = isListView,
                onSortOrderClick = {},
                onChangeViewTypeClicked = {},
                nodeSelectionState = NodeSelectionState(),
                listState = listState,
                gridState = gridState,
            )
        }
    }

    @Test
    fun `test that NodesView scrolls the list to the highlighted item`() {
        val listState = LazyListState()
        setHighlightedContent(isListView = true, listState = listState, gridState = LazyGridState())

        composeTestRule.waitForIdle()

        assertThat(listState.firstVisibleItemIndex).isGreaterThan(0)
    }

    @Test
    fun `test that NodesView scrolls the grid to the highlighted item`() {
        val gridState = LazyGridState()
        setHighlightedContent(isListView = false, listState = LazyListState(), gridState = gridState)

        composeTestRule.waitForIdle()

        assertThat(gridState.firstVisibleItemIndex).isGreaterThan(0)
    }

    private var sortConfiguration by mutableStateOf(NodeSortConfiguration.default)
    private var sortableItems by mutableStateOf((1L..60L).map { previewFolderNodeUiItem(id = it) })
    private val sizeDescending =
        NodeSortConfiguration(NodeSortOption.Size, SortDirection.Descending)

    private fun setSortableContent(
        isListView: Boolean,
        listState: LazyListState,
        gridState: LazyGridState,
    ) {
        composeTestRule.setContent {
            NodesView(
                modifier = Modifier.fillMaxSize(),
                items = sortableItems,
                onMenuClicked = {},
                onItemClicked = {},
                onLongClicked = {},
                sortConfiguration = sortConfiguration,
                isListView = isListView,
                onSortOrderClick = {},
                onChangeViewTypeClicked = {},
                nodeSelectionState = NodeSelectionState(),
                listState = listState,
                gridState = gridState,
                showSortOrder = false,
                showChangeViewType = false,
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `test that NodesView keeps the list position when sort configuration is unchanged`() {
        val listState = LazyListState(firstVisibleItemIndex = 30)
        setSortableContent(isListView = true, listState = listState, gridState = LazyGridState())

        assertThat(listState.firstVisibleItemIndex).isEqualTo(30)
    }

    @Test
    fun `test that NodesView keeps the list position when items update without a sort change`() {
        val listState = LazyListState(firstVisibleItemIndex = 30)
        setSortableContent(isListView = true, listState = listState, gridState = LazyGridState())

        sortableItems = sortableItems.toList()
        composeTestRule.waitForIdle()

        assertThat(listState.firstVisibleItemIndex).isEqualTo(30)
    }

    @Test
    fun `test that NodesView keeps the list position until the re-sorted items arrive`() {
        val listState = LazyListState(firstVisibleItemIndex = 30)
        setSortableContent(isListView = true, listState = listState, gridState = LazyGridState())

        sortConfiguration = sizeDescending
        composeTestRule.waitForIdle()

        assertThat(listState.firstVisibleItemIndex).isEqualTo(30)
    }

    @Test
    fun `test that NodesView scrolls the list to the top when re-sorted items arrive after the sort change`() {
        val listState = LazyListState(firstVisibleItemIndex = 30)
        setSortableContent(isListView = true, listState = listState, gridState = LazyGridState())

        sortConfiguration = sizeDescending
        composeTestRule.waitForIdle()
        sortableItems = sortableItems.reversed()
        composeTestRule.waitForIdle()

        assertThat(listState.firstVisibleItemIndex).isEqualTo(0)
    }

    @Test
    fun `test that NodesView scrolls the list to the top when sort and items change together`() {
        val listState = LazyListState(firstVisibleItemIndex = 30)
        setSortableContent(isListView = true, listState = listState, gridState = LazyGridState())

        composeTestRule.runOnIdle {
            sortConfiguration = sizeDescending
            sortableItems = sortableItems.reversed()
        }
        composeTestRule.waitForIdle()

        assertThat(listState.firstVisibleItemIndex).isEqualTo(0)
    }

    @Test
    fun `test that NodesView scrolls the grid to the top when re-sorted grid items arrive after the sort change`() {
        val gridState = LazyGridState(firstVisibleItemIndex = 30)
        setSortableContent(isListView = false, listState = LazyListState(), gridState = gridState)

        sortConfiguration = sizeDescending
        composeTestRule.waitForIdle()
        sortableItems = sortableItems.reversed()
        composeTestRule.waitForIdle()

        assertThat(gridState.firstVisibleItemIndex).isEqualTo(0)
    }

    private var showContent by mutableStateOf(true)

    private fun setRestorableContent(listState: LazyListState) {
        composeTestRule.setContent {
            val holder = rememberSaveableStateHolder()
            if (showContent) {
                holder.SaveableStateProvider("nodes") {
                    NodesView(
                        modifier = Modifier.fillMaxSize(),
                        items = sortableItems,
                        onMenuClicked = {},
                        onItemClicked = {},
                        onLongClicked = {},
                        sortConfiguration = sortConfiguration,
                        isListView = true,
                        nodeSelectionState = NodeSelectionState(),
                        listState = listState,
                        showSortOrder = false,
                        showChangeViewType = false,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `test that NodesView keeps the list position when restored without a sort change`() {
        val listState = LazyListState(firstVisibleItemIndex = 30)
        setRestorableContent(listState)

        showContent = false
        composeTestRule.waitForIdle()
        showContent = true
        composeTestRule.waitForIdle()

        assertThat(listState.firstVisibleItemIndex).isEqualTo(30)
    }

    @Test
    fun `test that NodesView scrolls the list to the top when restored after the sort changed while hidden`() {
        val listState = LazyListState(firstVisibleItemIndex = 30)
        setRestorableContent(listState)

        showContent = false
        composeTestRule.waitForIdle()
        sortConfiguration = sizeDescending
        sortableItems = sortableItems.reversed()
        showContent = true
        composeTestRule.waitForIdle()

        assertThat(listState.firstVisibleItemIndex).isEqualTo(0)
    }

    private fun setPagingContent(
        listState: LazyListState,
        viewportSize: Dp? = null,
        onVisibleRangeChanged: ((IntRange) -> Unit)? = null,
    ) {
        val manyItems = (1L..60L).map { previewFolderNodeUiItem(id = it) }
        composeTestRule.setContent {
            Box(modifier = viewportSize?.let { Modifier.size(it) } ?: Modifier.fillMaxSize()) {
                NodesView(
                    modifier = Modifier.fillMaxSize(),
                    items = manyItems,
                    onMenuClicked = {},
                    onItemClicked = {},
                    onLongClicked = {},
                    sortConfiguration = NodeSortConfiguration.default,
                    isListView = true,
                    nodeSelectionState = NodeSelectionState(),
                    listState = listState,
                    onVisibleRangeChanged = onVisibleRangeChanged,
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `test that NodesView reports the visible item range once per change without the header`() {
        val listState = LazyListState()
        val ranges = mutableListOf<IntRange>()
        setPagingContent(listState = listState, onVisibleRangeChanged = { ranges += it })

        assertThat(ranges).hasSize(1)
        assertThat(ranges.single().first).isEqualTo(0)

        composeTestRule.runOnIdle { listState.requestScrollToItem(20) }
        composeTestRule.waitForIdle()

        // Lazy index 20 is the 20th item because index 0 is the sort header
        assertThat(ranges).hasSize(2)
        assertThat(ranges[1].first).isEqualTo(19)
    }

    @Test
    fun `test that NodesView does not report a visible range for an empty viewport`() {
        val ranges = mutableListOf<IntRange>()
        setPagingContent(
            listState = LazyListState(),
            viewportSize = 0.dp,
            onVisibleRangeChanged = { ranges += it },
        )

        assertThat(ranges).isEmpty()
    }
}
