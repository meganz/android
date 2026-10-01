package mega.privacy.android.domain.entity.chat.messages.reactions

import kotlinx.serialization.Serializable

/**
 * Data class for storing message reactions data.
 *
 * @param msgId Id of the message
 * @param reaction [String] of the reaction
 * @param count Number of users who reacted with this reaction
 */
@Serializable
data class ReactionUpdate(
    val msgId: Long,
    val reaction: String,
    val count: Int,
)