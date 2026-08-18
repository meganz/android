package mega.privacy.android.feature.sync.ui.synclist

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.android.tools.screenshot.PreviewTest
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.ui.SyncMonitorState
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersScreen
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersUiState
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesState
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesState
import mega.privacy.android.shared.sync.ui.permissions.SyncPermissionsManager

/**
 * Covers the assembled sync list — warning banners, chip row and chip content laid out together
 * with the FAB. The individual pieces have their own goldens; this pins how the shell composes
 * them.
 */
class SyncListContentScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SyncListTabWithSyncs() {
        AndroidThemeForPreviews {
            SyncListTabContent(
                syncFoldersUiState = SyncFoldersUiState(syncUiItems = syncItems),
                syncStalledIssuesState = SyncStalledIssuesState(stalledIssues = emptyList()),
                syncSolvedIssuesState = SyncSolvedIssuesState(),
                syncNotificationState = SyncMonitorState(),
                stalledIssuesCount = 0,
                onSyncFolderClicked = {},
                onBackupFolderClicked = {},
                syncPermissionsManager = SyncPermissionsManager(LocalContext.current),
                onOpenUpgradeAccountClicked = {},
                onDismissNotification = {},
                onSyncRefresh = {},
                chipContent = { _, _ -> SyncFoldersChipContent() },
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SyncListTabOverQuota() {
        AndroidThemeForPreviews {
            SyncListTabContent(
                syncFoldersUiState = SyncFoldersUiState(
                    syncUiItems = syncItems,
                    isStorageOverQuota = true,
                ),
                syncStalledIssuesState = SyncStalledIssuesState(stalledIssues = emptyList()),
                syncSolvedIssuesState = SyncSolvedIssuesState(),
                syncNotificationState = SyncMonitorState(),
                stalledIssuesCount = 0,
                onSyncFolderClicked = {},
                onBackupFolderClicked = {},
                syncPermissionsManager = SyncPermissionsManager(LocalContext.current),
                onOpenUpgradeAccountClicked = {},
                onDismissNotification = {},
                onSyncRefresh = {},
                chipContent = { _, _ -> SyncFoldersChipContent(isStorageOverQuota = true) },
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SyncListTabLowBattery() {
        AndroidThemeForPreviews {
            SyncListTabContent(
                syncFoldersUiState = SyncFoldersUiState(
                    syncUiItems = syncItems,
                    isLowBatteryLevel = true,
                ),
                syncStalledIssuesState = SyncStalledIssuesState(stalledIssues = emptyList()),
                syncSolvedIssuesState = SyncSolvedIssuesState(),
                syncNotificationState = SyncMonitorState(),
                stalledIssuesCount = 0,
                onSyncFolderClicked = {},
                onBackupFolderClicked = {},
                syncPermissionsManager = SyncPermissionsManager(LocalContext.current),
                onOpenUpgradeAccountClicked = {},
                onDismissNotification = {},
                onSyncRefresh = {},
                chipContent = { _, _ -> SyncFoldersChipContent(isLowBatteryLevel = true) },
            )
        }
    }

    @Composable
    private fun SyncFoldersChipContent(
        isLowBatteryLevel: Boolean = false,
        isStorageOverQuota: Boolean = false,
    ) {
        SyncFoldersScreen(
            syncUiItems = syncItems,
            cardExpanded = {},
            pauseRunClicked = {},
            removeFolderClicked = {},
            onAddNewSyncClicked = {},
            onAddNewBackupClicked = {},
            issuesInfoClicked = {},
            onOpenDeviceFolderClicked = {},
            onOpenMegaFolderClicked = {},
            onCameraUploadsSettingsClicked = {},
            isLowBatteryLevel = isLowBatteryLevel,
            isStorageOverQuota = isStorageOverQuota,
            isLoading = false,
            deviceName = "Device Name",
        )
    }

    private val syncItems = persistentListOf(
        syncUiItem(1L, "Competitors documentation", SyncStatus.SYNCED),
        syncUiItem(2L, "Product roadmap", SyncStatus.SYNCING),
    )
}

private fun syncUiItem(id: Long, name: String, status: SyncStatus) = SyncUiItem(
    id = id,
    syncType = SyncType.TYPE_TWOWAY,
    folderPairName = name,
    status = status,
    hasStalledIssues = false,
    deviceStoragePath = "/storage/emulated/0/Download",
    megaStoragePath = name,
    megaStorageNodeId = NodeId(id),
    expanded = false,
    uriPath = UriPath("/storage/emulated/0/Download"),
)
