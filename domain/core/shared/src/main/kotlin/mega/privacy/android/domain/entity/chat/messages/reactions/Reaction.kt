package mega.privacy.android.domain.entity.chat.messages.reactions

import kotlinx.serialization.Serializable

/**
 * Data class for storing message reactions data.
 *
 * @param reaction [String] of the reaction
 * @param count Number of users who reacted with this reaction
 * @param userHandles List of user handles who reacted with this reaction
 * @param hasMe Whether the current user has reacted with this reaction
 */
@Serializable
data class Reaction(
    val reaction: String,
    val count: Int,
    val userHandles: List<Long>,
    val hasMe: Boolean,
)