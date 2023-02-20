package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import nz.mega.sdk.MegaChatScheduledMeetingOccurr

/**
 * Mapper to convert [MegaChatScheduledMeetingOccurr] to [ChatScheduledMeetingOccurr]
 */
typealias ChatScheduledMeetingOccurrMapper = (@JvmSuppressWildcards MegaChatScheduledMeetingOccurr) -> @JvmSuppressWildcards ChatScheduledMeetingOccurr

internal fun toChatScheduledMeetingOccur(megaChatScheduledMeetingOccurr: MegaChatScheduledMeetingOccurr): ChatScheduledMeetingOccurr =
    ChatScheduledMeetingOccurr(
        megaChatScheduledMeetingOccurr.schedId(),
        megaChatScheduledMeetingOccurr.parentSchedId(),
        megaChatScheduledMeetingOccurr.isCancelled(),
        megaChatScheduledMeetingOccurr.timezone(),
        megaChatScheduledMeetingOccurr.startDateTime(),
        megaChatScheduledMeetingOccurr.endDateTime(),
        megaChatScheduledMeetingOccurr.overrides(),
    )

private fun MegaChatScheduledMeetingOccurr.isCancelled(): Boolean =
    cancelled() != null && cancelled() > 0
