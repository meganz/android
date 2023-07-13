package mega.privacy.android.domain.entity.contacts

/**
 * Contact link
 *
 * @property isContact
 * @property email
 * @property contactHandle
 * @property contactLinkHandle
 * @property fullName
 */
data class ContactLink(
    val isContact: Boolean = false,
    val email: String? = null,
    val contactHandle: Long? = null,
    val contactLinkHandle: Long? = null,
    val fullName: String? = null,
)