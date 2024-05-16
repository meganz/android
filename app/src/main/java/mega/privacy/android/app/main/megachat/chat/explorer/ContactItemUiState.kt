package mega.privacy.android.app.main.megachat.chat.explorer

import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.contacts.User

/**
 * The contact item for [ChatExplorerListItem] ui state
 *
 * @property contact [Contact]
 * @property user [User]
 * @property lastGreen Time elapsed (minutes) since the last time user was green.
 */
data class ContactItemUiState(
    val contact: Contact? = null,
    val user: User? = null,
    var lastGreen: String = "",
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ContactItemUiState

        if (contact != other.contact) return false
        if (user != other.user) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contact?.hashCode() ?: 0
        result = 31 * result + (user?.hashCode() ?: 0)
        return result
    }
}
