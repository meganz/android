package mega.privacy.android.app.utils

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import dagger.hilt.android.EntryPointAccessors
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.di.EntryPointsModule
import timber.log.Timber
import kotlin.math.sqrt

/**
 * Class to detect shake device gesture
 */
class ShakeDetector : SensorEventListener {
    private var shakeListener: OnShakeListener? = null
    private var mShakeTimestamp: Long = 0
    private var mShakeCount = 0
    private val vibrator by lazy {
        EntryPointAccessors.fromApplication(MegaApplication.getInstance(),
            EntryPointsModule.VibratorGateway::class.java).vibrator
    }

    /**
     * Initializes shake listener
     */
    fun setOnShakeListener(listener: OnShakeListener?) {
        shakeListener = listener
    }

    /**
     * Shake listener interface
     */
    interface OnShakeListener {
        /**
         * Passes shake count
         * @param count : Shake count
         */
        fun onShake(count: Int)
    }

    /**
     * detects sensor accuracy change
     *
     * @param sensor : @Sensor object
     * @param accuracy: Accuracy
     */
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // ignore
    }

    /**
     * Detects sensor change
     *
     * @param event: SensorEvent
     */
    @Suppress("DEPRECATION")
    override fun onSensorChanged(event: SensorEvent) {
        shakeListener?.let {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH

            // gForce will be close to 1 when there is no movement.
            val gForce = sqrt((gX * gX + gY * gY + gZ * gZ))
            if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                val now = System.currentTimeMillis()
                // ignore shake events too close to each other (500ms)
                if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                    return
                }

                // reset the shake count after 3 seconds of no shakes
                if (mShakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now) {
                    mShakeCount = 0
                }
                mShakeTimestamp = now
                mShakeCount++
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(200,
                        VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    //deprecated in API 26
                    vibrator.vibrate(200)
                }
                it.onShake(mShakeCount)
            }
        } ?: run {
            Timber.d("Shake listener is null. Please use #setOnShakeListener")
        }
    }

    companion object {
        private const val SHAKE_THRESHOLD_GRAVITY = 2.7f
        private const val SHAKE_SLOP_TIME_MS = 500
        private const val SHAKE_COUNT_RESET_TIME_MS = 3000
    }
}