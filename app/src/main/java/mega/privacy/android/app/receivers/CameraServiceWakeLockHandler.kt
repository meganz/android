package mega.privacy.android.app.receivers

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

/**
 * A Handler class that performs Wake Lock operations for Camera Uploads
 *
 * @property context
 */
class CameraServiceWakeLockHandler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var wakeLock: PowerManager.WakeLock? = null

    /**
     * Starts the Wake Lock to prevent the app from sleeping when Camera Uploads
     * proceeds to upload content
     */
    @SuppressLint("WakelockTimeout")
    fun startWakeLock() {
        val powerManager = context.getSystemService(POWER_SERVICE) as PowerManager

        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MegaDownloadServicePowerLock:"
        )

        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire()
            Timber.d("WakeLock has started")
        }
    }

    /**
     * Stops the Wake Lock, restoring the app's normal procedures when it is inactive
     */
    fun stopWakeLock() {
        if (wakeLock?.isHeld == true) {
            try {
                wakeLock?.release()
                Timber.d("WakeLock has stopped")
            } catch (e: Exception) {
                Timber.e("Error stopping WakeLock with Exception: $e")
            }
        }
    }
}