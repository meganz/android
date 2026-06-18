package mega.privacy.android.feature.cloudexplorer.presentation.favouritesexplorer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.triggered
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NODES_EXPLORER_EMPTY_VIEW_TAG
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerSharedUiState
import mega.privacy.android.shared.nodes.components.previewdata.LocalNodeHeaderPreviewData
import mega.privacy.android.shared.nodes.components.previewdata.previewFolderNodeUiItem
import mega.privacy.android.shared.nodes.model.NodeHeaderItemUiState
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class FavouritesExplorerContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the empty view is shown when there are no favourites in file picker mode`() {
        setContent(
            isFolderPicker = false,
            uiStateShared = NodesExplorerSharedUiState(
                nodesLoadingState = NodesLoadingState.FullyLoaded,
            ),
        )

        composeTestRule.onNodeWithTag(NODES_EXPLORER_EMPTY_VIEW_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the empty view is shown when there are no favourites in folder picker mode`() {
        setContent(
            isFolderPicker = true,
            uiStateShared = NodesExplorerSharedUiState(
                nodesLoadingState = NodesLoadingState.FullyLoaded,
            ),
        )

        composeTestRule.onNodeWithTag(NODES_EXPLORER_EMPTY_VIEW_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that favourites are shown and the empty view is hidden when there are nodes`() {
        setContent(
            isFolderPicker = false,
            uiStateShared = NodesExplorerSharedUiState(
                nodesLoadingState = NodesLoadingState.FullyLoaded,
                items = listOf(previewFolderNodeUiItem(1L, name = FOLDER_NAME)),
            ),
        )

        composeTestRule.onNodeWithTag(NODES_EXPLORER_EMPTY_VIEW_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithText(FOLDER_NAME, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that the navigate back event invokes the callback and is consumed`() {
        var navigatedBack = false
        var consumed = false
        setContent(
            isFolderPicker = false,
            uiStateShared = NodesExplorerSharedUiState(
                nodesLoadingState = NodesLoadingState.FullyLoaded,
                navigateBack = triggered,
            ),
            onNavigateBack = { navigatedBack = true },
            consumeNavigateBack = { consumed = true },
        )

        assertThat(navigatedBack).isTrue()
        assertThat(consumed).isTrue()
    }

    private fun setContent(
        isFolderPicker: Boolean,
        uiStateShared: NodesExplorerSharedUiState,
        onNavigateBack: () -> Unit = {},
        consumeNavigateBack: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            AndroidThemeForPreviews {
                CompositionLocalProvider(
                    LocalNodeHeaderPreviewData provides NodeHeaderItemUiState.Data(
                        viewType = ViewType.LIST,
                        nodeSortConfiguration = NodeSortConfiguration.default,
                    ),
                ) {
                    FavouritesExplorerContent(
                        uiStateShared = uiStateShared,
                        isFolderPicker = isFolderPicker,
                        onNavigateBack = onNavigateBack,
                        consumeNavigateBack = consumeNavigateBack,
                        onFolderClick = {},
                        onRefreshNodes = {},
                    )
                }
            }
        }
    }

    private companion object {
        const val FOLDER_NAME = "Test favourite folder"
    }
}
