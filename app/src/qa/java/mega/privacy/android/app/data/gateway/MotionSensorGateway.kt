package mega.privacy.android.app.data.gateway

import android.hardware.SensorEventListener

/**
 * Gateway to interact with @SensorEventListener
 */
interface MotionSensorGateway {

    /**
     * Function to register listener & monitor motion events
     *
     * @param sensorEventListener: @SensorEventListener
     */
    fun monitorMotionEvents(sensorEventListener: SensorEventListener)

    /**
     * Function to unregister @SensorEventListener
     *
     * @param sensorEventListener: @SensorEventListener
     */
    fun unregisterListener(sensorEventListener: SensorEventListener)
}