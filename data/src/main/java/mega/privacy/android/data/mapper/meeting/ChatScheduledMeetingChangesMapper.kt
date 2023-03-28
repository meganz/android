package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.chat.ScheduledMeetingChanges
import nz.mega.sdk.MegaChatScheduledMeeting
import javax.inject.Inject

/**
 * Mapper to convert chat scheduled meeting changes to [ScheduledMeetingChanges]
 */
internal class ChatScheduledMeetingChangesMapper @Inject constructor() {
    operator fun invoke(schedMeet: MegaChatScheduledMeeting?): ScheduledMeetingChanges? = when {
        schedMeet == null -> null
        schedMeet.hasChanged(MegaChatScheduledMeeting.SC_START.toLong()) -> ScheduledMeetingChanges.StartDate
        schedMeet.hasChanged(MegaChatScheduledMeeting.SC_END.toLong()) -> ScheduledMeetingChanges.EndDate
        schedMeet.hasChanged(MegaChatScheduledMeeting.SC_TITLE.toLong()) -> ScheduledMeetingChanges.Title
        schedMeet.hasChanged(MegaChatScheduledMeeting.SC_DESC.toLong()) -> ScheduledMeetingChanges.Description
        schedMeet.hasChanged(MegaChatScheduledMeeting.SC_ATTR.toLong()) -> ScheduledMeetingChanges.Attributes
        schedMeet.hasChanged(MegaChatScheduledMeeting.SC_OVERR.toLong()) -> ScheduledMeetingChanges.OverrideDateTime
        schedMeet.hasChanged(MegaChatScheduledMeeting.SC_CANC.toLong()) -> ScheduledMeetingChanges.CancelledFlag
        schedMeet.hasChanged(MegaChatScheduledMeeting.SC_FLAGS.toLong()) -> ScheduledMeetingChanges.ScheduledMeetingsFlags
        schedMeet.hasChanged(MegaChatScheduledMeeting.SC_RULES.toLong()) -> ScheduledMeetingChanges.RepetitionRules
        schedMeet.hasChanged(MegaChatScheduledMeeting.SC_FLAGS_SIZE.toLong()) -> ScheduledMeetingChanges.ScheduledMeetingFlagsSize
        else -> ScheduledMeetingChanges.Unknown
    }
}