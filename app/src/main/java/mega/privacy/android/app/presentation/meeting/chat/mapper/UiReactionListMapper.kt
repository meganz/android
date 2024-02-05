package mega.privacy.android.app.presentation.meeting.chat.mapper

import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import javax.inject.Inject

/**
 * Mapper to convert a list of [Reaction] to a list of [UIReaction].
 *
 */
class UiReactionListMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param reactions List of [Reaction]
     * @return [UIReaction].
     */
    operator fun invoke(reactions: List<Reaction>) =
        reactions.map { reaction ->
            with(reaction) {
                UIReaction(
                    reaction = this.reaction,
                    count = count,
                    hasMe = hasMe,
                )
            }
        }
}