package mega.privacy.android.feature.sync.ui

import androidx.activity.ComponentActivity
import kotlinx.collections.immutable.toImmutableList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.feature.sync.ui.synclist.SOLVED_ISSUES_CHIP_TEST_TAG
import mega.privacy.android.feature.sync.ui.synclist.STALLED_ISSUES_CHIP_TEST_TAG
import mega.privacy.android.feature.sync.ui.synclist.SYNC_FOLDERS_CHIP_TEST_TAG
import mega.privacy.android.feature.sync.ui.synclist.SyncListAction
import mega.privacy.android.feature.sync.ui.synclist.SyncListRoute
import mega.privacy.android.feature.sync.ui.synclist.SyncListState
import mega.privacy.android.feature.sync.ui.synclist.SyncListViewModel
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersUiState
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersViewModel
import mega.privacy.android.feature.sync.ui.synclist.folders.TEST_TAG_SYNC_LIST_SCREEN_FAB
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesState
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesViewModel
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesState
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesViewModel
import mega.privacy.android.shared.original.core.ui.controls.buttons.MULTI_FAB_MAIN_FAB_TEST_TAG
import mega.privacy.android.shared.original.core.ui.controls.buttons.MULTI_FAB_OPTION_ROW_TEST_TAG
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.mobile.analytics.event.AndroidBackupFABButtonPressedEvent
import mega.privacy.mobile.analytics.event.AndroidSyncFABButtonEvent
import mega.privacy.mobile.analytics.event.AndroidSyncMultiFABButtonPressedEvent
import mega.privacy.mobile.analytics.event.SyncListFoldersButtonPressedEvent
import mega.privacy.mobile.analytics.event.SyncListIssuesButtonPressedEvent
import mega.privacy.mobile.analytics.event.SyncListSolvedIssuesButtonPressedEvent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
internal class SyncListRouteTest {

    private val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    private val viewModel: SyncListViewModel = mock()
    private val state: StateFlow<SyncListState> = mock()
    private val syncPermissionsManager: SyncPermissionsManager = mock()

    private val syncFoldersViewModel: SyncFoldersViewModel = mock()
    private val syncFoldersUiState: StateFlow<SyncFoldersUiState> = mock()
    private val syncStalledIssuesViewModel: SyncStalledIssuesViewModel = mock()
    private val syncStalledIssuesState: StateFlow<SyncStalledIssuesState> = mock()
    private val syncSolvedIssuesViewModel: SyncSolvedIssuesViewModel = mock()
    private val syncSolvedIssuesState: StateFlow<SyncSolvedIssuesState> = mock()
    private val syncIssueNotificationViewModel: SyncIssueNotificationViewModel = mock()
    private val syncMonitorState: StateFlow<SyncMonitorState> = mock()

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
            expanded = false,
            uriPath = UriPath("content://com.android.externalstorage.documents/document/primary%3ADCIM")
        )
    )

    @Before
    fun setupMock(): Unit = runBlocking {
        whenever(state.value).thenReturn(SyncListState())
        whenever(viewModel.state).thenReturn(state)
        whenever(syncMonitorState.value).thenReturn(
            SyncMonitorState()
        )
        whenever(syncFoldersUiState.value).thenReturn(
            SyncFoldersUiState(
                syncUiItems = synUiItems.toImmutableList(),
            )
        )
        whenever(syncFoldersViewModel.uiState).thenReturn(syncFoldersUiState)
        whenever(syncStalledIssuesState.value).thenReturn(SyncStalledIssuesState(emptyList()))
        whenever(syncStalledIssuesViewModel.state).thenReturn(syncStalledIssuesState)
        whenever(syncSolvedIssuesState.value).thenReturn(SyncSolvedIssuesState(mock()))
        whenever(syncSolvedIssuesViewModel.state).thenReturn(syncSolvedIssuesState)
        whenever(syncIssueNotificationViewModel.state).thenReturn(syncMonitorState)
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
                syncIssueNotificationViewModel = syncIssueNotificationViewModel,
                onSyncSettingsClicked = {},
                onOpenMegaFolderClicked = {},
                onCameraUploadsSettingsClicked = {},
                onStalledIssueMoreClicked = {},
            )
        }
    }

    @Test
    fun `test that tap on and expand the multi FAB sends the right analytics tracker event`() {
        setComposeContent()
        composeTestRule.onNodeWithTag(MULTI_FAB_MAIN_FAB_TEST_TAG).performClick()
        assertThat(analyticsRule.events).contains(AndroidSyncMultiFABButtonPressedEvent)
    }

    @Test
    fun `test that tap on Sync FAB sends the right analytics tracker event`() {
        setComposeContent()
        composeTestRule.onNodeWithTag(MULTI_FAB_MAIN_FAB_TEST_TAG).performClick()
        composeTestRule.onNodeWithTag(
            "${MULTI_FAB_OPTION_ROW_TEST_TAG}_${
                composeTestRule.activity.getString(
                    R.string.sync_toolbar_title
                )
            }"
        ).performClick()
        assertThat(analyticsRule.events).contains(AndroidSyncFABButtonEvent)
    }

    @Test
    fun `test that tap on Backup FAB sends the right analytics tracker event`() {
        setComposeContent()
        composeTestRule.onNodeWithTag(MULTI_FAB_MAIN_FAB_TEST_TAG).performClick()
        composeTestRule.onNodeWithTag(
            "${MULTI_FAB_OPTION_ROW_TEST_TAG}_${
                composeTestRule.activity.getString(
                    sharedR.string.sync_add_new_backup_toolbar_title
                )
            }"
        ).performClick()
        assertThat(analyticsRule.events).contains(AndroidBackupFABButtonPressedEvent)
    }

    @Test
    fun `test that tap on Folders chip sends the right analytics tracker event`() {
        setComposeContent()
        composeTestRule.onNodeWithTag(SYNC_FOLDERS_CHIP_TEST_TAG).performClick()
        assertThat(analyticsRule.events).contains(SyncListFoldersButtonPressedEvent)
    }

    @Test
    fun `test that tap on Stalled Issues chip sends the right analytics tracker event`() {
        setComposeContent()
        composeTestRule.onNodeWithTag(STALLED_ISSUES_CHIP_TEST_TAG).performClick()
        assertThat(analyticsRule.events).contains(SyncListIssuesButtonPressedEvent)
    }

    @Test
    fun `test that tap on Solved Issues chip sends the right analytics tracker event`() {
        setComposeContent()
        composeTestRule.onNodeWithTag(SOLVED_ISSUES_CHIP_TEST_TAG).performClick()
        assertThat(analyticsRule.events).contains(SyncListSolvedIssuesButtonPressedEvent)
    }

    @Test
    fun `test that SyncListRoute displays correct device name in title`() {
        val expectedDeviceName = "Test Device"
        whenever(state.value).thenReturn(SyncListState(deviceName = expectedDeviceName))

        setComposeContent()

        // Verify the device name is displayed as title
        composeTestRule.onNodeWithText(expectedDeviceName).assertIsDisplayed()
    }

    @Test
    fun `test that sync folders SnackBarShown is dispatched when route leaves composition mid snackbar`() {
        whenever(syncFoldersUiState.value).thenReturn(
            SyncFoldersUiState(
                syncUiItems = synUiItems.toImmutableList(),
                snackbarMessage = sharedR.string.sync_snackbar_message_confirm_sync_stopped,
            )
        )

        setComposeContentWithDisposeSwitch().value = false
        composeTestRule.waitForIdle()

        verify(syncFoldersViewModel).handleAction(SyncFoldersAction.SnackBarShown)
    }

    @Test
    fun `test that stalled issues SnackBarShown is dispatched when route leaves composition mid snackbar`() {
        whenever(syncStalledIssuesState.value).thenReturn(
            SyncStalledIssuesState(
                stalledIssues = emptyList(),
                snackbarMessageContent = triggered(sharedR.string.sync_stalled_issue_resolved),
            )
        )

        setComposeContentWithDisposeSwitch().value = false
        composeTestRule.waitForIdle()

        verify(syncStalledIssuesViewModel).handleAction(SyncListAction.SnackBarShown)
    }

    private fun setComposeContentWithSnackbarHost(hostState: SnackbarHostState) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalSnackBarHostState provides hostState) {
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
                    syncIssueNotificationViewModel = syncIssueNotificationViewModel,
                    onSyncSettingsClicked = {},
                    onOpenMegaFolderClicked = {},
                    onCameraUploadsSettingsClicked = {},
                    onStalledIssueMoreClicked = {},
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `test that stalled issues snackbar is consumed before it finishes displaying`() {
        whenever(syncStalledIssuesState.value).thenReturn(
            SyncStalledIssuesState(
                stalledIssues = emptyList(),
                snackbarMessageContent = triggered(sharedR.string.sync_stalled_issues_resolved),
            )
        )

        setComposeContentWithSnackbarHost(SnackbarHostState())

        verify(syncStalledIssuesViewModel).handleAction(SyncListAction.SnackBarShown)
    }

    @Test
    fun `test that sync folders snackbar is consumed before it finishes displaying`() {
        whenever(syncFoldersUiState.value).thenReturn(
            SyncFoldersUiState(
                syncUiItems = synUiItems.toImmutableList(),
                snackbarMessage = sharedR.string.sync_snackbar_message_confirm_sync_stopped,
            )
        )

        setComposeContentWithSnackbarHost(SnackbarHostState())

        verify(syncFoldersViewModel).handleAction(SyncFoldersAction.SnackBarShown)
    }

    @Test
    fun `test that sync folders snackbar keeps the moved folder name once consumed`() {
        whenever(syncFoldersUiState.value).thenReturn(
            SyncFoldersUiState(
                syncUiItems = synUiItems.toImmutableList(),
                snackbarMessage = sharedR.string.sync_snackbar_message_confirm_backup_moved,
                movedFolderName = "Camera uploads",
            )
        )
        val hostState = SnackbarHostState()

        setComposeContentWithSnackbarHost(hostState)

        val expected = composeTestRule.activity.getString(
            sharedR.string.sync_snackbar_message_confirm_backup_moved,
            "Camera uploads",
        )
        assertThat(hostState.currentSnackbarData?.visuals?.message).isEqualTo(expected)
    }

    /**
     * Mirrors how Cloud Drive hosts the Syncs tab: the host owns the scaffold, and so the only
     * snackbar host, while the tab supplies content only.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    private fun setComposeContentInHostScaffold(hostState: SnackbarHostState) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalSnackBarHostState provides hostState) {
                MegaScaffoldWithTopAppBarScrollBehavior { paddingValues ->
                    SyncListRoute(
                        isInCloudDrive = true,
                        viewModel = viewModel,
                        syncPermissionsManager = syncPermissionsManager,
                        onSyncFolderClicked = {},
                        onBackupFolderClicked = {},
                        onSelectStopBackupDestinationClicked = {},
                        onOpenUpgradeAccountClicked = {},
                        syncFoldersViewModel = syncFoldersViewModel,
                        syncStalledIssuesViewModel = syncStalledIssuesViewModel,
                        syncSolvedIssuesViewModel = syncSolvedIssuesViewModel,
                        syncIssueNotificationViewModel = syncIssueNotificationViewModel,
                        onSyncSettingsClicked = {},
                        onOpenMegaFolderClicked = {},
                        onCameraUploadsSettingsClicked = {},
                        onStalledIssueMoreClicked = {},
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `test that the stalled issues snackbar is displayed once when the sync list is hosted as a tab`() {
        whenever(syncStalledIssuesState.value).thenReturn(
            SyncStalledIssuesState(
                stalledIssues = emptyList(),
                snackbarMessageContent = triggered(sharedR.string.sync_stalled_issue_resolved),
            )
        )

        setComposeContentInHostScaffold(SnackbarHostState())

        val message =
            composeTestRule.activity.getString(sharedR.string.sync_stalled_issue_resolved)
        composeTestRule.onAllNodesWithText(message).assertCountEquals(1)
    }

    @Test
    fun `test that the sync folders snackbar is displayed once when the sync list is hosted as a tab`() {
        whenever(syncFoldersUiState.value).thenReturn(
            SyncFoldersUiState(
                syncUiItems = synUiItems.toImmutableList(),
                snackbarMessage = sharedR.string.sync_snackbar_message_confirm_sync_stopped,
            )
        )

        setComposeContentInHostScaffold(SnackbarHostState())

        val message = composeTestRule.activity.getString(
            sharedR.string.sync_snackbar_message_confirm_sync_stopped
        )
        composeTestRule.onAllNodesWithText(message).assertCountEquals(1)
    }

    private val manySyncUiItems = (0 until 30).map { index ->
        synUiItems.first().copy(id = index.toLong(), folderPairName = "Folder Name $index")
    }

    @Test
    fun `test that the standalone screen FAB sits at the bottom end of the screen`() {
        whenever(syncFoldersUiState.value).thenReturn(
            SyncFoldersUiState(syncUiItems = manySyncUiItems.toImmutableList())
        )
        setComposeContent()
        composeTestRule.waitForIdle()

        val root = composeTestRule.onRoot().getUnclippedBoundsInRoot()
        val fab = composeTestRule
            .onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_FAB)
            .getUnclippedBoundsInRoot()

        assertThat((root.bottom - fab.bottom).value).isWithin(0.5f).of(16f)
        assertThat((root.right - fab.right).value).isWithin(0.5f).of(16f)
    }

    @Test
    fun `test that the FAB stays anchored to the bottom of the tab while the sync list scrolls`() {
        whenever(syncFoldersUiState.value).thenReturn(
            SyncFoldersUiState(syncUiItems = manySyncUiItems.toImmutableList())
        )
        setComposeContentInHostScaffold(SnackbarHostState())

        val before = composeTestRule
            .onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_FAB)
            .getUnclippedBoundsInRoot()
        composeTestRule.onRoot().performTouchInput { swipeUp() }
        composeTestRule.waitForIdle()
        val after = composeTestRule
            .onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_FAB)
            .getUnclippedBoundsInRoot()

        assertThat(after.top).isEqualTo(before.top)
        assertThat(after.left).isEqualTo(before.left)
    }

    @Test
    fun `test that the tab FAB sits at the bottom end of the tab area`() {
        whenever(syncFoldersUiState.value).thenReturn(
            SyncFoldersUiState(syncUiItems = manySyncUiItems.toImmutableList())
        )
        setComposeContentInHostScaffold(SnackbarHostState())

        val root = composeTestRule.onRoot().getUnclippedBoundsInRoot()
        val fab = composeTestRule
            .onNodeWithTag(TEST_TAG_SYNC_LIST_SCREEN_FAB)
            .getUnclippedBoundsInRoot()

        assertThat((root.bottom - fab.bottom).value).isWithin(0.5f).of(16f)
        assertThat((root.right - fab.right).value).isWithin(0.5f).of(16f)
    }

    private fun setComposeContentWithDisposeSwitch() = mutableStateOf(true).also { switch ->
        composeTestRule.setContent {
            if (switch.value) {
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
                    syncIssueNotificationViewModel = syncIssueNotificationViewModel,
                    onSyncSettingsClicked = {},
                    onOpenMegaFolderClicked = {},
                    onCameraUploadsSettingsClicked = {},
                    onStalledIssueMoreClicked = {},
                )
            }
        }
        composeTestRule.waitForIdle()
    }

}
