package mega.privacy.android.feature.chat.list.view

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Screenshot tests for the dialogs shown when a meeting action cannot be carried out.
 */
class MeetingActionDialogsScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun StartMeetingBlockedByCall() {
        AndroidThemeForPreviews {
            MeetingBlockedByCallDialog(
                message = sharedR.string.chat_start_meeting_already_in_call_error,
                onDismiss = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun JoinMeetingBlockedByCall() {
        AndroidThemeForPreviews {
            MeetingBlockedByCallDialog(
                message = sharedR.string.can_only_join_one_call_error_message,
                onDismiss = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun BluetoothPermissionRequired() {
        AndroidThemeForPreviews {
            BluetoothPermissionRequiredDialog(onDismiss = {})
        }
    }
}
