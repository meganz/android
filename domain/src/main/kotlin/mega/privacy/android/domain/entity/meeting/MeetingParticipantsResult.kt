package mega.privacy.android.domain.entity.meeting

/**
 * Meeting participants
 *
 * @property firstUserChar
 * @property firstUserAvatar
 * @property firstUserColor
 * @property secondUserChar
 * @property secondUserAvatar
 * @property secondUserColor
 */
data class MeetingParticipantsResult(
    val firstUserChar: String? = null,
    val firstUserAvatar: String? = null,
    val firstUserColor: Int? = null,
    val secondUserChar: String? = null,
    val secondUserAvatar: String? = null,
    val secondUserColor: Int? = null,
)
