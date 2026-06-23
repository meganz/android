package mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.triggered
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.shared.nodes.components.previewdata.LocalNodeHeaderPreviewData
import mega.privacy.android.shared.nodes.components.previewdata.previewFileNodeUiItem
import mega.privacy.android.shared.nodes.components.previewdata.previewFolderNodeUiItem
import mega.privacy.android.shared.nodes.model.NodeHeaderItemUiState
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class NodesExplorerScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the empty view is shown when the root folder has no nodes`() {
        setContent(uiState = nodeExplorerDataState(isRoot = true))

        composeTestRule.onNodeWithTag(NODES_EXPLORER_EMPTY_VIEW_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the empty view is shown when a sub folder has no nodes`() {
        setContent(uiState = nodeExplorerDataState(isRoot = false))

        composeTestRule.onNodeWithTag(NODES_EXPLORER_EMPTY_VIEW_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that nodes are shown and the empty view is hidden when there are nodes`() {
        setContent(
            uiState = nodeExplorerDataState(
                items = listOf(previewFolderNodeUiItem(1L, name = FOLDER_NAME))
                        + previewFileNodeUiItem(10L),
            ),
        )

        composeTestRule.onNodeWithTag(NODES_EXPLORER_EMPTY_VIEW_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithText(FOLDER_NAME, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that sensitive nodes are hidden when hidden nodes are enabled and not shown`() {
        setContent(
            uiState = nodeExplorerDataState(
                isHiddenNodesEnabled = true,
                showHiddenNodes = false,
                items = listOf(previewFolderNodeUiItem(1L, name = FOLDER_NAME).copy(isSensitive = true)),
            ),
        )

        composeTestRule.onNodeWithText(FOLDER_NAME, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `test that sensitive nodes are shown when the user opted to show hidden nodes`() {
        setContent(
            uiState = nodeExplorerDataState(
                isHiddenNodesEnabled = true,
                showHiddenNodes = true,
                items = listOf(previewFolderNodeUiItem(1L, name = FOLDER_NAME).copy(isSensitive = true)),
            ),
        )

        composeTestRule.onNodeWithText(FOLDER_NAME, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that sensitive nodes are shown when the hidden nodes feature is disabled`() {
        setContent(
            uiState = nodeExplorerDataState(
                isHiddenNodesEnabled = false,
                showHiddenNodes = false,
                items = listOf(previewFolderNodeUiItem(1L, name = FOLDER_NAME).copy(isSensitive = true)),
            ),
        )

        composeTestRule.onNodeWithText(FOLDER_NAME, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that nodes are rendered in grid view`() {
        setContent(
            uiState = nodeExplorerDataState(
                items = listOf(previewFolderNodeUiItem(1L, name = FOLDER_NAME)),
            ),
            viewType = ViewType.GRID,
        )

        composeTestRule.onNodeWithText(FOLDER_NAME, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that the navigate back event invokes the callback and is consumed`() {
        var navigatedBack = false
        var consumed = false
        setContent(
            uiState = nodeExplorerDataState(navigateBack = triggered),
            onNavigateBack = { navigatedBack = true },
            consumeNavigateBack = { consumed = true },
        )

        assertThat(navigatedBack).isTrue()
        assertThat(consumed).isTrue()
    }

    private fun setContent(
        uiState: NodeExplorerUiState,
        viewType: ViewType = ViewType.LIST,
        onNavigateBack: () -> Unit = {},
        consumeNavigateBack: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            AndroidThemeForPreviews {
                CompositionLocalProvider(
                    LocalNodeHeaderPreviewData provides NodeHeaderItemUiState.Data(
                        viewType = viewType,
                        nodeSortConfiguration = NodeSortConfiguration.default,
                    ),
                ) {
                    NodesExplorerScreenContent(
                        uiState = uiState,
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
        const val FOLDER_NAME = "Test folder"
    }
}
