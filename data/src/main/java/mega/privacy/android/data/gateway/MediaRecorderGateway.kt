package mega.privacy.android.data.gateway

import java.io.File

/**
 * Media recorder gateway
 */
interface MediaRecorderGateway {
    /**
     * Start recording audio from device microphone to the destination file
     */
    fun startRecording(destination: File)

    /**
     * Stop recording audio
     */
    fun stopRecording()

    /**
     * Get the current max amplitude of the recorded audio. 0 if audio is not being recorded at the moment.
     */
    fun getCurrentMaxAmplitude(): Int
}