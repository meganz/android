package mega.privacy.android.feature.sync.ui.newfolderpair

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.ui.preview.CombinedThemePhoneLandscapePreviews
import mega.privacy.android.shared.sync.ui.permissions.SyncPermissionsManager

/**
 * Landscape is covered because the content lays itself out differently there, narrowing to 45% and
 * centring rather than filling the width.
 */
class SyncNewFolderScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SyncNewFolderTwoWay() {
        AndroidThemeForPreviews {
            Scaffold(syncType = SyncType.TYPE_TWOWAY)
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SyncNewFolderBackup() {
        AndroidThemeForPreviews {
            Scaffold(syncType = SyncType.TYPE_BACKUP)
        }
    }

    @PreviewTest
    @CombinedThemePhoneLandscapePreviews
    @Composable
    fun SyncNewFolderTwoWayLandscape() {
        AndroidThemeForPreviews {
            Scaffold(syncType = SyncType.TYPE_TWOWAY)
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SyncNewFolderWithSelection() {
        AndroidThemeForPreviews {
            Scaffold(
                syncType = SyncType.TYPE_TWOWAY,
                selectedLocalFolder = "/storage/emulated/0/DCIM",
            )
        }
    }

    @Composable
    private fun Scaffold(
        syncType: SyncType,
        selectedLocalFolder: String = "",
    ) {
        SyncNewFolderScreenScaffold(
            state = SyncNewFolderState(
                syncType = syncType,
                deviceName = "Pixel 8 Pro",
            ),
            selectedLocalFolder = selectedLocalFolder,
            selectedLocalFolderUri = "",
            selectedMegaFolder = null,
            onSelectFolder = {},
            selectMegaFolderClicked = {},
            syncClicked = {},
            syncPermissionsManager = SyncPermissionsManager(LocalContext.current),
            showStorageOverQuota = false,
            onDismissStorageOverQuota = {},
            onDismissRenameAndCreateBackupDialog = {},
            onRenameAndCreateBackupSucceeded = {},
            onOpenUpgradeAccount = {},
            onBackClicked = {},
            onShowSnackbarConsumed = {},
        )
    }
}
