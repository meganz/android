package mega.privacy.android.app.snackbarListeners

import android.app.Activity
import android.content.Context
import android.view.View
import mega.privacy.android.app.MegaApplication.Companion.getPushNotificationSettingManagement
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.DISMISS_ACTION_SNACKBAR
import mega.privacy.android.app.utils.Constants.MESSAGE_SNACKBAR_TYPE
import mega.privacy.android.app.utils.Constants.MUTE_NOTIFICATIONS_SNACKBAR_TYPE
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.destination.ChatListNavKey
import mega.privacy.android.navigation.destination.ChatNavKey

@Suppress("unused")
class SnackbarNavigateOption @JvmOverloads constructor(
    private val context: Context,
    private val type: Int? = 0,
    private val idChat: Long? = null,
    private val megaNavigator: MegaNavigator,
) : View.OnClickListener {

    override fun onClick(v: View) {
        when (type) {
            DISMISS_ACTION_SNACKBAR -> {
                //Do nothing, only dismiss
            }

            MUTE_NOTIFICATIONS_SNACKBAR_TYPE -> getPushNotificationSettingManagement().controlMuteNotifications(
                context,
                Constants.NOTIFICATIONS_ENABLED,
                null
            )

            MESSAGE_SNACKBAR_TYPE -> {
                val intent = if (idChat != null) {
                    MegaActivity.getIntentWithExtraDestinations(
                        context,
                        listOf(ChatNavKey(idChat)),
                    )
                } else {
                    MegaActivity.getIntentWithExtraDestinations(
                        context,
                        listOf(ChatListNavKey()),
                    )
                }
                context.startActivity(intent)
                (context as? Activity)?.finish()
            }
        }
    }
}