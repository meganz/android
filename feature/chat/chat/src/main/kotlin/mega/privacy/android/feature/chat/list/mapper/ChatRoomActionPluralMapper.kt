package mega.privacy.android.feature.chat.list.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.feature.chat.list.model.ChatRoomActionMessage
import javax.inject.Inject

/**
 * Resolves a count-bearing chat room action message to the wording its quantity calls for in the
 * current locale. The snackbar queue takes either a string resource or finished text, so a plurals
 * resource has to be resolved here before the message can be queued.
 */
internal class ChatRoomActionPluralMapper @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /**
     * @param message Count-bearing message to resolve.
     * @return Text of [message] for the quantity it reports.
     */
    operator fun invoke(message: ChatRoomActionMessage.Plural): String =
        context.resources.getQuantityString(
            message.messageId,
            message.quantity,
            message.quantity,
        )
}
