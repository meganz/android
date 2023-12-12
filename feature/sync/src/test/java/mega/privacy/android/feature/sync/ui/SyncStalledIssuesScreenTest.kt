package mega.privacy.android.feature.sync.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.core.R
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import mega.privacy.android.feature.sync.ui.model.StalledIssueDetailedInfo
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesRoute
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesState
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesViewModel
import mega.privacy.android.feature.sync.ui.views.TAG_SYNC_LIST_SCREEN_NO_ITEMS
import mega.privacy.android.feature.sync.ui.views.TEST_TAG_STALLED_ISSUE_CARD_TEXT_NODE_NAME
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
class SyncStalledIssuesScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val viewModel: SyncStalledIssuesViewModel = Mockito.mock()
    private val state: StateFlow<SyncStalledIssuesState> = Mockito.mock()

    @Test
    fun `test that stalled issues list is displayed when there are stalled issues`() {
        val folderName = "Folder name"
        whenever(state.value).thenReturn(
            SyncStalledIssuesState(
                listOf(
                    StalledIssueUiItem(
                        nodeIds = listOf(NodeId(1L)),
                        localPaths = listOf(folderName),
                        issueType = StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose,
                        conflictName = "Names conflict",
                        nodeNames = listOf(folderName),
                        icon = R.drawable.ic_folder_list,
                        detailedInfo = StalledIssueDetailedInfo("", ""),
                        actions = listOf(
                            StalledIssueResolutionAction(
                                "choose local",
                                StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE
                            )
                        )
                    )
                )
            )
        )
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncStalledIssuesRoute(
                {},
                {},
                viewModel = viewModel,
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_STALLED_ISSUE_CARD_TEXT_NODE_NAME)
            .assertIsDisplayed()
    }

    @Test
    fun `test that stalled issues list is empty where there are no stalled issues`() {
        whenever(state.value).thenReturn(SyncStalledIssuesState(emptyList()))
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncStalledIssuesRoute(
                {},
                {},
                viewModel = viewModel,
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_STALLED_ISSUE_CARD_TEXT_NODE_NAME)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAG_SYNC_LIST_SCREEN_NO_ITEMS)
            .assertIsDisplayed()
    }
}
