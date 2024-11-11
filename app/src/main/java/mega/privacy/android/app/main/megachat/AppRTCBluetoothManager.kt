/*
 *  Copyright 2016 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
package mega.privacy.android.app.main.megachat

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.Process
import mega.privacy.android.domain.entity.call.AudioDevice
import mega.privacy.android.domain.entity.call.BluetoothStates
import org.webrtc.ThreadUtils
import timber.log.Timber
import javax.inject.Inject

/**
 * AppRTCProximitySensor manages functions related to Bluetoth devices in the
 * AppRTC demo.
 */
class AppRTCBluetoothManager @Inject constructor(
    private val apprtcContext: Context,
    private var apprtcAudioManager: AppRTCAudioManager,
) {

    private val handler: Handler
    private var scoConnectionAttempts: Int = 0

    /**
     * BluetoothState
     */
    var bluetoothState: BluetoothStates = BluetoothStates.Uninitialized
    private val bluetoothServiceListener: BluetoothProfile.ServiceListener
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothHeadset: BluetoothHeadset? = null
    private var bluetoothDevice: BluetoothDevice? = null
    private val bluetoothHeadsetReceiver: BroadcastReceiver
    private var isFirstConnection = false

    /**
     * Runs when the Bluetooth timeout expires. We use that timeout after calling
     * startScoAudio() or stopScoAudio() because we're not guaranteed to get a callback after those calls.
     */
    private val bluetoothTimeoutRunnable = Runnable { bluetoothTimeout() }
    private val bluetoothInitTimeoutRunnable = Runnable { updateAudioDeviceState() }

    /**
     * Implementation of an interface that notifies BluetoothProfile IPC clients when they have been
     * connected to or disconnected from the service.
     */
    private inner class BluetoothServiceListener : BluetoothProfile.ServiceListener {
        /** Called to notify the client when the proxy object has been connected to the service.
         * Once we have the profile proxy object, we can use it to monitor the state of the
         * connection and perform other operations that are relevant to the headset profile **/
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HEADSET || bluetoothState == BluetoothStates.Uninitialized) {
                return
            }
            Timber.d("BluetoothServiceListener.onServiceConnected: BT state is $bluetoothState")
            // Android only supports one connected Bluetooth Headset at a time.
            bluetoothHeadset = proxy as BluetoothHeadset
            isFirstConnection = true
            updateAudioDeviceState()
            Timber.d("onServiceConnected done: BT state=$bluetoothState")
        }

        /** Notifies the client when the proxy object has been disconnected from the service.  */
        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HEADSET || bluetoothState == BluetoothStates.Uninitialized) {
                return
            }

            Timber.d("BluetoothServiceListener.onServiceDisconnected: BT state is $bluetoothState")
            stopScoAudio()
            bluetoothHeadset = null
            bluetoothDevice = null
            bluetoothState = BluetoothStates.HeadsetUnavailable
            updateAudioDeviceState()
            Timber.d("onServiceDisconnected done: BT state=$bluetoothState")
        }
    }

    // Intent broadcast receiver which handles changes in Bluetooth device availability.
    // Detects headset changes and Bluetooth SCO state changes.
    private inner class BluetoothHeadsetBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (bluetoothState == BluetoothStates.Uninitialized) {
                return
            }

            // Change in connection state of the Headset profile. Note that the
            // change does not tell us anything about whether we're streaming
            // audio to BT over SCO. Typically received when user turns on a BT
            // headset while audio is active using another audio device.
            when (intent.action) {
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state =
                        intent.getIntExtra(
                            BluetoothHeadset.EXTRA_STATE,
                            BluetoothHeadset.STATE_DISCONNECTED
                        )
                    Timber.d(
                        "BluetoothHeadsetBroadcastReceiver.onReceive: action is ACTION_CONNECTION_STATE_CHANGED, Bluetooth Headset state is ${
                            stateToString(
                                state
                            )
                        }, StickyBroadcast is ${isInitialStickyBroadcast}, bluetooth state is $bluetoothState"
                    )

                    when (state) {
                        BluetoothHeadset.STATE_CONNECTED -> {
                            scoConnectionAttempts = 0
                            updateAudioDeviceState()
                        }

                        BluetoothHeadset.STATE_CONNECTING, BluetoothHeadset.STATE_DISCONNECTING -> {}

                        BluetoothHeadset.STATE_DISCONNECTED -> {
                            // Bluetooth is probably powered off during the call.
                            stopScoAudio()
                            updateAudioDeviceState()
                        }
                    }
                    // Change in the audio (SCO) connection state of the Headset profile.
                    // Typically received after call to startScoAudio() has finalized.
                }

                BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED -> {
                    val state = intent.getIntExtra(
                        BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_AUDIO_DISCONNECTED
                    )
                    Timber.d(
                        "BluetoothHeadsetBroadcastReceiver.onReceive: action is ACTION_AUDIO_STATE_CHANGED, Bluetooth Headset state is ${
                            stateToString(
                                state
                            )
                        }, StickyBroadcast is  ${isInitialStickyBroadcast}, bluetooth state is $bluetoothState"
                    )
                    when (state) {
                        BluetoothHeadset.STATE_AUDIO_CONNECTED -> {
                            cancelTimer()
                            if (bluetoothState == BluetoothStates.SCOConnecting) {
                                Timber.d("Bluetooth audio SCO is now connected")
                                bluetoothState = BluetoothStates.SCOConnected
                                scoConnectionAttempts = 0
                                updateAudioDeviceState()
                            } else {
                                Timber.d("Unexpected state BluetoothHeadset.STATE_AUDIO_CONNECTED")
                            }
                        }

                        BluetoothHeadset.STATE_AUDIO_CONNECTING -> {
                            Timber.d("Bluetooth audio SCO is now connecting...")
                        }

                        BluetoothHeadset.STATE_AUDIO_DISCONNECTED -> {
                            Timber.d("Bluetooth audio SCO is now disconnected")
                            if (isInitialStickyBroadcast) {
                                Timber.d("Ignore STATE_AUDIO_DISCONNECTED initial sticky broadcast.")
                                return
                            }
                            updateAudioDeviceState()
                        }
                    }
                }
            }
        }
    }

    init {
        ThreadUtils.checkIsOnMainThread()
        bluetoothState = BluetoothStates.Uninitialized
        bluetoothServiceListener = BluetoothServiceListener()
        bluetoothHeadsetReceiver = BluetoothHeadsetBroadcastReceiver()
        handler = Handler(Looper.getMainLooper())
        start()
    }

    /**
     * Activates components required to detect Bluetooth devices and to enable
     * BT SCO (audio is routed via BT SCO) for the headset profile. The end
     * state will be HEADSET_UNAVAILABLE but a state machine has started which
     * will start a state change sequence where the final outcome depends on
     * if/when the BT headset is enabled.
     * Example of state change sequence when start() is called while BT device
     * is connected and enabled:
     * UNINITIALIZED --> HEADSET_UNAVAILABLE --> HEADSET_AVAILABLE -->
     * SCO_CONNECTING --> SCO_CONNECTED <==> audio is now routed via BT SCO.
     * Note that the AppRTCAudioManager is also involved in driving this state
     * change.
     **/
    fun start() {
        ThreadUtils.checkIsOnMainThread()
        Timber.d("start")

        if (!hasPermission(apprtcContext, Manifest.permission.BLUETOOTH)) {
            Timber.w("Process (pid=" + Process.myPid() + ") lacks BLUETOOTH permission")
            return
        }
        if (bluetoothState != BluetoothStates.Uninitialized) {
            Timber.w("Invalid BT state")
            return
        }
        bluetoothHeadset = null
        bluetoothDevice = null
        scoConnectionAttempts = 0
        // Get a handle to the default local Bluetooth adapter.
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Timber.w("Device does not support Bluetooth")
            return
        }
        // Ensure that the device supports use of BT SCO audio for off call use cases.
        if (apprtcAudioManager.getAudioManager()?.isBluetoothScoAvailableOffCall == false) {
            Timber.e("Bluetooth SCO audio is not available off call")
            return
        }
        bluetoothAdapter?.let {
            logBluetoothAdapterInfo(it)
        }
        // Establish a connection to the HEADSET profile (includes both Bluetooth Headset and
        // Hands-Free) proxy object and install a listener.
        if (!getBluetoothProfileProxy(
                apprtcContext, bluetoothServiceListener, BluetoothProfile.HEADSET
            )
        ) {
            Timber.e("BluetoothAdapter.getProfileProxy(HEADSET) failed")
            return
        }
        // Register receivers for BluetoothHeadset change notifications.
        val bluetoothHeadsetFilter = IntentFilter()
        // Register receiver for change in connection state of the Headset profile.
        bluetoothHeadsetFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        // Register receiver for change in audio connection state of the Headset profile.
        bluetoothHeadsetFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)
        registerReceiver(bluetoothHeadsetReceiver, bluetoothHeadsetFilter)
        Timber.d("Bluetooth proxy for headset profile has started")
        bluetoothState = BluetoothStates.HeadsetUnavailable
        startTimerInit()
        Timber.d("Start bluetooth, state is $bluetoothState")
    }

    /**
     * Stops and closes all components related to Bluetooth audio.
     */
    fun stop() {
        ThreadUtils.checkIsOnMainThread()
        Timber.d("Stop bluetooth, state is $bluetoothState")
        if (bluetoothAdapter == null) {
            return
        }
        // Stop BT SCO connection with remote device if needed.
        stopScoAudio()
        // Close down remaining BT resources.
        if (bluetoothState == BluetoothStates.Uninitialized) {
            return
        }

        unregisterReceiver(bluetoothHeadsetReceiver)

        cancelTimer()
        if (bluetoothHeadset != null) {
            bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset)
            bluetoothHeadset = null
        }
        bluetoothAdapter = null
        bluetoothDevice = null
        bluetoothState = BluetoothStates.Uninitialized
        Timber.d("stop done: BT state=$bluetoothState")
    }

    /**
     * Starts Bluetooth SCO connection with remote device.
     * Note that the phone application always has the priority on the usage of the SCO connection
     * for telephony. If this method is called while the phone is in call it will be ignored.
     * Similarly, if a call is received or sent while an application is using the SCO connection,
     * the connection will be lost for the application and NOT returned automatically when the call
     * ends. Also note that: up to and including API version JELLY_BEAN_MR1, this method initiates a
     * virtual voice call to the Bluetooth headset. After API version JELLY_BEAN_MR2 only a raw SCO
     * audio connection is established.
     *
     *
     * higher. It might be required to initiates a virtual voice call since many devices do not
     * accept SCO audio without a "call".
     */
    fun startScoAudio(): Boolean {
        ThreadUtils.checkIsOnMainThread()
        Timber.d("startSco: BT state= $bluetoothState, attempts $scoConnectionAttempts, SCO is on: $isScoOn")

        if (scoConnectionAttempts >= MAX_SCO_CONNECTION_ATTEMPTS) {
            Timber.e("BT SCO connection fails - no more attempts")
            return false
        }
        if (bluetoothState != BluetoothStates.HeadsetAvailable) {
            Timber.e("BT SCO connection fails - no headset available")
            return false
        }
        // Start BT SCO channel and wait for ACTION_AUDIO_STATE_CHANGED.
        Timber.d("Starting Bluetooth SCO and waits for ACTION_AUDIO_STATE_CHANGED...")
        // The SCO connection establishment can take several seconds, hence we cannot rely on the
        // connection to be available when the method returns but instead register to receive the
        // intent ACTION_SCO_AUDIO_STATE_UPDATED and wait for the state to be SCO_AUDIO_STATE_CONNECTED.

        bluetoothState = BluetoothStates.SCOConnecting
        apprtcAudioManager.getAudioManager()?.startBluetoothSco()
        apprtcAudioManager.getAudioManager()?.isBluetoothScoOn = true
        scoConnectionAttempts++
        startTimer()
        Timber.d("startScoAudio done: BT state= $bluetoothState SCO is on: $isScoOn")
        return true
    }

    /**
     * Stops Bluetooth SCO connection with remote device.
     */
    fun stopScoAudio() {
        ThreadUtils.checkIsOnMainThread()
        Timber.d("stopScoAudio: BT state= $bluetoothState, SCO is on: $isScoOn")

        if (bluetoothState != BluetoothStates.SCOConnecting && bluetoothState != BluetoothStates.SCOConnected) {
            return
        }
        cancelTimer()
        apprtcAudioManager.getAudioManager()?.stopBluetoothSco()
        apprtcAudioManager.getAudioManager()?.isBluetoothScoOn = false

        bluetoothState = BluetoothStates.SCODisconnecting
        Timber.d("stopScoAudio done: BT state is $bluetoothState, SCO is on $isScoOn")
    }

    /**
     * Devices connected
     */
    val connected: Int
        get() {
            if (bluetoothHeadset != null) {
                bluetoothHeadset?.connectedDevices?.let {
                    return it.size
                }

            }
            return 0
        }

    /**
     * Use the BluetoothHeadset proxy object (controls the Bluetooth Headset
     * Service via IPC) to update the list of connected devices for the HEADSET
     * profile. The internal state will change to HEADSET_UNAVAILABLE or to
     * HEADSET_AVAILABLE and |bluetoothDevice| will be mapped to the connected
     * device if available.
     */
    fun updateDevice() {
        if (bluetoothState == BluetoothStates.Uninitialized || bluetoothHeadset == null) return
        Timber.d("updateDevice")
        // Get connected devices for the headset profile. Returns the set of
        // devices which are in state STATE_CONNECTED. The BluetoothDevice class
        // is just a thin wrapper for a Bluetooth hardware address.
        bluetoothHeadset?.connectedDevices?.let { devices ->
            if (devices.isEmpty()) {
                bluetoothDevice = null
                bluetoothState = BluetoothStates.HeadsetUnavailable
                Timber.d("No connected bluetooth headset")
            } else {
                // Always use first device in list. Android only supports one device.
                bluetoothDevice = devices[0]
                bluetoothState = BluetoothStates.HeadsetAvailable
                if (isFirstConnection) {
                    apprtcAudioManager.changeUserSelectedAudioDeviceForHeadphone(AudioDevice.Bluetooth)
                    isFirstConnection = false
                }
            }
        }

        Timber.d(
            "updateDevice done: BT state=$bluetoothState"
        )
    }

    private fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?) {
        apprtcContext.registerReceiver(receiver, filter)
    }

    private fun unregisterReceiver(receiver: BroadcastReceiver?) {
        apprtcContext.unregisterReceiver(receiver)
    }

    private fun getBluetoothProfileProxy(
        context: Context?, listener: BluetoothProfile.ServiceListener?, profile: Int,
    ): Boolean {
        return bluetoothAdapter!!.getProfileProxy(context, listener, profile)
    }

    private fun hasPermission(context: Context?, permission: String): Boolean {
        return (apprtcContext.checkPermission(permission, Process.myPid(), Process.myUid())
                == PackageManager.PERMISSION_GRANTED)
    }

    /**
     * Logs the state of the local Bluetooth adapter.
     */
    @SuppressLint("HardwareIds")
    private fun logBluetoothAdapterInfo(localAdapter: BluetoothAdapter) {
        // Log the set of BluetoothDevice objects that are bonded (paired) to the local adapter.
        val pairedDevices = localAdapter.bondedDevices
        if (pairedDevices.isNotEmpty()) {
            Timber.d("paired devices:")
            for (device in pairedDevices) {
                Timber.d(" name=" + device?.name + ", address=" + device.address)
            }
        }
    }

    /**
     * Ensures that the audio manager updates its list of available audio devices.
     */
    private fun updateAudioDeviceState() {
        ThreadUtils.checkIsOnMainThread()
        cancelTimeInit()
        apprtcAudioManager.updateAudioDeviceState()
    }

    private fun startTimerInit() {
        ThreadUtils.checkIsOnMainThread()
        handler.postDelayed(bluetoothInitTimeoutRunnable, BLUETOOTH_TIMEOUT_MS.toLong())
    }

    private fun cancelTimeInit() {
        ThreadUtils.checkIsOnMainThread()
        handler.removeCallbacks(bluetoothInitTimeoutRunnable)
    }

    /**
     * Starts timer which times out after BLUETOOTH_SCO_TIMEOUT_MS milliseconds.
     */
    private fun startTimer() {
        ThreadUtils.checkIsOnMainThread()
        handler.postDelayed(bluetoothTimeoutRunnable, BLUETOOTH_SCO_TIMEOUT_MS.toLong())
    }

    /**
     * Cancels any outstanding timer tasks.
     */
    fun cancelTimer() {
        ThreadUtils.checkIsOnMainThread()
        handler.removeCallbacks(bluetoothTimeoutRunnable)
    }

    /**
     * Called when start of the BT SCO channel takes too long time. Usually
     * happens when the BT device has been turned on during an ongoing call.
     */
    private fun bluetoothTimeout() {
        ThreadUtils.checkIsOnMainThread()
        if (bluetoothState == BluetoothStates.Uninitialized || bluetoothHeadset == null) return
        Timber.d("bluetoothTimeout: BT state= $bluetoothState, attempts = $scoConnectionAttempts, SCO is on $isScoOn")

        if (bluetoothState != BluetoothStates.SCOConnecting) {
            return
        }
        // Bluetooth SCO should be connecting; check the latest result.
        var scoConnected = false
        bluetoothHeadset?.connectedDevices?.let { devices ->
            if (devices.size > 0) {
                bluetoothDevice = devices[0]
                if (bluetoothHeadset?.isAudioConnected(bluetoothDevice) == true) {
                    Timber.d("SCO connected with ${bluetoothDevice?.name}")
                    scoConnected = true
                } else {
                    Timber.d("SCO is not connected with " + bluetoothDevice?.name)
                }
            }
        }

        if (scoConnected) {
            // We thought BT had timed out, but it's actually on; updating state.
            bluetoothState = BluetoothStates.SCOConnected
            scoConnectionAttempts = 0
        } else {
            // Give up and "cancel" our request by calling stopBluetoothSco().
            Timber.w("BT failed to connect after timeout")
            stopScoAudio()
        }
        updateAudioDeviceState()
        Timber.d(
            "bluetoothTimeout done: BT state is $bluetoothState"
        )
    }

    private val isScoOn: Boolean
        /**
         * Checks whether audio uses Bluetooth SCO.
         */
        get() = apprtcAudioManager.getAudioManager()?.isBluetoothScoOn == true

    /**
     * Converts BluetoothAdapter states into local string representations.
     */
    private fun stateToString(state: Int): String {
        return when (state) {
            BluetoothAdapter.STATE_DISCONNECTED -> "DISCONNECTED"
            BluetoothAdapter.STATE_CONNECTED -> "CONNECTED"
            BluetoothAdapter.STATE_CONNECTING -> "CONNECTING"
            BluetoothAdapter.STATE_DISCONNECTING -> "DISCONNECTING"
            BluetoothAdapter.STATE_OFF -> "OFF"
            BluetoothAdapter.STATE_ON -> "ON"
            BluetoothAdapter.STATE_TURNING_OFF ->                 // Indicates the local Bluetooth adapter is turning off. Local clients should immediately
                // attempt graceful disconnection of any remote links.
                "TURNING_OFF"

            BluetoothAdapter.STATE_TURNING_ON ->                 // Indicates the local Bluetooth adapter is turning on. However local clients should wait
                // for STATE_ON before attempting to use the adapter.
                "TURNING_ON"

            else -> "INVALID"
        }
    }

    companion object {
        // Timeout interval for starting or stopping audio to a Bluetooth SCO device.
        private const val BLUETOOTH_SCO_TIMEOUT_MS = 4000
        private const val BLUETOOTH_TIMEOUT_MS = 500

        // Maximum number of SCO connection attempts.
        private const val MAX_SCO_CONNECTION_ATTEMPTS = 2

        /**
         * Construction.
         */
        @JvmStatic
        fun create(
            context: Context,
            apprtcAudioManager: AppRTCAudioManager,
        ): AppRTCBluetoothManager {
            return AppRTCBluetoothManager(
                context,
                apprtcAudioManager
            )
        }
    }
}
