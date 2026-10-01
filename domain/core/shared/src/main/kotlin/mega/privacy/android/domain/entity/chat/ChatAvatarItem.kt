package mega.privacy.android.domain.entity.chat

/**
 * Chat avatar item
 *
 * @property placeholderText
 * @property uri
 * @property color
 * @constructor Create empty Chat avatar item
 */
data class ChatAvatarItem(
    val placeholderText: String? = null,
    val uri: String? = null,
    val color: Int? = null,
)
