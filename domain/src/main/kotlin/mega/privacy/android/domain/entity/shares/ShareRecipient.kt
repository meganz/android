package mega.privacy.android.domain.entity.shares

import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.UserChatStatus

/**
 * Share recipient
 */
sealed interface ShareRecipient {
    val email: String
    val permission: AccessPermission
    val isPending: Boolean

    /**
     * Non contact
     *
     * @property email
     * @property permission
     * @property isPending
     */
    data class NonContact(
        override val email: String,
        override val permission: AccessPermission,
        override val isPending: Boolean,
    ) : ShareRecipient

    /**
     * Contact
     *
     * @property handle
     * @property email
     * @property contactData
     * @property isVerified
     * @property permission
     * @property isPending
     * @property status
     * @property defaultAvatarColor
     */
    data class Contact(
        val handle: Long,
        override val email: String,
        val contactData: ContactData,
        val isVerified: Boolean,
        override val permission: AccessPermission,
        override val isPending: Boolean,
        val status: UserChatStatus,
        val defaultAvatarColor: Int,
    ) : ShareRecipient
}