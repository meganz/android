package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.entity.contacts.InviteContactRequest

/**
 * Use case for inviting new contact
 */
fun interface InviteContact {

    /**
     * Invoke
     *
     * @param email Email of the new contact
     * @param handle Handle of the contact
     * @param message Message for the user (can be NULL)
     * @return Invite contact request status
     */
    suspend operator fun invoke(email: String, handle: Long, message: String?): InviteContactRequest
}