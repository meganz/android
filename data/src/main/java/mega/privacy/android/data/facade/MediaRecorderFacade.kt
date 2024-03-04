package mega.privacy.android.data.facade

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.data.gateway.MediaRecorderGateway
import java.io.File
import javax.inject.Inject

internal class MediaRecorderFacade @Inject constructor(
    @ApplicationContext private val context: Context,
) : MediaRecorderGateway {
    private var androidMediaRecorder: MediaRecorder? = null

    override fun startRecording(destination: File) {
        createNewMediaRecorder().let {
            it.setOutputFile(destination)
            it.prepare()
            it.start()
        }
    }

    override fun stopRecording() {
        androidMediaRecorder?.let {
            runCatching { it.stop() }
            it.reset()
            it.release()
        }
        androidMediaRecorder = null
    }

    override fun getCurrentMaxAmplitude() = androidMediaRecorder?.maxAmplitude ?: 0
    private fun createNewMediaRecorder(): MediaRecorder {
        stopRecording()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.also {
            it.setAudioSource(MediaRecorder.AudioSource.MIC)
            it.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            it.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            it.setAudioEncodingBitRate(50000)
            it.setAudioSamplingRate(44100)
            it.setAudioChannels(1)
            androidMediaRecorder = it
        }
    }
}