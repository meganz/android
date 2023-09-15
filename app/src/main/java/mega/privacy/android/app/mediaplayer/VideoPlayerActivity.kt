package mega.privacy.android.app.mediaplayer

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.database.ContentObserver
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.provider.Settings.System.ACCELEROMETER_ROTATION
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.OfflineFileInfoActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.dragger.DragToExitSupport
import mega.privacy.android.app.databinding.ActivityVideoPlayerBinding
import mega.privacy.android.app.di.mediaplayer.VideoPlayer
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.mediaplayer.VideoPlayerViewModel.Companion.VIDEO_TYPE_RESTART_PLAYBACK_POSITION
import mega.privacy.android.app.mediaplayer.VideoPlayerViewModel.Companion.VIDEO_TYPE_RESUME_PLAYBACK_POSITION
import mega.privacy.android.app.mediaplayer.VideoPlayerViewModel.Companion.VIDEO_TYPE_SHOW_PLAYBACK_POSITION_DIALOG
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerCallback
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.app.presentation.fileinfo.FileInfoActivity
import mega.privacy.android.app.usecase.exception.MegaException
import mega.privacy.android.app.utils.AlertDialogUtil
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil.removeAttachmentMessage
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants.EVENT_NOT_ALLOW_PLAY
import mega.privacy.android.app.utils.Constants.EXTRA_SERIALIZE_STRING
import mega.privacy.android.app.utils.Constants.FILE_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_ALBUM_SHARING
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.FROM_IMAGE_VIEWER
import mega.privacy.android.app.utils.Constants.FROM_BACKUPS
import mega.privacy.android.app.utils.Constants.FROM_INCOMING_SHARES
import mega.privacy.android.app.utils.Constants.HANDLE
import mega.privacy.android.app.utils.Constants.BACKUPS_ADAPTER
import mega.privacy.android.app.utils.Constants.INCOMING_SHARES_ADAPTER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_COPY_FROM
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FIRST_LEVEL
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FROM
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MOVE_FROM
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.MEDIA_PLAYER_TOOLBAR_SHOW_HIDE_DURATION_MS
import mega.privacy.android.app.utils.Constants.NAME
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.RECENTS_ADAPTER
import mega.privacy.android.app.utils.Constants.RUBBISH_BIN_ADAPTER
import mega.privacy.android.app.utils.Constants.SEARCH_ADAPTER
import mega.privacy.android.app.utils.Constants.URL_FILE_LINK
import mega.privacy.android.app.utils.Constants.VERSIONS_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaNodeDialogUtil
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MenuUtils.toggleAllMenuItemsVisibility
import mega.privacy.android.app.utils.RunOnUIThreadUtils
import mega.privacy.android.app.utils.Util.isDarkMode
import mega.privacy.android.app.utils.getFragmentFromNavHost
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.exception.BlockedMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import org.jetbrains.anko.configuration
import timber.log.Timber
import javax.inject.Inject

/**
 * Video player activity
 */
@AndroidEntryPoint
class VideoPlayerActivity : MediaPlayerActivity() {
    /**
     * MediaPlayerGateway for video player
     */
    @VideoPlayer
    @Inject
    lateinit var mediaPlayerGateway: MediaPlayerGateway

    private lateinit var binding: ActivityVideoPlayerBinding

    private val videoViewModel: VideoPlayerViewModel by viewModels()

    private var takenDownDialog: AlertDialog? = null

    private var currentOrientation: Int = SCREEN_ORIENTATION_SENSOR_PORTRAIT

    private var mediaPlayerIntent: Intent? = null
    private var isPlayingAfterReady = false
    private var currentPlayingHandle: Long? = null

    private var playbackPositionDialog: Dialog? = null

    private val headsetPlugReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_HEADSET_PLUG) {
                if (intent.getIntExtra(INTENT_KEY_STATE, -1) == STATE_HEADSET_UNPLUGGED) {
                    mediaPlayerGateway.setPlayWhenReady(false)
                }
            }
        }
    }

    private val dragToExit by lazy {
        DragToExitSupport(
            context = this,
            dragActivated = this::onDragActivated
        ) {
            finish()
            overridePendingTransition(0, android.R.anim.fade_out)
        }
    }

    /**
     * Handle events when a Back Press is detected
     */
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            videoViewModel.uiState.value.let { state ->
                // If popup is shown, close the popup when the back button is pressed
                if (state.isSpeedPopupShown || state.isVideoOptionPopupShown) {
                    if (state.isSpeedPopupShown) {
                        videoViewModel.updateIsSpeedPopupShown(false)
                    }
                    if (state.isVideoOptionPopupShown) {
                        videoViewModel.updateIsVideoOptionPopupShown(false)
                    }
                } else {
                    retryConnectionsAndSignalPresence()
                    if (!navController.navigateUp()) {
                        finish()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup the Back Press dispatcher to receive Back Press events
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        val extras = intent.extras
        if (extras == null) {
            finish()
            return
        }

        val rebuildPlaylist = intent.getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true)
        val adapterType = intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
        if (adapterType == INVALID_VALUE && rebuildPlaylist) {
            finish()
            return
        }

        if (savedInstanceState != null) {
            nodeAttacher.restoreState(savedInstanceState)
            nodeSaver.restoreState(savedInstanceState)
        }

        currentOrientation = configuration.orientation
        observeRotationSettingsChange()

        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)

        setContentView(dragToExit.wrapContentView(binding.root))
        dragToExit.observeThumbnailLocation(this, intent)

        binding.toolbar.apply {
            collapseIcon =
                AppCompatResources.getDrawable(
                    this@VideoPlayerActivity,
                    androidx.appcompat.R.drawable.abc_ic_ab_back_material
                )
            collapseIcon?.setTint(Color.WHITE)
        }.post {
            updateToolbar(videoViewModel.screenLockState.value)
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        createPlayer()
        setupToolbar()
        setupNavDestListener()
        setupObserver()
        initMediaData()

        if (savedInstanceState == null) {
            // post to next UI cycle so that MediaPlayerFragment's onCreateView is called
            RunOnUIThreadUtils.post {
                getFragmentFromNavHost(
                    navHostId = R.id.nav_host_fragment,
                    fragmentClass = VideoPlayerFragment::class.java
                )?.runEnterAnimation(dragToExit)
            }
        }

        if (CallUtil.participatingInACall()) {
            showNotAllowPlayAlert()
        }

        LiveEventBus.get(EVENT_NOT_ALLOW_PLAY, Boolean::class.java)
            .observe(this) {
                showNotAllowPlayAlert()
            }
        AudioPlayerService.pauseAudioPlayer(this)
        registerReceiver(headsetPlugReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))
    }

    private fun createPlayer() {
        with(videoViewModel) {
            val nameChangeCallback: (title: String?, artist: String?, album: String?) -> Unit =
                { title, artist, album ->
                    val nodeName =
                        getPlaylistItem(mediaPlayerGateway.getCurrentMediaItem()?.mediaId)?.nodeName
                            ?: ""

                    if (!(title.isNullOrEmpty() && artist.isNullOrEmpty()
                                && album.isNullOrEmpty() && nodeName.isEmpty())
                    ) {
                        videoViewModel.updateMetadataState(Metadata(title, artist, album, nodeName))
                        mediaPlayerGateway.invalidatePlayerNotification()
                    }
                }

            val mediaPlayerCallback: MediaPlayerCallback = object : MediaPlayerCallback {
                override fun onMediaItemTransitionCallback(
                    handle: String?,
                    isUpdateName: Boolean,
                ) {
                    videoViewModel.setCurrentPlayingVideoSize(null)
                    handle?.let {
                        setCurrentPlayingHandle(it.toLong())
                        lifecycleScope.launch {
                            monitorPlaybackTimes(it.toLong()) { positionInMs ->
                                when (videoPlayType) {
                                    VIDEO_TYPE_RESUME_PLAYBACK_POSITION ->
                                        positionInMs?.let { position ->
                                            mediaPlayerGateway.playerSeekToPositionInMs(position)
                                        }

                                    VIDEO_TYPE_RESTART_PLAYBACK_POSITION -> {
                                        // Remove current playback history, if video type is restart
                                        deletePlaybackInformation(it.toLong())
                                    }

                                    VIDEO_TYPE_SHOW_PLAYBACK_POSITION_DIALOG -> {
                                        // Detect the media item whether is transition by comparing
                                        // currentPlayingHandle if is parameter handle
                                        if (currentPlayingHandle != it.toLong()
                                            && positionInMs != null && positionInMs > 0
                                        ) {
                                            // If the dialog is not showing before build sources,
                                            // the video is paused before the dialog is dismissed.
                                            isPlayingAfterReady = false
                                            updateShowPlaybackPositionDialogState(
                                                showPlaybackPositionDialogState.value.copy(
                                                    showPlaybackDialog = true,
                                                    mediaItemName = getPlaylistItem(it)?.nodeName,
                                                    playbackPosition = positionInMs,
                                                    isDialogShownBeforeBuildSources = false
                                                )
                                            )
                                        } else {
                                            // If currentPlayHandle is parameter handle and there is
                                            // no playback history, the video is playing after ready
                                            isPlayingAfterReady = true
                                            // Set playWhenReady to be true to ensure the video is playing after ready
                                            if (!mediaPlayerGateway.getPlayWhenReady()) {
                                                mediaPlayerGateway.setPlayWhenReady(true)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (isUpdateName) {
                            val nodeName = getPlaylistItem(it)?.nodeName ?: ""
                            videoViewModel.updateMetadataState(Metadata(null, null, null, nodeName))
                        }
                        currentPlayingHandle = handle.toLong()
                    }
                }

                override fun onShuffleModeEnabledChangedCallback(shuffleModeEnabled: Boolean) {
                }

                override fun onRepeatModeChangedCallback(repeatToggleMode: RepeatToggleMode) {
                    setVideoRepeatMode(repeatToggleMode)
                }

                override fun onPlayWhenReadyChangedCallback(playWhenReady: Boolean) {
                    updateMediaPlaybackState(!playWhenReady)
                }

                override fun onPlaybackStateChangedCallback(state: Int) {
                    val isPaused = videoViewModel.mediaPlaybackState.value
                    when {
                        state == MEDIA_PLAYER_STATE_ENDED && !isPaused -> {
                            updateMediaPlaybackState(true)
                        }

                        state == MEDIA_PLAYER_STATE_READY -> {
                            // This case is only for video player
                            if (isPaused && mediaPlayerGateway.getPlayWhenReady()) {
                                updateMediaPlaybackState(false)
                                sendVideoPlayerActivatedEvent()
                            } else {
                                // Detect videoPlayType and isPlayingAfterReady after video is ready
                                // If videoPlayType is VIDEO_TYPE_SHOW_PLAYBACK_POSITION_DIALOG, and
                                // isPlayingAfterReady is false, the video is paused after it's ready
                                if (videoPlayType == VIDEO_TYPE_SHOW_PLAYBACK_POSITION_DIALOG
                                    && !isPlayingAfterReady
                                ) {
                                    mediaPlayerGateway.setPlayWhenReady(false)
                                }
                            }
                        }
                    }
                }

                override fun onPlayerErrorCallback() {
                    onPlayerError()
                }

                override fun onVideoSizeCallback(videoWidth: Int, videoHeight: Int) {
                    videoViewModel.setCurrentPlayingVideoSize(videoWidth to videoHeight)
                    updateOrientationBasedOnVideoSize(videoWidth, videoHeight)
                }
            }

            mediaPlayerGateway.createPlayer(
                repeatToggleMode = videoRepeatToggleMode(),
                nameChangeCallback = nameChangeCallback,
                mediaPlayerCallback = mediaPlayerCallback
            )
        }
    }

    @OptIn(FlowPreview::class)
    private fun setupObserver() {
        with(viewModel) {
            getCollision().observe(this@VideoPlayerActivity) { collision ->
                nameCollisionActivityContract?.launch(arrayListOf(collision))
            }

            onSnackbarMessage().observe(this@VideoPlayerActivity) { message ->
                showSnackBarForVideoPlayer(getString(message))
            }

            onExceptionThrown().observe(this@VideoPlayerActivity, ::manageException)

            itemToRemove.observe(this@VideoPlayerActivity) { handle ->
                videoViewModel.removeItem(handle)
            }

            renameUpdate.observe(this@VideoPlayerActivity) { node ->
                node?.let {
                    MegaNodeDialogUtil.showRenameNodeDialog(
                        context = this@VideoPlayerActivity,
                        node = it,
                        snackbarShower = this@VideoPlayerActivity,
                        actionNodeCallback = object : ActionNodeCallback {
                            override fun finishRenameActionWithSuccess(newName: String) {
                                videoViewModel.updateItemName(it.handle, newName)
                                //Avoid the dialog is shown repeatedly when screen is rotated.
                                viewModel.renameUpdate(null)
                            }
                        })
                }
            }
        }

        collectFlow(viewModel.menuClickEventFlow.debounce { (menuId) ->
            if (menuId == R.id.share) {
                TIMEOUT_FOR_SHARED_MENU_ITEM
            } else {
                TIMEOUT_FOR_DEFAULT_MENU_ITEM
            }
        }) { (menuId, adapterType, playingHandle, launchIntent) ->
            when (menuId) {
                R.id.save_to_device -> {
                    videoViewModel.sendSaveToDeviceButtonClickedEvent()
                    when (adapterType) {
                        OFFLINE_ADAPTER -> nodeSaver.saveOfflineNode(
                            handle = playingHandle,
                            fromMediaViewer = true
                        )

                        ZIP_ADAPTER -> {
                            val mediaItem = mediaPlayerGateway.getCurrentMediaItem()
                            mediaItem?.localConfiguration?.uri?.let { uri ->
                                videoViewModel.getPlaylistItem(mediaItem.mediaId)
                                    ?.let { playlistItem ->
                                        nodeSaver.saveUri(
                                            uri = uri,
                                            name = playlistItem.nodeName,
                                            size = playlistItem.size,
                                            fromMediaViewer = true
                                        )
                                    }
                            }
                        }

                        FROM_CHAT -> {
                            getChatMessageNode()?.let { node ->
                                nodeSaver.saveNode(
                                    node = node,
                                    highPriority = true,
                                    fromMediaViewer = true,
                                    needSerialize = true
                                )
                            }
                        }

                        FILE_LINK_ADAPTER -> {
                            launchIntent.getStringExtra(EXTRA_SERIALIZE_STRING)?.let { serialize ->
                                MegaNode.unserialize(serialize)?.let { currentDocument ->
                                    Timber.d("currentDocument NOT NULL")
                                    nodeSaver.saveNode(
                                        currentDocument,
                                        isFolderLink = false,
                                        fromMediaViewer = true,
                                        needSerialize = true
                                    )
                                } ?: Timber.w("currentDocument is NULL")
                            }
                        }

                        FROM_ALBUM_SHARING -> {
                            viewModel.getNodeForAlbumSharing(playingHandle)?.let { node ->
                                nodeSaver.saveNode(
                                    node = node,
                                    fromMediaViewer = true,
                                    needSerialize = true
                                )
                            }
                        }

                        else -> {
                            nodeSaver.saveHandle(
                                handle = playingHandle,
                                isFolderLink = adapterType == FOLDER_LINK_ADAPTER,
                                fromMediaViewer = true
                            )
                        }
                    }
                }

                R.id.properties -> {
                    videoViewModel.sendInfoButtonClickedEvent()
                    // Pause the video when the file info page is opened, and allow the video to
                    // revert to playing after back to the video player page.
                    mediaPlayerGateway.setPlayWhenReady(false)
                    videoViewModel.setPlayingReverted(true)
                    val intent: Intent
                    if (adapterType == OFFLINE_ADAPTER) {
                        intent = Intent(this, OfflineFileInfoActivity::class.java).apply {
                            putExtra(HANDLE, playingHandle.toString())
                        }
                        Timber.d("onOptionsItemSelected properties offline handle $playingHandle")
                    } else {
                        val node = megaApi.getNodeByHandle(playingHandle)
                        intent = Intent(this, FileInfoActivity::class.java).apply {
                            putExtra(HANDLE, playingHandle)
                            putExtra(NAME, node?.name)
                        }

                        val fromIncoming =
                            (adapterType == SEARCH_ADAPTER || adapterType == RECENTS_ADAPTER) &&
                                    NodeController(this).nodeComesFromIncoming(node)

                        when {
                            adapterType == INCOMING_SHARES_ADAPTER || fromIncoming -> {
                                intent.putExtra(
                                    INTENT_EXTRA_KEY_FROM,
                                    FROM_INCOMING_SHARES
                                )
                                intent.putExtra(INTENT_EXTRA_KEY_FIRST_LEVEL, false)
                            }

                            adapterType == BACKUPS_ADAPTER -> {
                                intent.putExtra(
                                    INTENT_EXTRA_KEY_FROM,
                                    FROM_BACKUPS
                                )
                            }
                        }
                    }
                    startActivity(intent)
                }

                R.id.chat_import -> {
                    selectImportFolderLauncher.launch(
                        Intent(this, FileExplorerActivity::class.java).apply {
                            action = FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER
                        }
                    )
                }

                R.id.share -> {
                    videoViewModel.sendShareButtonClickedEvent()
                    when (adapterType) {
                        OFFLINE_ADAPTER, ZIP_ADAPTER -> {
                            val mediaItem = mediaPlayerGateway.getCurrentMediaItem()
                            videoViewModel.getPlaylistItem(mediaItem?.mediaId)?.nodeName
                                ?.let { nodeName ->
                                    mediaItem?.localConfiguration?.uri?.let { uri ->
                                        FileUtil.shareUri(this, nodeName, uri)
                                    }
                                }
                        }

                        FILE_LINK_ADAPTER -> {
                            MegaNodeUtil.shareLink(
                                context = this,
                                fileLink = launchIntent.getStringExtra(URL_FILE_LINK)
                            )
                        }

                        else -> {
                            videoViewModel.run {
                                MegaNodeUtil.shareNode(
                                    context = this@VideoPlayerActivity,
                                    node = megaApi.getNodeByHandle(getCurrentPlayingHandle())
                                )
                            }
                        }
                    }
                }

                R.id.send_to_chat -> {
                    videoViewModel.sendSendToChatButtonClickedEvent()
                    nodeAttacher.attachNode(handle = playingHandle)
                }

                R.id.get_link -> {
                    videoViewModel.sendGetLinkButtonClickedEvent()
                    if (!MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog(
                            node = megaApi.getNodeByHandle(playingHandle),
                            context = this
                        )
                    ) {
                        LinksUtil.showGetLinkActivity(this, playingHandle)
                    }
                }

                R.id.remove_link -> {
                    videoViewModel.sendRemoveLinkButtonClickedEvent()
                    megaApi.getNodeByHandle(playingHandle)?.let { node ->
                        if (!MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog(node, this)) {
                            AlertsAndWarnings.showConfirmRemoveLinkDialog(this) {
                                megaApi.disableExport(
                                    node,
                                    OptionalMegaRequestListenerInterface(
                                        onRequestFinish = { _, error ->
                                            if (error.errorCode == MegaError.API_OK) {
                                                // Some times checking node.isExported immediately will still
                                                // get true, so let's add some delay here.
                                                RunOnUIThreadUtils.runDelay(500L) {
                                                    refreshMenuOptionsVisibility()
                                                }
                                            }
                                        })
                                )
                            }
                        }
                    }
                }

                R.id.chat_save_for_offline -> {
                    PermissionUtils.checkNotificationsPermission(this)
                    getChatMessage().let { (chatId, message) ->
                        message?.let {
                            ChatController(this).saveForOffline(
                                it.megaNodeList,
                                megaChatApi.getChatRoom(chatId),
                                true,
                                this
                            )
                        }
                    }
                }

                R.id.rename -> {
                    viewModel.renameUpdate(node = megaApi.getNodeByHandle(playingHandle))
                }

                R.id.move -> {
                    selectFolderToMoveLauncher.launch(
                        Intent(this, FileExplorerActivity::class.java).apply {
                            action = FileExplorerActivity.ACTION_PICK_MOVE_FOLDER
                            putExtra(INTENT_EXTRA_KEY_MOVE_FROM, longArrayOf(playingHandle))
                        }
                    )
                }

                R.id.copy -> {
                    if (getStorageState() == StorageState.PayWall) {
                        AlertsAndWarnings.showOverDiskQuotaPaywallWarning()
                    } else {
                        selectFolderToCopyLauncher.launch(
                            Intent(this, FileExplorerActivity::class.java).apply {
                                action = FileExplorerActivity.ACTION_PICK_COPY_FOLDER
                                putExtra(INTENT_EXTRA_KEY_COPY_FROM, longArrayOf(playingHandle))
                            }
                        )
                    }
                }

                R.id.move_to_trash -> {
                    if (adapterType == FROM_CHAT) {
                        getChatMessage().let { (chatId, message) ->
                            message?.let { removeAttachmentMessage(this, chatId, it) }
                        }
                    } else {
                        MegaNodeDialogUtil.moveToRubbishOrRemove(
                            handle = playingHandle,
                            activity = this,
                            snackbarShower = this
                        )
                    }
                }
            }
        }

        with(videoViewModel) {
            collectFlow(screenLockState) { isLock ->
                updateToolbar(isLock)
                setDraggable(!isLock)
            }

            collectFlow(
                targetFlow = playerSourcesState,
                minActiveState = Lifecycle.State.CREATED
            ) { mediaPlaySources ->
                if (mediaPlaySources.mediaItems.isNotEmpty()) {
                    playSource(mediaPlaySources)
                }
            }

            collectFlow(mediaItemToRemoveState) { index ->
                index?.let {
                    mediaPlayerGateway.mediaItemRemoved(it)?.let { handle ->
                        val nodeName = getPlaylistItem(handle)?.nodeName ?: ""
                        videoViewModel.updateMetadataState(Metadata(null, null, null, nodeName))
                    }
                }
            }

            collectFlow(errorState) { megaException ->
                megaException?.let {
                    this@VideoPlayerActivity.onError(it)
                }
            }

            collectFlow(itemsClearedState) { isCleared ->
                if (isCleared == true) {
                    stopPlayer()
                }
            }

            collectFlow(metadataState) { metadata ->
                if (navController.currentDestination?.id == R.id.video_main_player) {
                    updateToolbarTitleBasedOnOrientation(metadata)
                }

                dragToExit.nodeChanged(
                    getCurrentPlayingHandle()
                )
            }

            // Put in the Activity to avoid Fragment recreate to cause the state changes when the screen rotated
            collectFlow(
                uiState.map { it.subtitleDisplayState }.distinctUntilChanged()
            ) { state ->
                // According to the state of the subtitle dialog displayed and the
                // state of the playback position dialog displayed to pause or play the video.
                mediaPlayerGateway.setPlayWhenReady(
                    !state.isSubtitleDialogShown
                            && !videoViewModel.showPlaybackPositionDialogState.value.showPlaybackDialog
                )
            }

            // Put in the Activity to avoid Fragment recreate to cause the state changes when the screen rotated
            collectFlow(showPlaybackPositionDialogState) { state ->
                if (state.showPlaybackDialog) {
                    mediaPlayerGateway.setPlayWhenReady(false)
                    playbackPositionDialog =
                        MaterialAlertDialogBuilder(this@VideoPlayerActivity)
                            .setTitle(R.string.video_playback_position_dialog_title)
                            .setMessage(
                                String.format(
                                    getString(
                                        R.string.video_playback_position_dialog_message
                                    ),
                                    state.mediaItemName,
                                    formatMillisecondsToString(
                                        state.playbackPosition ?: 0
                                    )
                                )
                            )
                            .setNegativeButton(
                                R.string.video_playback_position_dialog_resume_button
                            ) { _, _ ->
                                if (state.isDialogShownBeforeBuildSources)
                                    setResumePlaybackPositionBeforeBuildSources()
                                else
                                    setResumePlaybackPosition(state.playbackPosition)

                            }
                            .setPositiveButton(
                                R.string.video_playback_position_dialog_restart_button
                            ) { _, _ ->
                                if (state.isDialogShownBeforeBuildSources)
                                    setRestartPlayVideoBeforeBuildSources()
                                else
                                    setRestartPlayVideo()
                            }
                            .setOnCancelListener {
                                if (state.isDialogShownBeforeBuildSources) {
                                    cancelPlaybackPositionDialogBeforeBuildSources()
                                } else {
                                    cancelPlaybackPositionDialog()
                                }
                            }.setOnDismissListener {
                                mediaPlayerGateway.setPlayWhenReady(true)
                            }.create().apply {
                                show()
                            }
                }
            }
        }
    }

    /**
     * Update orientation according to the video size.
     *
     * @param videoWidth the width of the video
     * @param videoHeight the height of the video
     */
    private fun updateOrientationBasedOnVideoSize(videoWidth: Int, videoHeight: Int) {
        val rotationMode = Settings.System.getInt(
            contentResolver,
            ACCELEROMETER_ROTATION,
            SCREEN_BRIGHTNESS_MODE_MANUAL
        )

        currentOrientation =
            if (videoWidth > videoHeight) {
                SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }

        requestedOrientation =
            if (rotationMode == SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                SCREEN_ORIENTATION_SENSOR
            } else {
                currentOrientation
            }
    }

    private fun initMediaData() {
        currentPlayingHandle = intent?.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)

        videoViewModel.monitorPlaybackTimes(currentPlayingHandle) { positionInMs ->
            // If the first video contains playback history, show dialog before build sources
            if (positionInMs != null && positionInMs > 0) {
                mediaPlayerIntent = intent
                // Set the playing handle when the playback position dialog is displayed,
                // to avoid the toolbar cannot be shown caused by the playing handle is null.
                videoViewModel.setCurrentPlayingHandle(currentPlayingHandle ?: INVALID_HANDLE)
                with(videoViewModel) {
                    updateShowPlaybackPositionDialogState(
                        showPlaybackPositionDialogState.value.copy(
                            showPlaybackDialog = true,
                            mediaItemName = intent?.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME),
                            playbackPosition = positionInMs
                        )
                    )
                }
            } else {
                videoViewModel.initVideoSources(intent)
            }
        }
    }

    private val rotationContentObserver by lazy(LazyThreadSafetyMode.NONE) {
        object : ContentObserver(Handler(mainLooper)) {
            override fun onChange(selfChange: Boolean) {
                val rotationMode = Settings.System.getInt(
                    contentResolver,
                    ACCELEROMETER_ROTATION,
                    SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                requestedOrientation =
                    if (rotationMode == SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                        SCREEN_ORIENTATION_SENSOR
                    } else {
                        currentOrientation
                    }
            }
        }
    }

    private fun observeRotationSettingsChange() {
        contentResolver.registerContentObserver(
            Settings.System.getUriFor(ACCELEROMETER_ROTATION),
            true,
            rotationContentObserver
        )
    }

    private fun showNotAllowPlayAlert() {
        showSnackBarForVideoPlayer(getString(R.string.not_allow_play_alert))
    }

    override fun onResume() {
        super.onResume()
        refreshMenuOptionsVisibility()
    }

    override fun onStop() {
        super.onStop()
        videoViewModel.savePlaybackTimes()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Because the Activity is not recreated when the screen rotated, reload the Fragment
        // to load the different orientation layouts according to the rotation orientation.
        if (navController.currentDestination?.id == R.id.video_main_player) {
            navController.popBackStack()
            navController.navigate(R.id.video_main_player)
            updateToolbarTitleBasedOnOrientation(videoViewModel.metadataState.value)
            // The video option popup is closed when the screen orientation is landscape.
            if (newConfig.orientation == ORIENTATION_LANDSCAPE &&
                videoViewModel.uiState.value.isVideoOptionPopupShown
            ) {
                videoViewModel.updateIsVideoOptionPopupShown(false)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        nodeAttacher.saveState(outState)
        nodeSaver.saveState(outState)
    }

    private fun setupNavDestListener() {
        navController.addOnDestinationChangedListener { _, dest, _ ->
            setupToolbarColors()
            when (dest.id) {
                R.id.video_main_player -> {
                    if (currentOrientation == ORIENTATION_PORTRAIT) {
                        supportActionBar?.title = ""
                    }
                }

                R.id.video_playlist,
                -> {
                    // Pause the video when the playlist page is opened, and allow the video to
                    // revert to playing after back to the video player page.
                    mediaPlayerGateway.setPlayWhenReady(false)
                    videoViewModel.setPlayingReverted(true)
                }
            }
            refreshMenuOptionsVisibility()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        onBackPressedCallback.remove()
        contentResolver.unregisterContentObserver(rotationContentObserver)
        if (isFinishing) {
            mediaPlayerGateway.playerStop()
            mediaPlayerGateway.playerRelease()
            dragToExit.showPreviousHiddenThumbnail()
            AudioPlayerService.resumeAudioPlayer(this)
        }
        unregisterReceiver(headsetPlugReceiver)
        nodeSaver.destroy()
        AlertDialogUtil.dismissAlertDialogIfExists(takenDownDialog)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        optionsMenu = menu

        menuInflater.inflate(R.menu.menu_video_player, menu)

        menu.findItem(R.id.get_link).title = resources.getQuantityString(R.plurals.get_links, 1)

        searchMenuItem = menu.findItem(R.id.action_search).apply {
            actionView?.let { searchView ->
                if (searchView is SearchView) {
                    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String): Boolean {
                            return true
                        }

                        override fun onQueryTextChange(newText: String): Boolean {
                            videoViewModel.searchQueryUpdate(newText)
                            return true
                        }

                    })
                }
            }
            setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                    return true
                }

                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    videoViewModel.searchQueryUpdate(null)
                    return true
                }
            })
        }

        refreshMenuOptionsVisibility()

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val playingHandle = videoViewModel.getCurrentPlayingHandle()
        val adapterType = intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)

        when (item.itemId) {
            R.id.save_to_device,
            R.id.properties,
            R.id.chat_import,
            R.id.share,
            R.id.send_to_chat,
            R.id.get_link,
            R.id.remove_link,
            R.id.chat_save_for_offline,
            R.id.rename,
            R.id.move,
            R.id.copy,
            R.id.move_to_trash,
            -> {
                if (item.itemId == R.id.properties && adapterType != OFFLINE_ADAPTER) {
                    val node = megaApi.getNodeByHandle(playingHandle)
                    if (node == null) {
                        Timber.e("onOptionsItemSelected properties non-offline null node")
                        return false
                    }
                }
                viewModel.updateMenuClickEventFlow(
                    menuId = item.itemId,
                    adapterType = adapterType,
                    playingHandle = playingHandle,
                    launchIntent = intent
                )
                return true
            }
        }
        return false
    }

    private fun refreshMenuOptionsVisibility() {
        val menu = optionsMenu
        if (menu == null) {
            Timber.d("refreshMenuOptionsVisibility menu is null")
            return
        }

        val currentFragmentId = navController.currentDestination?.id
        if (currentFragmentId == null) {
            Timber.d("refreshMenuOptionsVisibility currentFragment is null")
            return
        }

        intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE).let { adapterType ->
            when (currentFragmentId) {
                R.id.video_playlist -> {
                    menu.toggleAllMenuItemsVisibility(false)
                    searchMenuItem?.isVisible = true
                    // Display the select option
                    menu.findItem(R.id.select).isVisible = true
                }

                R.id.video_main_player -> {
                    when {
                        adapterType == OFFLINE_ADAPTER -> {
                            menu.toggleAllMenuItemsVisibility(false)

                            menu.findItem(R.id.properties).isVisible =
                                currentFragmentId == R.id.video_main_player

                            menu.findItem(R.id.share).isVisible =
                                currentFragmentId == R.id.video_main_player
                        }

                        adapterType == RUBBISH_BIN_ADAPTER || megaApi.isInRubbish(
                            megaApi.getNodeByHandle(
                                videoViewModel.getCurrentPlayingHandle()
                            )
                        ) -> {
                            menu.toggleAllMenuItemsVisibility(false)

                            menu.findItem(R.id.properties).isVisible =
                                currentFragmentId == R.id.video_main_player

                            val moveToTrash = menu.findItem(R.id.move_to_trash) ?: return
                            moveToTrash.isVisible = true
                            moveToTrash.title = getString(R.string.context_remove)
                        }

                        adapterType == FROM_CHAT -> {
                            menu.toggleAllMenuItemsVisibility(false)

                            menu.findItem(R.id.save_to_device).isVisible = true
                            menu.findItem(R.id.chat_import).isVisible = true
                            menu.findItem(R.id.chat_save_for_offline).isVisible = true

                            menu.findItem(R.id.share).isVisible = false

                            menu.findItem(R.id.move_to_trash)?.let { moveToTrash ->
                                val pair = getChatMessage()
                                val message = pair.second

                                val canRemove = message != null
                                        && message.userHandle == megaChatApi.myUserHandle
                                        && message.isDeletable
                                moveToTrash.isVisible = canRemove
                                if (canRemove) {
                                    moveToTrash.title = getString(R.string.context_remove)
                                }
                            }
                        }

                        adapterType == FILE_LINK_ADAPTER -> {
                            menu.toggleAllMenuItemsVisibility(false)

                            menu.findItem(R.id.save_to_device).isVisible = true
                            menu.findItem(R.id.share).isVisible = true
                        }

                        adapterType == ZIP_ADAPTER -> {
                            menu.toggleAllMenuItemsVisibility(false)
                        }

                        adapterType == FOLDER_LINK_ADAPTER
                                || adapterType == FROM_IMAGE_VIEWER
                                || adapterType == FROM_ALBUM_SHARING
                                || adapterType == VERSIONS_ADAPTER -> {
                            menu.toggleAllMenuItemsVisibility(false)
                            menu.findItem(R.id.save_to_device).isVisible = true
                        }

                        else -> {
                            val node =
                                megaApi.getNodeByHandle(videoViewModel.getCurrentPlayingHandle())
                            if (node == null) {
                                Timber.d("refreshMenuOptionsVisibility node is null")

                                menu.toggleAllMenuItemsVisibility(false)
                                return
                            }

                            menu.toggleAllMenuItemsVisibility(true)
                            searchMenuItem?.isVisible = false

                            menu.findItem(R.id.save_to_device).isVisible = true
                            // Hide the select, select all, and clear options
                            menu.findItem(R.id.select).isVisible = false
                            menu.findItem(R.id.remove).isVisible = false

                            menu.findItem(R.id.properties).isVisible =
                                currentFragmentId == R.id.video_main_player

                            menu.findItem(R.id.share).isVisible =
                                currentFragmentId == R.id.video_main_player
                                        && MegaNodeUtil.showShareOption(
                                    adapterType = adapterType,
                                    isFolderLink = false,
                                    handle = node.handle
                                )
                            menu.findItem(R.id.send_to_chat).isVisible = true

                            val access = megaApi.getAccess(node)
                            val isAccessOwner = access == MegaShare.ACCESS_OWNER

                            menu.findItem(R.id.get_link).isVisible =
                                isAccessOwner && !node.isExported
                            menu.findItem(R.id.remove_link).isVisible =
                                isAccessOwner && node.isExported

                            menu.findItem(R.id.chat_import).isVisible = false
                            menu.findItem(R.id.chat_save_for_offline).isVisible = false

                            when (access) {
                                MegaShare.ACCESS_READWRITE,
                                MegaShare.ACCESS_READ,
                                MegaShare.ACCESS_UNKNOWN,
                                -> {
                                    menu.findItem(R.id.rename).isVisible = false
                                    menu.findItem(R.id.move).isVisible = false
                                }

                                MegaShare.ACCESS_FULL,
                                MegaShare.ACCESS_OWNER,
                                -> {
                                    menu.findItem(R.id.rename).isVisible = true
                                    menu.findItem(R.id.move).isVisible = true
                                }
                            }

                            menu.findItem(R.id.move_to_trash).isVisible =
                                node.parentHandle != megaApi.rubbishNode?.handle
                                        && (access == MegaShare.ACCESS_FULL
                                        || access == MegaShare.ACCESS_OWNER)

                            menu.findItem(R.id.copy).isVisible = true
                        }
                    }
                }
            }
            // After establishing the Options menu, check if read-only properties should be applied
            checkIfShouldApplyReadOnlyState(menu)
        }
    }

    /**
     * Checks and applies read-only restrictions (unable to Favourite, Rename, Move, or Move to Rubbish Bin)
     * on the Options toolbar if the [MegaNode] is a Backup node.
     *
     * @param menu The Options Menu
     */
    private fun checkIfShouldApplyReadOnlyState(menu: Menu) {
        videoViewModel.getCurrentPlayingHandle().let { playingHandle ->
            megaApi.getNodeByHandle(playingHandle)?.let { node ->
                if (megaApi.isInInbox(node)) {
                    with(menu) {
                        findItem(R.id.move_to_trash).isVisible = false
                        findItem(R.id.move).isVisible = false
                        findItem(R.id.rename).isVisible = false
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        nodeSaver.handleRequestPermissionsResult(requestCode = requestCode)
    }

    override fun setupToolbar() {
        setSupportActionBar(binding.toolbar)

        supportActionBar?.run {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            title = ""
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun setToolbarTitle(title: String) {
        binding.toolbar.title = title
    }

    /**
     * Update toolbar title based on orientation
     *
     * @param metadata Metadata
     */
    internal fun updateToolbarTitleBasedOnOrientation(metadata: Metadata) {
        setToolbarTitle(
            if (configuration.orientation == ORIENTATION_LANDSCAPE) {
                metadata.title ?: metadata.nodeName
            } else {
                ""
            }
        )
    }

    override fun hideToolbar(animate: Boolean) {
        toolbarDisplayedAnimation(animate, -binding.toolbar.measuredHeight.toFloat())
        hideSystemUI()
    }

    override fun showToolbar(animate: Boolean) {
        toolbarDisplayedAnimation(animate, TRANSLATION_Y_ZERO)
        showSystemUI()
    }

    private fun toolbarDisplayedAnimation(animate: Boolean, translationY: Float) {
        binding.toolbar.let { toolbar ->
            if (animate) {
                toolbar.animate()
                    .translationY(translationY)
                    .setDuration(MEDIA_PLAYER_TOOLBAR_SHOW_HIDE_DURATION_MS)
                    .start()
            } else {
                toolbar.animate().cancel()
                toolbar.translationY = translationY
            }
        }
    }

    private fun hideSystemUI() {
        with(WindowInsetsControllerCompat(window, binding.root)) {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun showSystemUI() {
        WindowInsetsControllerCompat(
            window,
            binding.root
        ).show(WindowInsetsCompat.Type.systemBars())
    }

    /**
     * Update toolbar
     *
     * @param isHide true is hidden, otherwise is shown
     */
    private fun updateToolbar(isHide: Boolean) {
        with(binding.toolbar) {
            animate().cancel()
            translationY =
                if (isHide) {
                    -measuredHeight.toFloat()
                } else {
                    TRANSLATION_Y_ZERO
                }
        }
    }

    override fun setupToolbarColors(showElevation: Boolean) {
        val isDarkMode = isDarkMode(this)
        val isMainPlayer = navController.currentDestination?.id == R.id.video_main_player
        val isPlaylist = navController.currentDestination?.id == R.id.video_playlist
        @ColorRes val toolbarBackgroundColor: Int
        @ColorInt val statusBarColor: Int
        val toolbarElevation: Float

        val elevationStatusBarColor =
            if (showElevation) {
                val elevation = resources.getDimension(R.dimen.toolbar_elevation)
                ColorUtils.getColorForElevation(this, elevation)
            } else {
                ContextCompat.getColor(this, android.R.color.transparent)
            }

        val elevationToolbarBackgroundColor =
            if (showElevation) {
                R.color.action_mode_background
            } else {
                R.color.black
            }

        WindowCompat.setDecorFitsSystemWindows(window, !isMainPlayer || !isPlaylist)

        updatePaddingForSystemUI(isMainPlayer, isPlaylist)

        binding.rootLayout.setBackgroundColor(getColor(R.color.black))

        when {
            (isMainPlayer || isPlaylist) && !isDarkMode -> {
                moveToDarkModeUI()
                toolbarElevation = TOOLBAR_ELEVATION_ZERO
                if (isMainPlayer) {
                    toolbarBackgroundColor = R.color.grey_alpha_070
                    statusBarColor = ContextCompat.getColor(this, R.color.black)
                } else {
                    toolbarBackgroundColor = elevationToolbarBackgroundColor
                    statusBarColor = elevationStatusBarColor
                }
            }

            isDarkMode -> {
                toolbarElevation = TOOLBAR_ELEVATION_ZERO
                toolbarBackgroundColor = elevationToolbarBackgroundColor
                statusBarColor = elevationStatusBarColor
            }

            else -> {
                toolbarElevation =
                    if (showElevation) {
                        resources.getDimension(R.dimen.toolbar_elevation)
                    } else {
                        TOOLBAR_ELEVATION_ZERO
                    }
                toolbarBackgroundColor =
                    if (showElevation) {
                        R.color.white
                    } else {
                        android.R.color.transparent
                    }
                statusBarColor = ContextCompat.getColor(this, R.color.white_dark_grey)
            }
        }

        window.statusBarColor = statusBarColor
        binding.toolbar.setBackgroundColor(ContextCompat.getColor(this, toolbarBackgroundColor))
        binding.toolbar.elevation = toolbarElevation
    }

    private fun moveToDarkModeUI() {
        with(binding.toolbar) {
            context.setTheme(R.style.videoPlayerToolbarThemeDark)
            navigationIcon?.setTint(Color.WHITE)
            setTitleTextColor(Color.WHITE)
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightNavigationBars = false
            isAppearanceLightStatusBars = false
        }
        window.navigationBarColor = getColor(android.R.color.transparent)
    }

    private fun updatePaddingForSystemUI(
        isVideoPlayerMainView: Boolean,
        isVideoPlaylist: Boolean,
    ) {
        binding.rootLayout.post {
            ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout) { _, windowInsets ->
                if (isVideoPlayerMainView || isVideoPlaylist) {
                    windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).let { insets ->
                        val horizontalInsets: Pair<Int, Int> =
                            if (resources.configuration.orientation == ORIENTATION_LANDSCAPE) {
                                // If the navigation bar at left side, add the padding that the
                                // width is equals height of status bar for right side.
                                if (insets.left > insets.right) {
                                    Pair(insets.left, Integer.max(insets.top, insets.bottom))
                                } else {
                                    Pair(Integer.max(insets.top, insets.bottom), insets.right)
                                }
                            } else {
                                Pair(0, 0)
                            }
                        // Only when not all inset values of the system UI are 0, update the padding.
                        // Avoid the view looks flicker
                        if (!(insets.top == 0 && insets.bottom == 0 && insets.left == 0 && insets.right == 0)) {
                            binding.toolbar.updatePadding(
                                left = horizontalInsets.first,
                                top = insets.top,
                                right = horizontalInsets.second,
                                bottom = 0
                            )
                            if (isVideoPlayerMainView) {
                                videoViewModel.updatePlayerControllerPaddingState(
                                    left = horizontalInsets.first,
                                    right = horizontalInsets.second,
                                    bottom = insets.bottom
                                )
                            }
                        }

                        binding.rootLayout.updatePadding(
                            left = if (isVideoPlaylist) horizontalInsets.first else 0,
                            top = 0,
                            right = if (isVideoPlaylist) horizontalInsets.second else 0,
                            bottom = if (isVideoPlaylist) insets.bottom else 0
                        )
                    }
                }
                WindowInsetsCompat.CONSUMED
            }
        }
    }

    override fun setDraggable(draggable: Boolean) {
        dragToExit.setDraggable(draggable)
    }

    private fun onDragActivated(activated: Boolean) {
        getFragmentFromNavHost(
            navHostId = R.id.nav_host_fragment,
            fragmentClass = VideoPlayerFragment::class.java
        )?.onDragActivated(dragToExit = dragToExit, activated = activated)
    }

    /**
     * Show snack bar
     * @param type
     * @param content
     * @param chatId
     */
    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        content?.let {
            showSnackBarForVideoPlayer(it)
        }
    }

    private fun onError(megaException: mega.privacy.android.domain.exception.MegaException) {
        when (megaException) {
            is QuotaExceededMegaException -> showGeneralTransferOverQuotaWarning()
            is BlockedMegaException -> {
                if (!AlertDialogUtil.isAlertDialogShown(takenDownDialog)) {
                    takenDownDialog = AlertsAndWarnings.showTakenDownAlert(this)
                }
            }
        }
    }

    /**
     * Shows the result of an exception.
     *
     * @param throwable The exception.
     */
    private fun manageException(throwable: Throwable) {
        if (!manageCopyMoveException(throwable) && throwable is MegaException) {
            throwable.message?.let { errorMessage ->
                showSnackBarForVideoPlayer(errorMessage)
            }
        }
    }

    /**
     * Show the customized snack bar for video player because video player always shows dark mode.
     *
     * @param message the message that will be shown
     */
    internal fun showSnackBarForVideoPlayer(message: String) {
        Snackbar.make(binding.rootLayout, message, Snackbar.LENGTH_LONG).let { snackBar ->
            with(snackBar.view as Snackbar.SnackbarLayout) {
                setBackgroundColor(getColor(R.color.white))
                findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                    .setTextColor(getColor(R.color.dark_grey))
            }
            snackBar.show()
        }
    }

    /**
     * Launch Activity and stop player
     */
    override fun launchActivity(intent: Intent) {
        startActivity(intent)
        stopPlayer()
    }

    private fun stopPlayer() {
        mediaPlayerGateway.playerStop()
        finish()
    }

    private fun setRestartPlayVideo() {
        updateDialogShownStateAndVideoPlayType(VIDEO_TYPE_RESTART_PLAYBACK_POSITION)
        // Set playWhenReady to be true, making the video is playing after the restart button is clicked
        if (!mediaPlayerGateway.getPlayWhenReady()) {
            mediaPlayerGateway.setPlayWhenReady(true)
        }
        // If the restart button is clicked, remove playback information of current item
        videoViewModel.deletePlaybackInformation(videoViewModel.getCurrentPlayingHandle())
    }

    private fun setRestartPlayVideoBeforeBuildSources() {
        updateDialogShownStateAndVideoPlayType(VIDEO_TYPE_RESTART_PLAYBACK_POSITION)
        // Initial video sources after the restart button is clicked
        videoViewModel.initVideoSources(intent)
    }

    private fun setResumePlaybackPosition(playbackPosition: Long?) {
        updateDialogShownStateAndVideoPlayType(VIDEO_TYPE_RESUME_PLAYBACK_POSITION)
        // Seek to playback position history after the resume button is clicked
        playbackPosition?.let {
            mediaPlayerGateway.playerSeekToPositionInMs(it)
        }
        // Set playWhenReady to be true, making the video is playing after the resume button is clicked
        if (!mediaPlayerGateway.getPlayWhenReady()) {
            mediaPlayerGateway.setPlayWhenReady(true)
        }
    }

    private fun setResumePlaybackPositionBeforeBuildSources() {
        updateDialogShownStateAndVideoPlayType(VIDEO_TYPE_RESUME_PLAYBACK_POSITION)
        // Initial video sources after the resume button is clicked
        videoViewModel.initVideoSources(intent)
    }

    private fun cancelPlaybackPositionDialog() {
        updateDialogShownStateAndVideoPlayType(VIDEO_TYPE_SHOW_PLAYBACK_POSITION_DIALOG)
    }

    private fun cancelPlaybackPositionDialogBeforeBuildSources() {
        updateDialogShownStateAndVideoPlayType(VIDEO_TYPE_SHOW_PLAYBACK_POSITION_DIALOG)
        videoViewModel.initVideoSources(intent)
        // If the dialog is cancelled, set PlayWhenReady to be false to paused video after build sources.
        mediaPlayerGateway.setPlayWhenReady(false)
    }

    /**
     * Update dialog shon state and video play type
     *
     * @param type video play type
     */
    private fun updateDialogShownStateAndVideoPlayType(type: Int) {
        // Set showDialog to be false, avoid the dialog is shown repeatedly when screen is rotated
        with(videoViewModel) {
            updateShowPlaybackPositionDialogState(
                showPlaybackPositionDialogState.value.copy(
                    showPlaybackDialog = false
                )
            )
            setVideoPlayType(type)
        }
    }

    companion object {
        private const val MEDIA_PLAYER_STATE_ENDED = 4
        private const val MEDIA_PLAYER_STATE_READY = 3

        private const val INTENT_KEY_STATE = "state"
        private const val STATE_HEADSET_UNPLUGGED = 0
    }
}
