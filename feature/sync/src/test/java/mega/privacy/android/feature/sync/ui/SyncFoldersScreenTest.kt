package mega.privacy.android.feature.sync.ui

import mega.privacy.android.shared.resources.R as sharedResR
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.core.test.AnalyticsTestRule
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.feature.sync.ui.stopbackup.STOP_BACKUP_CONFIRMATION_DIALOG_BODY_TEST_TAG
import mega.privacy.android.feature.sync.ui.stopbackup.STOP_BACKUP_CONFIRMATION_DIALOG_DELETE_OPTION_ROW_TEST_TAG
import mega.privacy.android.feature.sync.ui.stopbackup.STOP_BACKUP_CONFIRMATION_DIALOG_MOVE_OPTION_ROW_TEST_TAG
import mega.privacy.android.feature.sync.ui.stopbackup.StopBackupConfirmationDialogBody
import mega.privacy.android.feature.sync.ui.stopbackup.model.StopBackupState
import mega.privacy.android.feature.sync.ui.synclist.folders.STOP_SYNC_CONFIRM_DIALOG_TEST_TAG

import mega.privacy.android.feature.sync.ui.synclist.folders.StopSyncConfirmDialog
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersRoute
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersScreen
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersUiState
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersViewModel
import mega.privacy.android.feature.sync.ui.synclist.folders.TEST_TAG_SYNC_LIST_SCREEN_EMPTY_STATUS_BUTTON
import mega.privacy.android.feature.sync.ui.synclist.folders.TEST_TAG_SYNC_LIST_SCREEN_EMPTY_STATUS_TEXT_FOR_FREE_ACCOUNTS
import mega.privacy.android.feature.sync.ui.synclist.folders.TEST_TAG_SYNC_LIST_SCREEN_LOADING_STATE
import mega.privacy.android.feature.sync.ui.views.TAG_SYNC_LIST_SCREEN_NO_ITEMS
import mega.privacy.android.feature.sync.ui.views.TEST_TAG_SYNC_ITEM_VIEW
import mega.privacy.android.shared.original.core.ui.controls.dialogs.internal.CANCEL_TAG
import mega.privacy.android.shared.original.core.ui.controls.dialogs.internal.CONFIRM_TAG
import mega.privacy.android.shared.original.core.ui.controls.dialogs.internal.TITLE_TAG
import mega.privacy.mobile.analytics.event.SyncCardExpandedEvent
import mega.privacy.mobile.analytics.event.SyncFoldersListDisplayedEvent
import mega.privacy.mobile.analytics.event.SyncListEmptyStateUpgradeButtonPressedEvent
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
@RunWith(AndroidJUnit4::class)
class SyncFoldersScreenTest {

    private val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val viewModel: SyncFoldersViewModel = Mockito.mock()
    private val state: StateFlow<SyncFoldersUiState> = Mockito.mock()

    @Test
    fun `test that folders list is displayed when there are folders`() {
        val folderName = "Folder name"
        val syncFoldersUiState = SyncFoldersUiState(
            listOf(
                SyncUiItem(
                    id = 1L,
                    syncType = SyncType.TYPE_TWOWAY,
                    folderPairName = folderName,
                    status = SyncStatus.SYNCING,
                    hasStalledIssues = false,
                    deviceStoragePath = folderName,
                    megaStoragePath = folderName,
                    megaStorageNodeId = NodeId(1234L),
                    expanded = false
                )
            )
        )
        whenever(state.value).thenReturn(
            syncFoldersUiState
        )
        whenever(viewModel.uiState).thenReturn(state)
        composeTestRule.setContent {
            SyncFoldersRoute(
                viewModel = viewModel,
                onSelectStopBackupDestinationClicked = {},
                addFolderClicked = {},
                upgradeAccountClicked = {},
                issuesInfoClicked = {},
                uiState = syncFoldersUiState,
                snackBarHostState = SnackbarHostState(),
                deviceName = "Device Name",
                isBackupForAndroidEnabled = false,
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_ITEM_VIEW).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_SYNC_LIST_SCREEN_NO_ITEMS).assertDoesNotExist()
    }

    @Test
    fun `test that folders list empty state is properly displayed when there are no synced folders (free account)`() {
        whenever(state.value).thenReturn(SyncFoldersUiState(emptyList()))
        whenever(viewModel.uiState).thenReturn(state)
        composeTestRule.setContent {
            SyncFoldersRoute(
                viewModel = viewModel,
                addFolderClicked = {},
                onSelectStopBackupDestinationClicked = {},
                upgradeAccountClicked = {},
                issuesInfoClicked = {},
                uiState = SyncFoldersUiState(emptyList()),
                snackBarHostState = SnackbarHostState(),
                deviceName = "Device Name",
                isBackupForAndroidEnabled = false,
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_ITEM_VIEW)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAG_SYNC_LIST_SCREEN_NO_ITEMS)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_EMPTY_STATUS_TEXT_FOR_FREE_ACCOUNTS)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_EMPTY_STATUS_BUTTON)
            .assertIsDisplayed()
            .assertTextEquals(context.getString(sharedResR.string.general_upgrade_now_label))
    }

    @Test
    fun `test that folders list empty state is properly displayed when backup feature is enabled (free account)`() {
        whenever(state.value).thenReturn(SyncFoldersUiState(emptyList()))
        whenever(viewModel.uiState).thenReturn(state)
        composeTestRule.setContent {
            SyncFoldersRoute(
                viewModel = viewModel,
                addFolderClicked = {},
                onSelectStopBackupDestinationClicked = {},
                upgradeAccountClicked = {},
                issuesInfoClicked = {},
                uiState = SyncFoldersUiState(emptyList()),
                snackBarHostState = SnackbarHostState(),
                deviceName = "Device Name",
                isBackupForAndroidEnabled = true,
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_ITEM_VIEW)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAG_SYNC_LIST_SCREEN_NO_ITEMS)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_EMPTY_STATUS_TEXT_FOR_FREE_ACCOUNTS)
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_EMPTY_STATUS_BUTTON)
            .assertIsDisplayed()
            .assertTextEquals(context.getString(sharedResR.string.device_center_sync_backup_see_upgrade_options_button_label))
    }

    @Test
    fun `test that folders list empty state is properly displayed when there are no synced folders (non free account)`() {
        whenever(state.value).thenReturn(
            SyncFoldersUiState(
                syncUiItems = emptyList(), isFreeAccount = false
            )
        )
        whenever(viewModel.uiState).thenReturn(state)
        composeTestRule.setContent {
            SyncFoldersRoute(
                viewModel = viewModel,
                addFolderClicked = {},
                onSelectStopBackupDestinationClicked = {},
                upgradeAccountClicked = {},
                issuesInfoClicked = {},
                uiState = SyncFoldersUiState(
                    syncUiItems = emptyList(), isFreeAccount = false
                ),
                snackBarHostState = SnackbarHostState(),
                deviceName = "Device Name",
                isBackupForAndroidEnabled = false,
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_ITEM_VIEW).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAG_SYNC_LIST_SCREEN_NO_ITEMS).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_EMPTY_STATUS_TEXT_FOR_FREE_ACCOUNTS)
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_EMPTY_STATUS_BUTTON)
            .assertIsDisplayed()
            .assertTextEquals(context.getString(sharedResR.string.device_center_sync_add_new_syn_button_option))
    }

    @Test
    fun `test that folders list empty state is properly displayed when backup feature is enabled (non free account)`() {
        whenever(state.value).thenReturn(
            SyncFoldersUiState(
                syncUiItems = emptyList(), isFreeAccount = false
            )
        )
        whenever(viewModel.uiState).thenReturn(state)
        composeTestRule.setContent {
            SyncFoldersRoute(
                viewModel = viewModel,
                addFolderClicked = {},
                onSelectStopBackupDestinationClicked = {},
                upgradeAccountClicked = {},
                issuesInfoClicked = {},
                uiState = SyncFoldersUiState(
                    syncUiItems = emptyList(), isFreeAccount = false
                ),
                snackBarHostState = SnackbarHostState(),
                deviceName = "Device Name",
                isBackupForAndroidEnabled = true,
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_ITEM_VIEW).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAG_SYNC_LIST_SCREEN_NO_ITEMS).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_EMPTY_STATUS_TEXT_FOR_FREE_ACCOUNTS)
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_EMPTY_STATUS_BUTTON)
            .assertDoesNotExist()
    }

    @Test
    fun `test that click the empty state button on a free account sends the right analytics tracker event`() {
        whenever(state.value).thenReturn(SyncFoldersUiState(emptyList()))
        whenever(viewModel.uiState).thenReturn(state)
        composeTestRule.setContent {
            SyncFoldersRoute(
                viewModel = viewModel,
                addFolderClicked = {},
                onSelectStopBackupDestinationClicked = {},
                upgradeAccountClicked = {},
                issuesInfoClicked = {},
                uiState = SyncFoldersUiState(emptyList()),
                snackBarHostState = SnackbarHostState(),
                deviceName = "Device Name",
                isBackupForAndroidEnabled = false,
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_EMPTY_STATUS_BUTTON).performClick()
        assertThat(analyticsRule.events).contains(SyncListEmptyStateUpgradeButtonPressedEvent)
    }

    @Test
    fun `test that click the empty state button on a non free account doesn't send any analytics tracker event`() {
        whenever(state.value).thenReturn(
            SyncFoldersUiState(
                syncUiItems = emptyList(), isFreeAccount = false
            )
        )
        whenever(viewModel.uiState).thenReturn(state)
        composeTestRule.setContent {
            SyncFoldersRoute(
                viewModel = viewModel,
                addFolderClicked = {},
                onSelectStopBackupDestinationClicked = {},
                upgradeAccountClicked = {},
                issuesInfoClicked = {},
                uiState = SyncFoldersUiState(
                    syncUiItems = emptyList(), isFreeAccount = false
                ),
                snackBarHostState = SnackbarHostState(),
                deviceName = "Device Name",
                isBackupForAndroidEnabled = false,
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_EMPTY_STATUS_BUTTON).performClick()
        assertThat(analyticsRule.events).isEmpty()
    }

    @Test
    fun `test that the loading screen is shown`() {
        composeTestRule.setContent {
            SyncFoldersScreen(
                syncUiItems = emptyList(),
                cardExpanded = {},
                pauseRunClicked = {},
                removeFolderClicked = {},
                addFolderClicked = {},
                upgradeAccountClicked = {},
                issuesInfoClicked = {},
                onOpenDeviceFolderClicked = {},
                onOpenMegaFolderClicked = {},
                isLowBatteryLevel = false,
                isFreeAccount = false,
                isLoading = true,
                showSyncsPausedErrorDialog = false,
                onShowSyncsPausedErrorDialogDismissed = {},
                deviceName = "Device Name",
                isBackupForAndroidEnabled = false,
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_LOADING_STATE).assertIsDisplayed()
    }

    @Test
    fun `test that stop sync confirm dialog is properly displayed `() {
        composeTestRule.setContent {
            StopSyncConfirmDialog(
                onConfirm = {},
                onDismiss = {},
            )
        }

        composeTestRule.onNodeWithTag(STOP_SYNC_CONFIRM_DIALOG_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedResR.string.sync_stop_sync_confirm_dialog_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedResR.string.sync_stop_sync_confirm_dialog_message))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedResR.string.sync_stop_sync_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedResR.string.general_dialog_cancel_button))
            .assertIsDisplayed()
    }

    @Test
    fun `test that stop backup confirm dialog is properly displayed `() {
        composeTestRule.setContent {
            StopBackupConfirmationDialogBody(
                state = StopBackupState(),
                onConfirm = { _, _ -> },
                onDismiss = {},
                onSelectStopBackupDestinationClicked = {},
            )
        }
        composeTestRule.onNodeWithTag(TITLE_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(STOP_BACKUP_CONFIRMATION_DIALOG_BODY_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(STOP_BACKUP_CONFIRMATION_DIALOG_MOVE_OPTION_ROW_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(STOP_BACKUP_CONFIRMATION_DIALOG_DELETE_OPTION_ROW_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(CANCEL_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CONFIRM_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that display the list of Sync Folders send the right analytics tracker event`() {
        composeTestRule.setContent {
            SyncFoldersScreen(
                syncUiItems = listOf(
                    SyncUiItem(
                        id = 1L,
                        syncType = SyncType.TYPE_TWOWAY,
                        folderPairName = "Sync Name",
                        status = SyncStatus.SYNCING,
                        deviceStoragePath = "Device Path",
                        hasStalledIssues = false,
                        megaStoragePath = "MEGA Path",
                        megaStorageNodeId = NodeId(1111L),
                        expanded = false,
                    ),
                    SyncUiItem(
                        id = 2L,
                        syncType = SyncType.TYPE_BACKUP,
                        folderPairName = "Backup Name",
                        status = SyncStatus.SYNCING,
                        deviceStoragePath = "Device Path",
                        hasStalledIssues = false,
                        megaStoragePath = "MEGA Path",
                        megaStorageNodeId = NodeId(2222L),
                        expanded = false,
                    )
                ),
                cardExpanded = {},
                pauseRunClicked = {},
                removeFolderClicked = {},
                addFolderClicked = {},
                upgradeAccountClicked = {},
                issuesInfoClicked = {},
                onOpenDeviceFolderClicked = {},
                onOpenMegaFolderClicked = {},
                isLowBatteryLevel = false,
                isFreeAccount = false,
                isLoading = false,
                showSyncsPausedErrorDialog = false,
                onShowSyncsPausedErrorDialogDismissed = {},
                deviceName = "Device Name",
                isBackupForAndroidEnabled = false,
            )
        }
        assertThat(analyticsRule.events).contains(SyncFoldersListDisplayedEvent)
    }

    @Test
    fun `test that expand a Sync card sends the right analytics tracker event`() {
        composeTestRule.setContent {
            SyncFoldersScreen(
                syncUiItems = listOf(
                    SyncUiItem(
                        id = 1L,
                        syncType = SyncType.TYPE_TWOWAY,
                        folderPairName = "Sync Name",
                        status = SyncStatus.SYNCING,
                        deviceStoragePath = "Device Path",
                        hasStalledIssues = false,
                        megaStoragePath = "MEGA Path",
                        megaStorageNodeId = NodeId(1111L),
                        expanded = false,
                    ),
                ),
                cardExpanded = {},
                pauseRunClicked = {},
                removeFolderClicked = {},
                addFolderClicked = {},
                upgradeAccountClicked = {},
                issuesInfoClicked = {},
                onOpenDeviceFolderClicked = {},
                onOpenMegaFolderClicked = {},
                isLowBatteryLevel = false,
                isFreeAccount = false,
                isLoading = false,
                showSyncsPausedErrorDialog = false,
                onShowSyncsPausedErrorDialogDismissed = {},
                deviceName = "Device Name",
                isBackupForAndroidEnabled = false,
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_ITEM_VIEW).assertIsDisplayed().performClick()
        assertThat(analyticsRule.events).contains(SyncCardExpandedEvent)
    }

    @Test
    fun `test that collapse a Sync card does not send any analytics tracker event `() {
        composeTestRule.setContent {
            SyncFoldersScreen(
                syncUiItems = listOf(
                    SyncUiItem(
                        id = 1L,
                        syncType = SyncType.TYPE_TWOWAY,
                        folderPairName = "Sync Name",
                        status = SyncStatus.SYNCING,
                        deviceStoragePath = "Device Path",
                        hasStalledIssues = false,
                        megaStoragePath = "MEGA Path",
                        megaStorageNodeId = NodeId(1111L),
                        expanded = true,
                    ),
                ),
                cardExpanded = {},
                pauseRunClicked = {},
                removeFolderClicked = {},
                addFolderClicked = {},
                upgradeAccountClicked = {},
                issuesInfoClicked = {},
                onOpenDeviceFolderClicked = {},
                onOpenMegaFolderClicked = {},
                isLowBatteryLevel = false,
                isFreeAccount = false,
                isLoading = false,
                showSyncsPausedErrorDialog = false,
                onShowSyncsPausedErrorDialogDismissed = {},
                deviceName = "Device Name",
                isBackupForAndroidEnabled = false,
            )
        }
        analyticsRule.events.clear()
        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_ITEM_VIEW).assertIsDisplayed().performClick()
        assertThat(analyticsRule.events).isEmpty()
    }
}