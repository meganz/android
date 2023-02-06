package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType

/**
 * Chat scheduled rules
 *
 * @property freq           The frequency of the scheduled meeting [OccurrenceFrequencyType]
 * @property interval       The repetition interval in relation to the frequency
 * @property until          When the repetitions should end
 */
data class ChatScheduledRules(
    val freq: OccurrenceFrequencyType,
    val interval: Int? = null,
    val until: Long? = null,
)
