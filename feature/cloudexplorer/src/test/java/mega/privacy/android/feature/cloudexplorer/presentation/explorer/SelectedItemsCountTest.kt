package mega.privacy.android.feature.cloudexplorer.presentation.explorer

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodeExplorerUiState
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.nodeExplorerDataState
import mega.privacy.android.navigation.destination.CopyNavKey
import mega.privacy.android.shared.nodes.components.previewdata.LocalNodeHeaderPreviewData
import mega.privacy.android.shared.nodes.components.previewdata.previewFileNodeUiItem
import mega.privacy.android.shared.nodes.model.NodeHeaderItemUiState
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeViewItem
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
internal class SelectedItemsCountTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val analyticsRule = AnalyticsTestRule()

    @Test
    fun `test that selecting a file in file picker mode shows the selected count as the title`() {
        setContent(
            explorerMode = ExplorerMode.ShareFilesToChat,
            items = listOf(previewFileNodeUiItem(id = FILE_ID, name = FILE_NAME)),
        )

        composeTestRule.onNodeWithText(FILE_NAME, useUnmergedTree = true).performClick()

        composeTestRule.onNodeWithText(SELECTED_COUNT).assertIsDisplayed()
    }

    @Test
    fun `test that the search action is shown when no items are selected`() {
        setContent(
            explorerMode = ExplorerMode.ShareFilesToChat,
            items = listOf(previewFileNodeUiItem(id = FILE_ID, name = FILE_NAME)),
        )

        composeTestRule.onNodeWithTag(SEARCH_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the search action is hidden when items are selected`() {
        setContent(
            explorerMode = ExplorerMode.ShareFilesToChat,
            items = listOf(previewFileNodeUiItem(id = FILE_ID, name = FILE_NAME)),
        )

        composeTestRule.onNodeWithText(FILE_NAME, useUnmergedTree = true).performClick()

        composeTestRule.onNodeWithTag(SEARCH_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that disabled nodes are not counted as selected`() {
        setContent(
            explorerMode = ExplorerMode.AddVideosToPlaylist,
            items = listOf(previewFileNodeUiItem(id = FILE_ID, name = FILE_NAME)),
            disabledNodeIds = setOf(NodeId(FILE_ID)),
        )

        composeTestRule.onNodeWithText(SELECTED_COUNT).assertDoesNotExist()
    }

    @Test
    fun `test that the navigation button clears the selection instead of closing when items are selected`() {
        var closed = false
        var navigatedBack = false
        setContent(
            explorerMode = ExplorerMode.ShareFilesToChat,
            items = listOf(previewFileNodeUiItem(id = FILE_ID, name = FILE_NAME)),
            onCloseExplorerScreen = { closed = true },
            onNavigateBack = { navigatedBack = true },
        )
        composeTestRule.onNodeWithText(FILE_NAME, useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithText(SELECTED_COUNT).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(NAVIGATION_ICON).performClick()

        composeTestRule.onNodeWithText(SELECTED_COUNT).assertDoesNotExist()
        assertThat(closed).isFalse()
        assertThat(navigatedBack).isFalse()
    }

    @Test
    fun `test that select all is available without any prior selection`() {
        setContent(
            explorerMode = ExplorerMode.ShareFilesToChat,
            items = files(10L, 20L, 30L),
        )

        composeTestRule.onNodeWithTag(SELECT_ALL_TAG).performClick()

        composeTestRule.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun `test that select all selects every selectable node`() {
        setContent(
            explorerMode = ExplorerMode.ShareFilesToChat,
            items = files(10L, 20L, 30L),
        )
        composeTestRule.onNodeWithText(fileName(10L), useUnmergedTree = true).performClick()

        composeTestRule.onNodeWithTag(SELECT_ALL_TAG).performClick()

        composeTestRule.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun `test that select all excludes disabled nodes`() {
        setContent(
            explorerMode = ExplorerMode.ShareFilesToChat,
            items = files(10L, 20L, 30L),
            disabledNodeIds = setOf(NodeId(20L)),
        )
        composeTestRule.onNodeWithText(fileName(10L), useUnmergedTree = true).performClick()

        composeTestRule.onNodeWithTag(SELECT_ALL_TAG).performClick()

        composeTestRule.onNodeWithText("2").assertIsDisplayed()
    }

    @Test
    fun `test that select all excludes hidden sensitive nodes`() {
        setContent(
            explorerMode = ExplorerMode.ShareFilesToChat,
            items = listOf(
                previewFileNodeUiItem(id = 10L, name = fileName(10L)),
                previewFileNodeUiItem(id = 20L, name = fileName(20L)).copy(isSensitive = true),
                previewFileNodeUiItem(id = 30L, name = fileName(30L)),
            ),
            isHiddenNodesEnabled = true,
            showHiddenNodes = false,
        )
        composeTestRule.onNodeWithText(fileName(10L), useUnmergedTree = true).performClick()

        composeTestRule.onNodeWithTag(SELECT_ALL_TAG).performClick()

        composeTestRule.onNodeWithText("2").assertIsDisplayed()
    }

    @Test
    fun `test that the select all action is hidden once all nodes are selected`() {
        setContent(
            explorerMode = ExplorerMode.ShareFilesToChat,
            items = files(10L, 20L),
        )
        composeTestRule.onNodeWithText(fileName(10L), useUnmergedTree = true).performClick()

        composeTestRule.onNodeWithTag(SELECT_ALL_TAG).performClick()

        composeTestRule.onNodeWithTag(SELECT_ALL_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that select all shows the selecting spinner while nodes are still loading`() {
        setContent(
            explorerMode = ExplorerMode.ShareFilesToChat,
            items = files(10L, 20L),
            nodesLoadingState = NodesLoadingState.PartiallyLoaded,
        )

        composeTestRule.onNodeWithTag(SELECT_ALL_TAG).performClick()

        composeTestRule.onNodeWithTag(SELECTING_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SELECT_ALL_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the title shows the selecting label while nodes are still loading`() {
        setContent(
            explorerMode = ExplorerMode.ShareFilesToChat,
            items = files(10L, 20L),
            nodesLoadingState = NodesLoadingState.PartiallyLoaded,
        )

        composeTestRule.onNodeWithTag(SELECT_ALL_TAG).performClick()

        composeTestRule.onNodeWithText(selectingLabel()).assertIsDisplayed()
    }

    @Test
    fun `test that select all keeps selecting nodes that load after the initial click`() {
        val nodesUiState = MutableStateFlow<NodeExplorerUiState>(
            nodeExplorerDataState(
                items = files(10L),
                isRoot = false,
                nodesLoadingState = NodesLoadingState.PartiallyLoaded,
            )
        )
        setContent(explorerMode = ExplorerMode.ShareFilesToChat, nodesUiState = nodesUiState)
        composeTestRule.onNodeWithTag(SELECT_ALL_TAG).performClick()
        composeTestRule.onNodeWithText(selectingLabel()).assertIsDisplayed()

        nodesUiState.value = nodeExplorerDataState(
            items = files(10L, 20L),
            isRoot = false,
            nodesLoadingState = NodesLoadingState.FullyLoaded,
        )

        composeTestRule.onNodeWithText("2").assertIsDisplayed()
        composeTestRule.onNodeWithTag(SELECTING_TAG).assertDoesNotExist()
    }

    private fun files(vararg ids: Long): List<NodeViewItem<TypedNode>> =
        ids.map { previewFileNodeUiItem(id = it, name = fileName(it)) }

    private fun fileName(id: Long) = "File ${('A' + (id / 10 % 26).toInt())}"

    private fun selectingLabel(): String =
        ApplicationProvider.getApplicationContext<Context>()
            .getString(sharedR.string.app_bar_selection_mode_description)

    private fun setContent(
        explorerMode: ExplorerMode,
        items: List<NodeViewItem<TypedNode>> = emptyList(),
        disabledNodeIds: Set<NodeId> = emptySet(),
        isHiddenNodesEnabled: Boolean = false,
        showHiddenNodes: Boolean = false,
        nodesLoadingState: NodesLoadingState = NodesLoadingState.FullyLoaded,
        nodesUiState: MutableStateFlow<NodeExplorerUiState> = MutableStateFlow(
            nodeExplorerDataState(
                items = items,
                isRoot = false,
                isHiddenNodesEnabled = isHiddenNodesEnabled,
                showHiddenNodes = showHiddenNodes,
                nodesLoadingState = nodesLoadingState,
            )
        ),
        onCloseExplorerScreen: () -> Unit = {},
        onNavigateBack: () -> Unit = {},
    ) {
        val nodes = mock<NodesExplorerViewModel> { on { uiState } doReturn nodesUiState }
        val owner = explorerViewModelStoreOwner(nodes = nodes)

        composeTestRule.setContent {
            AndroidThemeForPreviews {
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides owner,
                    LocalNodeHeaderPreviewData provides NodeHeaderItemUiState.Data(
                        viewType = ViewType.LIST,
                        nodeSortConfiguration = NodeSortConfiguration.default,
                    ),
                ) {
                    ExplorerScreen(
                        explorerMode = explorerMode,
                        startNavKey = CopyNavKey(emptyList()),
                        isInnerNavigation = true,
                        nodeExplorerId = NodeId(-1),
                        nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                        onCloseExplorerScreen = onCloseExplorerScreen,
                        onNavigateBack = onNavigateBack,
                        onNavigate = {},
                        isProcessingAction = false,
                        disabledNodeIds = disabledNodeIds,
                    )
                }
            }
        }
    }

    private companion object {
        const val FILE_ID = 10L
        const val FILE_NAME = "Selected file"
        const val SELECTED_COUNT = "1"
        const val NAVIGATION_ICON = "Navigation Icon"
        const val SEARCH_TAG = "app_bar:search"
        const val SELECT_ALL_TAG = "node_selection_action:select_all"
        const val SELECTING_TAG = "node_selection_action:selecting"
    }
}
