package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.app.presentation.meeting.RecurringMeetingInfoViewModel
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import mega.privacy.android.domain.entity.meeting.OccurrenceItem

/**
 * Data class defining the state of [RecurringMeetingInfoViewModel]
 *
 * @property chatId             Chat id.
 * @property schedId            Scheduled meeting id.
 * @property schedTitle         Scheduled meeting title.
 * @property typeOccurs         [OccurrenceFrequencyType].
 * @property occurrencesList    List of [OccurrenceFrequencyType]
 * @property firstParticipant   First participant in the chat room.
 * @property secondParticipant  Second participant in the chat room.
 **/
data class RecurringMeetingInfoState(
    val chatId: Long = -1,
    val schedId: Long = -1,
    val schedTitle: String? = "",
    val typeOccurs: OccurrenceFrequencyType = OccurrenceFrequencyType.Invalid,
    val occurrencesList: List<OccurrenceItem> = emptyList(),
    val firstParticipant: ChatParticipant? = null,
    val secondParticipant: ChatParticipant? = null,
) {
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