package mega.privacy.android.feature.sync.ui.synclist.folders

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.ui.model.SyncUiItem

class SyncFoldersScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SyncFoldersScreenEmpty() {
        AndroidThemeForPreviews {
            SyncFoldersScreen(
                syncUiItems = emptyList(),
                cardExpanded = {},
                pauseRunClicked = {},
                removeFolderClicked = {},
                onAddNewSyncClicked = {},
                onAddNewBackupClicked = {},
                issuesInfoClicked = {},
                onOpenDeviceFolderClicked = {},
                onOpenMegaFolderClicked = {},
                onCameraUploadsSettingsClicked = {},
                isLowBatteryLevel = false,
                isStorageOverQuota = false,
                isLoading = false,
                deviceName = "Device Name",
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SyncFoldersScreenLoading() {
        AndroidThemeForPreviews {
            SyncFoldersScreen(
                syncUiItems = emptyList(),
                cardExpanded = {},
                pauseRunClicked = {},
                removeFolderClicked = {},
                onAddNewSyncClicked = {},
                onAddNewBackupClicked = {},
                issuesInfoClicked = {},
                onOpenDeviceFolderClicked = {},
                onOpenMegaFolderClicked = {},
                onCameraUploadsSettingsClicked = {},
                isLowBatteryLevel = false,
                isStorageOverQuota = false,
                isLoading = true,
                deviceName = "Device Name",
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SyncFoldersScreenWithSyncs() {
        AndroidThemeForPreviews {
            SyncFoldersScreen(
                syncUiItems = listOf(
                    syncUiItem(1L, "Competitors documentation", SyncStatus.SYNCED),
                    syncUiItem(2L, "Product roadmap", SyncStatus.SYNCING),
                ),
                cardExpanded = {},
                pauseRunClicked = {},
                removeFolderClicked = {},
                onAddNewSyncClicked = {},
                onAddNewBackupClicked = {},
                issuesInfoClicked = {},
                onOpenDeviceFolderClicked = {},
                onOpenMegaFolderClicked = {},
                onCameraUploadsSettingsClicked = {},
                isLowBatteryLevel = false,
                isStorageOverQuota = false,
                isLoading = false,
                deviceName = "Device Name",
            )
        }
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
}
