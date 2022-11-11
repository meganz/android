package mega.privacy.android.app.contacts.group.data

import mega.privacy.android.domain.entity.chat.ChatPermissions
import mega.privacy.android.domain.entity.contacts.ContactItem

/**
 * View item that represents a Group Chat participant at UI level.
 *
 * @property user            [ContactItem]
 * @property permissions     [ChatPermissions].
 */
data class GroupChatParticipant(
    val user: ContactItem,
    val permissions: ChatPermissions,
)
