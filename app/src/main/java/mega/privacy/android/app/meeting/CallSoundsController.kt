package mega.privacy.android.app.meeting

import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import timber.log.Timber
import java.io.IOException
import javax.inject.Singleton


@Singleton
class CallSoundsController constructor(
    private val audioManager: AudioManager
) {

    enum class TypeSound {
        INCOMING_CALL, OUTGOING_CALL, CALL_ENDED
    }

    private var mMediaPlayer: MediaPlayer? = null

    fun playSound(type: TypeSound) {
        if (mMediaPlayer == null)
            mMediaPlayer = MediaPlayer()

        mMediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.stop()
            }
            val res = MegaApplication.getInstance().baseContext.resources
            var afd: AssetFileDescriptor? = null
            when (type) {
                TypeSound.INCOMING_CALL -> {

                    mp.isLooping = true

                }
                TypeSound.OUTGOING_CALL -> {
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_VOICE_CALL,
                        audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL),
                        0
                    )
                    afd = res.openRawResourceFd(R.raw.outgoing_voice_video_call)

                    mp.apply {
                        isLooping = false
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING)
                                .build()
                        )
                    }
                }
                TypeSound.CALL_ENDED -> {
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_VOICE_CALL,
                        audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL),
                        0
                    )
                    afd = res.openRawResourceFd(R.raw.end_call)
                    mp.apply {
                        isLooping = false
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING)
                                .build()
                        )
                    }
                }
            }

            mp.setOnPreparedListener {
                mp.start()
            }

            try {
                afd?.let {
                    mp.setDataSource(
                        it.fileDescriptor,
                        it.startOffset,
                        it.length
                    )

                    mp.prepareAsync()
                }
            } catch (e: IOException) {
                Timber.e("IOException preparing mediaPlayer", e)
            }
        }
    }

    fun pauseSound() {
        mMediaPlayer?.apply {
            if (isPlaying) {
                pause()
            }
        }
    }

    fun stopSound() {
        mMediaPlayer?.apply {
            stop()
            reset()
            release()
        }
    }
}