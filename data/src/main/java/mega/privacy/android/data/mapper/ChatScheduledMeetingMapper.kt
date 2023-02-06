package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.chat.ChatScheduledFlags
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.chat.ScheduledMeetingChanges
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import nz.mega.sdk.MegaChatScheduledFlags
import nz.mega.sdk.MegaChatScheduledMeeting
import nz.mega.sdk.MegaChatScheduledRules

/**
 * Mapper to convert [MegaChatScheduledMeeting] to [ChatScheduledMeeting]
 */
typealias ChatScheduledMeetingMapper = (@JvmSuppressWildcards MegaChatScheduledMeeting) -> @JvmSuppressWildcards ChatScheduledMeeting

internal fun toChatScheduledMeeting(megaChatScheduledMeeting: MegaChatScheduledMeeting): ChatScheduledMeeting =
    ChatScheduledMeeting(
        megaChatScheduledMeeting.chatId(),
        megaChatScheduledMeeting.schedId(),
        megaChatScheduledMeeting.parentSchedId(),
        megaChatScheduledMeeting.organizerUserId(),
        megaChatScheduledMeeting.timezone(),
        megaChatScheduledMeeting.startDateTime(),
        megaChatScheduledMeeting.endDateTime(),
        megaChatScheduledMeeting.title(),
        megaChatScheduledMeeting.description(),
        megaChatScheduledMeeting.attributes(),
        megaChatScheduledMeeting.overrides(),
        megaChatScheduledMeeting.flags()?.mapFlags(),
        megaChatScheduledMeeting.rules()?.mapRules(),
        megaChatScheduledMeeting.mapChanges()
    )

private fun MegaChatScheduledFlags.mapFlags(): ChatScheduledFlags =
    ChatScheduledFlags(emailsDisabled(), isEmpty)

private fun MegaChatScheduledRules.mapRules(): ChatScheduledRules =
    ChatScheduledRules(
        mapToOccurrenceFreq(freq()),
        interval(),
        until()
    )

private fun mapToOccurrenceFreq(freq: Int): OccurrenceFrequencyType =
    when (freq) {
        MegaChatScheduledRules.FREQ_DAILY -> OccurrenceFrequencyType.Daily
        MegaChatScheduledRules.FREQ_WEEKLY -> OccurrenceFrequencyType.Weekly
        MegaChatScheduledRules.FREQ_MONTHLY -> OccurrenceFrequencyType.Monthly
        else -> OccurrenceFrequencyType.Invalid
    }

private fun MegaChatScheduledMeeting.mapChanges(): ScheduledMeetingChanges = when {
    hasChanged(MegaChatScheduledMeeting.SC_NEW_SCHED.toLong()) -> ScheduledMeetingChanges.NewScheduledMeeting
    hasChanged(MegaChatScheduledMeeting.SC_PARENT.toLong()) -> ScheduledMeetingChanges.ParentScheduledMeetingId
    hasChanged(MegaChatScheduledMeeting.SC_TZONE.toLong()) -> ScheduledMeetingChanges.TimeZone
    hasChanged(MegaChatScheduledMeeting.SC_START.toLong()) -> ScheduledMeetingChanges.StartDate
    hasChanged(MegaChatScheduledMeeting.SC_END.toLong()) -> ScheduledMeetingChanges.EndDate
    hasChanged(MegaChatScheduledMeeting.SC_TITLE.toLong()) -> ScheduledMeetingChanges.Title
    hasChanged(MegaChatScheduledMeeting.SC_DESC.toLong()) -> ScheduledMeetingChanges.Description
    hasChanged(MegaChatScheduledMeeting.SC_ATTR.toLong()) -> ScheduledMeetingChanges.Attributes
    hasChanged(MegaChatScheduledMeeting.SC_OVERR.toLong()) -> ScheduledMeetingChanges.OverrideDateTime
    hasChanged(MegaChatScheduledMeeting.SC_CANC.toLong()) -> ScheduledMeetingChanges.CancelledFlag
    hasChanged(MegaChatScheduledMeeting.SC_FLAGS.toLong()) -> ScheduledMeetingChanges.ScheduledMeetingsFlags
    hasChanged(MegaChatScheduledMeeting.SC_RULES.toLong()) -> ScheduledMeetingChanges.RepetitionRules
    else -> ScheduledMeetingChanges.ScheduledMeetingFlagsSize
}
