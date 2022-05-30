package mega.privacy.android.app.fcm

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.domain.usecase.*
import mega.privacy.android.app.fcm.PushMessage.Companion.toPushMessage
import timber.log.Timber

/**
 * Worker class to manage push notifications.
 *
 * @property getCredentials                 Required for checking credentials.
 * @property rootNodeExists                 Required for checking if it is logged in.
 * @property fastLogin                      Required for performing fast login.
 * @property fetchNodes                     Required for fetching nodes.
 * @property initMegaChat                   Required for initializing megaChat.
 * @property pushReceived                   Required for notifying received pushes.
 * @property monitorNodeUpdates             Required for checking share updates.
 * @property monitorContactRequestUpdates   Required for checking contact request updates.
 */
@HiltWorker
class PushMessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getCredentials: GetCredentials,
    private val rootNodeExists: RootNodeExists,
    private val fastLogin: FastLogin,
    private val fetchNodes: FetchNodes,
    private val initMegaChat: InitMegaChat,
    private val pushReceived: PushReceived,
    private val monitorNodeUpdates: MonitorNodeUpdates,
    private val monitorContactRequestUpdates: MonitorContactRequestUpdates,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            val credentials = getCredentials()

            if (credentials == null) {
                Timber.e("No user credentials, process terminates!")
                Result.failure()
            }

            val session = credentials!!.session

            val pushMessage = inputData.toPushMessage()

            if (!rootNodeExists() && !MegaApplication.isLoggingIn()) {
                Timber.d("Needs fast login")

                var success = runInitChat(session)

                if (!success) {
                    return@withContext Result.failure()
                }

                success = runFastLogin(session)

                if (!success) {
                    return@withContext Result.failure()
                }

                success = runFetchNodes()

                if (!success) {
                    return@withContext Result.failure()
                }

                Timber.d("PushMessage.type: ${pushMessage.type}")

                if (pushMessage.type == TYPE_CHAT) {
                    success = runPushReceived(pushMessage.shouldBeep())

                    if (!success) {
                        return@withContext Result.failure()
                    }
                }
            } else {
                Timber.d("No fast login")
                when (pushMessage.type) {
                    TYPE_SHARE_FOLDER, TYPE_CONTACT_REQUEST, TYPE_ACCEPTANCE -> {

                    }
                    TYPE_CALL -> {

                    }
                    TYPE_CHAT -> {
                        val success = runPushReceived(pushMessage.shouldBeep())

                        if (!success) {
                            return@withContext Result.failure()
                        }
                    }
                }
            }

            Result.success()
        }

    /**
     * Performs a fast login.
     *
     * @return True if the request finishes with success, false otherwise.
     */
    private suspend fun runFastLogin(session: String): Boolean =
        kotlin.runCatching { fastLogin(session) }
            .fold(
                { true },
                { error ->
                    Timber.e("Fast login error. ", error)
                    false
                }
            )

    /**
     * Performs a fetch nodes.
     *
     * @return True if the request finishes with success, false otherwise.
     */
    private suspend fun runFetchNodes(): Boolean =
        kotlin.runCatching { fetchNodes() }
            .fold(
                { true },
                { error ->
                    Timber.e("Fetch nodes error. ", error)
                    false
                }
            )

    /**
     * Initializes chat API.
     *
     * @return True if the request finishes with success, false otherwise.
     */
    private suspend fun runInitChat(session: String): Boolean =
        kotlin.runCatching { initMegaChat(session) }
            .fold(
                { true },
                { error ->
                    Timber.e("Init chat error. ", error)
                    false
                }
            )

    /**
     * Notifies a push received.
     *
     * @return True if the request finishes with success, false otherwise.
     */
    private suspend fun runPushReceived(beep: Boolean): Boolean =
        kotlin.runCatching { pushReceived(beep) }
            .fold(
                { request ->
                    ChatAdvancedNotificationBuilder.newInstance(applicationContext)
                        .generateChatNotification(request)
                    true
                },
                { error ->
                    Timber.e("Push received error. ", error)
                    false
                }
            )

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return super.getForegroundInfo()
    }

    companion object {
        private const val TYPE_SHARE_FOLDER = "1"
        private const val TYPE_CHAT = "2"
        private const val TYPE_CONTACT_REQUEST = "3"
        private const val TYPE_CALL = "4"
        private const val TYPE_ACCEPTANCE = "5"
    }
}