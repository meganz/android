package mega.privacy.android.app.mediaplayer

import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent

/**
 * Helper class to manage the MediaSession.
 *
 * @property context Context to use.
 * @property onPlayPauseClicked Callback to be called when the play/pause button is clicked.
 * @property onNextClicked Callback to be called when the next button is clicked.
 * @property onPreviousClicked Callback to be called when the previous button is clicked.
 */
class MediaSessionHelper(
    private val context: Context,
    private val onPlayPauseClicked: () -> Unit,
    private val onNextClicked: () -> Unit,
    private val onPreviousClicked: () -> Unit,
) {
    private var mediaSession: MediaSessionCompat? = null

    /**
     * Setup the MediaSession.
     */
    fun setupMediaSession() {
        mediaSession = MediaSessionCompat(context, TAG).apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                    val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        mediaButtonEvent?.getParcelableExtra(
                            Intent.EXTRA_KEY_EVENT,
                            KeyEvent::class.java
                        )
                    } else {
                        mediaButtonEvent?.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                    }
                    if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
                        when (keyEvent.keyCode) {
                            KeyEvent.KEYCODE_MEDIA_PLAY,
                            KeyEvent.KEYCODE_MEDIA_PAUSE,
                            KeyEvent.KEYCODE_HEADSETHOOK,
                                -> onPlayPauseClicked()

                            KeyEvent.KEYCODE_MEDIA_NEXT -> onNextClicked()

                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> onPreviousClicked()
                        }
                    }
                    return super.onMediaButtonEvent(mediaButtonEvent)
                }
            })
            isActive = true
        }
    }

    /**
     * Release the MediaSession.
     */
    fun releaseMediaSession() {
        mediaSession?.release()
        mediaSession = null
    }

    companion object {
        private const val TAG = "MediaSessionHelper"
    }
}