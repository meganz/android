package mega.privacy.android.app.presentation.contact.invite.mapper

import mega.privacy.android.app.R
import mega.privacy.android.app.main.model.InviteContactUiState.MessageTypeUiState.Singular
import mega.privacy.android.app.presentation.contact.invite.model.EmailValidationResult.InvalidResult
import mega.privacy.android.app.presentation.contact.invite.model.EmailValidationResult.ValidResult
import mega.privacy.android.domain.entity.contacts.EmailInvitationsInputValidity
import mega.privacy.android.domain.entity.contacts.EmailInvitationsInputValidity.AlreadyInContacts
import mega.privacy.android.domain.entity.contacts.EmailInvitationsInputValidity.MyOwnEmail
import mega.privacy.android.domain.entity.contacts.EmailInvitationsInputValidity.Pending
import mega.privacy.android.domain.entity.contacts.EmailInvitationsInputValidity.Valid
import javax.inject.Inject

/**
 * A mapper class to map the validity of an email to the email validation result.
 */
class EmailValidationResultMapper @Inject constructor() {

    /**
     * Invocation method.
     *
     * @param email The email.
     * @param validity The validity result of the email.
     * @return The validity result.
     */
    operator fun invoke(email: String, validity: EmailInvitationsInputValidity) = when (validity) {
        Valid -> ValidResult

        MyOwnEmail -> InvalidResult(
            message = Singular(
                R.string.error_own_email_as_contact
            )
        )

        AlreadyInContacts -> InvalidResult(
            message = Singular(
                id = R.string.context_contact_already_exists,
                argument = email
            )
        )

        Pending -> InvalidResult(
            message = Singular(
                id = R.string.invite_not_sent_already_sent,
                argument = email
            )
        )
    }
}
