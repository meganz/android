package mega.privacy.android.app.domain.repository

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.app.data.gateway.MotionSensorGateway
import mega.privacy.android.app.data.gateway.VibratorGateway
import mega.privacy.android.app.presentation.featureflag.model.ShakeEvent
import javax.inject.Inject

class DefaultShakeDetectorRepository @Inject constructor(
    val vibratorGateway: VibratorGateway,
    val motionSensorGateway: MotionSensorGateway,
) : ShakeDetectorRepository {

    override fun vibrateDevice() {
        vibratorGateway.vibrateDevice(SHAKE_INTERVAL)
    }

    override fun getVibrationCount() = flowOf(true)

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