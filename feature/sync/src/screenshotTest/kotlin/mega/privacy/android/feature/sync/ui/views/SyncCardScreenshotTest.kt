package mega.privacy.android.feature.sync.ui.views

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.sync.domain.entity.SyncPauseReason
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

class SyncCardScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SyncCardPausedReasons() {
        OriginalTheme(isDark = isSystemInDarkTheme()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                listOf(
                    SyncPauseReason.BatterySaver to "Competitors documentation",
                    SyncPauseReason.LowBattery to "Product roadmap",
                    SyncPauseReason.NoWifi to "Design handoff",
                ).forEach { (reason, folderPairName) ->
                    SyncCard(
                        sync = pausedSync(folderPairName),
                        expandClicked = {},
                        pauseRunClicked = {},
                        removeFolderClicked = {},
                        issuesInfoClicked = {},
                        onOpenDeviceFolderClicked = {},
                        onOpenMegaFolderClicked = {},
                        onCameraUploadsSettingsClicked = {},
                        isLowBatteryLevel = false,
                        isStorageOverQuota = false,
                        errorRes = null,
                        deviceName = "Device Name",
                        syncPauseReason = reason,
                    )
                }
            }
        }
    }

    private fun pausedSync(folderPairName: String) = SyncUiItem(
        id = folderPairName.hashCode().toLong(),
        syncType = SyncType.TYPE_TWOWAY,
        folderPairName = folderPairName,
        status = SyncStatus.PAUSED,
        hasStalledIssues = false,
        deviceStoragePath = "/storage/emulated/0/Download",
        megaStoragePath = "Competitors documentation",
        megaStorageNodeId = NodeId(1234L),
        expanded = false,
        uriPath = UriPath("/storage/emulated/0/Download"),
    )
}
