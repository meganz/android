package mega.privacy.android.app.main.dialog.contactlink

import android.app.Activity
import dagger.hilt.android.scopes.ActivityScoped
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import javax.inject.Inject

@ActivityScoped
internal class InviteContactRequestStringMapper @Inject constructor(
    private val activity: Activity,
) {
    operator fun invoke(request: InviteContactRequest, email: String): String {
        return when (request) {
            InviteContactRequest.Sent ->
                activity.getString(R.string.context_contact_request_sent, email)

            InviteContactRequest.Resent ->
                activity.getString(R.string.context_contact_invitation_resent)

            InviteContactRequest.Deleted ->
                activity.getString(R.string.context_contact_invitation_deleted)

            InviteContactRequest.AlreadySent ->
                activity.getString(R.string.invite_not_sent_already_sent, email)

            InviteContactRequest.AlreadyContact ->
                activity.getString(R.string.context_contact_already_exists, email)

            InviteContactRequest.InvalidEmail ->
                activity.getString(R.string.error_own_email_as_contact)

            InviteContactRequest.InvalidStatus -> ""
        }
    }
}