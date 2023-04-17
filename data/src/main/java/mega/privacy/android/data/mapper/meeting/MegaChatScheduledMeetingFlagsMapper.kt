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
            val megaChatScheduledFlags: MegaChatScheduledFlags =
                MegaChatScheduledFlags.createInstance()
            megaChatScheduledFlags.setEmailsDisabled(flags.isEmailsDisabled)
            return@let megaChatScheduledFlags
        }
}