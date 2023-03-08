package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import nz.mega.sdk.MegaChatScheduledMeetingOccurr
import javax.inject.Inject

/**
 * Chat scheduled meeting occurr mapper implementation
 */
internal class ChatScheduledMeetingOccurrMapperImpl @Inject constructor() :
    ChatScheduledMeetingOccurrMapper {

    override fun invoke(megaChatScheduledMeetingOccurr: MegaChatScheduledMeetingOccurr) =
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
}
