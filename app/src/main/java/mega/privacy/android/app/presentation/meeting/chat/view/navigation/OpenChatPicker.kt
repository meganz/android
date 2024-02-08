package mega.privacy.android.app.presentation.meeting.chat.view.navigation

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import mega.privacy.android.app.main.megachat.ChatExplorerActivity

internal fun openChatPicker(
    context: Context,
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
) {
    Intent(context, ChatExplorerActivity::class.java).also {
        launcher.launch(it)
    }
}