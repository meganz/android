package mega.privacy.android.feature.chat.list.view

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Screenshot tests for the meeting actions bottom sheet shown from the Meetings tab FAB.
 */
class MeetingActionsBottomSheetScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun MeetingActionsSheet() {
        AndroidThemeForPreviews {
            MeetingActionsSheetContent(onActionClick = {})
        }
    }
}
