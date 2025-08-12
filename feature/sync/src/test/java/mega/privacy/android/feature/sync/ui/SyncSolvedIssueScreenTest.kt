package mega.privacy.android.feature.sync.ui

import mega.privacy.android.icon.pack.R as IconPackR
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.ui.model.SolvedIssueUiItem
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesRoute
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesState
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesViewModel
import mega.privacy.android.feature.sync.ui.views.TAG_SYNC_LIST_SCREEN_NO_ITEMS
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
class SyncSolvedIssueScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val viewModel: SyncSolvedIssuesViewModel = Mockito.mock()
    private val state: StateFlow<SyncSolvedIssuesState> = Mockito.mock()


    @Test
    fun `test that solved issues list is displayed with local folder name`() {
        val folderName = "Folder name"
        val folderPath = "/storage/emulated/0/Folder name"
        val resolutionExplanation = "Folders were merged"
        whenever(state.value).thenReturn(
            SyncSolvedIssuesState(
                listOf(
                    SolvedIssueUiItem(
                        nodeIds = listOf(),
                        nodeNames = listOf(folderName),
                        localPaths = listOf(folderPath),
                        resolutionExplanation = resolutionExplanation,
                        icon = IconPackR.drawable.ic_folder_medium_solid,
                    )
                )
            )
        )
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncSolvedIssuesRoute(
                viewModel,
            )
        }

        composeTestRule.onNodeWithText(folderName).assertIsDisplayed()
        composeTestRule.onNodeWithText(folderPath).assertIsDisplayed()
        composeTestRule.onNodeWithText(resolutionExplanation).assertIsDisplayed()
    }

    @Test
    fun `test that solved issues list is displayed with MEGA folder name`() {
        val folderName = "Folder name"
        val resolutionExplanation = "Folders were merged"
        whenever(state.value).thenReturn(
            SyncSolvedIssuesState(
                listOf(
                    SolvedIssueUiItem(
                        nodeIds = listOf(NodeId(1L)),
                        nodeNames = listOf(folderName),
                        localPaths = listOf(),
                        resolutionExplanation = resolutionExplanation,
                        icon = IconPackR.drawable.ic_folder_medium_solid,
                    )
                )
            )
        )
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncSolvedIssuesRoute(
                viewModel,
            )
        }

        composeTestRule.onNodeWithText(folderName).assertIsDisplayed()
        composeTestRule.onNodeWithText(resolutionExplanation).assertIsDisplayed()
    }

    @Test
    fun `test that solved issues list is empty where there are no solved issues`() {
        whenever(state.value).thenReturn(SyncSolvedIssuesState(emptyList()))
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncSolvedIssuesRoute(
                viewModel,
            )
        }

        composeTestRule.onNodeWithTag(TAG_SYNC_LIST_SCREEN_NO_ITEMS)
            .assertIsDisplayed()
    }
}