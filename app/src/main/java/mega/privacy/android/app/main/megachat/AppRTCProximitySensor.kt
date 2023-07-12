/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
package mega.privacy.android.app.main.megachat

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import org.webrtc.ThreadUtils
import timber.log.Timber

/**
 * AppRTCProximitySensor manages functions related to the proximity sensor in
 * the AppRTC demo.
 * On most device, the proximity sensor is implemented as a boolean-sensor.
 * It returns just two values "NEAR" or "FAR". Thresholding is done on the LUX
 * value i.e. the LUX value of the light sensor is compared with a threshold.
 * A LUX-value more than the threshold means the proximity sensor returns "FAR".
 * Anything less than the threshold value and the sensor  returns "NEAR".
 */
class AppRTCProximitySensor private constructor(context: Context, sensorStateListener: Runnable) :
    SensorEventListener {
    // This class should be created, started and stopped on one thread
    // (e.g. the main thread). We use |nonThreadSafe| to ensure that this is
    // the case. Only active when |DEBUG| is set to true.
    private val threadChecker = ThreadUtils.ThreadChecker()
    private val onSensorStateListener: Runnable?
    private val sensorManager: SensorManager
    private var proximitySensor: Sensor? = null
    private var lastStateReportIsNear = false
    private val powerManager: PowerManager
    private val wakeLock: PowerManager.WakeLock?
    private val field = 0x00000020

    init {
        Timber.d("AppRTCProximitySensor${AppRTCUtils.getThreadInfo()}")
        onSensorStateListener = sensorStateListener
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(field, context.javaClass.name)
    }

    /**
     * Activate the proximity sensor. Also do initialization if called for the
     * first time.
     */
    fun start(): Boolean {
        threadChecker.checkIsOnValidThread()
        Timber.d("start${AppRTCUtils.getThreadInfo()}")
        if (!initDefaultSensor()) {
            // Proximity sensor is not supported on this device.
            return false
        }
        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        return true
    }

    /** Deactivate the proximity sensor.  */
    fun stop() {
        turnOnScreen()
        threadChecker.checkIsOnValidThread()
        Timber.d("stop${AppRTCUtils.getThreadInfo()}")
        if (proximitySensor == null) {
            return
        }
        sensorManager.unregisterListener(this, proximitySensor)
    }

    /** Getter for last reported state. Set to true if "near" is reported.  */
    fun sensorReportsNearState(): Boolean {
        threadChecker.checkIsOnValidThread()
        return lastStateReportIsNear
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        threadChecker.checkIsOnValidThread()
        AppRTCUtils.assertIsTrue(sensor.type == Sensor.TYPE_PROXIMITY)
        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            Timber.e("The values returned by this sensor cannot be trusted")
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        threadChecker.checkIsOnValidThread()
        AppRTCUtils.assertIsTrue(event.sensor.type == Sensor.TYPE_PROXIMITY)
        // As a best practice; do as little as possible within this method and
        // avoid blocking.
        val distanceInCentimeters = event.values[0]
        lastStateReportIsNear = if (distanceInCentimeters < proximitySensor!!.maximumRange) {
            Timber.d("Proximity sensor => NEAR state")
            true
        } else {
            Timber.d("Proximity sensor => FAR state")
            false
        }

        // Report about new state to listening client. Client can then call
        // sensorReportsNearState() to query the current state (NEAR or FAR).
        onSensorStateListener?.run()
        Timber.d("onSensorChanged${AppRTCUtils.getThreadInfo()}: accuracy=${event.accuracy}, timestamp=${event.timestamp}, distance=${event.values[0]}")
    }

    /**
     * Get default proximity sensor if it exists. Tablet devices (e.g. Nexus 7)
     * does not support this type of sensor and false will be returned in such
     * cases.
     */
    private fun initDefaultSensor(): Boolean {
        if (proximitySensor != null) {
            return true
        }
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        if (proximitySensor == null) {
            return false
        }
        logProximitySensorInfo()
        return true
    }

    /** Helper method for logging information about the proximity sensor.  */
    private fun logProximitySensorInfo() {
        if (proximitySensor == null) {
            return
        }
        val info = buildString {
            appendLine("Proximity sensor: ")
            appendLine("\tname: ").append(proximitySensor?.name)
            appendLine("\tvendor: ").append(proximitySensor?.vendor)
            appendLine("\tpower: ").append(proximitySensor?.power)
            appendLine("\tresolution: ").append(proximitySensor?.resolution)
            appendLine("\tmax range: ").append(proximitySensor?.maximumRange)
            appendLine("\tmin delay: ").append(proximitySensor?.minDelay)
            appendLine("\ttype: ").append(proximitySensor?.stringType)
            appendLine("\tmax delay: ").append(proximitySensor?.maxDelay)
            appendLine("\treporting mode: ").append(proximitySensor?.reportingMode)
            appendLine("\tisWakeUpSensor: ").append(proximitySensor?.isWakeUpSensor)
        }
        Timber.d(info)
    }

    /**
     * Turn on screen
     *
     */
    fun turnOnScreen() {
        if (wakeLock != null && wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    /**
     * Turn off screen
     *
     */
    fun turnOffScreen() {
        if (wakeLock != null && !wakeLock.isHeld) {
            wakeLock.acquire()
        }
    }

    companion object {
        /** Construction  */
        fun create(context: Context, sensorStateListener: Runnable): AppRTCProximitySensor {
            return AppRTCProximitySensor(context, sensorStateListener)
        }
    }
}