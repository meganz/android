package mega.privacy.android.app.snackbarListeners

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import mega.privacy.android.app.MegaApplication.Companion.getPushNotificationSettingManagement
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.ACTION_CHAT_NOTIFICATION_MESSAGE
import mega.privacy.android.app.utils.Constants.CHAT_ID
import mega.privacy.android.app.utils.Constants.DISMISS_ACTION_SNACKBAR
import mega.privacy.android.app.utils.Constants.EXTRA_MOVE_TO_CHAT_SECTION
import mega.privacy.android.app.utils.Constants.MESSAGE_SNACKBAR_TYPE
import mega.privacy.android.app.utils.Constants.MUTE_NOTIFICATIONS_SNACKBAR_TYPE
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.destination.ChatListNavKey
import mega.privacy.android.navigation.destination.ChatNavKey

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
                if (context is ManagerActivity) {
                    context.moveToChatSection(idChat ?: -1)
                } else {
                    megaNavigator.openManagerActivity(
                        context = context,
                        action = ACTION_CHAT_NOTIFICATION_MESSAGE,
                        bundle = Bundle().apply {
                            idChat?.let { putLong(CHAT_ID, idChat) }
                            putBoolean(EXTRA_MOVE_TO_CHAT_SECTION, true)
                        },
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP,
                        singleActivityDestination = idChat?.let { ChatNavKey(idChat) }
                            ?: ChatListNavKey()
                    )
                    (context as? Activity)?.finish()
                }
            }
        }
    }
}