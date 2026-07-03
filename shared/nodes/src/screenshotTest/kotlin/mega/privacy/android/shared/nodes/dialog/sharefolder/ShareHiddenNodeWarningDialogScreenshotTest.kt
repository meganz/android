package mega.privacy.android.shared.nodes.dialog.sharefolder

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

class ShareHiddenNodeWarningDialogScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ShareHiddenNodeWarningDialogSingleFolder() {
        AndroidThemeForPreviews {
            ShareHiddenNodeWarningDialog(
                sharingMultipleFolders = false,
                onConfirm = {},
                onCancel = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ShareHiddenNodeWarningDialogMultipleFolders() {
        AndroidThemeForPreviews {
            ShareHiddenNodeWarningDialog(
                sharingMultipleFolders = true,
                onConfirm = {},
                onCancel = {},
            )
        }
    }
}
