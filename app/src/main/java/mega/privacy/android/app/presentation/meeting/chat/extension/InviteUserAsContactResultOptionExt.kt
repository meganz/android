package mega.privacy.android.app.presentation.meeting.chat.extension

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.InviteUserAsContactResultOption

/**
 * Map InviteUserAsContactResultOption to string
 *
 * @param context
 */
fun InviteUserAsContactResultOption.toInfoText(context: Context) = when (this) {
    is InviteUserAsContactResultOption.ContactInviteSent -> context.getString(R.string.contact_invited)
    is InviteUserAsContactResultOption.ContactAlreadyInvitedError -> context.getString(
        R.string.context_contact_already_invited,
        email
    )

    is InviteUserAsContactResultOption.OwnEmailAsContactError -> context.getString(R.string.error_own_email_as_contact)
    is InviteUserAsContactResultOption.GeneralError -> context.getString(R.string.general_error)
}