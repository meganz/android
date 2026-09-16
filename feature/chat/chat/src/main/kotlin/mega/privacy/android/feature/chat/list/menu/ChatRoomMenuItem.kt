package mega.privacy.android.feature.chat.list.menu

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.feature.chat.list.model.ChatRoomActionMessage
import mega.privacy.android.feature.chat.list.model.ChatRoomActionUiItem
import mega.privacy.android.feature.chat.list.model.ChatRoomMenuItemConfirmation
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

/**
 * A single, self-contained chat room action offered in the chat list item actions bottom sheet.
 *
 * Each implementation owns its own eligibility ([shouldDisplay]), rendering metadata
 * ([label], [icon], [isDestructive], [testTag]), optional destructive [confirmation], behaviour
 * ([onClick]) and user feedback ([successMessage], [failureMessage]).
 *
 * Rendering and behaviour are both resolved against the whole set of chat rooms the sheet was
 * opened for, never a representative one, so an action that presents differently per chat — a mute
 * toggle, say — shows one label and applies exactly what that label promises to every chat in the
 * selection. New actions are added by contributing another implementation into the
 * `Map<Int, ChatRoomMenuItem>` multibinding, whose unique key fixes the action's position — no
 * central dispatch to change.
 */
interface ChatRoomMenuItem {

    /**
     * Action label string resource resolved for [items].
     *
     * @param items Domain chat rooms the sheet was opened for.
     */
    @StringRes
    fun label(items: List<ChatRoomItem>): Int

    /**
     * Leading icon shown next to the label, resolved for [items].
     *
     * @param items Domain chat rooms the sheet was opened for.
     */
    fun icon(items: List<ChatRoomItem>): ImageVector

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
     * Whether the action is available for [item]. The action is offered only when every chat room
     * in the selection is eligible.
     *
     * @param item Domain chat room to test.
     */
    suspend fun shouldDisplay(item: ChatRoomItem): Boolean

    /**
     * Run the action on every chat room in [items].
     *
     * @param items Domain chat rooms the sheet was opened for.
     * @return The chat rooms the action could not be applied to, empty when it reached them all.
     * Throwing instead reports that none of them were reached.
     */
    suspend fun onClick(items: List<ChatRoomItem>): List<ChatRoomItem>

    /**
     * Message confirming the action ran, or null when it needs no confirmation.
     *
     * @param items Domain chat rooms the action ran on.
     */
    fun successMessage(items: List<ChatRoomItem>): ChatRoomActionMessage? = null

    /**
     * Message reporting that the action failed, or null when the failure needs no message.
     *
     * @param items Domain chat rooms the action was attempted on.
     */
    fun failureMessage(items: List<ChatRoomItem>): ChatRoomActionMessage? = null
}

/**
 * Applies [action] to every chat room instead of stopping at the first failure, so a bulk action
 * that only partly succeeds can be reported as what it was.
 *
 * @return The chat rooms [action] threw for, in their original order.
 */
internal suspend fun List<ChatRoomItem>.failuresOf(
    action: suspend (ChatRoomItem) -> Unit,
): List<ChatRoomItem> = filter { item ->
    runCatchingCancellable { action(item) }
        .onFailure { Timber.e(it, "Chat room action failed for chat ${item.chatId}") }
        .isFailure
}

/**
 * Runs [block] and captures its failures, except when the calling coroutine has itself been
 * cancelled: that cancellation is rethrown so it keeps propagating instead of being reported to
 * the user as an ordinary failure.
 *
 * A [CancellationException] raised while the caller is still active — a timeout inside the call,
 * say — is an ordinary failure, and is captured like any other, so one chat timing out neither
 * abandons the rest of a bulk action nor leaves the user without an outcome.
 */
internal suspend inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    runCatching(block).onFailure { currentCoroutineContext().ensureActive() }

internal fun ChatRoomMenuItem.toUiItem(items: List<ChatRoomItem>): ChatRoomActionUiItem =
    ChatRoomActionUiItem(
        label = label(items),
        icon = icon(items),
        testTag = testTag,
        isDestructive = isDestructive,
        confirmation = confirmation,
    )
