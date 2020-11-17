package mega.privacy.android.app.audioplayer.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.IBinder
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.*
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.R
import mega.privacy.android.app.audioplayer.AudioPlayerActivity
import mega.privacy.android.app.audioplayer.miniplayer.MiniAudioPlayerController
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

    lateinit var viewModel: AudioPlayerServiceViewModel

    private lateinit var trackSelector: DefaultTrackSelector
    lateinit var exoPlayer: SimpleExoPlayer
        private set
    private lateinit var playerNotificationManager: PlayerNotificationManager

    private val _metadata = MutableLiveData<Metadata>()
    val metadata: LiveData<Metadata> = _metadata

    private var needPlayWhenGoForeground = false

    override fun onCreate() {
        super.onCreate()

        viewModel = AudioPlayerServiceViewModel(this, megaApi, dbHandler)

        createPlayer()
        createPlayerControlNotification()
        observeLiveData()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        MiniAudioPlayerController.notifyAudioPlayerPlaying(true)
    }

    private fun createPlayer() {
        trackSelector = DefaultTrackSelector(this)
        exoPlayer = SimpleExoPlayer.Builder(this, DefaultRenderersFactory(this))
            .setTrackSelector(trackSelector)
            .build()

        exoPlayer.addListener(MetadataExtractor(trackSelector) { title, artist, album ->
            val nodeName =
                viewModel.getPlaylistItem(exoPlayer.currentMediaItem?.mediaId)?.nodeName ?: ""
            _metadata.value = Metadata(title, artist, album, nodeName)
        })

        exoPlayer.shuffleModeEnabled = viewModel.shuffleEnabled()
        exoPlayer.repeatMode = viewModel.repeatMode()

        exoPlayer.addListener(object : Player.EventListener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val handle = mediaItem?.mediaId ?: return
                viewModel.playingHandle = handle.toLong()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                viewModel.setShuffleEnabled(shuffleModeEnabled)

                if (shuffleModeEnabled) {
                    exoPlayer.setShuffleOrder(viewModel.newShuffleOrder())
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                viewModel.setRepeatMode(repeatMode)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                viewModel.paused = !playWhenReady
            }

            override fun onPlaybackStateChanged(state: Int) {
                when {
                    state == Player.STATE_ENDED && !viewModel.paused -> viewModel.paused = true
                    state == Player.STATE_READY && viewModel.paused -> viewModel.paused = false
                }
            }
        })

        exoPlayer.setShuffleOrder(viewModel.shuffleOrder)
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
                    stopAudioPlayer()
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

        viewModel.mediaItemToRemove.observe(this, Observer {
            if (it < exoPlayer.mediaItemCount) {
                val nextIndex = exoPlayer.nextWindowIndex
                if (nextIndex != C.INDEX_UNSET) {
                    val nextItem = exoPlayer.getMediaItemAt(nextIndex)
                    val nodeName = viewModel.getPlaylistItem(nextItem.mediaId)?.nodeName ?: ""
                    _metadata.value = Metadata(null, null, null, nodeName)
                }
                exoPlayer.removeMediaItem(it)
            }
        })

        viewModel.nodeNameUpdate.observe(this, Observer {
            val meta = _metadata.value ?: return@Observer
            _metadata.value = Metadata(meta.title, meta.artist, meta.album, it)
        })
    }

    private fun playSource(
        mediaItems: List<MediaItem>,
        newIndexForCurrentItem: Int,
        nameToDisplay: String?
    ) {
        if (nameToDisplay != null) {
            _metadata.value = Metadata(null, null, null, nameToDisplay)
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
            stopAudioPlayer()
        }
    }

    fun stopAudioPlayer() {
        exoPlayer.stop()
        MiniAudioPlayerController.notifyAudioPlayerPlaying(false)
        stopSelf()
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

    private fun playing() =
        exoPlayer.playWhenReady && exoPlayer.playbackState != Player.STATE_ENDED

    companion object {
        private const val PLAYBACK_NOTIFICATION_ID = 1
    }
}
