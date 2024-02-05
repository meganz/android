package mega.privacy.android.core.ui.controls.chat.messages.reaction.model

/**
 * UI reaction.
 *
 * @property reaction [String] of the reaction
 * @property count Count of the reaction
 * @property hasMe Whether the current user has reacted with this reaction
 */
data class UIReaction(
    val reaction: String,
    val count: Int,
    val hasMe: Boolean,
)