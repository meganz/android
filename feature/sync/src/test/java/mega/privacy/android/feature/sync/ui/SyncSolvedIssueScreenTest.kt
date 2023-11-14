package mega.privacy.android.feature.sync.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.core.R
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.ui.model.SolvedIssueUiItem
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SOLVED_ISSUES_MENU_ACTION_NODE_HEADER_WITH_BODY
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
    fun `test that solved issues list is displayed when there are solved issues`() {
        val folderName = "Folder name"
        val resolutionExplanation = "Folders were merged"
        whenever(state.value).thenReturn(
            SyncSolvedIssuesState(
                listOf(
                    SolvedIssueUiItem(
                        nodeIds = listOf(NodeId(1L)),
                        localPaths = listOf(folderName),
                        resolutionExplanation = resolutionExplanation,
                        icon = R.drawable.ic_folder_list,
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

        composeTestRule.onNodeWithTag(SOLVED_ISSUES_MENU_ACTION_NODE_HEADER_WITH_BODY)
            .assertIsDisplayed()
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

        composeTestRule.onNodeWithTag(SOLVED_ISSUES_MENU_ACTION_NODE_HEADER_WITH_BODY)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAG_SYNC_LIST_SCREEN_NO_ITEMS)
            .assertIsDisplayed()
    }
}