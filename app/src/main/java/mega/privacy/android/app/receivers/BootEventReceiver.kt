package mega.privacy.android.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
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

    /**
     * Receive an Intent broadcast
     *
     * Beginning Android 15, [Intent.ACTION_BOOT_COMPLETED] Receivers are not allowed to start
     * Foreground Services using dataSync Foreground Service Types
     */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED) &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM
        ) {
            Timber.d("BOOT_COMPLETED received. Starting Camera Uploads")
            applicationScope.launch {
                runCatching {
                    startCameraUploadUseCase()
                }.onFailure { Timber.e(it) }
            }
        }
    }
}
