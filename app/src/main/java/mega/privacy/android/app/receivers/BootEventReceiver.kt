package mega.privacy.android.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Broadcast receiver triggered when the device has finished booting
 */
@AndroidEntryPoint
class BootEventReceiver : BroadcastReceiver() {

    /**
     * Application scope to run coroutines
     */
    @ApplicationScope
    @Inject
    lateinit var applicationScope: CoroutineScope

    /**
     * Schedule camera upload
     */
    @Inject
    lateinit var startCameraUploadUseCase: StartCameraUploadUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Timber.d("BootEventReceiver")
            applicationScope.launch {
                runCatching {
                    startCameraUploadUseCase
                }.onFailure { Timber.e(it) }
            }
        }
    }
}
