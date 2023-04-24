package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.chat.ChatScheduledFlags
import nz.mega.sdk.MegaChatScheduledFlags
import javax.inject.Inject

/**
 * Mapper to convert [ChatScheduledFlags] to [MegaChatScheduledFlags]
 */
internal class MegaChatScheduledMeetingFlagsMapper @Inject constructor() {
    operator fun invoke(chatScheduledFlags: ChatScheduledFlags?): MegaChatScheduledFlags? =
        chatScheduledFlags?.let { flags ->
            MegaChatScheduledFlags.createInstance().apply {
                setSendEmails(flags.sendEmails)
            }
        }
}