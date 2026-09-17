package mega.privacy.android.feature.chat.list.menu

import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.feature.chat.list.mapper.ChatRoomActionPluralMapper
import mega.privacy.android.feature.chat.list.model.ChatRoomActionMessage
import mega.privacy.android.feature.chat.list.model.ChatRoomActionUiItem
import mega.privacy.android.feature.chat.runCatchingCancellable
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import timber.log.Timber
import javax.inject.Inject

/**
 * Resolves and runs the contributed [ChatRoomMenuItem]s for a set of chat rooms, and reports the
 * outcome to the user. Shared by every surface that offers chat room actions, so the chat list and
 * the archived chats list offer the same actions with the same feedback.
 */
internal class ChatRoomActions @Inject constructor(
    private val chatRoomMenuItems: Map<Int, @JvmSuppressWildcards ChatRoomMenuItem>,
    private val chatRoomActionPluralMapper: ChatRoomActionPluralMapper,
    private val snackbarEventQueue: SnackbarEventQueue,
) {

    /**
     * Resolve the ordered actions available for [rooms] by keeping only the contributed actions
     * every chat room is eligible for, each rendered for the set as a whole.
     *
     * @param rooms Chat rooms the actions were requested for.
     * @return Ordered, renderable actions common to [rooms], or empty when none apply.
     */
    suspend fun resolve(rooms: List<ChatRoomItem>): List<ChatRoomActionUiItem> {
        if (rooms.isEmpty()) return emptyList()
        return orderedMenuItems()
            .filter { menuItem -> rooms.all { menuItem.shouldDisplay(it) } }
            .map { it.toUiItem(rooms) }
    }

    /**
     * Run the action identified by [actionId] on [rooms] and queue the message the action reports
     * for the outcome.
     *
     * A bulk action that reaches some of the chats and not others is reported as the failure it
     * partly was, naming only the chats it missed: the chats it did reach have already changed in
     * the list the message appears over, so reporting the whole run as failed would contradict
     * what the user can see.
     *
     * @param actionId Test-tag identity of the picked action.
     * @param rooms Chat rooms the action was picked for.
     */
    suspend fun run(actionId: String, rooms: List<ChatRoomItem>) {
        if (rooms.isEmpty()) return
        val menuItem = chatRoomMenuItems.values.firstOrNull { it.testTag == actionId } ?: return
        val failed = runCatchingCancellable { menuItem.onClick(rooms) }
            .getOrElse { e ->
                Timber.e(e, "Failed to run action $actionId on ${rooms.size} chat rooms")
                rooms
            }
        val message = if (failed.isEmpty()) {
            menuItem.successMessage(rooms)
        } else {
            menuItem.failureMessage(failed)
        }
        message?.let { queue(it) }
    }

    private fun orderedMenuItems(): List<ChatRoomMenuItem> =
        chatRoomMenuItems.entries.sortedBy { it.key }.map { it.value }

    private suspend fun queue(message: ChatRoomActionMessage) {
        runCatchingCancellable {
            when (message) {
                is ChatRoomActionMessage.Text ->
                    snackbarEventQueue.queueMessage(
                        message.messageId,
                        *message.args.toTypedArray(),
                    )

                is ChatRoomActionMessage.Plural ->
                    snackbarEventQueue.queueMessage(chatRoomActionPluralMapper(message))
            }
        }.onFailure { Timber.e(it, "Failed to queue the chat room action message") }
    }
}
