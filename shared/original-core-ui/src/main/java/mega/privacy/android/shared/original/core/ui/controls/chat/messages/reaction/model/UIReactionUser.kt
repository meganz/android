package mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * User who gives the reaction
 *
 * @property userHandle handle of user
 * @property name name of user
 * @property avatarContent avatar composable of user
 */
data class UIReactionUser(
    val userHandle: Long,
    val name: String = "",
    val avatarContent: @Composable (Long, Modifier) -> Unit = { _, _ -> },
)