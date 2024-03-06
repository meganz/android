package mega.privacy.android.app.presentation.meeting.chat.view.navigation

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import mega.privacy.android.app.main.FileExplorerActivity

internal fun openFileExplorerActivity(
    context: Context,
    launcher: ActivityResultLauncher<Intent>
) {
    val intent =
        Intent(context, FileExplorerActivity::class.java).apply {
            action = FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER
        }

    launcher.launch(intent)
}