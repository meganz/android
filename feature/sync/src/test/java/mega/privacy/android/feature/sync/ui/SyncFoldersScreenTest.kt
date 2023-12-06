package mega.privacy.android.feature.sync.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersRoute
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersState
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersViewModel
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.ui.views.TAG_SYNC_LIST_SCREEN_NO_ITEMS
import mega.privacy.android.feature.sync.ui.views.TEST_TAG_SYNC_ITEM_VIEW
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
@RunWith(AndroidJUnit4::class)
class SyncFoldersScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val viewModel: SyncFoldersViewModel = Mockito.mock()
    private val state: StateFlow<SyncFoldersState> = Mockito.mock()

    @Test
    fun `test that folders list is displayed when there are folders`() {
        val folderName = "Folder name"
        whenever(state.value).thenReturn(
            SyncFoldersState(
                listOf(
                    SyncUiItem(
                        id = 1L,
                        folderPairName = folderName,
                        status = SyncStatus.SYNCING,
                        hasStalledIssues = false,
                        deviceStoragePath = folderName,
                        megaStoragePath = folderName,
                        method = R.string.sync_two_way,
                        expanded = false
                    )
                )
            )
        )
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncFoldersRoute(
                viewModel = viewModel,
                addFolderClicked = {},
                issuesInfoClicked = {},
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_ITEM_VIEW)
            .assertIsDisplayed()
    }

    @Test
    fun `test that folders list is empty where there are no folders`() {
        whenever(state.value).thenReturn(SyncFoldersState(emptyList()))
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncFoldersRoute(
                viewModel = viewModel,
                addFolderClicked = {},
                issuesInfoClicked = {},
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_ITEM_VIEW)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAG_SYNC_LIST_SCREEN_NO_ITEMS)
            .assertIsDisplayed()
    }
}