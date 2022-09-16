package mega.privacy.android.app.globalmanagement

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import me.leolin.shortcutbadger.ShortcutBadger
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.main.megachat.BadgeIntentService
import mega.privacy.android.app.utils.Constants
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatNotificationListenerInterface
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Mega chat notification handler
 *
 * @property megaApi
 * @property megaChatApi
 * @property application
 * @property activityLifecycleHandler
 */
@Singleton
class MegaChatNotificationHandler @Inject constructor(
    @MegaApi
    private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    private val application: Application,
    private val activityLifecycleHandler: ActivityLifecycleHandler,
) : MegaChatNotificationListenerInterface {
    /**
     * On chat notification
     *
     * @param api
     * @param chatId
     * @param msg
     */
    override fun onChatNotification(api: MegaChatApiJava?, chatId: Long, msg: MegaChatMessage?) {
        Timber.d("onChatNotification")

        updateAppBadge()

        if (MegaApplication.getOpenChatId() == chatId) {
            Timber.d("Do not update/show notification - opened chat")
            return
        }

        if (MegaApplication.getInstance().isRecentChatVisible) {
            Timber.d("Do not show notification - recent chats shown")
            return
        }

        if (activityLifecycleHandler.isActivityVisible) {
            try {
                msg ?: return
                val notificationManager =
                    application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(Constants.NOTIFICATION_GENERAL_PUSH_CHAT)
                if (msg.status == MegaChatMessage.STATUS_NOT_SEEN) {
                    if (msg.type == MegaChatMessage.TYPE_NORMAL
                        || msg.type == MegaChatMessage.TYPE_CONTACT_ATTACHMENT
                        || msg.type == MegaChatMessage.TYPE_NODE_ATTACHMENT
                        || msg.type == MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT
                    ) {
                        if (msg.isDeleted) {
                            Timber.d("Message deleted")
                            megaChatApi.pushReceived(false)
                        } else if (msg.isEdited) {
                            Timber.d("Message edited")
                            megaChatApi.pushReceived(false)
                        } else {
                            Timber.d("New normal message")
                            megaChatApi.pushReceived(true)
                        }
                    } else if (msg.type == MegaChatMessage.TYPE_TRUNCATE) {
                        Timber.d("New TRUNCATE message")
                        megaChatApi.pushReceived(false)
                    }
                } else {
                    Timber.d("Message SEEN")
                    megaChatApi.pushReceived(false)
                }
            } catch (e: Exception) {
                Timber.e(e, "EXCEPTION when showing chat notification")
            }
        } else {
            Timber.d("Do not notify chat messages: app in background")
        }
    }

    /**
     * Update app badge
     */
    fun updateAppBadge() {
        Timber.d("updateAppBadge")
        var totalHistoric = 0
        var totalIpc = 0
        if (megaApi.rootNode != null) {
            totalHistoric = megaApi.numUnreadUserAlerts
            totalIpc = megaApi.incomingContactRequests.orEmpty().size
        }
        val chatUnread = megaChatApi.unreadChats
        val totalNotifications = totalHistoric + totalIpc + chatUnread
        //Add Android version check if needed
        if (totalNotifications == 0) {
            //Remove badge indicator - no unread chats
            ShortcutBadger.applyCount(application, 0)
            //Xiaomi support
            application.startService(Intent(application,
                BadgeIntentService::class.java).putExtra("badgeCount", 0))
        } else {
            //Show badge with indicator = unread
            ShortcutBadger.applyCount(application, abs(totalNotifications))
            //Xiaomi support
            application.startService(Intent(application,
                BadgeIntentService::class.java).putExtra("badgeCount", totalNotifications))
        }
    }
}