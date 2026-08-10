package mega.privacy.android.shared.contact.mapper

import androidx.compose.ui.graphics.Color
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.shared.contact.model.AvatarData
import java.io.File
import javax.inject.Inject

/**
 * Maps a domain [ChatAvatarItem] to the presentational [AvatarData] consumed by
 * `ContactItemView`.
 *
 * Produces an [AvatarData.Image] when the chat avatar has a uri, otherwise an
 * [AvatarData.Initials] built from the placeholder text and color.
 */
class ChatAvatarItemMapper @Inject constructor() {

    /**
     * Invoke
     *
     * @param chatAvatarItem Domain chat avatar.
     * @return mapped avatar
     */
    operator fun invoke(chatAvatarItem: ChatAvatarItem): AvatarData {
        val uri = chatAvatarItem.uri
        return if (uri != null) {
            AvatarData.Image(file = File(uri))
        } else {
            AvatarData.Initials(
                initials = chatAvatarItem.placeholderText.orEmpty(),
                avatarColor = chatAvatarItem.color?.let { Color(it) } ?: Color.Black,
            )
        }
    }
}
