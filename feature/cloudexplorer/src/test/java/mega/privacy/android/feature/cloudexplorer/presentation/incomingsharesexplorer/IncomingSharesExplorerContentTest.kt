package mega.privacy.android.feature.cloudexplorer.presentation.incomingsharesexplorer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.triggered
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NODES_EXPLORER_EMPTY_VIEW_TAG
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodeExplorerUiState
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.nodeExplorerDataState
import mega.privacy.android.shared.nodes.components.previewdata.LocalNodeHeaderPreviewData
import mega.privacy.android.shared.nodes.components.previewdata.previewIncomingShareFolderNodeUiItem
import mega.privacy.android.shared.nodes.model.NodeHeaderItemUiState
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class IncomingSharesExplorerContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the empty view is shown when there are no incoming shares`() {
        setContent(
            uiState = nodeExplorerDataState(nodeSourceType = NodeSourceType.INCOMING_SHARES),
        )

        composeTestRule.onNodeWithTag(NODES_EXPLORER_EMPTY_VIEW_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that incoming shares are shown and the empty view is hidden when there are nodes`() {
        setContent(
            uiState = nodeExplorerDataState(
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
                items = listOf(previewIncomingShareFolderNodeUiItem(1L, name = FOLDER_NAME)),
            ),
        )

        composeTestRule.onNodeWithTag(NODES_EXPLORER_EMPTY_VIEW_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithText(FOLDER_NAME, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that clicking a folder with write access navigates into it`() {
        var clickedFolderId: NodeId? = null
        setContent(
            uiState = nodeExplorerDataState(
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
                items = listOf(
                    previewIncomingShareFolderNodeUiItem(
                        id = 1L,
                        name = FOLDER_NAME,
                        access = AccessPermission.READWRITE,
                    ),
                ),
            ),
            onFolderClick = { clickedFolderId = it },
        )

        composeTestRule.onNodeWithText(FOLDER_NAME, useUnmergedTree = true).performClick()

        assertThat(clickedFolderId).isEqualTo(NodeId(1L))
    }

    @Test
    fun `test that clicking a read only folder does not navigate into it`() {
        var clickedFolderId: NodeId? = null
        setContent(
            uiState = nodeExplorerDataState(
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
                items = listOf(
                    previewIncomingShareFolderNodeUiItem(
                        id = 1L,
                        name = FOLDER_NAME,
                        access = AccessPermission.READ,
                    ),
                ),
            ),
            onFolderClick = { clickedFolderId = it },
        )

        composeTestRule.onNodeWithText(FOLDER_NAME, useUnmergedTree = true).performClick()

        assertThat(clickedFolderId).isNull()
    }

    @Test
    fun `test that the navigate back event invokes the callback and is consumed`() {
        var navigatedBack = false
        var consumed = false
        setContent(
            uiState = nodeExplorerDataState(
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
                navigateBack = triggered,
            ),
            onNavigateBack = { navigatedBack = true },
            consumeNavigateBack = { consumed = true },
        )

        assertThat(navigatedBack).isTrue()
        assertThat(consumed).isTrue()
    }

    @Test
    fun `test that clicking a read write folder does not navigate when full access is required`() {
        var clickedFolderId: NodeId? = null
        setContent(
            uiState = nodeExplorerDataState(
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
                items = listOf(
                    previewIncomingShareFolderNodeUiItem(
                        id = 1L,
                        name = FOLDER_NAME,
                        access = AccessPermission.READWRITE,
                    ),
                ),
            ),
            requiresFullAccessShares = true,
            onFolderClick = { clickedFolderId = it },
        )

        composeTestRule.onNodeWithText(FOLDER_NAME, useUnmergedTree = true).performClick()

        assertThat(clickedFolderId).isNull()
    }

    @Test
    fun `test that clicking a full access folder navigates when full access is required`() {
        var clickedFolderId: NodeId? = null
        setContent(
            uiState = nodeExplorerDataState(
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
                items = listOf(
                    previewIncomingShareFolderNodeUiItem(
                        id = 1L,
                        name = FOLDER_NAME,
                        access = AccessPermission.FULL,
                    ),
                ),
            ),
            requiresFullAccessShares = true,
            onFolderClick = { clickedFolderId = it },
        )

        composeTestRule.onNodeWithText(FOLDER_NAME, useUnmergedTree = true).performClick()

        assertThat(clickedFolderId).isEqualTo(NodeId(1L))
    }

    private fun setContent(
        uiState: NodeExplorerUiState,
        onFolderClick: (NodeId) -> Unit = {},
        onNavigateBack: () -> Unit = {},
        consumeNavigateBack: () -> Unit = {},
        requiresFullAccessShares: Boolean = false,
    ) {
        composeTestRule.setContent {
            AndroidThemeForPreviews {
                CompositionLocalProvider(
                    LocalNodeHeaderPreviewData provides NodeHeaderItemUiState.Data(
                        viewType = ViewType.LIST,
                        nodeSortConfiguration = NodeSortConfiguration.default,
                    ),
                ) {
                    IncomingSharesExplorerContent(
                        uiState = uiState,
                        onNavigateBack = onNavigateBack,
                        consumeNavigateBack = consumeNavigateBack,
                        onFolderClick = onFolderClick,
                        onRefreshNodes = {},
                        requiresFullAccessShares = requiresFullAccessShares,
                    )
                }
            }
        }
    }

    private companion object {
        const val FOLDER_NAME = "Test shared folder"
    }
}
