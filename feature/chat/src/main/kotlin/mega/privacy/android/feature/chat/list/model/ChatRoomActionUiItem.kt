package mega.privacy.android.feature.chat.list.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Renderable representation of a single chat room action row in the actions bottom sheet.
 *
 * @property label Action label string resource.
 * @property icon Leading icon.
 * @property testTag Stable identity used both as the row test tag and to run the action.
 * @property isDestructive Whether the action is rendered with destructive styling.
 * @property confirmation Confirmation prompt to show before running, or null to run immediately.
 */
@Immutable
data class ChatRoomActionUiItem(
    @param:StringRes val label: Int,
    val icon: ImageVector,
    val testTag: String,
    val isDestructive: Boolean,
    val confirmation: ChatRoomMenuItemConfirmation?,
)
