package mega.privacy.android.app.presentation.apiserver.view.navigation

import android.content.Context
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.utils.Constants.ACTION_REFRESH_API_SERVER
import mega.privacy.android.navigation.destination.LoginNavKey

internal fun openLoginActivity(context: Context) {
    val intent = MegaActivity.getIntentWithExtraDestinations(
        context,
        listOf(LoginNavKey())
    ).apply {
        action = ACTION_REFRESH_API_SERVER
    }
    context.startActivity(intent)
}
