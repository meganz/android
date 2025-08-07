package mega.privacy.android.feature.sync.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderScreenRoute
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderState
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderViewModel
import mega.privacy.android.feature.sync.ui.newfolderpair.TAG_SYNC_NEW_FOLDER_SCREEN_SYNC_BUTTON
import mega.privacy.android.feature.sync.ui.newfolderpair.TAG_SYNC_NEW_FOLDER_SCREEN_TOOLBAR
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.feature.sync.ui.views.SELECT_DEVICE_FOLDER_OPTION_TEST_TAG
import mega.privacy.android.shared.original.core.ui.controls.appbar.APP_BAR_BACK_BUTTON_TAG
import mega.privacy.android.shared.resources.R as sharedResR
import mega.privacy.mobile.analytics.event.AndroidSyncSelectDeviceFolderButtonPressedEvent
import mega.privacy.mobile.analytics.event.SyncNewFolderScreenBackNavigationEvent
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
internal class SyncNewFolderScreenRouteTest {

    private val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsTestRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsTestRule).around(composeTestRule)

    private val viewModel: SyncNewFolderViewModel = mock()
    private val state: StateFlow<SyncNewFolderState> = mock()
    private val syncPermissionsManager: SyncPermissionsManager = mock()

    @Test
    fun `test that all sync Sync New Folder components are visible (Sync Type = TYPE_TWOWAY)`() {
        whenever(state.value).thenReturn(
            SyncNewFolderState(
                syncType = SyncType.TYPE_TWOWAY,
                deviceName = "Device Name",
            )
        )
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncNewFolderScreenRoute(
                viewModel,
                syncPermissionsManager = syncPermissionsManager,
                openNextScreen = {},
                openSelectMegaFolderScreen = {},
                openUpgradeAccount = {},
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithTag(TAG_SYNC_NEW_FOLDER_SCREEN_TOOLBAR)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedResR.string.sync_add_new_sync_folder_header_text))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_two_way))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_folder_choose_device_folder_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_folders_choose_mega_folder_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_SYNC_NEW_FOLDER_SCREEN_SYNC_BUTTON)
            .assertIsDisplayed()
    }

    @Test
    fun `test that all sync Sync New Folder components are visible (Sync Type = TYPE_BACKUP)`() {
        whenever(state.value).thenReturn(
            SyncNewFolderState(
                syncType = SyncType.TYPE_BACKUP,
                deviceName = "Device Name",
            )
        )
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncNewFolderScreenRoute(
                viewModel,
                syncPermissionsManager = syncPermissionsManager,
                openNextScreen = {},
                openSelectMegaFolderScreen = {},
                openUpgradeAccount = {},
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithTag(TAG_SYNC_NEW_FOLDER_SCREEN_TOOLBAR)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedResR.string.sync_add_new_backup_header_text))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedResR.string.sync_add_new_backup_choose_device_folder_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedResR.string.sync_add_new_backup_choose_mega_folder_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_SYNC_NEW_FOLDER_SCREEN_SYNC_BUTTON)
            .assertIsDisplayed()
    }

    @Test
    fun `test that selected local folder name is correctly displayed (Sync Type = TYPE_TWOWAY)`() {
        val localFolderName = "local_folder"
        whenever(state.value).thenReturn(
            SyncNewFolderState(
                syncType = SyncType.TYPE_TWOWAY,
                deviceName = "Device Name",
                selectedLocalFolder = localFolderName,
                selectedFolderName = localFolderName,
            )
        )
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncNewFolderScreenRoute(
                viewModel,
                syncPermissionsManager = syncPermissionsManager,
                openNextScreen = {},
                openSelectMegaFolderScreen = {},
                openUpgradeAccount = {},
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText(localFolderName)
            .assertIsDisplayed()
    }

    @Test
    fun `test that selected local folder name is correctly displayed (Sync Type = TYPE_BACKUP)`() {
        val localFolderName = "local_folder"
        whenever(state.value).thenReturn(
            SyncNewFolderState(
                syncType = SyncType.TYPE_BACKUP,
                deviceName = "Device Name",
                selectedLocalFolder = localFolderName,
                selectedFolderName = localFolderName,
            )
        )
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncNewFolderScreenRoute(
                viewModel,
                syncPermissionsManager = syncPermissionsManager,
                openNextScreen = {},
                openSelectMegaFolderScreen = {},
                openUpgradeAccount = {},
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
                syncType = SyncType.TYPE_TWOWAY,
                deviceName = "Device Name",
                selectedMegaFolder = RemoteFolder(
                    id = NodeId(0L), megaFolderName
                ),
            )
        )
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncNewFolderScreenRoute(
                viewModel,
                openNextScreen = {},
                syncPermissionsManager = syncPermissionsManager,
                openSelectMegaFolderScreen = {},
                openUpgradeAccount = {},
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText(megaFolderName)
            .assertIsDisplayed()
    }

    @Test
    fun `test that sync button is not clickable when local folder or mega folder are not filled (Sync Type = TYPE_TWOWAY)`() {
        val emptyState = SyncNewFolderState(
            syncType = SyncType.TYPE_TWOWAY,
            deviceName = "Device Name",
        )
        val openNextScreenCallback = mock<(SyncNewFolderState) -> Unit>()
        whenever(state.value).thenReturn(emptyState)
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncNewFolderScreenRoute(
                viewModel,
                syncPermissionsManager = syncPermissionsManager,
                openNextScreen = openNextScreenCallback,
                openSelectMegaFolderScreen = {},
                openUpgradeAccount = {},
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithTag(TAG_SYNC_NEW_FOLDER_SCREEN_SYNC_BUTTON)
            .performClick()

        verifyNoInteractions(openNextScreenCallback)
    }

    @Test
    fun `test that sync button is not clickable when local folder or mega folder are not filled (Sync Type = TYPE_BACKUP)`() {
        val emptyState = SyncNewFolderState(
            syncType = SyncType.TYPE_BACKUP,
            deviceName = "Device Name",
        )
        val openNextScreenCallback = mock<(SyncNewFolderState) -> Unit>()
        whenever(state.value).thenReturn(emptyState)
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncNewFolderScreenRoute(
                viewModel,
                syncPermissionsManager = syncPermissionsManager,
                openNextScreen = openNextScreenCallback,
                openSelectMegaFolderScreen = {},
                openUpgradeAccount = {},
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithTag(TAG_SYNC_NEW_FOLDER_SCREEN_SYNC_BUTTON)
            .performClick()

        verifyNoInteractions(openNextScreenCallback)
    }

    @Test
    fun `test that click on select mega folder button invokes openSelectMegaFolderScreen lambda`() {
        val emptyState = SyncNewFolderState(
            syncType = SyncType.TYPE_TWOWAY,
            deviceName = "Device Name",
        )
        val openSelectMegaFolderScreenLambda = mock<() -> Unit>()
        whenever(state.value).thenReturn(emptyState)
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncNewFolderScreenRoute(
                viewModel,
                openNextScreen = {},
                syncPermissionsManager = syncPermissionsManager,
                openSelectMegaFolderScreen = openSelectMegaFolderScreenLambda,
                openUpgradeAccount = {},
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_folders_choose_mega_folder_title))
            .performClick()

        verify(openSelectMegaFolderScreenLambda).invoke()
    }

    @Test
    fun `test that click the app bar back button sends the right analytics tracker event (Sync Type = TYPE_TWOWAY)`() {
        whenever(state.value).thenReturn(
            SyncNewFolderState(
                syncType = SyncType.TYPE_TWOWAY,
                deviceName = "Device Name",
            )
        )
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncNewFolderScreenRoute(
                viewModel,
                syncPermissionsManager = syncPermissionsManager,
                openNextScreen = {},
                openSelectMegaFolderScreen = {},
                openUpgradeAccount = {},
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithTag(APP_BAR_BACK_BUTTON_TAG).assertExists().assertIsDisplayed()
            .performClick()
        assertThat(analyticsTestRule.events).contains(SyncNewFolderScreenBackNavigationEvent)
    }

    @Test
    fun `test that click the app bar back button sends the right analytics tracker event (Sync Type = TYPE_BACKUP)`() {
        whenever(state.value).thenReturn(
            SyncNewFolderState(
                syncType = SyncType.TYPE_BACKUP,
                deviceName = "Device Name",
            )
        )
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncNewFolderScreenRoute(
                viewModel,
                syncPermissionsManager = syncPermissionsManager,
                openNextScreen = {},
                openSelectMegaFolderScreen = {},
                openUpgradeAccount = {},
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithTag(APP_BAR_BACK_BUTTON_TAG).assertExists().assertIsDisplayed()
            .performClick()
        assertThat(analyticsTestRule.events).contains(SyncNewFolderScreenBackNavigationEvent)
    }

    @Test
    fun `test that click on select device folder option send the right analytics tracker event`() {
        whenever(state.value).thenReturn(
            SyncNewFolderState(
                syncType = SyncType.TYPE_BACKUP,
                deviceName = "Device Name",
            )
        )
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncNewFolderScreenRoute(
                viewModel,
                syncPermissionsManager = syncPermissionsManager,
                openNextScreen = {},
                openSelectMegaFolderScreen = {},
                openUpgradeAccount = {},
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithTag(SELECT_DEVICE_FOLDER_OPTION_TEST_TAG).assertExists()
            .assertIsDisplayed().performClick()
        assertThat(analyticsTestRule.events).contains(
            AndroidSyncSelectDeviceFolderButtonPressedEvent
        )
    }

    @Test
    fun `test that SyncStorageQuotaExceedWarning is displayed when storage quota is exceeded`() {
        whenever(state.value).thenReturn(
            SyncNewFolderState(
                syncType = SyncType.TYPE_TWOWAY,
                deviceName = "Device Name",
                isStorageOverQuota = true
            )
        )
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            SyncNewFolderScreenRoute(
                viewModel,
                syncPermissionsManager = syncPermissionsManager,
                openNextScreen = {},
                openSelectMegaFolderScreen = {},
                openUpgradeAccount = {},
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedResR.string.sync_error_storage_over_quota_banner_title))
            .assertIsDisplayed()
    }
}
