package mega.privacy.android.feature.chat.meeting.left.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.test.weblate.WeblateSnackbarScreenshot
import mega.privacy.android.feature.chat.meeting.left.model.LeftMeetingUiState
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Weblate reference render for the snackbar the left meeting screen reports a full call with.
 *
 * The snackbar animates in from the scaffold's snackbar host, so it never appears in a static
 * render of the screen itself — [WeblateSnackbarScreenshot] pins a MEGA-styled one over the screen
 * content instead, giving translators the message in the context it actually shows up in.
 */
class WeblateLeftMeetingSnackbarScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun TooManyParticipantsSnackbar() {
        AndroidThemeForPreviews {
            WeblateSnackbarScreenshot(
                snackbarText = stringResource(sharedR.string.call_error_too_many_participants),
            ) {
                LeftMeetingScreen(
                    uiState = LeftMeetingUiState.None,
                    onCloseClick = {},
                    onCreateAccountClick = {},
                    onNoticeShown = {},
                )
            }
        }
    }
}
