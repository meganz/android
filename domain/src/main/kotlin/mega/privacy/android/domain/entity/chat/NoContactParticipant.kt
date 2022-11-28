package mega.privacy.android.domain.entity.chat

/**
 * Data class of a MEGA user.
 *
 * @property handle                 Participant identifier.
 * @property email                  Participant email.
 * @property fullName               Participant name
 * @property avatarUri              Participant avatar
 * @property defaultAvatarColor     Participant default avatar color.
 */
data class NoContactParticipant(
    val handle: Long,
    val email: String,
    val fullName: String?,
    val avatarUri: String?,
    val defaultAvatarColor: String,
)
