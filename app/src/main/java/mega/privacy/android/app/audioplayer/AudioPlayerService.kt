package mega.privacy.android.app.audioplayer

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.IBinder
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.*
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
class AudioPlayerService : LifecycleService(), LifecycleObserver {

    @Inject
    lateinit var megaApi: MegaApiAndroid

    @Inject
    lateinit var dbHandler: DatabaseHandler

    private val binder = AudioPlayerServiceBinder(this)

    private lateinit var viewModel: AudioPlayerViewModel

    private lateinit var trackSelector: DefaultTrackSelector
    lateinit var exoPlayer: SimpleExoPlayer
        private set
    private lateinit var playerNotificationManager: PlayerNotificationManager

    private val _metadata = MutableLiveData<Metadata>()
    val metadata: LiveData<Metadata> = _metadata

    private var needPlayWhenGoForeground = false

    override fun onCreate() {
        super.onCreate()

        viewModel = AudioPlayerViewModel(this, megaApi, dbHandler)

        createPlayer()
        createPlayerControlNotification()
        observeLiveData()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun createPlayer() {
        trackSelector = DefaultTrackSelector(this)
        exoPlayer = SimpleExoPlayer.Builder(this, DefaultRenderersFactory(this))
            .setTrackSelector(trackSelector)
            .build()

        exoPlayer.addListener(MetadataExtractor(trackSelector) { title, artist ->
            _metadata.value = Metadata(title, artist, exoPlayer.currentMediaItem?.mediaId ?: "")
        })

        exoPlayer.shuffleModeEnabled = viewModel.shuffleEnabled()
        exoPlayer.repeatMode = viewModel.repeatMode()

        exoPlayer.addListener(object : Player.EventListener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val tag = mediaItem?.playbackProperties?.tag ?: return
                if (tag is Long) {
                    viewModel.playingHandle = tag
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                viewModel.setShuffleEnabled(shuffleModeEnabled)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                viewModel.setRepeatMode(repeatMode)
            }
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
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        viewModel.buildPlayerSource(intent)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun observeLiveData() {
        viewModel.playerSource.observe(this, Observer { playSource(it.first, it.second, it.third) })
    }

    private fun playSource(
        mediaItems: List<MediaItem>,
        newIndexForCurrentItem: Int,
        displayNodeNameFirst: Boolean
    ) {
        if (displayNodeNameFirst && mediaItems.isNotEmpty()) {
            _metadata.value = Metadata(null, null, mediaItems.first().mediaId)
        }

        if (newIndexForCurrentItem == INVALID_VALUE) {
            exoPlayer.setMediaItems(mediaItems)
        } else {
            val oldIndexForCurrentItem = exoPlayer.currentWindowIndex
            val oldItemsCount = exoPlayer.mediaItemCount
            if (oldIndexForCurrentItem != oldItemsCount - 1) {
                exoPlayer.removeMediaItems(oldIndexForCurrentItem + 1, oldItemsCount)
            }
            if (oldIndexForCurrentItem != 0) {
                exoPlayer.removeMediaItems(0, oldIndexForCurrentItem)
            }

            if (newIndexForCurrentItem != 0) {
                exoPlayer.addMediaItems(0, mediaItems.subList(0, newIndexForCurrentItem))
            }
            if (newIndexForCurrentItem != mediaItems.size - 1) {
                exoPlayer.addMediaItems(
                    mediaItems.subList(newIndexForCurrentItem + 1, mediaItems.size)
                )
            }
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

    fun mainPlayerUIClosed() {
        if (!playing()) {
            stopSelf()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        if (needPlayWhenGoForeground) {
            exoPlayer.playWhenReady = true
            needPlayWhenGoForeground = false
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onMoveToBackground() {
        if (!viewModel.backgroundPlayEnabled() && playing()) {
            exoPlayer.playWhenReady = false
            needPlayWhenGoForeground = true
        }
    }

    fun backgroundPlayEnabled() = viewModel.backgroundPlayEnabled()

    fun toggleBackgroundPlay() = viewModel.toggleBackgroundPlay()

    private fun playing() =
        exoPlayer.playWhenReady && exoPlayer.playbackState != Player.STATE_ENDED

    companion object {
        private const val PLAYBACK_NOTIFICATION_ID = 1
    }
}
