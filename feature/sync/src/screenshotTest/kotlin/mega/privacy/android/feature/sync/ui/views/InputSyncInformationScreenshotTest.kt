package mega.privacy.android.feature.sync.ui.views

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.sync.SyncType

/**
 * The card the setup flow is built around. The backup variant differs from two-way: its MEGA row is
 * a fixed, non-clickable path rather than a selectable folder.
 */
class InputSyncInformationScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun TwoWayEmpty() {
        AndroidThemeForPreviews {
            InputSyncInformationView(
                syncType = SyncType.TYPE_TWOWAY,
                deviceName = "Pixel 8 Pro",
                selectDeviceFolderClicked = {},
                selectMegaFolderClicked = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun TwoWaySelected() {
        AndroidThemeForPreviews {
            InputSyncInformationView(
                syncType = SyncType.TYPE_TWOWAY,
                deviceName = "Pixel 8 Pro",
                selectDeviceFolderClicked = {},
                selectMegaFolderClicked = {},
                selectedDeviceFolder = "/storage/emulated/0/DCIM",
                selectedMegaFolder = "Camera uploads",
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun Backup() {
        AndroidThemeForPreviews {
            InputSyncInformationView(
                syncType = SyncType.TYPE_BACKUP,
                deviceName = "Pixel 8 Pro",
                selectDeviceFolderClicked = {},
                selectMegaFolderClicked = {},
                selectedDeviceFolder = "/storage/emulated/0/DCIM",
            )
        }
    }
}
