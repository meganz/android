package mega.privacy.android.app.mediaplayer.service

import android.content.Intent

/**
 * Extending MediaPlayerService is to support two running instances at the same time,
 * video player and audio player, so that video player could "interrupt" audio player,
 * and resume audio player when video player is stopped.
 */
class AudioPlayerService : MediaPlayerService() {
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        //Stop audio player when app is killed.
        stopAudioPlayer()
    }
}
