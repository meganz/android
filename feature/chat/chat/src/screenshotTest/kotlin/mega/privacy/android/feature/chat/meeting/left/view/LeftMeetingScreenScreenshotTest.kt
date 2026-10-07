package mega.privacy.android.feature.chat.meeting.left.view

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.chat.meeting.left.model.LeftMeetingUiState

/**
 * Screenshot tests for [LeftMeetingScreen], covering the sign up invitation on its own and the
 * dialog raised when the call ended on the organiser's free plan participant cap.
 */
class LeftMeetingScreenScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun LeftMeetingScreenInvitation() {
        AndroidThemeForPreviews {
            LeftMeetingScreen(
                uiState = LeftMeetingUiState.None,
                onCloseClick = {},
                onCreateAccountClick = {},
                onNoticeShown = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun LeftMeetingScreenFreePlanParticipantsLimit() {
        AndroidThemeForPreviews {
            LeftMeetingScreen(
                uiState = LeftMeetingUiState.FreePlanParticipantsLimit,
                onCloseClick = {},
                onCreateAccountClick = {},
                onNoticeShown = {},
            )
        }
    }
}
