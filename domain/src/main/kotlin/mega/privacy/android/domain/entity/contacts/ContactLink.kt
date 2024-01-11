package mega.privacy.android.domain.entity.contacts

/**
 * Contact link
 *
 * @property isContact
 * @property email
 * @property contactHandle
 * @property contactLinkHandle
 * @property fullName
 * @property status
 */
data class ContactLink(
    val isContact: Boolean = false,
    val email: String? = null,
    val contactHandle: Long = -1L,
    val contactLinkHandle: Long = -1L,
    val fullName: String? = null,
    val status: UserChatStatus? = null,
)