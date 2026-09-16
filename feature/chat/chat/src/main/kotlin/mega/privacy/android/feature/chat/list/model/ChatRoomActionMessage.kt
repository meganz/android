package mega.privacy.android.feature.chat.list.model

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * User-facing message reported after a chat room action ran, queued as a snackbar.
 */
sealed interface ChatRoomActionMessage {

    /**
     * Message whose wording is fixed, optionally naming the chats it is about.
     *
     * @property messageId Message string resource.
     * @property args Format arguments for [messageId], in order.
     */
    data class Text(
        @param:StringRes val messageId: Int,
        val args: List<Any>,
    ) : ChatRoomActionMessage

    /**
     * Message whose wording depends on how many chats the action reached, so that every language
     * gets the plural form its own rules call for rather than a single count-agnostic phrasing.
     *
     * @property messageId Message plurals resource.
     * @property quantity Number of chats the message reports on, both selecting the plural form and
     * filling the count in it.
     */
    data class Plural(
        @param:PluralsRes val messageId: Int,
        val quantity: Int,
    ) : ChatRoomActionMessage
}
