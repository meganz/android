package mega.privacy.android.app.presentation.apiserver.view.navigation

import android.content.Context
import android.content.Intent
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.utils.Constants.ACTION_REFRESH_API_SERVER
import mega.privacy.android.app.utils.Constants.LOGIN_FRAGMENT
import mega.privacy.android.app.utils.Constants.VISIBLE_FRAGMENT

internal fun openLoginActivity(context: Context, isSingleActivityEnabled: Boolean) {
    val intent = Intent(
        context,
        if (isSingleActivityEnabled) MegaActivity::class.java else LoginActivity::class.java
    ).apply {
        putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
        action = ACTION_REFRESH_API_SERVER
    }
    context.startActivity(intent)
}