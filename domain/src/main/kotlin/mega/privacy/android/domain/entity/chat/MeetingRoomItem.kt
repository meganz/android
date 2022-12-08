package mega.privacy.android.domain.entity.chat

/**
 * Meeting room item
 *
 * @property chatId
 * @property title
 * @property lastMessage
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
 * @property lastUserChar
 * @property lastUserAvatar
 * @property schedId
 * @property scheduledStartTimestamp
 * @property scheduledTimestampFormatted
 * @constructor Create empty Meeting room item
 */
data class MeetingRoomItem constructor(
    val chatId: Long,
    val title: String,
    val lastMessage: String,
    val unreadCount: Int,
    val hasPermissions: Boolean,
    val isActive: Boolean,
    val isPublic: Boolean,
    val isMuted: Boolean,
    val lastTimestamp: Long,
    val lastTimestampFormatted: String,
    val highlight: Boolean = false,
    val firstUserChar: Char? = null,
    val firstUserAvatar: String? = null,
    val lastUserChar: Char? = null,
    val lastUserAvatar: String? = null,
    val schedId: Long? = null,
    val scheduledStartTimestamp: Long? = null,
    val scheduledTimestampFormatted: String? = null,
) {

    fun isSingleMeeting(): Boolean =
        lastUserChar == null
}
