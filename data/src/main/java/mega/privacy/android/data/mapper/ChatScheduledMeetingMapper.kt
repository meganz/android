package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.chat.ChatScheduledFlags
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.chat.ScheduledMeetingChanges
import nz.mega.sdk.MegaChatScheduledFlags
import nz.mega.sdk.MegaChatScheduledMeeting
import nz.mega.sdk.MegaChatScheduledRules
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Mapper to convert [MegaChatScheduledMeeting] to [ChatScheduledMeeting]
 */
typealias ChatScheduledMeetingMapper = (@JvmSuppressWildcards MegaChatScheduledMeeting) -> @JvmSuppressWildcards ChatScheduledMeeting

private val dateTimeZoneFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss").withZone(ZoneOffset.UTC)

internal fun toChatScheduledMeeting(megaChatScheduledMeeting: MegaChatScheduledMeeting): ChatScheduledMeeting =
    ChatScheduledMeeting(
        megaChatScheduledMeeting.chatId(),
        megaChatScheduledMeeting.schedId(),
        megaChatScheduledMeeting.parentSchedId(),
        megaChatScheduledMeeting.organizerUserId(),
        megaChatScheduledMeeting.timezone()?.mapTimezone(),
        megaChatScheduledMeeting.startDateTime()?.mapTimestamp(),
        megaChatScheduledMeeting.endDateTime()?.mapTimestamp(),
        megaChatScheduledMeeting.title(),
        megaChatScheduledMeeting.description(),
        megaChatScheduledMeeting.attributes(),
        megaChatScheduledMeeting.overrides(),
        megaChatScheduledMeeting.flags()?.mapFlags(),
        megaChatScheduledMeeting.rules()?.mapRules(),
        megaChatScheduledMeeting.mapChanges()
    )

private fun String.mapTimestamp(): ZonedDateTime =
    ZonedDateTime.parse(this, dateTimeZoneFormatter)

private fun String.mapTimezone(): ZoneId =
    ZoneId.of(this)

private fun MegaChatScheduledFlags.mapFlags(): ChatScheduledFlags =
    ChatScheduledFlags(emailsDisabled())

private fun MegaChatScheduledRules.mapRules(): ChatScheduledRules =
    ChatScheduledRules(freq(), interval(), until())

fun MegaChatScheduledMeeting.mapChanges(): ScheduledMeetingChanges =
    when {
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
