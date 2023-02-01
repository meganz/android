package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.app.presentation.meeting.RecurringMeetingInfoViewModel
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.entity.meeting.OccursType

/**
 * Data class defining the state of [RecurringMeetingInfoViewModel]
 *
 * @property chatId             Chat id.
 * @property schedId            Scheduled meeting id.
 * @property schedTitle         Scheduled meeting title.
 * @property typeOccurs         [OccursType].
 * @property occurrencesList    List of [ChatScheduledMeetingOccurr]
 * @property firstParticipant   First participant in the chat room.
 * @property lastParticipant    Last participant in the chat room.
 **/
data class RecurringMeetingInfoState(
    val chatId: Long = -1,
    val schedId: Long = -1,
    val schedTitle: String = "",
    val typeOccurs: OccursType = OccursType.Daily,
    val occurrencesList: List<ChatScheduledMeetingOccurr> = emptyList(),
    val firstParticipant: ChatParticipant? = null,
    val lastParticipant: ChatParticipant? = null,
)