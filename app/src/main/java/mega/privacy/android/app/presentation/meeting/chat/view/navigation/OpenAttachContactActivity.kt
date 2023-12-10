package mega.privacy.android.app.presentation.meeting.chat.view.navigation

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import mega.privacy.android.app.R
import mega.privacy.android.app.main.AddContactActivity
import mega.privacy.android.app.utils.Constants

internal fun openAttachContactActivity(
    context: Context,
    attachContactLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
) {
    Intent(context, AddContactActivity::class.java).apply {
        putExtra(Constants.INTENT_EXTRA_KEY_CONTACT_TYPE, Constants.CONTACT_TYPE_MEGA)
        putExtra(Constants.INTENT_EXTRA_KEY_CHAT, true)
        putExtra(
            Constants.INTENT_EXTRA_KEY_TOOL_BAR_TITLE,
            context.getString(R.string.send_contacts)
        )
    }.also {
        attachContactLauncher.launch(it)
    }
}