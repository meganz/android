package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import nz.mega.sdk.MegaChatScheduledMeetingOccurr

/**
 * Chat scheduled meeting occurr mapper
 */
internal fun interface ChatScheduledMeetingOccurrMapper {

    operator fun invoke(megaChatScheduledMeetingOccurr: MegaChatScheduledMeetingOccurr): ChatScheduledMeetingOccurr
}
