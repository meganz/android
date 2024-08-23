package mega.privacy.android.app.presentation.meeting.chat.view.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import mega.privacy.android.app.presentation.filelink.FileLinkComposeActivity
import mega.privacy.android.app.utils.Constants

internal fun openFileLinkActivity(
    context: Context,
    fileLink: Uri,
) {
    Intent(
        context,
        FileLinkComposeActivity::class.java
    ).apply {

        putExtra(
            Constants.OPENED_FROM_CHAT,
            true
        )
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        action = Constants.ACTION_OPEN_MEGA_LINK
        data = fileLink
    }.also {
        context.startActivity(it)
    }
}