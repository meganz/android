package mega.privacy.android.app.mediaplayer.service

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import timber.log.Timber

/**
 * Hosts the revamped video player's [ExoPlayer], so the player's lifetime stops being the
 * ViewModel's and background playback can be added later without moving it again.
 *
 * **Bind-only, and silent.** Nothing sends this service a start Intent: callers reach it through a
 * `MediaController` built from its `SessionToken`. Notifications are suppressed by
 * [onUpdateNotification] alone, which is Media3's only route to its notification manager. The audio
 * player is a started foreground service and needs its placeholder-notification machinery for the
 * `startForegroundService` contract; none of that belongs here.
 *
 * The manifest entry still declares the `MediaSessionService` intent filter even though nothing
 * dispatches that action to it: `SessionToken` resolves a `ComponentName` by querying the manifest
 * for that action, and without it the token cannot be constructed at all.
 *
 * Playback therefore behaves exactly as it did when the ViewModel owned the player: Media3 destroys
 * a bound service once no controller is attached and its sessions are released, so leaving the
 * player stops playback. No notification, no foreground, no background playback — yet.
 */
@OptIn(UnstableApi::class)
class VideoPlayerService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val exoPlayer = ExoPlayer.Builder(this).build()
        player = exoPlayer
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                ): MediaSession.ConnectionResult =
                    if (controller.packageName == packageName) {
                        super.onConnect(session, controller)
                    } else {
                        MediaSession.ConnectionResult.reject()
                    }
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    /**
     * Deliberately does not call `super`, which is the only route to Media3's notification manager
     * and to the `startForegroundService` it uses to promote this service. Overriding it here is
     * what keeps the player silent.
     */
    override fun onUpdateNotification(session: MediaSession, startInForeground: Boolean) = Unit

    override fun onDestroy() {
        // Released separately so a failure to release one cannot leave the other held.
        runCatching { mediaSession?.release() }
            .onFailure { Timber.e(it, "Failed to release the video player's MediaSession") }
        mediaSession = null
        runCatching { player?.release() }
            .onFailure { Timber.e(it, "Failed to release the video player") }
        player = null
        super.onDestroy()
    }
}
