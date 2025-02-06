package mega.privacy.android.feature.sync.ui

import androidx.activity.ComponentActivity
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
import mega.privacy.android.feature.sync.ui.synclist.SyncListRoute
import mega.privacy.android.feature.sync.ui.synclist.SyncListState
import mega.privacy.android.feature.sync.ui.synclist.SyncListViewModel
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersUiState
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersViewModel
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesState
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesViewModel
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesState
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesViewModel
import mega.privacy.android.shared.original.core.ui.controls.buttons.MULTI_FAB_MAIN_FAB_TEST_TAG
import mega.privacy.android.shared.original.core.ui.controls.buttons.MULTI_FAB_OPTION_ROW_TEST_TAG
import mega.privacy.android.shared.resources.R
import mega.privacy.android.shared.sync.featuretoggles.SyncFeatures
import mega.privacy.mobile.analytics.event.AndroidBackupFABButtonPressedEvent
import mega.privacy.mobile.analytics.event.AndroidSyncMultiFABButtonPressedEvent
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
    private val syncFoldersUiState: StateFlow<SyncFoldersUiState> = mock()
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
        whenever(state.value).thenReturn(SyncListState(enabledFlags = setOf(SyncFeatures.BackupForAndroid)))
        whenever(viewModel.state).thenReturn(state)
        whenever(syncFoldersUiState.value).thenReturn(
            SyncFoldersUiState(
                syncUiItems = synUiItems,
                enabledFlags = setOf(SyncFeatures.BackupForAndroid)
            )
        )
        whenever(syncFoldersViewModel.uiState).thenReturn(syncFoldersUiState)
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
                onOpenMegaFolderClicked = {}
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
    fun `test that tap on Backup FAB sends the right analytics tracker event`() {
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
