package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.chat.ChatScheduledFlags
import nz.mega.sdk.MegaChatScheduledFlags
import javax.inject.Inject

/**
 * Mapper to convert [MegaChatScheduledFlags] to [ChatScheduledFlags]
 */
internal class ChatScheduledMeetingFlagsMapper @Inject constructor(
) {
    operator fun invoke(megaChatScheduledFlags: MegaChatScheduledFlags): ChatScheduledFlags =
        ChatScheduledFlags(megaChatScheduledFlags.emailsDisabled(), megaChatScheduledFlags.isEmpty)
}