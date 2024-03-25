package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.contacts.ContactRequestStatus
import javax.inject.Inject

/**
 * A use case to validate if the requested contact by email is in a pending or accepted state
 */
class IsContactRequestByEmailInPendingOrAcceptedStateUseCase @Inject constructor() {

    /**
     * Invocation method to check the contact's request status
     *
     * @param request [ContactRequest]
     * @param email
     * @return True if the request status is in a pending or accepted state, false otherwise
     */
    operator fun invoke(request: ContactRequest, email: String): Boolean {
        val hasSameEmail = request.targetEmail == email
        val isAccepted = request.status == ContactRequestStatus.Accepted
        val isPending = request.status == ContactRequestStatus.Unresolved
        return hasSameEmail && (isAccepted || isPending)
    }
}
