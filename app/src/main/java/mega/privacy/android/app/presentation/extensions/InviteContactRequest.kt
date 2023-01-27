package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.contacts.InviteContactRequest

internal val InviteContactRequest.dialogTitle: Int
    get() = when (this) {
        InviteContactRequest.Sent -> R.string.invite_sent
        else -> R.string.invite_not_sent
    }

internal val InviteContactRequest.dialogContent: Int
    get() = when (this) {
        InviteContactRequest.Sent -> R.string.invite_sent_text
        InviteContactRequest.AlreadyContact -> R.string.invite_not_sent_text_already_contact
        InviteContactRequest.InvalidEmail -> R.string.error_own_email_as_contact
        else -> R.string.invite_not_sent_text_error
    }

internal val InviteContactRequest.success: Boolean
    get() = when (this) {
        InviteContactRequest.Sent, InviteContactRequest.AlreadyContact, InviteContactRequest.InvalidEmail -> true
        else -> false
    }

internal val InviteContactRequest.printEmail: Boolean
    get() = when (this) {
        InviteContactRequest.AlreadyContact -> true
        else -> false
    }