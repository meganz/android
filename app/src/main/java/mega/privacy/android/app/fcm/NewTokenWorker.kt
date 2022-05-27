package mega.privacy.android.app.fcm

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.privacy.android.app.domain.usecase.GetPushToken
import mega.privacy.android.app.domain.usecase.RegisterPushNotifications
import mega.privacy.android.app.domain.usecase.SetPushToken
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import timber.log.Timber

/**
 * Worker class to manage device token updates.
 *
 * @property getPushToken               Required for getting push token.
 * @property registerPushNotifications  Required for registering push notifications.
 * @property setPushToken               Required for setting push token.
 */
@HiltWorker
class NewTokenWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getPushToken: GetPushToken,
    private val registerPushNotifications: RegisterPushNotifications,
    private val setPushToken: SetPushToken
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            val newToken = inputData.getString(NEW_TOKEN)

            if (newToken.isNullOrEmpty() || getPushToken() == newToken) {
                Timber.d("No need to register new token.")
            } else {
                Timber.d("Push service's new token: $newToken")

                setPushToken(
                    registerPushNotifications
                        .invoke(inputData.getInt(DEVICE_TYPE, INVALID_VALUE), newToken)
                )
            }

            Result.success()
        }

    companion object {
        const val NEW_TOKEN = "NEW_TOKEN"
        const val DEVICE_TYPE = "DEVICE_TYPE"
        const val WORK_NAME = "NewTokenWorker"
    }
}