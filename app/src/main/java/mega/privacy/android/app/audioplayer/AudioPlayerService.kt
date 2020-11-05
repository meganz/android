package mega.privacy.android.app.audioplayer

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.IBinder
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants.*
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class AudioPlayerService : Service() {

    @Inject
    lateinit var megaApi: MegaApiAndroid

    private lateinit var mediaSourceFactory: ProgressiveMediaSource.Factory
    private lateinit var trackSelector: DefaultTrackSelector
    private lateinit var exoPlayer: SimpleExoPlayer
    private lateinit var playerNotificationManager: PlayerNotificationManager

    private val metadata = MutableLiveData<Metadata>()

    override fun onCreate() {
        super.onCreate()

        mediaSourceFactory =
            ProgressiveMediaSource.Factory(DefaultDataSourceFactory(this, EXO_PLAYER_UA))

        trackSelector = DefaultTrackSelector(this)
        exoPlayer = SimpleExoPlayer.Builder(this, DefaultRenderersFactory(this))
            .setTrackSelector(trackSelector)
            .build()

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
            setFastForwardIncrementMs(0)
            setRewindIncrementMs(0)

            setPlayer(exoPlayer)
        }
    }

    /**
     * Will be called by our activity to get information about exo player.
     */
    override fun onBind(intent: Intent): IBinder? {
        val type = intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
        if (type == INVALID_VALUE) {
            return null
        }

        when (type) {
            OFFLINE_ADAPTER -> {
                val path = intent.getStringExtra(INTENT_EXTRA_KEY_PATH) ?: return null
                val file = File(path)
                preparePlayer(Uri.fromFile(file), file.name)
            }
            AUDIO_SEARCH_ADAPTER, AUDIO_BROWSE_ADAPTER -> {
                val handle = intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)
                val node = megaApi.getNodeByHandle(handle) ?: return null
                preparePlayer(Uri.parse(megaApi.httpServerGetLocalLink(node)), node.name)
            }
        }

        return AudioPlayerServiceBinder(exoPlayer, metadata)
    }

    private fun preparePlayer(uri: Uri, nodeName: String) {
        exoPlayer.addAnalyticsListener(MetadataExtractor(trackSelector) { trackName, artistName ->
            metadata.value = Metadata(trackName, artistName, nodeName)
        })

        exoPlayer.playWhenReady = true
        exoPlayer.prepare(mediaSourceFactory.createMediaSource(uri))
    }

    override fun onDestroy() {
        super.onDestroy()

        playerNotificationManager.setPlayer(null)

        exoPlayer.release()
    }

    companion object {
        private const val PLAYBACK_NOTIFICATION_ID = 1
    }
}
