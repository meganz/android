package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.app.presentation.meeting.RecurringMeetingInfoViewModel
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType

/**
 * Data class defining the state of [RecurringMeetingInfoViewModel]
 *
 * @property finish             True, if the activity is to be terminated.
 * @property chatId             Chat id.
 * @property schedId            Scheduled meeting id.
 * @property schedTitle         Scheduled meeting title.
 * @property schedUntil         Timestamp of last occurrence.
 * @property typeOccurs         [OccurrenceFrequencyType].
 * @property occurrencesList    List of [ChatScheduledMeetingOccurr]
 * @property firstParticipant   First participant in the chat room.
 * @property secondParticipant  Last participant in the chat room.
 * @property showSeeMoreButton  True, if see more occurrences button should be shown.
 * @property is24HourFormat     True, if it's 24 hour format.
 **/
data class RecurringMeetingInfoState(
    val finish: Boolean = false,
    val chatId: Long = -1,
    val schedId: Long = -1,
    val schedTitle: String? = "",
    val schedUntil: Long = 0L,
    val typeOccurs: OccurrenceFrequencyType = OccurrenceFrequencyType.Invalid,
    val occurrencesList: List<ChatScheduledMeetingOccurr> = emptyList(),
    val firstParticipant: ChatParticipant? = null,
    val secondParticipant: ChatParticipant? = null,
    val showSeeMoreButton: Boolean = false,
    val is24HourFormat: Boolean = false,
) {
    /**
     * Check if the occurrences list does not contain occurrences
     *
     * @return  true if its empty, false otherwise
     */
    fun isEmptyOccurrencesList(): Boolean = occurrencesList.isEmpty()

    /**
     * Check if the recurring meeting does not contain participants
     *
     * @return  true if its empty, false otherwise
     */
    fun isEmptyMeeting(): Boolean = firstParticipant == null

    /**
     * Check if recurring meeting contains only 1 user
     *
     * @return  true if its single, false otherwise
     */
    fun isSingleMeeting(): Boolean =
        secondParticipant == null
}