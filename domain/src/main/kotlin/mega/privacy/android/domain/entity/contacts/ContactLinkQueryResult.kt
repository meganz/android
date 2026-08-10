package mega.privacy.android.domain.entity.contacts

import kotlinx.serialization.Serializable

/**
 * Contact link query result
 *
 * @property isContact
 * @property email
 * @property contactHandle
 * @property contactLinkHandle
 * @property fullName
 * @property status
 * @property avatarFileInBase64
 */
@Serializable
data class ContactLinkQueryResult(
    val isContact: Boolean = false,
    val email: String? = null,
    val contactHandle: Long = -1L,
    val contactLinkHandle: Long = -1L,
    val fullName: String? = null,
    val status: UserChatStatus? = null,
    val avatarFileInBase64: String? = null,
)