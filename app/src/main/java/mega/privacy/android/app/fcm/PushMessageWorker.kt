package mega.privacy.android.app.fcm

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import mega.privacy.android.app.domain.usecase.GetCredentials
import mega.privacy.android.app.domain.usecase.MonitorContactRequestUpdates
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.domain.usecase.RootNodeExists
import mega.privacy.android.app.fcm.PushMessage.Companion.toPushMessage
import timber.log.Timber

/**
 * Worker class to manage push notifications.
 *
 * @property getCredentials               Required for checking credentials.
 * @property rootNodeExists                 Required for checking if it is logged in.
 * @property monitorNodeUpdates             Required for checking share updates.
 * @property monitorContactRequestUpdates   Required for checking contact request updates.
 */
@HiltWorker
class PushMessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getCredentials: GetCredentials,
    private val rootNodeExists: RootNodeExists,
    private val monitorNodeUpdates: MonitorNodeUpdates,
    private val monitorContactRequestUpdates: MonitorContactRequestUpdates
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val credentials = getCredentials()

        if (credentials == null) {
            Timber.e("No user credentials, process terminates!")
            return Result.failure()
        }

        val pushMessage = inputData.toPushMessage()

        when (pushMessage.type) {
            TYPE_SHARE_FOLDER, TYPE_CONTACT_REQUEST, TYPE_ACCEPTANCE -> {

            }
            TYPE_CALL -> {

            }
            TYPE_CHAT -> {

            }
        }

        return Result.success()
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