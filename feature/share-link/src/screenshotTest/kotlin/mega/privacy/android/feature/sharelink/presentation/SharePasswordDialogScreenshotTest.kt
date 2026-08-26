package mega.privacy.android.feature.sharelink.presentation

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

class SharePasswordDialogScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SharePasswordDialogDefault() {
        AndroidThemeForPreviews {
            SharePasswordDialog(
                onShareWithPassword = {},
                onShareLinkOnly = {},
                onDismiss = {},
            )
        }
    }
}
