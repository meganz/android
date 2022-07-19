package mega.privacy.android.app.data.gateway

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import timber.log.Timber
import javax.inject.Inject

/**
 * Implementation of @VibratorGateway to communicate with system vibrator
 */
@Suppress("DEPRECATION")
class VibratorFacade @Inject constructor(val vibrator: Vibrator) : VibratorGateway {

    /**
     * Function to call system vibrator
     */
    override fun vibrateDevice(mils: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(mils,
                VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(mils)
        }
    }
}