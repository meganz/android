package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType

/**
 * Get the appropriate frequency string for a scheduled meeting.
 *
 * @param rules             [ChatScheduledRules]
 * @param isWeekdays        True, if it's weekdays. False, if not.
 */
@Composable
fun getScheduledMeetingFrequencyText(
    rules: ChatScheduledRules,
    isWeekdays: Boolean,
): String = when (rules.freq) {
    OccurrenceFrequencyType.Invalid -> stringResource(id = R.string.meetings_schedule_meeting_recurrence_never_label)
    OccurrenceFrequencyType.Daily ->
        if (isWeekdays) {
            stringResource(id = R.string.meetings_schedule_meeting_recurrence_every_weekday_label)
        } else {
            pluralStringResource(
                id = R.plurals.meetings_schedule_meeting_recurrence_every_number_of_days_label,
                count = rules.interval,
                rules.interval
            )
        }

    OccurrenceFrequencyType.Weekly -> stringResource(id = R.string.meetings_schedule_meeting_recurrence_weekly_label)
    OccurrenceFrequencyType.Monthly -> stringResource(id = R.string.meetings_schedule_meeting_recurrence_monthly_label)
}



