package mega.privacy.android.app.data.repository

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import mega.privacy.android.app.data.gateway.MotionSensorGateway
import mega.privacy.android.app.data.gateway.VibratorGateway
import mega.privacy.android.app.domain.repository.ShakeDetectorRepository
import mega.privacy.android.app.presentation.featureflag.model.ShakeEvent
import javax.inject.Inject

/**
 * Repository to handle interaction to gateway
 */
class DefaultShakeDetectorRepository @Inject constructor(
    private val vibratorGateway: VibratorGateway,
    private val motionSensorGateway: MotionSensorGateway,
) : ShakeDetectorRepository {

    /**
     * Function to call @VibratorGateway to vibrate device
     */
    override fun vibrateDevice() {
        vibratorGateway.vibrateDevice(SHAKE_INTERVAL)
    }

    /**
     * Function to monitor sensor event and return flow of @ShakeEvent
     *
     * @return Flow of @ShakeEvent
     */
    override fun monitorShakeEvents(): Flow<ShakeEvent> {
        return callbackFlow {
            val sensorEventListener = object : SensorEventListener {
                override fun onSensorChanged(sensorEvent: SensorEvent?) {
                    sensorEvent?.let {
                        trySend(ShakeEvent(it.values[0], it.values[1], it.values[2]))
                    }
                }

                override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
                    // Do Nothing
                }
            }
            motionSensorGateway.monitorMotionEvents(sensorEventListener)
            awaitClose {
                motionSensorGateway.unregisterListener(sensorEventListener)
            }
        }
    }

    companion object {
        private const val SHAKE_INTERVAL = 300L
    }
}