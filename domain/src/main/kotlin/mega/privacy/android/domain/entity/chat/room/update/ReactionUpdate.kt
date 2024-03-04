package mega.privacy.android.domain.entity.chat.room.update

/**
 * Reaction update
 *
 * @property msgId
 * @property reaction
 * @property count
 */
data class ReactionUpdate(
    val msgId: Long,
    val reaction: String,
    val count: Int,
) : Update