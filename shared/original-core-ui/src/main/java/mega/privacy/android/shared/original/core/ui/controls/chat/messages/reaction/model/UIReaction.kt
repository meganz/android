package mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model

import mega.privacy.android.shared.original.core.ui.controls.chip.formatNumberWithMaxDigits

/**
 * Data for a reaction
 *
 * @property reaction [String] of the reaction
 * @property shortCode short code for the emoji. For example: ":smile:" is the short code for "U+263A"
 * @property count Count of the reaction
 * @property hasMe Whether the current user has given reaction to this message
 * @property userList list of [UIReactionUser] who has given reaction to this message
 */
data class UIReaction(
    val reaction: String,
    val count: Int,
    val shortCode: String = "",
    val hasMe: Boolean,
    val userList: List<UIReactionUser> = emptyList(),
) {
    /**
     * Text to represent the total of reactions of this type
     */
    fun countString(maxDigits: Int = 2) = count.formatNumberWithMaxDigits(maxDigits)
}
