package mega.privacy.android.app.data.gateway

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import javax.inject.Inject

/**
 * Implementation of @VibratorGateway to communicate with system vibrator
 */
@Suppress("DEPRECATION")
class VibratorFacade @Inject constructor(
    /**
     * @param : @Vibrator
     */
    val vibrator: Vibrator,
) : VibratorGateway {

    /**
     * Function to call system vibrator
     *
     * @param milliseconds: Milliseconds to vibrate the device
     */
    override fun vibrateDevice(milliseconds: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds,
                VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(milliseconds)
        }
    }
}