package mega.privacy.android.data.model

import nz.mega.sdk.MegaChatScheduledMeeting
import nz.mega.sdk.MegaChatScheduledMeetingListenerInterface

/**
 * Chat update events corresponding to [MegaChatScheduledMeetingListenerInterface] callbacks.
 */
sealed class ScheduledMeetingUpdate {

    /**
     * On chat scheduled meeting item update.
     *
     * @property item [MegaChatScheduledMeeting] representing a scheduled meeting chat.
     */
    data class OnChatSchedMeetingUpdate(
        val item: MegaChatScheduledMeeting?,
    ) : ScheduledMeetingUpdate()

    /**
     * On chat scheduled meeting occurrences item update.
     *
     * @property chatId     Chat id
     * @property append     If append is true, new occurrences has been received from API (no need to discard current ones)
     */
    data class OnSchedMeetingOccurrencesUpdate(val chatId: Long, val append: Boolean) :
        ScheduledMeetingUpdate()
}
