package mega.privacy.android.shared.nodes.dialog.removelink

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

class RemoveNodeLinkDialogM3ScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun RemoveNodeLinkDialogSingleLink() {
        AndroidThemeForPreviews {
            RemoveNodeLinkDialogBodyM3(
                count = 1,
                onConfirm = {},
                onDismiss = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun RemoveNodeLinkDialogMultipleLinks() {
        AndroidThemeForPreviews {
            RemoveNodeLinkDialogBodyM3(
                count = 2,
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}
