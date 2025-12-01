package mega.privacy.android.app.presentation.extensions.contacts

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.navigation.destination.ContactsNavKey
import mega.privacy.android.navigation.destination.ContactsNavKey.NavType
import mega.privacy.android.shared.resources.R as sharedR

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
        context.getString(sharedR.string.contacts_invite_already_received)

    InviteContactRequest.AlreadyContact ->
        context.getString(R.string.context_contact_already_exists, email)

    InviteContactRequest.InvalidEmail ->
        context.getString(R.string.error_own_email_as_contact)

    InviteContactRequest.InvalidStatus -> ""
}

internal fun InviteContactRequest.getAction(context: Context) = when (this) {
    InviteContactRequest.Sent,
    InviteContactRequest.AlreadySent,
        -> context.getString(R.string.tab_sent_requests)

    InviteContactRequest.AlreadyReceived ->
        context.getString(R.string.tab_received_requests)

    else -> null
}

internal fun InviteContactRequest.getNavigation() = when (this) {
    InviteContactRequest.Sent,
    InviteContactRequest.AlreadySent,
        -> ContactsNavKey(NavType.SentRequests)

    InviteContactRequest.AlreadyReceived -> ContactsNavKey(NavType.ReceivedRequests)

    else -> null
}