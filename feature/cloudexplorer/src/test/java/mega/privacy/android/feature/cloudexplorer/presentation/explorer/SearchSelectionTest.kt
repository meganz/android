package mega.privacy.android.feature.cloudexplorer.presentation.explorer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodeExplorerUiState
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.nodeExplorerDataState
import mega.privacy.android.feature.cloudexplorer.presentation.search.ExplorerSearchUiState
import mega.privacy.android.feature.cloudexplorer.presentation.search.ExplorerSearchViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.search.viewModelStoreOwnerOf
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.navigation.destination.CopyNavKey
import mega.privacy.android.shared.nodes.components.previewdata.LocalNodeHeaderPreviewData
import mega.privacy.android.shared.nodes.components.previewdata.previewFileNodeUiItem
import mega.privacy.android.shared.nodes.model.NodeHeaderItemUiState
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
internal class SearchSelectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val analyticsRule = AnalyticsTestRule()

    @Test
    fun `test that selecting a node hides the search action`() {
        setContent()
        composeTestRule.onNodeWithTag(CommonMenuAction.Search.testTag).assertIsDisplayed()

        composeTestRule.onNodeWithText(FILE_NAME, useUnmergedTree = true).performClick()

        composeTestRule.onNodeWithText(SELECTED_COUNT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CommonMenuAction.Search.testTag).assertDoesNotExist()
    }

    @Test
    fun `test that tapping the close button while searching clears the selection`() {
        setContent()
        composeTestRule.onNodeWithTag(CommonMenuAction.Search.testTag).performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextInput(QUERY)
        composeTestRule.onNodeWithText(FILE_NAME, useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag(ACTION_BUTTONS_VIEW_TAG).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(NAVIGATION_ICON).performClick()

        composeTestRule.onNodeWithText(SELECTED_COUNT).assertDoesNotExist()
    }

    private fun setContent() {
        val file = previewFileNodeUiItem(id = FILE_ID, name = FILE_NAME)
        val nodes = mock<NodesExplorerViewModel> {
            on { uiState } doReturn MutableStateFlow<NodeExplorerUiState>(
                nodeExplorerDataState(
                    items = listOf(file),
                    searchItems = listOf(file),
                    searchedQuery = QUERY,
                    searchLoadingState = NodesLoadingState.FullyLoaded,
                    isRoot = false,
                )
            )
        }
        val search = mock<ExplorerSearchViewModel> {
            on { uiState } doReturn MutableStateFlow(
                ExplorerSearchUiState.Data(debouncedQuery = QUERY, recentSearches = emptyList())
            )
        }
        val owner = viewModelStoreOwnerOf(
            NodesExplorerViewModel::class.java to nodes,
            ExplorerViewModel::class.java to stubExplorerViewModel(),
            ExplorerSearchViewModel::class.java to search,
        )

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
                        explorerMode = ExplorerMode.ShareFilesToChat,
                        startNavKey = CopyNavKey(emptyList()),
                        isInnerNavigation = true,
                        nodeExplorerId = NodeId(-1),
                        nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                        onCloseExplorerScreen = {},
                        onNavigateBack = {},
                        onNavigate = {},
                        isProcessingAction = false,
                    )
                }
            }
        }
    }

    private companion object {
        const val FILE_ID = 10L
        const val FILE_NAME = "Selected file"
        const val SELECTED_COUNT = "1"
        const val QUERY = "report"
        const val NAVIGATION_ICON = "Navigation Icon"
    }
}
