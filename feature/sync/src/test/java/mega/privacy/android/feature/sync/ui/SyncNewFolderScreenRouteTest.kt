package mega.privacy.android.feature.sync.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderScreenRoute
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderState
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderViewModel
import mega.privacy.android.feature.sync.ui.newfolderpair.TAG_SYNC_NEW_FOLDER_SCREEN_SYNC_BUTTON
import mega.privacy.android.feature.sync.ui.newfolderpair.TAG_SYNC_NEW_FOLDER_SCREEN_TOOLBAR
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
class SyncNewFolderScreenRouteTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val viewModel: SyncNewFolderViewModel = mock()
    private val state: StateFlow<SyncNewFolderState> = mock()
    private val syncPermissionsManager: SyncPermissionsManager = mock()

    @Test
    fun `test that all sync Sync New Folder components are visible`() {
        whenever(state.value).thenReturn(SyncNewFolderState())
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncNewFolderScreenRoute(
                viewModel,
                syncPermissionsManager = syncPermissionsManager,
                openNextScreen = {},
                openSelectMegaFolderScreen = {},
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithTag(TAG_SYNC_NEW_FOLDER_SCREEN_TOOLBAR)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_folder_choose_device_folder_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_folders_choose_mega_folder_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_folders_method))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_two_way))
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_SYNC_NEW_FOLDER_SCREEN_SYNC_BUTTON)
            .assertIsDisplayed()
    }

    @Test
    fun `test that entered folder pair name is correctly displayed`() {
        val folderPairName = "some_folder_pair"
        whenever(state.value).thenReturn(SyncNewFolderState(folderPairName = folderPairName))
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncNewFolderScreenRoute(
                viewModel,
                syncPermissionsManager = syncPermissionsManager,
                openNextScreen = {},
                openSelectMegaFolderScreen = {},
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText(folderPairName)
            .assertIsDisplayed()
    }

    @Test
    fun `test that selected local folder name is correctly displayed`() {
        val localFolderName = "local_folder"
        whenever(state.value).thenReturn(SyncNewFolderState(selectedLocalFolder = localFolderName))
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncNewFolderScreenRoute(
                viewModel,
                syncPermissionsManager = syncPermissionsManager,
                openNextScreen = {},
                openSelectMegaFolderScreen = {},
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText(localFolderName)
            .assertIsDisplayed()
    }

    @Test
    fun `test that selected MEGA folder name is correctly displayed`() {
        val megaFolderName = "mega_folder"
        whenever(state.value).thenReturn(
            SyncNewFolderState(
                selectedMegaFolder = RemoteFolder(
                    id = 0,
                    megaFolderName
                )
            )
        )
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncNewFolderScreenRoute(
                viewModel,
                openNextScreen = {},
                syncPermissionsManager = syncPermissionsManager,
                openSelectMegaFolderScreen = {},
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText(megaFolderName)
            .assertIsDisplayed()
    }

    @Test
    fun `test that sync button is not clickable when local folder or mega folder are not filled`() {
        val emptyState = SyncNewFolderState()
        val openNextScreenCallback = mock<(SyncNewFolderState) -> Unit>()
        whenever(state.value).thenReturn(emptyState)
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncNewFolderScreenRoute(
                viewModel,
                syncPermissionsManager = syncPermissionsManager,
                openNextScreen = openNextScreenCallback,
                openSelectMegaFolderScreen = {},
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithTag(TAG_SYNC_NEW_FOLDER_SCREEN_SYNC_BUTTON)
            .performClick()

        verifyNoInteractions(openNextScreenCallback)
    }

    @Test
    fun `test that click on select mega folder button invokes openSelectMegaFolderScreen lambda`() {
        val emptyState = SyncNewFolderState()
        val openSelectMegaFolderScreenLambda = mock<() -> Unit>()
        whenever(state.value).thenReturn(emptyState)
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncNewFolderScreenRoute(
                viewModel,
                openNextScreen = {},
                syncPermissionsManager = syncPermissionsManager,
                openSelectMegaFolderScreen = openSelectMegaFolderScreenLambda,
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_folders_choose_mega_folder_title))
            .performClick()

        verify(openSelectMegaFolderScreenLambda).invoke()
    }

    @Test
    fun `test that only all files access banner is shown when all files access permission is not granted`() {
        val emptyState = SyncNewFolderState(selectedMegaFolder = RemoteFolder(0, ""))
        whenever(state.value).thenReturn(emptyState)
        whenever(viewModel.state).thenReturn(state)
        whenever(syncPermissionsManager.isManageExternalStoragePermissionGranted())
            .thenReturn(false)
        whenever(syncPermissionsManager.isDisableBatteryOptimizationGranted())
            .thenReturn(false)
        composeTestRule.setContent {
            SyncNewFolderScreenRoute(
                viewModel,
                openNextScreen = {},
                syncPermissionsManager = syncPermissionsManager,
                openSelectMegaFolderScreen = { },
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText("We need to access your device storage in order to sync your local folder. Click here to grant access.")
            .assertDoesNotExist()
    }
}