package mega.privacy.android.domain.entity.call

/**
 * Bluetooth states
 */
enum class BluetoothStates {
    /**
     *  Bluetooth is not available; no adapter or Bluetooth is off
     */
    Uninitialized,

    /**
     *  Bluetooth error happened when trying to start Bluetooth
     */
    Error,

    /**
     * Bluetooth proxy object for the Headset profile exists, but no connected headset devices, SCO is not started or disconnected.
     */
    HeadsetUnavailable,

    /**
     * Bluetooth proxy object for the Headset profile connected, connected Bluetooth headset present, but SCO is not started or disconnected.
     */
    HeadsetAvailable,

    /**
     * Bluetooth audio SCO connection with remote device is closing
     */
    SCODisconnecting,

    /**
     * Bluetooth audio SCO connection with remote device is initiated
     */
    SCOConnecting,

    /**
     * Bluetooth audio SCO connection with remote device is established
     */
    SCOConnected,
}
