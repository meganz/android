package mega.privacy.android.app.presentation.meeting.chat.view.navigation

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import mega.privacy.android.app.main.megachat.chat.explorer.ChatExplorerActivity
import mega.privacy.android.app.utils.Constants

internal fun openChatPicker(
    context: Context,
    chatId: Long,
    launcher: ActivityResultLauncher<Intent>,
) {
    Intent(context, ChatExplorerActivity::class.java).also { intent ->
        with(intent) {
            action = Constants.ACTION_FORWARD_MESSAGES
            putExtra(Constants.ID_CHAT_FROM, chatId)
            launcher.launch(this)
        }
    }
}