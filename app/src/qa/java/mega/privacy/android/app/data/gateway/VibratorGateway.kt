package mega.privacy.android.app.data.gateway

/**
 * Gateway to communicate with system vibrator
 */
interface VibratorGateway {

    /**
     * Function to call system vibrator
     *
     * @param milliseconds : Milliseconds to vibrate the device
     */
    fun vibrateDevice(milliseconds: Long)
}