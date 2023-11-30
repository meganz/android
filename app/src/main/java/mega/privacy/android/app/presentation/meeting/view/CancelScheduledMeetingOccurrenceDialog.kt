package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.getDayAndMonth
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import java.time.Instant
import java.time.temporal.ChronoField
import kotlin.random.Random

/**
 * Show cancel scheduled meeting dialog
 *
 * @param occurrence    Occurrence to cancel
 * @param onConfirm             To be triggered when confirm button is pressed
 * @param onDismiss             To be triggered when dialog is hidden
 */
@Composable
fun CancelScheduledMeetingOccurrenceDialog(
    occurrence: ChatScheduledMeetingOccurr,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        title = stringResource(
            R.string.meetings_cancel_scheduled_meeting_occurrence_dialog_title,
            occurrence.getDayAndMonth().orEmpty()
        ),
        text = stringResource(id = R.string.meetings_cancel_scheduled_meeting_occurrence_dialog_message),
        confirmButtonText = stringResource(id = R.string.meetings_cancel_scheduled_meeting_occurrence_dialog_confirm_button),
        cancelButtonText = stringResource(id = R.string.meetings_cancel_scheduled_meeting_dialog_do_not_cancel_button),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

/**
 * [CancelScheduledMeetingOccurrenceDialog] preview
 */
@Preview
@Composable
fun PreviewCancelScheduledMeetingOccurrenceDialog() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        CancelScheduledMeetingOccurrenceDialog(
            occurrence = ChatScheduledMeetingOccurr(
                schedId = Random.nextLong(),
                parentSchedId = -1,
                isCancelled = false,
                timezone = null,
                startDateTime = Instant.parse("2023-05-30T10:00:00.00Z")
                    .getLong(ChronoField.INSTANT_SECONDS),
                endDateTime = Instant.parse("2023-05-30T11:00:00.00Z")
                    .getLong(ChronoField.INSTANT_SECONDS),
                overrides = null,
            ),
            onConfirm = {},
            onDismiss = {},
        )
    }
}