package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import nz.mega.sdk.MegaChatScheduledMeetingOccurr
import javax.inject.Inject

/**
 * Chat scheduled meeting occurrence mapper
 */
internal class ChatScheduledMeetingOccurrMapper @Inject constructor() {
    operator fun invoke(megaChatScheduledMeetingOccurr: MegaChatScheduledMeetingOccurr): ChatScheduledMeetingOccurr =
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
        cancelled() > 0
}
