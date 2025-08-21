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
import mega.privacy.android.shared.resources.R as sharedR
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
        val totalAlreadyReceived = requests.count { it == InviteContactRequest.AlreadyReceived }
        val totalFailedInvitations = emails.size - totalInvitationSent

        return when {
            isFromAchievement -> {
                // Sent back the result to the InviteFriendsRoute.
                NavigateUpWithResult(
                    result = InviteContactScreenResult(
                        key = InviteContactScreenResult.KEY_SENT_NUMBER,
                        totalInvitationsSent = totalInvitationSent
                    )
                )
            }

            totalInvitationSent == emails.size -> {
                // If all the invitations are successful.
                InvitationsSent(
                    messages = listOf(
                        Singular(id = sharedR.string.contacts_invites_sent)
                    ),
                    actionId = R.string.tab_sent_requests,
                )
            }

            emails.size == 1 && totalAlreadyReceived == 1 -> {
                //If the user you are trying to invite, already sent you an invitation.
                InvitationsSent(
                    messages = listOf(
                        Singular(id = sharedR.string.contacts_invite_already_received)
                    ),
                    actionId = R.string.tab_received_requests,
                )
            }

            emails.size == totalAlreadyReceived -> {
                // If all the users you are trying to invite, already sent you invitations.
                InvitationsSent(
                    messages = listOf(
                        Singular(id = sharedR.string.contacts_invites_already_received)
                    ),
                    actionId = R.string.tab_received_requests,
                )
            }

            totalAlreadyReceived > 0 && totalInvitationSent + totalAlreadyReceived == emails.size -> {
                // If some of the users you are trying to invite, already sent you invitations.
                InvitationsSent(
                    messages = listOf(
                        Plural(
                            id = sharedR.plurals.contacts_invites_sent_but_others_already_received,
                            quantity = totalInvitationSent,
                        )
                    ),
                    actionId = R.string.tab_received_requests,
                )
            }

            else -> {
                // If some of the invitations were sent successfully, and some failed.
                val requestsSent = Plural(
                    id = R.plurals.contact_snackbar_invite_contact_requests_sent,
                    quantity = totalInvitationSent,
                )
                val requestsNotSent = Plural(
                    id = R.plurals.contact_snackbar_invite_contact_requests_not_sent,
                    quantity = totalFailedInvitations,
                )

                InvitationsSent(
                    messages = listOf(requestsSent, requestsNotSent),
                    actionId = R.string.tab_sent_requests,
                )
            }
        }
    }
}
