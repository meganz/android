package mega.privacy.android.domain.entity.contacts.group

import mega.privacy.android.domain.entity.chat.ChatAvatarItem

/**
 * Contact group
 *
 * @property chatId
 * @property title
 * @property avatar
 * @property isPublic
 */
data class ContactGroup(
    val chatId: Long,
    val title: String,
    val avatar: List<ChatAvatarItem>,
    val isPublic: Boolean,
)