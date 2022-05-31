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
import mega.privacy.android.app.domain.exception.LoginAlreadyRunningException
import mega.privacy.android.app.domain.usecase.FastLogin
import mega.privacy.android.app.domain.usecase.FetchNodes
import mega.privacy.android.app.domain.usecase.GetCredentials
import mega.privacy.android.app.domain.usecase.InitMegaChat
import mega.privacy.android.app.domain.usecase.PushReceived
import mega.privacy.android.app.domain.usecase.RetryPendingConnections
import mega.privacy.android.app.domain.usecase.RootNodeExists
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
 * @property retryPendingConnections        Required for retrying pending connections.
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
    private val retryPendingConnections: RetryPendingConnections,
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

                kotlin.runCatching { initMegaChat(session) }
                    .fold(
                        { Timber.d("Init chat success.") },
                        { error ->
                            Timber.e("Init chat error.", error)
                            return@withContext Result.failure()
                        }
                    )

                kotlin.runCatching { fastLogin(session) }
                    .fold(
                        { Timber.d("Fast login success.") },
                        { error ->
                            if (error is LoginAlreadyRunningException) {
                                Timber.d(error, "No more actions required.")
                                return@withContext Result.success()
                            } else {
                                Timber.e("Fast login error.", error)
                                return@withContext Result.failure()
                            }
                        }
                    )

                kotlin.runCatching { fetchNodes() }
                    .fold(
                        { Timber.d("Fetch nodes success.") },
                        { error ->
                            Timber.e("Fetch nodes error.", error)
                            return@withContext Result.failure()
                        }
                    )
            } else {
                retryPendingConnections(disconnect = false)
            }

            Timber.d("PushMessage.type: ${pushMessage.type}")

            if (pushMessage.type == TYPE_CHAT) {
                kotlin.runCatching { pushReceived(pushMessage.shouldBeep()) }
                    .fold(
                        { request ->
                            ChatAdvancedNotificationBuilder.newInstance(applicationContext)
                                .generateChatNotification(request)
                        },
                        { error ->
                            Timber.e("Push received error. ", error)
                            return@withContext Result.failure()
                        }
                    )
            }

            Result.success()
        }

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