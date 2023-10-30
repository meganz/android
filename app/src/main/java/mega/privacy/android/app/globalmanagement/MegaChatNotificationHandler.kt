package mega.privacy.android.app.globalmanagement

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.leolin.shortcutbadger.ShortcutBadger
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.main.megachat.BadgeIntentService
import mega.privacy.android.app.presentation.notifications.chat.ChatMessageNotification
import mega.privacy.android.data.mapper.FileDurationMapper
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.account.GetNotificationCountUseCase
import mega.privacy.android.domain.usecase.chat.IsChatNotifiableUseCase
import mega.privacy.android.domain.usecase.notifications.GetChatMessageNotificationDataUseCase
import mega.privacy.android.domain.usecase.notifications.PushReceivedUseCase
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
 * @property megaChatApi
 * @property application
 * @property activityLifecycleHandler
 */
@Singleton
class MegaChatNotificationHandler @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid,
    private val application: Application,
    private val activityLifecycleHandler: ActivityLifecycleHandler,
    private val getNotificationCountUseCase: GetNotificationCountUseCase,
    private val notificationManager: NotificationManagerCompat,
    private val pushReceivedUseCase: PushReceivedUseCase,
    private val isChatNotifiableUseCase: IsChatNotifiableUseCase,
    private val getChatMessageNotificationDataUseCase: GetChatMessageNotificationDataUseCase,
    private val fileDurationMapper: FileDurationMapper,
    @ApplicationScope private val applicationScope: CoroutineScope,
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

        msg?.apply {
            if (type != MegaChatMessage.TYPE_NORMAL && type != MegaChatMessage.TYPE_NODE_ATTACHMENT &&
                type != MegaChatMessage.TYPE_CONTACT_ATTACHMENT && type != MegaChatMessage.TYPE_CONTAINS_META &&
                type != MegaChatMessage.TYPE_VOICE_CLIP
            ) {
                Timber.d("No notification required $type")
                return
            }

            updateAppBadge()

            val seenMessage = status == MegaChatMessage.STATUS_SEEN

            if (MegaApplication.openChatId == chatId && !seenMessage) {
                Timber.d("Do not update/show notification - opened chat")
                return
            }

            val shouldBeep = if (status == MegaChatMessage.STATUS_NOT_SEEN) {
                when {
                    isDeleted -> {
                        Timber.d("Message deleted")
                        false
                    }

                    isEdited -> {
                        Timber.d("Message edited")
                        false
                    }

                    else -> {
                        Timber.d("New normal message")
                        true
                    }
                }
            } else {
                Timber.d("Message SEEN")
                false
            }

            Timber.d("Should beep: $shouldBeep, Chat: $chatId, message: $msg?.msgId")

            applicationScope.launch {
                runCatching {
                    pushReceivedUseCase(shouldBeep, chatId)
                }.onSuccess {
                    if (!isChatNotifiableUseCase(chatId) || !areNotificationsEnabled())
                        return@launch

                    val data = getChatMessageNotificationDataUseCase(
                        shouldBeep,
                        chatId,
                        msgId,
                        runCatching { DEFAULT_NOTIFICATION_URI.toString() }.getOrNull()
                    ) ?: return@launch

                    ChatMessageNotification.show(
                        application,
                        data,
                        fileDurationMapper
                    )
                }.onFailure { error -> Timber.e(error) }
            }

        } ?: Timber.w("Message is null, no way to notify")
    }

    /**
     * Check if notifications are enabled and required permissions are granted
     *
     * @return  True if are enabled, false otherwise
     */
    private fun areNotificationsEnabled(): Boolean =
        notificationManager.areNotificationsEnabled() &&
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.checkSelfPermission(
                        application,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }

    /**
     * Update app badge
     */
    fun updateAppBadge() = applicationScope.launch {
        Timber.d("updateAppBadge")
        val totalNotifications = runCatching { getNotificationCountUseCase(true) }.getOrNull() ?: 0

        //Add Android version check if needed
        if (totalNotifications == 0) {
            //Remove badge indicator - no unread chats
            ShortcutBadger.applyCount(application, 0)
            //Xiaomi support
            application.startService(
                Intent(application, BadgeIntentService::class.java)
                    .putExtra("badgeCount", 0)
            )
        } else {
            //Show badge with indicator = unread
            ShortcutBadger.applyCount(application, abs(totalNotifications))
            //Xiaomi support
            application.startService(
                Intent(application, BadgeIntentService::class.java)
                    .putExtra("badgeCount", totalNotifications)
            )
        }
    }
}