package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.chat.ScheduledMeetingChanges
import nz.mega.sdk.MegaChatScheduledMeeting
import javax.inject.Inject

/**
 * Mapper to convert chat scheduled meeting changes to [ScheduledMeetingChanges]
 */
internal class ChatScheduledMeetingChangesMapper @Inject constructor() {
    operator fun invoke(schedMeet: MegaChatScheduledMeeting?): List<ScheduledMeetingChanges>? {
        if (schedMeet == null) return null

        val changes = mutableListOf<ScheduledMeetingChanges>()
        if (schedMeet.hasChanged(MegaChatScheduledMeeting.SC_NEW_SCHED.toLong())) {
            changes.add(ScheduledMeetingChanges.NewScheduledMeeting)
        }

        if (schedMeet.hasChanged(MegaChatScheduledMeeting.SC_START.toLong())) {
            changes.add(ScheduledMeetingChanges.StartDate)
        }

        if (schedMeet.hasChanged(MegaChatScheduledMeeting.SC_END.toLong())) {
            changes.add(ScheduledMeetingChanges.EndDate)
        }

        if (schedMeet.hasChanged(MegaChatScheduledMeeting.SC_TITLE.toLong())) {
            changes.add(ScheduledMeetingChanges.Title)
        }

        if (schedMeet.hasChanged(MegaChatScheduledMeeting.SC_DESC.toLong())) {
            changes.add(ScheduledMeetingChanges.Description)
        }

        if (schedMeet.hasChanged(MegaChatScheduledMeeting.SC_ATTR.toLong())) {
            changes.add(ScheduledMeetingChanges.Attributes)
        }

        if (schedMeet.hasChanged(MegaChatScheduledMeeting.SC_OVERR.toLong())) {
            changes.add(ScheduledMeetingChanges.OverrideDateTime)
        }

        if (schedMeet.hasChanged(MegaChatScheduledMeeting.SC_CANC.toLong())) {
            changes.add(ScheduledMeetingChanges.CancelledFlag)
        }

        if (schedMeet.hasChanged(MegaChatScheduledMeeting.SC_FLAGS.toLong())) {
            changes.add(ScheduledMeetingChanges.ScheduledMeetingsFlags)
        }

        if (schedMeet.hasChanged(MegaChatScheduledMeeting.SC_RULES.toLong())) {
            changes.add(ScheduledMeetingChanges.RepetitionRules)
        }

        if (schedMeet.hasChanged(MegaChatScheduledMeeting.SC_FLAGS_SIZE.toLong())) {
            changes.add(ScheduledMeetingChanges.ScheduledMeetingFlagsSize)
        }

        return changes
    }
}