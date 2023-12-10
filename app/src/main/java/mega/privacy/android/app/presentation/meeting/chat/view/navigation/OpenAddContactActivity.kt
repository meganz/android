package mega.privacy.android.app.presentation.meeting.chat.view.navigation

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import mega.privacy.android.app.R
import mega.privacy.android.app.main.AddContactActivity
import mega.privacy.android.app.utils.Constants

internal fun openAddContactActivity(
    context: Context,
    chatId: Long,
    addContactLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
) {
    val intent =
        Intent(context, AddContactActivity::class.java).apply {
            putExtra(
                AddContactActivity.EXTRA_CONTACT_TYPE,
                Constants.CONTACT_TYPE_MEGA
            )
            putExtra(Constants.INTENT_EXTRA_KEY_CHAT, true)
            putExtra(
                Constants.INTENT_EXTRA_KEY_CHAT_ID,
                chatId
            )
            putExtra(
                Constants.INTENT_EXTRA_KEY_TOOL_BAR_TITLE,
                context.getString(R.string.add_participants_menu_item)
            )
        }

    addContactLauncher.launch(intent)
}