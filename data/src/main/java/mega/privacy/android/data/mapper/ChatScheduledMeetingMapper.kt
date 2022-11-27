package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.chat.ChatScheduledFlags
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.chat.ScheduledMeetingChanges
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
        mapFlags(megaChatScheduledMeeting.flags()),
        mapRules(megaChatScheduledMeeting.rules()),
        mapChanges(megaChatScheduledMeeting)
    )

fun mapChanges(megaChatScheduledMeeting: MegaChatScheduledMeeting): ScheduledMeetingChanges =
    when {
        megaChatScheduledMeeting.hasChanged(MegaChatScheduledMeeting.SC_NEW_SCHED.toLong()) -> ScheduledMeetingChanges.NewScheduledMeeting
        megaChatScheduledMeeting.hasChanged(MegaChatScheduledMeeting.SC_PARENT.toLong()) -> ScheduledMeetingChanges.ParentScheduledMeetingId
        megaChatScheduledMeeting.hasChanged(MegaChatScheduledMeeting.SC_TZONE.toLong()) -> ScheduledMeetingChanges.TimeZone
        megaChatScheduledMeeting.hasChanged(MegaChatScheduledMeeting.SC_START.toLong()) -> ScheduledMeetingChanges.StartDate
        megaChatScheduledMeeting.hasChanged(MegaChatScheduledMeeting.SC_END.toLong()) -> ScheduledMeetingChanges.EndDate
        megaChatScheduledMeeting.hasChanged(MegaChatScheduledMeeting.SC_TITLE.toLong()) -> ScheduledMeetingChanges.Title
        megaChatScheduledMeeting.hasChanged(MegaChatScheduledMeeting.SC_DESC.toLong()) -> ScheduledMeetingChanges.Description
        megaChatScheduledMeeting.hasChanged(MegaChatScheduledMeeting.SC_ATTR.toLong()) -> ScheduledMeetingChanges.Attributes
        megaChatScheduledMeeting.hasChanged(MegaChatScheduledMeeting.SC_OVERR.toLong()) -> ScheduledMeetingChanges.OverrideDateTime
        megaChatScheduledMeeting.hasChanged(MegaChatScheduledMeeting.SC_CANC.toLong()) -> ScheduledMeetingChanges.CancelledFlag
        megaChatScheduledMeeting.hasChanged(MegaChatScheduledMeeting.SC_FLAGS.toLong()) -> ScheduledMeetingChanges.ScheduledMeetingsFlags
        megaChatScheduledMeeting.hasChanged(MegaChatScheduledMeeting.SC_RULES.toLong()) -> ScheduledMeetingChanges.RepetitionRules
        else -> ScheduledMeetingChanges.ScheduledMeetingFlagsSize
    }

fun mapFlags(flags: MegaChatScheduledFlags?): ChatScheduledFlags? =
    flags?.let { ChatScheduledFlags(it.emailsDisabled()) }

fun mapRules(rules: MegaChatScheduledRules?): ChatScheduledRules? =
    rules?.let { ChatScheduledRules(it.freq(), it.interval(), it.until()) }
