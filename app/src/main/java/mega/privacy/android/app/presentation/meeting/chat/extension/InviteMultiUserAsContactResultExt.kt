package mega.privacy.android.app.presentation.meeting.chat.extension

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.messages.InviteMultipleUsersAsContactResult

/**
 * Get the string for [InviteMultipleUsersAsContactResult]
 *
 * @param context
 * @return text to show
 */
fun InviteMultipleUsersAsContactResult.toString(context: Context): String = when (this) {
    is InviteMultipleUsersAsContactResult.SomeAlreadyRequestedSomeSent -> context.getString(
        R.string.number_existing_invite_contact_request,
        alreadyRequested
    ) + context.resources.getQuantityString(
        R.plurals.number_correctly_invite_contact_request, sent, sent
    )

    is InviteMultipleUsersAsContactResult.AllSent,
    -> context.resources.getQuantityString(
        R.plurals.number_correctly_invite_contact_request, sent, sent
    )

    is InviteMultipleUsersAsContactResult.SomeFailedSomeSent -> context.resources.getQuantityString(
        R.plurals.contact_snackbar_invite_contact_requests_sent,
        sent,
        sent
    ) + context.resources.getQuantityString(
        R.plurals.contact_snackbar_invite_contact_requests_not_sent,
        notSent,
        notSent
    )
}
