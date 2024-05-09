package mega.privacy.android.app.contacts.usecase

import mega.privacy.android.domain.entity.contacts.ContactRequestAction
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Use case to manage existing contact requests for current user.
 */
class ManageContactRequestUseCase @Inject constructor(private val contactsRepository: ContactsRepository) {
    /**
     * Invoke
     *
     * @param requestHandle         contact request identifier
     * @param contactRequestAction  contact request action
     */
    suspend operator fun invoke(requestHandle: Long, contactRequestAction: ContactRequestAction) =
        when (contactRequestAction) {
            ContactRequestAction.Accept, ContactRequestAction.Ignore, ContactRequestAction.Deny ->
                contactsRepository.manageReceivedContactRequest(requestHandle, contactRequestAction)

            ContactRequestAction.Remind, ContactRequestAction.Delete ->
                contactsRepository.manageSentContactRequest(requestHandle, contactRequestAction)

            else -> throw IllegalArgumentException("Invalid Reply Contact Request Action $contactRequestAction")
        }
}
