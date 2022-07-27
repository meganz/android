package mega.privacy.android.app.data.gateway

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Implementation of @MotionSensorGateway to interact with @SensorEventListener
 */
class MotionSensorFacade @Inject constructor(
    @ApplicationContext val context: Context,
) : MotionSensorGateway {

    private lateinit var sensorManager: SensorManager

    /**
     * Function to register listener & monitor motion events
     *
     * @param sensorEventListener: @SensorEventListener
     */
    override fun monitorMotionEvents(sensorEventListener: SensorEventListener) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(sensorEventListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI)
    }

    /**
     * Function to unregister @SensorEventListener
     *
     * @param sensorEventListener: @SensorEventListener
     */
    override fun unregisterListener(sensorEventListener: SensorEventListener) {
        sensorManager.unregisterListener(sensorEventListener)
    }
}