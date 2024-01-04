package mega.privacy.android.app.presentation.meeting.chat.view.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.utils.Constants.ACTION_JOIN_OPEN_CHAT_LINK
import mega.privacy.android.app.utils.Constants.LOGIN_FRAGMENT
import mega.privacy.android.app.utils.Constants.VISIBLE_FRAGMENT

internal fun startLoginActivity(
    context: Context,
    link: String? = null,
) {
    context.startActivity(Intent(context, LoginActivity::class.java).apply {
        action = ACTION_JOIN_OPEN_CHAT_LINK
        putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
        data = link?.let { Uri.parse(it) }
        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
    })
}