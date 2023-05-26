package mega.privacy.android.app.presentation.extensions.meeting

import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType

internal val OccurrenceFrequencyType.StringId: Int
    get() = when (this) {
        OccurrenceFrequencyType.Invalid -> R.string.meetings_schedule_meeting_recurrence_never_label
        OccurrenceFrequencyType.Daily -> R.string.meetings_schedule_meeting_recurrence_daily_label
        OccurrenceFrequencyType.Weekly -> R.string.meetings_schedule_meeting_recurrence_weekly_label
        OccurrenceFrequencyType.Monthly -> R.string.meetings_schedule_meeting_recurrence_monthly_label
    }