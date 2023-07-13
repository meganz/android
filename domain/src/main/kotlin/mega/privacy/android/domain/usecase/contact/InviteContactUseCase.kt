package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Use case for inviting new contact
 */
class InviteContactUseCase @Inject constructor(
    private val repository: ContactsRepository,
) {

    /**
     * Invoke
     *
     * @param email Email of the new contact
     * @param handle Handle of the contact
     * @param message Message for the user (can be NULL)
     * @return Invite contact request status
     */
    suspend operator fun invoke(email: String, handle: Long, message: String?) =
        repository.inviteContact(email, handle, message)
}