package mega.privacy.android.app.data.gateway

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MotionSensorFacade @Inject constructor(
    @ApplicationContext val context: Context,
) : MotionSensorGateway {

    private lateinit var sensorManager: SensorManager

    override fun monitorMotionEvents(sensorEventListener: SensorEventListener) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val acceleroMeter = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(sensorEventListener,
            acceleroMeter,
            SensorManager.SENSOR_DELAY_UI)
    }

    override fun unregisterListener(sensorEventListener: SensorEventListener) {
        sensorManager.unregisterListener(sensorEventListener)
    }
}