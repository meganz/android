package mega.privacy.android.app.presentation.meeting.chat.view.navigation

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.presentation.login.LoginNavKey
import mega.privacy.android.app.utils.Constants.ACTION_JOIN_OPEN_CHAT_LINK

internal fun startLoginActivity(
    context: Context,
    link: String? = null,
) {
    context.startActivity(
        MegaActivity.getIntentWithExtraDestinations(
            context,
            listOf(LoginNavKey())
        ).apply {
            action = ACTION_JOIN_OPEN_CHAT_LINK
            data = link?.toUri()
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        })
}