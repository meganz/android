package mega.privacy.android.app.snackbarListeners

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import mega.privacy.android.app.MegaApplication.Companion.getPushNotificationSettingManagement
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.utils.Constants

class SnackbarNavigateOption @JvmOverloads constructor(
    private val context: Context,
    private val type: Int? = 0,
    private val userEmail: String? = null,
    private val idChat: Long? = null,
) : View.OnClickListener {
    private val isSentAsMessageSnackbar = idChat != null

    override fun onClick(v: View) {
        //Intent to Settings
        if (type == Constants.DISMISS_ACTION_SNACKBAR) {
            //Do nothing, only dismiss
            return
        }
        if (context is ManagerActivity) {
            if (type == Constants.MUTE_NOTIFICATIONS_SNACKBAR_TYPE) {
                getPushNotificationSettingManagement().controlMuteNotifications(
                    context,
                    Constants.NOTIFICATIONS_ENABLED,
                    null
                )
            } else if (isSentAsMessageSnackbar) {
                idChat?.let { context.moveToChatSection(it) }
            } else {
                context.moveToSettingsSectionStorage()
            }
            return
        }
        val intent = Intent(context, ManagerActivity::class.java)
        if (isSentAsMessageSnackbar) {
            intent.action = Constants.ACTION_CHAT_NOTIFICATION_MESSAGE
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra(Constants.CHAT_ID, idChat)
            intent.putExtra(Constants.EXTRA_MOVE_TO_CHAT_SECTION, true)
        } else {
            intent.action = Constants.ACTION_SHOW_SETTINGS_STORAGE
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
        (context as? Activity)?.finish()
    }
}