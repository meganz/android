package mega.privacy.android.app.presentation.contact.invite.mapper

import mega.privacy.android.app.R
import mega.privacy.android.app.main.model.InviteContactUiState.InvitationStatusMessageUiState
import mega.privacy.android.app.main.model.InviteContactUiState.InvitationStatusMessageUiState.InvitationsSent
import mega.privacy.android.app.main.model.InviteContactUiState.InvitationStatusMessageUiState.NavigateUpWithResult
import mega.privacy.android.app.main.model.InviteContactUiState.MessageTypeUiState.Plural
import mega.privacy.android.app.main.model.InviteContactUiState.MessageTypeUiState.Singular
import mega.privacy.android.app.presentation.achievements.invites.view.InviteFriendsRoute
import mega.privacy.android.app.presentation.contact.invite.navigation.InviteContactScreenResult
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import javax.inject.Inject

/**
 * A mapper class to map the contact invitations result into [InvitationStatusMessageUiState].
 */
class InvitationStatusMessageUiMapper @Inject constructor() {

    /**
     * Invocation method.
     *
     * @param isFromAchievement Whether the request comes from the [InviteFriendsRoute].
     * @param requests The invitations result.
     * @param emails The emails that need to be invited.
     */
    operator fun invoke(
        isFromAchievement: Boolean,
        requests: List<InviteContactRequest>,
        emails: List<String>,
    ): InvitationStatusMessageUiState {
        val totalInvitationSent = requests.count { it == InviteContactRequest.Sent }
        // If the invitation is successful and the total number of invitations is one.
        if (emails.size == 1 && totalInvitationSent == 1 && !isFromAchievement) {
            return InvitationsSent(
                messages = listOf(
                    Singular(
                        id = R.string.context_contact_request_sent,
                        argument = emails[0]
                    )
                )
            )
        } else {
            val totalFailedInvitations = emails.size - totalInvitationSent
            // There are failed invitations.
            if (totalFailedInvitations > 0 && !isFromAchievement) {
                val requestsSent = Plural(
                    id = R.plurals.contact_snackbar_invite_contact_requests_sent,
                    quantity = totalInvitationSent,
                )
                val requestsNotSent = Plural(
                    id = R.plurals.contact_snackbar_invite_contact_requests_not_sent,
                    quantity = totalFailedInvitations,
                )
                return InvitationsSent(messages = listOf(requestsSent, requestsNotSent))
            } else {
                // All invitations are successfully sent.
                if (!isFromAchievement) {
                    return InvitationsSent(
                        messages = listOf(
                            Plural(
                                id = R.plurals.number_correctly_invite_contact_request,
                                quantity = emails.size,
                            )
                        )
                    )
                } else {
                    // Sent back the result to the InviteFriendsRoute.
                    return NavigateUpWithResult(
                        result = InviteContactScreenResult(
                            key = InviteContactScreenResult.KEY_SENT_NUMBER,
                            totalInvitationsSent = totalInvitationSent
                        )
                    )
                }
            }
        }
    }
}
