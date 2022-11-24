package mega.privacy.android.data.model

import nz.mega.sdk.MegaChatScheduledMeeting

/**
 * Chat update events corresponding to [MegaChatScheduledMeetingListenerInterface] callbacks.
 */
sealed class ScheduleMeetingUpdate {

    /**
     * On chat scheduled meeting item update.
     *
     * @property item [MegaChatScheduledMeeting] representing a scheduled meeting chat.
     */
    data class OnChatSchedMeetingUpdate(
        val item: MegaChatScheduledMeeting?,
    ) : ScheduleMeetingUpdate()

    /**
     * On chat scheduled meeting occurrences item update.
     *
     * @property chatId
     */
    data class OnSchedMeetingOccurrencesUpdate(val chatId: Long) : ScheduleMeetingUpdate()
}
