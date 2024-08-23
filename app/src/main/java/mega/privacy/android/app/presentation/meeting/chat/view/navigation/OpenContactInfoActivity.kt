package mega.privacy.android.app.presentation.meeting.chat.view.navigation

import android.content.Context
import android.content.Intent
import mega.privacy.android.app.presentation.contactinfo.ContactInfoActivity
import mega.privacy.android.app.utils.Constants

internal fun openContactInfoActivity(
    context: Context,
    email: String,
) {
    Intent(
        context,
        ContactInfoActivity::class.java
    ).apply {
        putExtra(Constants.NAME, email)
    }.also {
        context.startActivity(it)
    }
}