package mega.privacy.android.app.meeting

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import timber.log.Timber
import java.io.IOException

/**
 * Class responsible for playing call-related sounds
 */
class CallSoundsController {

    private var mMediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null

    /**
     * Method for selecting which sound to play
     *
     * @param type CallSoundType
     */
    fun playSound(type: CallSoundType) {
        audioManager =
            MegaApplication.getInstance().baseContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (mMediaPlayer == null)
            mMediaPlayer = MediaPlayer()

        mMediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.stop()
            }

            val res = MegaApplication.getInstance().baseContext.resources
            Timber.d("Play sound: $type")
            when (type) {
                CallSoundType.CALL_ENDED -> playSpecificSound(res.openRawResourceFd(R.raw.end_call))
                CallSoundType.PARTICIPANT_JOINED_CALL -> playSpecificSound(res.openRawResourceFd(R.raw.join_call))
                CallSoundType.PARTICIPANT_LEFT_CALL -> playSpecificSound(res.openRawResourceFd(R.raw.left_call))
                CallSoundType.CALL_RECONNECTING -> playSpecificSound(res.openRawResourceFd(R.raw.reconnecting))
            }
        }
    }

    /**
     * Method to play sound
     *
     * @param afd AssetFileDescriptor
     */
    fun playSpecificSound(afd: AssetFileDescriptor) {
        audioManager?.apply {
            setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                getStreamVolume(AudioManager.STREAM_VOICE_CALL),
                0)
        }

        mMediaPlayer?.apply {
            isLooping = false
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING)
                    .build()
            )
        }

        mMediaPlayer?.setOnPreparedListener {
            mMediaPlayer?.start()
        }

        try {
            afd.let {
                mMediaPlayer?.reset()
                mMediaPlayer?.setDataSource(
                    it.fileDescriptor,
                    it.startOffset,
                    it.length
                )

                mMediaPlayer?.prepareAsync()
            }

        } catch (e: IOException) {
            Timber.e("IOException preparing mediaPlayer", e)
        }

    }

    /**
     * Method to pause the sound
     */
    fun pauseSound() {
        mMediaPlayer?.apply {
            if (isPlaying) {
                pause()
            }
        }
    }

    /**
     * Method to stop the sound
     */
    fun stopSound() {
        mMediaPlayer?.apply {
            stop()
            reset()
        }
    }
}