package mega.privacy.android.app.presentation.extensions.contacts

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.contacts.InviteContactRequest

internal fun InviteContactRequest.getMessage(context: Context, email: String) = when (this) {
    InviteContactRequest.Sent ->
        context.getString(R.string.context_contact_request_sent, email)

    InviteContactRequest.Resent ->
        context.getString(R.string.context_contact_invitation_resent)

    InviteContactRequest.Deleted ->
        context.getString(R.string.context_contact_invitation_deleted)

    InviteContactRequest.AlreadySent ->
        context.getString(R.string.invite_not_sent_already_sent, email)

    InviteContactRequest.AlreadyReceived ->
        context.getString(R.string.invite_not_sent_already_sent, email)

    InviteContactRequest.AlreadyContact ->
        context.getString(R.string.context_contact_already_exists, email)

    InviteContactRequest.InvalidEmail ->
        context.getString(R.string.error_own_email_as_contact)

    InviteContactRequest.InvalidStatus -> ""
}