package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.usecase.account.contactrequest.GetOutgoingContactRequestsUseCase
import javax.inject.Inject

/**
 * A use case to check if the given email has been invited.
 *
 * @property isContactRequestByEmailInPendingOrAcceptedStateUseCase A use case to validate if the requested contact by email is in a pending or accepted state.
 * @property getOutgoingContactRequestsUseCase A use case to get the list of incoming contact requests.
 */
class IsEmailInPendingStateUseCase @Inject constructor(
    private val isContactRequestByEmailInPendingOrAcceptedStateUseCase: IsContactRequestByEmailInPendingOrAcceptedStateUseCase,
    private val getOutgoingContactRequestsUseCase: GetOutgoingContactRequestsUseCase,
) {

    /**
     * Invocation method.
     *
     * @param email The email that needs to be checked.
     * @return Boolean. Whether the email is in a pending state.
     */
    suspend operator fun invoke(email: String): Boolean {
        for (contactRequest in getOutgoingContactRequestsUseCase()) {
            if (isContactRequestByEmailInPendingOrAcceptedStateUseCase(contactRequest, email)) {
                return true
            }
        }
        return false
    }
}
