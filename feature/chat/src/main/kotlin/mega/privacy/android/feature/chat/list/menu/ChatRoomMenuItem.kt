package mega.privacy.android.feature.chat.list.menu

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.feature.chat.list.model.ChatRoomActionUiItem
import mega.privacy.android.feature.chat.list.model.ChatRoomMenuItemConfirmation

/**
 * A single, self-contained per-chat action offered in the chat list item actions bottom sheet.
 *
 * Each implementation owns its own eligibility ([shouldDisplay]), rendering metadata
 * ([label], [icon], [isDestructive], [testTag]), optional destructive [confirmation] and
 * behaviour ([onClick]). Rendering is resolved against the domain chat room the sheet was
 * opened for, so a single action can present differently per chat (e.g. a mute toggle).
 * New actions are added by contributing another implementation into the
 * `Map<Int, ChatRoomMenuItem>` multibinding, whose unique key fixes the action's position — no
 * central dispatch to change.
 */
interface ChatRoomMenuItem {

    /**
     * Action label string resource resolved for [item].
     *
     * @param item Domain chat room the sheet was opened for.
     */
    @StringRes
    fun label(item: ChatRoomItem): Int

    /**
     * Leading icon shown next to the label, resolved for [item].
     *
     * @param item Domain chat room the sheet was opened for.
     */
    fun icon(item: ChatRoomItem): ImageVector

    /**
     * Stable identity used both as the row test tag and to run the action.
     */
    val testTag: String

    /**
     * Whether the action is rendered with destructive styling.
     */
    val isDestructive: Boolean
        get() = false

    /**
     * Confirmation prompt to show before running, or null to run immediately.
     */
    val confirmation: ChatRoomMenuItemConfirmation?
        get() = null

    /**
     * Whether the action is available for [item].
     *
     * @param item Domain chat room the sheet was opened for.
     */
    suspend fun shouldDisplay(item: ChatRoomItem): Boolean

    /**
     * Run the action on [item].
     *
     * @param item Domain chat room the sheet was opened for.
     */
    suspend fun onClick(item: ChatRoomItem)
}

internal fun ChatRoomMenuItem.toUiItem(item: ChatRoomItem): ChatRoomActionUiItem =
    ChatRoomActionUiItem(
        label = label(item),
        icon = icon(item),
        testTag = testTag,
        isDestructive = isDestructive,
        confirmation = confirmation,
    )
