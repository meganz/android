package mega.privacy.android.app.presentation.meeting.chat.view.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import mega.privacy.android.app.presentation.folderlink.FolderLinkComposeActivity
import mega.privacy.android.app.utils.Constants

internal fun openFolderLinkActivity(
    context: Context,
    folderLink: Uri,
) {
    Intent(
        context,
        FolderLinkComposeActivity::class.java
    ).apply {

        putExtra(
            Constants.OPENED_FROM_CHAT,
            true
        )
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        action = Constants.ACTION_OPEN_MEGA_FOLDER_LINK
        data = folderLink
    }.also {
        context.startActivity(it)
    }
}