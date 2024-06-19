package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.entity.contacts.EmailInvitationsInputValidity
import mega.privacy.android.domain.entity.contacts.EmailInvitationsInputValidity.AlreadyInContacts
import mega.privacy.android.domain.entity.contacts.EmailInvitationsInputValidity.MyOwnEmail
import mega.privacy.android.domain.entity.contacts.EmailInvitationsInputValidity.Pending
import mega.privacy.android.domain.entity.contacts.EmailInvitationsInputValidity.Valid
import mega.privacy.android.domain.usecase.account.IsTheEmailMineUseCase
import javax.inject.Inject

/**
 * A use case to validate if the input email is valid.
 *
 * @property isTheEmailMineUseCase A use case to check if the given email is the current user's email.
 * @property isEmailInContactsUseCase A use case to check if the given email exists in the visible contacts.
 * @property isEmailInPendingStateUseCase A use case to check if the given email has been invited.
 */
class ValidateEmailInputForInvitationUseCase @Inject constructor(
    private val isTheEmailMineUseCase: IsTheEmailMineUseCase,
    private val isEmailInContactsUseCase: IsEmailInContactsUseCase,
    private val isEmailInPendingStateUseCase: IsEmailInPendingStateUseCase,
) {

    /**
     * Invocation method.
     *
     * @param email The inputted email.
     * @return The validity of the email.
     */
    suspend operator fun invoke(email: String): EmailInvitationsInputValidity =
        when {
            isTheEmailMineUseCase(email) -> MyOwnEmail
            isEmailInContactsUseCase(email) -> AlreadyInContacts
            isEmailInPendingStateUseCase(email) -> Pending
            else -> Valid
        }
}
