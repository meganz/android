package mega.privacy.android.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_POWER_CONNECTED
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.workers.ScheduleCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Broadcast receiver triggered when an external power has been connected to the device
 */
@AndroidEntryPoint
class ChargeEventReceiver : BroadcastReceiver() {

    /**
     * Application scope to run coroutines
     */
    @ApplicationScope
    @Inject
    lateinit var applicationScope: CoroutineScope

    /**
     *  Start camera upload
     */
    @Inject
    lateinit var startCameraUploadUseCase: StartCameraUploadUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals(ACTION_POWER_CONNECTED)) {
            Timber.d("ChargeEventReceiver")
            applicationScope.launch { startCameraUploadUseCase() }
        }
    }
}
