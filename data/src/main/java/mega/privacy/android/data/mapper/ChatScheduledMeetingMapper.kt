package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.chat.ChatScheduledFlags
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledRules
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
    )

fun mapFlags(flags: MegaChatScheduledFlags?): ChatScheduledFlags? =
    flags?.let { ChatScheduledFlags(it.emailsDisabled()) }

fun mapRules(rules: MegaChatScheduledRules?): ChatScheduledRules? =
    rules?.let { ChatScheduledRules(it.freq(), it.interval(), it.until()) }
