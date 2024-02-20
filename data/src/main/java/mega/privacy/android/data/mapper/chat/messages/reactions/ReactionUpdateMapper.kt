package mega.privacy.android.data.mapper.chat.messages.reactions

import mega.privacy.android.data.model.ChatRoomUpdate
import mega.privacy.android.domain.entity.chat.messages.reactions.ReactionUpdate
import javax.inject.Inject

/**
 * Mapper to convert [ChatRoomUpdate.OnReactionUpdate] to a [ReactionUpdate].
 */
class ReactionUpdateMapper @Inject constructor() {

    /**
     * Invoke.
     *
     * @param onReactionUpdate [ChatRoomUpdate.OnReactionUpdate].
     * @return [ReactionUpdate].
     */
    operator fun invoke(onReactionUpdate: ChatRoomUpdate.OnReactionUpdate) =
        with(onReactionUpdate) {
            ReactionUpdate(
                msgId,
                reaction,
                count
            )
        }
}