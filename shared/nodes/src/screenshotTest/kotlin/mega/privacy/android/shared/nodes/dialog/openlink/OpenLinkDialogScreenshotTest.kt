package mega.privacy.android.shared.nodes.dialog.openlink

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Screenshot tests for [OpenLinkDialog] in its generic and join-meeting modes.
 */
class OpenLinkDialogScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun OpenLinkDialogDefault() {
        AndroidThemeForPreviews {
            OpenLinkDialog(
                onOpenLink = {},
                onDismiss = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun OpenLinkDialogJoinMeeting() {
        AndroidThemeForPreviews {
            OpenLinkDialog(
                onOpenLink = {},
                onDismiss = {},
                isJoinMeeting = true,
            )
        }
    }
}
