package mega.privacy.android.domain.entity.chat

/**
 * Meeting room item
 *
 * @property chatId
 * @property title
 * @property lastMessage
 * @property isLastMessageVoiceClip
 * @property isLastMessageGeolocation
 * @property unreadCount
 * @property hasPermissions
 * @property isActive
 * @property isPublic
 * @property isMuted
 * @property lastTimestamp
 * @property lastTimestampFormatted
 * @property highlight
 * @property firstUserChar
 * @property firstUserAvatar
 * @property firstUserColor
 * @property lastUserChar
 * @property lastUserAvatar
 * @property lastUserColor
 * @property schedId
 * @property isRecurringDaily
 * @property isRecurringWeekly
 * @property isRecurringMonthly
 * @property isPending
 * @property scheduledStartTimestamp
 * @property scheduledEndTimestamp
 * @property scheduledTimestampFormatted
 */
data class MeetingRoomItem constructor(
    val chatId: Long,
    val title: String,
    val lastMessage: String? = null,
    val isLastMessageVoiceClip: Boolean = false,
    val isLastMessageGeolocation: Boolean = false,
    val unreadCount: Int,
    val hasPermissions: Boolean,
    val isActive: Boolean,
    val isPublic: Boolean,
    val isMuted: Boolean,
    val lastTimestamp: Long,
    val lastTimestampFormatted: String? = null,
    val highlight: Boolean = false,
    val firstUserChar: Char? = null,
    val firstUserAvatar: String? = null,
    val firstUserColor: Int? = null,
    val lastUserChar: Char? = null,
    val lastUserAvatar: String? = null,
    val lastUserColor: Int? = null,
    val schedId: Long? = null,
    val isRecurringDaily: Boolean = false,
    val isRecurringWeekly: Boolean = false,
    val isRecurringMonthly: Boolean = false,
    val isPending: Boolean = false,
    val scheduledStartTimestamp: Long? = null,
    val scheduledEndTimestamp: Long? = null,
    val scheduledTimestampFormatted: String? = null
) {

    fun isSingleMeeting(): Boolean =
        lastUserChar == null

    fun isScheduledMeeting(): Boolean =
        schedId != null

    fun isRecurring(): Boolean =
        isRecurringDaily || isRecurringWeekly || isRecurringMonthly
}
