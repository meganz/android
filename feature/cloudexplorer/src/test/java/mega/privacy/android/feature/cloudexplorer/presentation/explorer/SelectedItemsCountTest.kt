package mega.privacy.android.feature.cloudexplorer.presentation.explorer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
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

    private fun setContent(
        explorerMode: ExplorerMode,
        items: List<NodeViewItem<TypedNode>>,
        disabledNodeIds: Set<NodeId> = emptySet(),
        onCloseExplorerScreen: () -> Unit = {},
        onNavigateBack: () -> Unit = {},
    ) {
        val nodes = mock<NodesExplorerViewModel> {
            on { uiState } doReturn MutableStateFlow<NodeExplorerUiState>(
                nodeExplorerDataState(items = items, isRoot = false)
            )
        }
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
    }
}
