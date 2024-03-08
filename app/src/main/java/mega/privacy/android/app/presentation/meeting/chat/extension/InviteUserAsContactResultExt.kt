package mega.privacy.android.app.presentation.meeting.chat.extension

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.InviteUserAsContactResult

/**
 * Map InviteUserAsContactResultOption to string
 *
 * @param context
 */
fun InviteUserAsContactResult.toString(context: Context) = when (this) {
    is InviteUserAsContactResult.ContactInviteSent -> context.getString(R.string.contact_invited)
    is InviteUserAsContactResult.ContactAlreadyInvitedError -> context.getString(
        R.string.context_contact_already_invited,
        email
    )

    is InviteUserAsContactResult.OwnEmailAsContactError -> context.getString(R.string.error_own_email_as_contact)
    is InviteUserAsContactResult.GeneralError -> context.getString(R.string.general_error)
}