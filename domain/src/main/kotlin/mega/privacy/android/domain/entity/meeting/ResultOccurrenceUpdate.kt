package mega.privacy.android.domain.entity.meeting

/**
 * Result of occurrence update
 *
 * @property chatId     Chat Id
 * @property append     If append is true, new occurrences has been received from API (no need to discard current ones)
 */
data class ResultOccurrenceUpdate(val chatId: Long, val append: Boolean)
