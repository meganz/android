package mega.privacy.android.feature.cloudexplorer.presentation.picker

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.CLOUD_EXPLORER_VIEW_TAG
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.explorerViewModelStoreOwner
import mega.privacy.android.navigation.destination.CopyNavKey
import mega.privacy.android.navigation.destination.NodesExplorerNavKey
import mega.privacy.android.shared.nodes.components.previewdata.LocalNodeHeaderPreviewData
import mega.privacy.android.shared.nodes.model.NodeHeaderItemUiState
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TargetNodePickerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val analyticsRule = AnalyticsTestRule()

    private val viewModelStoreOwner = explorerViewModelStoreOwner()

    @Test
    fun `test that nothing is rendered while loading`() {
        setContent(uiState = TargetNodePickerUiState.Loading)

        composeTestRule.onNodeWithTag(CLOUD_EXPLORER_VIEW_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the explorer is rendered in the data state`() {
        setContent(uiState = TargetNodePickerUiState.Data(rootNodeId = NodeId(1L)))

        composeTestRule.onNodeWithTag(CLOUD_EXPLORER_VIEW_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the saved target path is pushed onto the back stack on first composition`() {
        val navigated = mutableListOf<List<NavKey>>()
        setContent(
            uiState = TargetNodePickerUiState.Data(
                rootNodeId = NodeId(1L),
                targetPath = listOf(NodeId(5L), NodeId(6L)),
            ),
            onNavigate = { navigated.add(it) },
        )

        val pushedPath = navigated.first().map { (it as NodesExplorerNavKey).nodeId }
        assertThat(pushedPath).containsExactly(NodeId(5L), NodeId(6L)).inOrder()
    }

    private fun setContent(
        uiState: TargetNodePickerUiState,
        onNavigate: (List<NavKey>) -> Unit = {},
    ) {
        composeTestRule.setContent {
            AndroidThemeForPreviews {
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides viewModelStoreOwner,
                    LocalNodeHeaderPreviewData provides NodeHeaderItemUiState.Data(
                        viewType = ViewType.LIST,
                        nodeSortConfiguration = NodeSortConfiguration.default,
                    ),
                ) {
                    TargetNodePickerScreen(
                        uiState = uiState,
                        startNavKey = CopyNavKey(emptyList()),
                        explorerMode = ExplorerMode.Copy,
                        onNavigateBack = {},
                        onNavigate = onNavigate,
                        onSelectFolder = {},
                    )
                }
            }
        }
    }
}
