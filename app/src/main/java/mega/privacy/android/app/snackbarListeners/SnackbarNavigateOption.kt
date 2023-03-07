package mega.privacy.android.app.snackbarListeners

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import mega.privacy.android.app.MegaApplication.Companion.getPushNotificationSettingManagement
import mega.privacy.android.app.contacts.ContactsActivity.Companion.getSentRequestsIntent
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.controllers.ContactController
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.presentation.manager.model.TransfersTab
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
            } else if (type == Constants.RESUME_TRANSFERS_TYPE) {
                context.selectDrawerItem(DrawerItem.TRANSFERS)
            } else if (isSentAsMessageSnackbar) {
                idChat?.let { context.moveToChatSection(it) }
            } else {
                context.moveToSettingsSectionStorage()
            }
            return
        }
        if (context is ChatActivity) {
            when (type) {
                Constants.INVITE_CONTACT_TYPE -> {
                    ContactController(context).inviteContact(userEmail)
                    return
                }
                Constants.SENT_REQUESTS_TYPE -> {
                    context.startActivity(getSentRequestsIntent(context))
                    return
                }
            }
        }
        val intent = Intent(context, ManagerActivity::class.java)
        if (isSentAsMessageSnackbar) {
            intent.action = Constants.ACTION_CHAT_NOTIFICATION_MESSAGE
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra(Constants.CHAT_ID, idChat)
            intent.putExtra(Constants.EXTRA_MOVE_TO_CHAT_SECTION, true)
        } else if (type == Constants.RESUME_TRANSFERS_TYPE) {
            intent.action = Constants.ACTION_SHOW_TRANSFERS
            intent.putExtra(Constants.OPENED_FROM_IMAGE_VIEWER, true)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            intent.putExtra(ManagerActivity.TRANSFERS_TAB, TransfersTab.PENDING_TAB)
        } else {
            intent.action = Constants.ACTION_SHOW_SETTINGS_STORAGE
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
        (context as? Activity)?.finish()
    }
}