package mega.privacy.android.app.presentation.meeting.chat.view.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.navigation.destination.FileLinkNavKey

internal fun openFileLinkActivity(
    context: Context,
    fileLink: Uri,
) {
    context.startActivity(
        MegaActivity.getIntentWithExtraDestinations(
            context,
            listOf(FileLinkNavKey(fileLink.toString())),
        ).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    )
}
