package mega.privacy.android.app.data.gateway

import android.hardware.SensorEventListener

interface MotionSensorGateway {

    fun monitorMotionEvents(sensorEventListener: SensorEventListener)

    fun unregisterListener(sensorEventListener: SensorEventListener)
}