package mega.privacy.android.feature.cloudexplorer.presentation.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodeExplorerUiState
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.nodeExplorerDataState
import mega.privacy.android.shared.nodes.components.previewdata.LocalNodeHeaderPreviewData
import mega.privacy.android.shared.nodes.components.previewdata.previewFolderNodeUiItem
import mega.privacy.android.shared.nodes.model.NodeHeaderItemUiState
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeViewItem
import mega.privacy.android.shared.nodes.selection.rememberNodeSelectionState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
internal class NodesExplorerSearchContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the empty results view is shown when the search returns no nodes`() {
        setContent(searchItems = emptyList())

        composeTestRule.onNodeWithTag(EXPLORER_SEARCH_EMPTY_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the search result nodes are shown`() {
        setContent(searchItems = listOf(previewFolderNodeUiItem(1L, name = FOLDER_NAME)))

        composeTestRule.onNodeWithTag(EXPLORER_SEARCH_EMPTY_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithText(FOLDER_NAME, useUnmergedTree = true).assertIsDisplayed()
    }

    private fun setContent(searchItems: List<NodeViewItem<TypedNode>>) {
        val searchViewModel = mock<ExplorerSearchViewModel> {
            on { uiState } doReturn MutableStateFlow(
                ExplorerSearchUiState.Data(debouncedQuery = QUERY, recentSearches = emptyList())
            )
        }
        val nodesViewModel = mock<NodesExplorerViewModel> {
            on { uiState } doReturn MutableStateFlow<NodeExplorerUiState>(
                nodeExplorerDataState(
                    searchItems = searchItems,
                    searchedQuery = QUERY,
                    searchLoadingState = NodesLoadingState.FullyLoaded,
                )
            )
        }
        composeTestRule.setContent {
            Content(
                viewModelStoreOwnerOf(
                    ExplorerSearchViewModel::class.java to searchViewModel,
                    NodesExplorerViewModel::class.java to nodesViewModel,
                )
            )
        }
    }

    @Composable
    private fun Content(owner: androidx.lifecycle.ViewModelStoreOwner) {
        AndroidThemeForPreviews {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides owner,
                LocalNodeHeaderPreviewData provides NodeHeaderItemUiState.Data(
                    viewType = ViewType.LIST,
                    nodeSortConfiguration = NodeSortConfiguration.default,
                ),
            ) {
                NodesExplorerSearchContent(
                    query = QUERY,
                    onQueryChanged = {},
                    nodeSelectionState = rememberNodeSelectionState(),
                    isFileSelectionEnabled = true,
                    videosOnly = false,
                    disabledNodeIds = emptySet(),
                    onNavigateToFolderPath = {},
                    onCloseSearch = {},
                    recentSearchesEnabled = true,
                )
            }
        }
    }

    private companion object {
        const val QUERY = "report"
        const val FOLDER_NAME = "Search folder"
    }
}
