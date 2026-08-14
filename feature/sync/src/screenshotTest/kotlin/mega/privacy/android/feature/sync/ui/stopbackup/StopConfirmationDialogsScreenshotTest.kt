package mega.privacy.android.feature.sync.ui.stopbackup

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.sync.ui.synclist.folders.StopSyncConfirmDialog

/**
 * Both confirmation dialogs the sync list can raise. The stop-backup one is the awkward shape: a
 * title, three body paragraphs and a stacked column of two options plus cancel.
 */
class StopConfirmationDialogsScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun StopBackupConfirmation() {
        AndroidThemeForPreviews {
            StopBackupConfirmationDialogBody(
                onConfirm = { _, _ -> },
                onDismiss = {},
                folderName = "Camera uploads",
                onSelectStopBackupDestinationClicked = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun StopSyncConfirmation() {
        AndroidThemeForPreviews {
            StopSyncConfirmDialog(
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}
