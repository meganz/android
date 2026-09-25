package mega.privacy.android.app.data.extensions

import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import mega.privacy.android.app.fcm.NewTokenWorker
import mega.privacy.android.app.fcm.PushMessageWorker
import mega.privacy.android.app.notifications.SendChatReplyWorker
import mega.privacy.android.data.facade.debugWorkInfo
import mega.privacy.android.domain.monitoring.CrashReporter

/**
 * Enqueues a [PushMessageWorker] request to manage a push notification.
 *
 * @param data  [Data] containing the push information.
 */
suspend fun WorkManager.enqueuePushMessage(data: Data, crashReporter: CrashReporter) {
    debugWorkInfo(crashReporter)

    enqueue(
        OneTimeWorkRequestBuilder<PushMessageWorker>()
            .setInputData(data)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
    )
}

/**
 * Enqueues a [SendChatReplyWorker] request to send a reply typed in a chat message notification.
 *
 * @param chatId    Chat the reply is sent to.
 * @param message   Reply text.
 */
fun WorkManager.enqueueChatReply(chatId: Long, message: String) {
    enqueue(
        OneTimeWorkRequestBuilder<SendChatReplyWorker>()
            .setInputData(
                Data.Builder()
                    .putLong(SendChatReplyWorker.CHAT_ID, chatId)
                    .putString(SendChatReplyWorker.MESSAGE, message)
                    .build()
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
    )
}

/**
 * Enqueues a unique [NewTokenWorker] request to register push notifications.
 *
 * @param newToken      Required token for register pushes.
 * @param deviceType    Type of device.
 */
suspend fun WorkManager.enqueueUniqueWorkNewToken(
    newToken: String,
    deviceType: Int,
    crashReporter: CrashReporter,
) {
    debugWorkInfo(crashReporter)

    enqueueUniqueWork(
        NewTokenWorker.WORK_NAME,
        ExistingWorkPolicy.REPLACE,
        OneTimeWorkRequestBuilder<NewTokenWorker>()
            .setInputData(
                Data.Builder()
                    .putString(NewTokenWorker.NEW_TOKEN, newToken)
                    .putInt(NewTokenWorker.DEVICE_TYPE, deviceType)
                    .build()
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
    )
}
