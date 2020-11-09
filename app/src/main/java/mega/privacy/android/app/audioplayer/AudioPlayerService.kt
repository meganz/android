package mega.privacy.android.app.audioplayer

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.IBinder
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_AUDIO_PLAYER_ID
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

@AndroidEntryPoint
class AudioPlayerService : LifecycleService() {

    @Inject
    lateinit var megaApi: MegaApiAndroid

    @Inject
    lateinit var dbHandler: DatabaseHandler

    private lateinit var viewModel: AudioPlayerViewModel

    private lateinit var trackSelector: DefaultTrackSelector
    private lateinit var exoPlayer: SimpleExoPlayer
    private lateinit var playerNotificationManager: PlayerNotificationManager

    private val metadata = MutableLiveData<Metadata>()

    override fun onCreate() {
        super.onCreate()

        viewModel = AudioPlayerViewModel(this, megaApi, dbHandler)

        createPlayer()
        createPlayerControlNotification()
        observeLiveData()
    }

    private fun createPlayer() {
        trackSelector = DefaultTrackSelector(this)
        exoPlayer = SimpleExoPlayer.Builder(this, DefaultRenderersFactory(this))
            .setTrackSelector(trackSelector)
            .build()

        exoPlayer.addListener(MetadataExtractor(trackSelector) { title, artist ->
            metadata.value = Metadata(title, artist, exoPlayer.currentMediaItem?.mediaId ?: "")
        })
    }

    private fun createPlayerControlNotification() {
        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
            applicationContext, NOTIFICATION_CHANNEL_AUDIO_PLAYER_ID,
            R.string.audio_player_notification_channel_name, 0, PLAYBACK_NOTIFICATION_ID,
            object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player): String {
                    return "content title"
                }

                @Nullable
                override fun createCurrentContentIntent(player: Player): PendingIntent? =
                    PendingIntent.getActivity(
                        applicationContext,
                        0,
                        Intent(applicationContext, AudioPlayerActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )

                @Nullable
                override fun getCurrentContentText(player: Player): String? {
                    return "content text"
                }

                @Nullable
                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ): Bitmap? {
                    return ContextCompat.getDrawable(
                        this@AudioPlayerService,
                        R.drawable.ic_audio_thumbnail
                    )?.toBitmap()
                }
            },
            object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationStarted(
                    notificationId: Int,
                    notification: Notification
                ) {
                    startForeground(notificationId, notification)
                }

                override fun onNotificationCancelled(notificationId: Int) {
                    stopSelf()
                }

                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean
                ) {
                    if (ongoing) {
                        // Make sure the service will not get destroyed while playing media.
                        startForeground(notificationId, notification)
                    } else {
                        // Make notification cancellable.
                        stopForeground(false)
                    }
                }
            }
        ).apply {
            setUseChronometer(false)
            setControlDispatcher(AudioPlayerControlDispatcher())

            setPlayer(exoPlayer)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)

        viewModel.buildPlayerSource(intent)

        return AudioPlayerServiceBinder(exoPlayer, metadata)
    }

    private fun observeLiveData() {
        viewModel.playerSource.observe(this) {
            playSource(it.first, it.second)
        }
    }

    private fun playSource(mediaItems: List<MediaItem>, seekWindow: Int) {
        if (seekWindow == INVALID_VALUE) {
            exoPlayer.setMediaItems(mediaItems)
        } else {
            exoPlayer.setMediaItems(mediaItems, seekWindow, exoPlayer.currentPosition)
        }
        exoPlayer.playWhenReady = true
        exoPlayer.prepare()
    }

    override fun onDestroy() {
        super.onDestroy()

        viewModel.clear()

        playerNotificationManager.setPlayer(null)
        exoPlayer.release()
    }

    companion object {
        private const val PLAYBACK_NOTIFICATION_ID = 1
    }
}
