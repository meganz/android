package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import nz.mega.sdk.MegaChatScheduledMeeting

/**
 * Chat scheduled meeting mapper
 */
internal fun interface ChatScheduledMeetingMapper {

    operator fun invoke(megaChatScheduledMeeting: MegaChatScheduledMeeting): ChatScheduledMeeting
}
