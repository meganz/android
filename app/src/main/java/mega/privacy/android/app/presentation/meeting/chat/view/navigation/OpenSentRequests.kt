package mega.privacy.android.app.presentation.meeting.chat.view.navigation

import android.content.Context
import mega.privacy.android.app.contacts.ContactsActivity

/**
 * Open sent requests
 */
internal fun openSentRequests(context: Context) {
    context.startActivity(ContactsActivity.getSentRequestsIntent(context))
}