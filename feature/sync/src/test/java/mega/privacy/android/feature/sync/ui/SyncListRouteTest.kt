package mega.privacy.android.feature.sync.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import mega.privacy.android.core.test.AnalyticsTestRule
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.feature.sync.ui.synclist.SyncListMenuAction.Companion.ADD_NEW_SYNC_ACTION_TEST_TAG
import mega.privacy.android.feature.sync.ui.synclist.SyncListRoute
import mega.privacy.android.feature.sync.ui.synclist.SyncListState
import mega.privacy.android.feature.sync.ui.synclist.SyncListViewModel
import mega.privacy.android.feature.sync.ui.synclist.TEST_TAG_SYNC_LIST_SCREEN_UPGRADE_DIALOG
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersState
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersViewModel
import mega.privacy.android.feature.sync.ui.synclist.folders.TEST_TAG_SYNC_LIST_SCREEN_FAB
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesState
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesViewModel
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesState
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesViewModel
import mega.privacy.android.shared.original.core.ui.controls.buttons.MULTI_FAB_MAIN_FAB_TEST_TAG
import mega.privacy.android.shared.original.core.ui.controls.buttons.MULTI_FAB_OPTION_ROW_TEST_TAG
import mega.privacy.android.shared.original.core.ui.controls.dialogs.internal.CANCEL_TAG
import mega.privacy.android.shared.original.core.ui.controls.dialogs.internal.CONFIRM_TAG
import mega.privacy.android.shared.original.core.ui.controls.menus.TAG_MENU_ACTIONS_SHOW_MORE
import mega.privacy.android.shared.resources.R
import mega.privacy.android.shared.sync.featuretoggles.SyncFeatures
import mega.privacy.mobile.analytics.event.AndroidBackupFABButtonPressedEvent
import mega.privacy.mobile.analytics.event.AndroidSyncMultiFABButtonPressedEvent
import mega.privacy.mobile.analytics.event.SyncFeatureUpgradeDialogCancelButtonPressedEvent
import mega.privacy.mobile.analytics.event.SyncFeatureUpgradeDialogDisplayedEvent
import mega.privacy.mobile.analytics.event.SyncFeatureUpgradeDialogUpgradeButtonPressedEvent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SyncListRouteTest {

    private val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    private val viewModel: SyncListViewModel = mock()
    private val state: StateFlow<SyncListState> = mock()
    private val syncPermissionsManager: SyncPermissionsManager = mock()

    private val syncFoldersViewModel: SyncFoldersViewModel = mock()
    private val syncFoldersState: StateFlow<SyncFoldersState> = mock()
    private val syncStalledIssuesViewModel: SyncStalledIssuesViewModel = mock()
    private val syncStalledIssuesState: StateFlow<SyncStalledIssuesState> = mock()
    private val syncSolvedIssuesViewModel: SyncSolvedIssuesViewModel = mock()
    private val syncSolvedIssuesState: StateFlow<SyncSolvedIssuesState> = mock()

    private val synUiItems = listOf(
        SyncUiItem(
            id = 1L,
            syncType = SyncType.TYPE_TWOWAY,
            folderPairName = "Folder Name",
            status = SyncStatus.SYNCING,
            hasStalledIssues = false,
            deviceStoragePath = "Folder Path",
            megaStoragePath = "MEGA Patch",
            megaStorageNodeId = NodeId(1234L),
            expanded = false
        )
    )

    @Before
    fun setupMock(): Unit = runBlocking {
        whenever(state.value).thenReturn(SyncListState())
        whenever(viewModel.state).thenReturn(state)
        whenever(syncFoldersState.value).thenReturn(SyncFoldersState(syncUiItems = synUiItems))
        whenever(syncFoldersViewModel.uiState).thenReturn(syncFoldersState)
        whenever(syncStalledIssuesState.value).thenReturn(SyncStalledIssuesState(emptyList()))
        whenever(syncStalledIssuesViewModel.state).thenReturn(syncStalledIssuesState)
        whenever(syncSolvedIssuesState.value).thenReturn(SyncSolvedIssuesState())
        whenever(syncSolvedIssuesViewModel.state).thenReturn(syncSolvedIssuesState)
    }

    private fun setComposeContent() {
        composeTestRule.setContent {
            SyncListRoute(
                viewModel = viewModel,
                syncPermissionsManager = syncPermissionsManager,
                onSyncFolderClicked = {},
                onBackupFolderClicked = {},
                onSelectStopBackupDestinationClicked = {},
                onOpenUpgradeAccountClicked = {},
                syncFoldersViewModel = syncFoldersViewModel,
                syncStalledIssuesViewModel = syncStalledIssuesViewModel,
                syncSolvedIssuesViewModel = syncSolvedIssuesViewModel,
            )
        }
    }

    @Test
    fun `test that click on FAB of the sync list on a free account displays the upgrade dialog`() {
        setComposeContent()
        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_FAB).performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_UPGRADE_DIALOG).assertIsDisplayed()
    }

    @Test
    fun `test that click on FAB of the sync list on a non free account doesn't display the upgrade dialog`() {
        whenever(state.value).thenReturn(SyncListState(isFreeAccount = false))
        setComposeContent()
        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_FAB).performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_UPGRADE_DIALOG)
            .assertIsNotDisplayed()
    }

    @Test
    fun `test that click add new sync menu entry on a free account displays the upgrade dialog`() {
        setComposeContent()
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).performClick()
        composeTestRule.onNodeWithTag(ADD_NEW_SYNC_ACTION_TEST_TAG).assertIsDisplayed()
            .performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_UPGRADE_DIALOG).assertIsDisplayed()
    }

    @Test
    fun `test that click add new sync menu entry on a non free account doesn't display the upgrade dialog`() {
        whenever(state.value).thenReturn(SyncListState(isFreeAccount = false))
        setComposeContent()
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).performClick()
        composeTestRule.onNodeWithTag(ADD_NEW_SYNC_ACTION_TEST_TAG).assertIsDisplayed()
            .performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_UPGRADE_DIALOG)
            .assertIsNotDisplayed()
    }

    @Test
    fun `test that display the upgrade dialog sends the right analytics tracker event`() {
        setComposeContent()
        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_FAB).performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_UPGRADE_DIALOG).assertIsDisplayed()
        assertThat(analyticsRule.events).contains(SyncFeatureUpgradeDialogDisplayedEvent)

        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).performClick()
        composeTestRule.onNodeWithTag(ADD_NEW_SYNC_ACTION_TEST_TAG).assertIsDisplayed()
            .performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_UPGRADE_DIALOG).assertIsDisplayed()
        assertThat(analyticsRule.events).contains(SyncFeatureUpgradeDialogDisplayedEvent)
    }

    @Test
    fun `test that click the confirm button of the upgrade dialog sends the right analytics tracker event`() {
        setComposeContent()
        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_FAB).performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_UPGRADE_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CONFIRM_TAG).assertIsDisplayed().performClick()
        assertThat(analyticsRule.events).contains(SyncFeatureUpgradeDialogUpgradeButtonPressedEvent)
    }

    @Test
    fun `test that click the cancel button of the upgrade dialog sends the right analytics tracker event`() {
        setComposeContent()
        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_FAB).performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_UPGRADE_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CANCEL_TAG).assertIsDisplayed().performClick()
        assertThat(analyticsRule.events).contains(SyncFeatureUpgradeDialogCancelButtonPressedEvent)
    }

    @Test
    fun `test that tap on and expand the multi FAB sends the right analytics tracker event`() {
        whenever(state.value).thenReturn(
            SyncListState(
                isFreeAccount = false,
                enabledFlags = setOf(SyncFeatures.BackupForAndroid),
            )
        )
        whenever(viewModel.state).thenReturn(state)
        whenever(syncFoldersState.value).thenReturn(
            SyncFoldersState(
                syncUiItems = emptyList(),
                isFreeAccount = false,
                enabledFlags = setOf(SyncFeatures.BackupForAndroid),
            )
        )
        setComposeContent()
        composeTestRule.onNodeWithTag(MULTI_FAB_MAIN_FAB_TEST_TAG).performClick()
        assertThat(analyticsRule.events).contains(AndroidSyncMultiFABButtonPressedEvent)
    }

    @Test
    fun `test that tap on Backup FAB sends the right analytics tracker event`() {
        whenever(state.value).thenReturn(
            SyncListState(
                isFreeAccount = false,
                enabledFlags = setOf(SyncFeatures.BackupForAndroid),
            )
        )
        whenever(viewModel.state).thenReturn(state)
        whenever(syncFoldersState.value).thenReturn(
            SyncFoldersState(
                syncUiItems = emptyList(),
                isFreeAccount = false,
                enabledFlags = setOf(SyncFeatures.BackupForAndroid),
            )
        )
        setComposeContent()
        composeTestRule.onNodeWithTag(MULTI_FAB_MAIN_FAB_TEST_TAG).performClick()
        composeTestRule.onNodeWithTag(
            "${MULTI_FAB_OPTION_ROW_TEST_TAG}_${
                composeTestRule.activity.getString(
                    R.string.sync_add_new_backup_toolbar_title
                )
            }"
        ).performClick()
        assertThat(analyticsRule.events).contains(AndroidBackupFABButtonPressedEvent)
    }
}