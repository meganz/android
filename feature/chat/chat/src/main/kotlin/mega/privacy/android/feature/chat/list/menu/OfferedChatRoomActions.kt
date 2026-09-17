package mega.privacy.android.feature.chat.list.menu

import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.feature.chat.list.model.ChatRoomActionUiItem

/**
 * The chat rooms an actions sheet was rendered from, with the [chatIds] they were resolved for and
 * the [actionIds] that resolution offered.
 *
 * Holding the offer lets a picked action run against the same read that decided which actions to
 * show and what their labels say, so an action reaches exactly the chats whose eligibility put it
 * in the sheet, and one that presents differently per chat applies the direction its label
 * promised. A second read taken at tap time can disagree with the first on both counts.
 */
internal data class OfferedChatRoomActions(
    val chatIds: Set<Long>,
    val rooms: List<ChatRoomItem>,
    val actionIds: Set<String>,
) {

    /**
     * The chat rooms [actionId] was offered for, or null when this offer does not cover it —
     * a different selection, or an action it never included.
     *
     * @param chatIds Ids of the chats the action was picked for.
     * @param actionId Test-tag identity of the picked action.
     */
    fun roomsFor(chatIds: Set<Long>, actionId: String): List<ChatRoomItem>? =
        rooms.takeIf { this.chatIds == chatIds && actionId in actionIds }

    companion object {

        /**
         * The offer [actions] resolved from [rooms] for [chatIds] represents.
         */
        fun of(
            chatIds: Set<Long>,
            rooms: List<ChatRoomItem>,
            actions: List<ChatRoomActionUiItem>,
        ): OfferedChatRoomActions = OfferedChatRoomActions(
            chatIds = chatIds,
            rooms = rooms,
            actionIds = actions.map(ChatRoomActionUiItem::testTag).toSet(),
        )
    }
}
