package mega.privacy.android.app.presentation.meeting.chat.mapper

import mega.privacy.android.app.components.twemoji.EmojiUtilsShortcodes
import mega.privacy.android.app.presentation.meeting.chat.view.ChatAvatar
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReactionUser
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import javax.inject.Inject

/**
 * Mapper to convert a list of [Reaction] to a list of [UIReaction].
 *
 */
class UiReactionListMapper @Inject constructor(
    private val emojiShortCodeMapper: EmojiShortCodeMapper,
) {
    /**
     * Invoke
     *
     * @param reactions List of [Reaction]
     * @return [UIReaction].
     */
    operator fun invoke(reactions: List<Reaction>) =
        reactions.map { reaction ->
            with(reaction) {
                val userList = reaction.userHandles.map {
                    UIReactionUser(
                        userHandle = it,
                        avatarContent = { userHandle, modifier ->
                            ChatAvatar(handle = userHandle, modifier)
                        }
                    )
                }
                UIReaction(
                    reaction = this.reaction,
                    count = count,
                    shortCode = emojiShortCodeMapper(this.reaction),
                    hasMe = hasMe,
                    userList = userList
                )
            }
        }
}