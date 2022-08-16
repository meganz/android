package mega.privacy.android.app.mediaplayer.service

import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.di.mediaplayer.AudioPlayer
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerServiceGateway
import javax.inject.Inject

/**
 * Extending MediaPlayerService is to support two running instances at the same time,
 * video player and audio player, so that video player could "interrupt" audio player,
 * and resume audio player when video player is stopped.
 */
@AndroidEntryPoint
class AudioPlayerService : MediaPlayerService() {
    /**
     * MediaPlayerGateway for audio player
     */
    @AudioPlayer
    @Inject
    override lateinit var mediaPlayerGateway: MediaPlayerGateway

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        //Stop audio player when app is killed.
        stopAudioPlayer()
    }
}
