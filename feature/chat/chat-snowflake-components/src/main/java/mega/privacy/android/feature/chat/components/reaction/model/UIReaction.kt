package mega.privacy.android.feature.chat.components.reaction.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.math.pow

/**
 * Data for a reaction.
 *
 * @property reaction The emoji of the reaction
 * @property count Number of users who gave this reaction
 * @property shortCode Short code for the emoji. For example, ":smile:" is the short code for "U+263A"
 * @property hasMe True if the current user gave this reaction
 * @property userList The users who gave this reaction
 */
data class UIReaction(
    val reaction: String,
    val count: Int,
    val shortCode: String = "",
    val hasMe: Boolean,
    val userList: List<UIReactionUser> = emptyList(),
) {
    /**
     * Text that shows the count of this reaction, capped at [maxDigits] digits.
     * For example, 150 with 2 digits shows as "99+".
     */
    fun countString(maxDigits: Int = 2): String {
        val maxValue = (10.0.pow(maxDigits) - 1).toInt()
        return if (count <= maxValue) count.toString() else "$maxValue+"
    }
}

/**
 * User who gave a reaction.
 *
 * @property userHandle Handle of the user
 * @property name Name of the user
 * @property avatarContent Avatar of the user. It receives the user handle and the modifier to apply.
 */
data class UIReactionUser(
    val userHandle: Long,
    val name: String = "",
    val avatarContent: @Composable (Long, Modifier) -> Unit = { _, _ -> },
)
