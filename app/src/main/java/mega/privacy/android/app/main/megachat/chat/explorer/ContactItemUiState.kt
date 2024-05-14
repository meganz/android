package mega.privacy.android.app.main.megachat.chat.explorer

import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.contacts.User

/**
 * The contact item for [ChatExplorerListItem] ui state
 *
 * @property contact [Contact]
 * @property user [User]
 * @property lastGreen Time elapsed (minutes) since the last time user was green.
 * @property isSelected True if the contact item is selected, false otherwise
 */
data class ContactItemUiState(
    val contact: Contact? = null,
    val user: User? = null,
    val lastGreen: String = "",
    val isSelected: Boolean = false,
)
