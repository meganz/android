package mega.privacy.android.app.mediaplayer

import mega.privacy.android.shared.resources.R as sharedR
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
import android.database.ContentObserver
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.provider.Settings.System.ACCELEROMETER_ROTATION
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.OfflineFileInfoActivity
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.di.mediaplayer.VideoPlayer
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.showSnackbarWithChat
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerCallback
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.mediaplayer.videoplayer.navigation.VideoPlayerNavigationGraph
import mega.privacy.android.app.mediaplayer.videoplayer.navigation.videoPlayerComposeNavigationGraph
import mega.privacy.android.app.presentation.container.AppContainer
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.fileinfo.FileInfoActivity
import mega.privacy.android.app.presentation.hidenode.HiddenNodesOnboardingActivity
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.photos.albums.add.AddToAlbumActivity
import mega.privacy.android.app.presentation.psa.PsaContainer
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.app.presentation.settings.model.StorageTargetPreference
import mega.privacy.android.app.presentation.transfers.attach.NodeAttachmentView
import mega.privacy.android.app.presentation.transfers.attach.NodeAttachmentViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModel
import mega.privacy.android.app.presentation.videoplayer.model.MediaPlaybackState
import mega.privacy.android.app.presentation.videoplayer.model.MenuOptionClickedContent
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerAddToAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerChatImportAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerCopyAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerFileInfoAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerHideAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerMoveAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerRubbishBinAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerSaveForOfflineAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerSendToChatAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerUnhideAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoSize
import mega.privacy.android.app.usecase.exception.MegaException
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ChatUtil.AUDIOFOCUS_DEFAULT
import mega.privacy.android.app.utils.ChatUtil.getRequest
import mega.privacy.android.app.utils.Constants.BACKUPS_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_BACKUPS
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.FROM_INCOMING_SHARES
import mega.privacy.android.app.utils.Constants.HANDLE
import mega.privacy.android.app.utils.Constants.INCOMING_SHARES_ADAPTER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_COPY_FROM
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_COPY_HANDLES
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_COPY_TO
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FIRST_LEVEL
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FROM
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IMPORT_TO
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MOVE_FROM
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MOVE_HANDLES
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MOVE_TO
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.NAME
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.RECENTS_ADAPTER
import mega.privacy.android.app.utils.Constants.SEARCH_ADAPTER
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaNodeDialogUtil
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.mobile.analytics.event.HideNodeMultiSelectMenuItemEvent
import mega.privacy.mobile.analytics.event.VideoPlayerInfoMenuItemEvent
import mega.privacy.mobile.analytics.event.VideoPlayerScreenEvent
import mega.privacy.mobile.analytics.event.VideoPlayerSendToChatMenuToolbarEvent
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject

/**
 * The activity for the video player
 */
@OptIn(ExperimentalMaterialNavigationApi::class)
@AndroidEntryPoint
class VideoPlayerComposeActivity : PasscodeActivity() {
    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    @Inject
    lateinit var megaNavigator: MegaNavigator

    /**
     * MediaPlayerGateway for video player
     */
    @VideoPlayer
    @Inject
    lateinit var mediaPlayerGateway: MediaPlayerGateway

    private val videoPlayerViewModel: VideoPlayerViewModel by viewModels()
    private val nodeAttachmentViewModel: NodeAttachmentViewModel by viewModels()

    private var currentOrientation: Int = SCREEN_ORIENTATION_SENSOR_PORTRAIT

    private val headsetPlugReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_HEADSET_PLUG) {
                if (intent.getIntExtra(INTENT_KEY_STATE, -1) == STATE_HEADSET_UNPLUGGED) {
                    mediaPlayerGateway.setPlayWhenReady(false)
                }
            }
        }
    }

    private lateinit var mediaSessionHelper: MediaSessionHelper
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioFocusListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    mediaPlayerGateway.setPlayWhenReady(false)
                }

                AudioManager.AUDIOFOCUS_GAIN -> {
                    mediaPlayerGateway.setPlayWhenReady(true)
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

    private val selectImportFolderLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val toHandle = result.data?.getLongExtra(INTENT_EXTRA_KEY_IMPORT_TO, INVALID_HANDLE)
            if (toHandle == null) return@registerForActivityResult
            videoPlayerViewModel.importChatNode(newParentHandle = NodeId(toHandle))
        }

    private val nameCollisionActivityContract = registerForActivityResult(
        NameCollisionActivityContract()
    ) { result ->
        result?.let {
            videoPlayerViewModel.updateSnackBarMessage(it)
        }
    }

    private val selectFolderToMoveLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val moveHandles = result.data?.getLongArrayExtra(INTENT_EXTRA_KEY_MOVE_HANDLES)
            val toHandle = result.data?.getLongExtra(INTENT_EXTRA_KEY_MOVE_TO, INVALID_HANDLE)
            if (moveHandles != null && moveHandles.isNotEmpty() && toHandle != null)
                videoPlayerViewModel.moveNode(
                    nodeHandle = moveHandles[0],
                    newParentHandle = toHandle,
                )
        }

    private val selectFolderToCopyLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val copyHandles = result.data?.getLongArrayExtra(INTENT_EXTRA_KEY_COPY_HANDLES)
            val toHandle = result.data?.getLongExtra(INTENT_EXTRA_KEY_COPY_TO, INVALID_HANDLE)
            if (copyHandles != null && copyHandles.isNotEmpty() && toHandle != null) {
                videoPlayerViewModel.copyNode(
                    nodeHandle = copyHandles[0],
                    newParentHandle = toHandle,
                )
            }
        }

    private val addToAlbumLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleAddToAlbumResult,
        )

    private val hiddenNodesOnboardingLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleHiddenNodesOnboardingResult,
        )

    private fun handleHiddenNodesOnboardingResult(result: ActivityResult) {
        if (result.resultCode != RESULT_OK) return

        videoPlayerViewModel.hideOrUnhideNodes(
            nodeIds = listOf(NodeId(videoPlayerViewModel.uiState.value.currentPlayingHandle)),
            hide = true,
        )

        val message = resources.getQuantityString(R.plurals.hidden_nodes_result_message, 1, 1)
        videoPlayerViewModel.updateSnackBarMessage(message)
    }

    override fun attachBaseContext(newBase: Context?) {
        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Analytics.tracker.trackEvent(VideoPlayerScreenEvent)
        enableEdgeToEdge()
        setupImmersiveMode()
        currentOrientation = resources.configuration.orientation
        observeRotationSettingsChange()
        val player = createPlayer()
        setContent {
            val mode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            var passcodeEnabled by remember { mutableStateOf(true) }

            val containers: List<@Composable (@Composable () -> Unit) -> Unit> = listOf(
                { OriginalTheme(isDark = mode.isDarkMode(), content = it) },
                {
                    PasscodeContainer(
                        passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                        canLock = { passcodeEnabled },
                        content = it
                    )
                },
                { PsaContainer(content = it) }
            )

            AppContainer(
                containers = containers
            ) {
                val bottomSheetNavigator = rememberBottomSheetNavigator()
                val navHostController =
                    rememberNavController(bottomSheetNavigator)
                val uiState by videoPlayerViewModel.uiState.collectAsStateWithLifecycle()
                val scaffoldState = rememberScaffoldState()

                NavHost(
                    navController = navHostController,
                    startDestination = VideoPlayerNavigationGraph
                ) {
                    videoPlayerComposeNavigationGraph(
                        navHostController = navHostController,
                        bottomSheetNavigator = bottomSheetNavigator,
                        scaffoldState = scaffoldState,
                        viewModel = videoPlayerViewModel,
                        handleAutoReplayIfPaused = ::handleAutoReplayIfPaused,
                        player = player
                    )
                }

                StartTransferComponent(
                    event = uiState.downloadEvent,
                    onConsumeEvent = videoPlayerViewModel::resetDownloadNode,
                    snackBarHostState = scaffoldState.snackbarHostState,
                    navigateToStorageSettings = {
                        megaNavigator.openSettings(
                            this,
                            StorageTargetPreference
                        )
                    }
                )

                NodeAttachmentView(
                    viewModel = nodeAttachmentViewModel,
                    showMessage = ::showSnackbarWithChat
                )
            }
        }
        videoPlayerViewModel.initVideoPlayerData(intent)
        registerReceiver(headsetPlugReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))
        setupObserver()
        initMediaSession()
    }

    private fun setupImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS

            window.setFlags(FLAG_LAYOUT_NO_LIMITS, FLAG_LAYOUT_NO_LIMITS)

            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun handleAutoReplayIfPaused() {
        with(videoPlayerViewModel.uiState.value) {
            if (mediaPlaybackState == MediaPlaybackState.Paused && isAutoReplay) {
                videoPlayerViewModel.updatePlaybackStateWithReplay(true)
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

    private fun createPlayer(): ExoPlayer {
        val nameChangeCallback: (title: String?, artist: String?, album: String?) -> Unit =
            { title, artist, album ->
                with(videoPlayerViewModel) {
                    val playingItemTitle = uiState.value.currentPlayingItemName ?: ""
                    updateMetadata(Metadata(title, artist, album, playingItemTitle))
                }
            }

        return mediaPlayerGateway.createPlayer(
            repeatToggleMode = videoPlayerViewModel.uiState.value.repeatToggleMode,
            nameChangeCallback = nameChangeCallback,
            mediaPlayerCallback = object : MediaPlayerCallback {
                override fun onMediaItemTransitionCallback(handle: String?, isUpdateName: Boolean) {
                    videoPlayerViewModel.onMediaItemTransition(handle, isUpdateName)
                }

                override fun onShuffleModeEnabledChangedCallback(shuffleModeEnabled: Boolean) {
                }

                override fun onRepeatModeChangedCallback(repeatToggleMode: RepeatToggleMode) =
                    videoPlayerViewModel.updateRepeatToggleMode(repeatToggleMode)

                override fun onPlayWhenReadyChangedCallback(playWhenReady: Boolean) {
                    videoPlayerViewModel.updatePlaybackState(
                        if (playWhenReady)
                            MediaPlaybackState.Playing
                        else
                            MediaPlaybackState.Paused
                    )
                }

                override fun onPlaybackStateChangedCallback(state: Int) {
                    videoPlayerViewModel.onPlaybackStateChanged(state)
                }

                override fun onPlayerErrorCallback() = videoPlayerViewModel.onPlayerError()

                override fun onVideoSizeCallback(videoWidth: Int, videoHeight: Int) {
                    if (videoWidth == 0 || videoHeight == 0) return
                    videoPlayerViewModel.updateCurrentPlayingVideoSize(
                        VideoSize(videoWidth, videoHeight)
                    )
                    updateOrientationBasedOnVideoSize(videoWidth, videoHeight)
                }
            }
        )
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

    private fun setupObserver() {
        mediaPlayerGateway.monitorMediaNotAllowPlayState().onEach { notAllow ->
            if (notAllow) {
                videoPlayerViewModel.updateSnackBarMessage(getString(R.string.not_allow_play_alert))
            }
        }.launchIn(lifecycleScope)

        videoPlayerViewModel.getCollision().observe(this) { collision ->
            nameCollisionActivityContract.launch(arrayListOf(collision))
        }

        videoPlayerViewModel.onSnackbarMessage().observe(this) { message ->
            videoPlayerViewModel.updateSnackBarMessage(getString(message))
        }

        videoPlayerViewModel.onExceptionThrown().observe(this, ::manageException)

        videoPlayerViewModel.onStartChatFileOfflineDownload().observe(this) {
            videoPlayerViewModel.startDownloadForOffline(it)
        }

        collectFlow(
            videoPlayerViewModel.uiState.map { it.clickedMenuAction }.distinctUntilChanged()
        ) {
            handleMenuActions(it)
        }

        collectFlow(videoPlayerViewModel.uiState.map { it.menuOptionClickedContent }
            .distinctUntilChanged()) {
            it?.let { content ->
                when (content) {
                    is MenuOptionClickedContent.ShareFile ->
                        FileUtil.shareUri(this, content.fileName, content.contentUri)

                    is MenuOptionClickedContent.ShareLink ->
                        MegaNodeUtil.shareLink(this, content.fileLink, content.title)

                    is MenuOptionClickedContent.ShareNode ->
                        MegaNodeUtil.shareNode(context = this, node = content.node)

                    is MenuOptionClickedContent.GetLink ->
                        if (!showTakenDownNodeActionNotAvailableDialog(content.node, this)) {
                            LinksUtil.showGetLinkActivity(
                                this,
                                videoPlayerViewModel.uiState.value.currentPlayingHandle
                            )
                        }

                    is MenuOptionClickedContent.RemoveLink ->
                        if (!showTakenDownNodeActionNotAvailableDialog(content.node, this)) {
                            AlertsAndWarnings.showConfirmRemoveLinkDialog(this) {
                                videoPlayerViewModel.removeLink()
                            }
                        }

                    is MenuOptionClickedContent.Rename ->
                        MegaNodeDialogUtil.showRenameNodeDialog(
                            context = this,
                            node = content.node,
                            snackbarShower = this,
                            actionNodeCallback = object : ActionNodeCallback {
                                override fun finishRenameActionWithSuccess(newName: String) {
                                    val newMetadata =
                                        videoPlayerViewModel.uiState.value.metadata.copy(nodeName = newName)
                                    videoPlayerViewModel.updateMetadata(newMetadata)
                                }
                            })
                }
                videoPlayerViewModel.clearMenuOptionClickedContent()
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
                videoPlayerViewModel.updateSnackBarMessage(errorMessage)
            }
        }
    }

    private fun handleMenuActions(action: VideoPlayerMenuAction?) {
        if (action == null) return
        val launchSource = intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
        val playingHandle = videoPlayerViewModel.uiState.value.currentPlayingHandle
        when (action) {
            VideoPlayerFileInfoAction -> handleFileInfoAction(launchSource, playingHandle)
            VideoPlayerChatImportAction -> handleChatImportAction()
            VideoPlayerSendToChatAction -> handleSendToChatAction(playingHandle)
            VideoPlayerSaveForOfflineAction -> handleSaveForOfflineAction()
            VideoPlayerHideAction -> handleHideAction(playingHandle)
            VideoPlayerUnhideAction -> handleUnhideAction(playingHandle)
            VideoPlayerMoveAction -> handleMoveAction(playingHandle)
            VideoPlayerCopyAction -> handleCopyAction(playingHandle)
            VideoPlayerRubbishBinAction -> handleMoveToRubbishAction(playingHandle)
            VideoPlayerAddToAction -> handleAddToAction(playingHandle)
            else ->
                if (launchSource == FROM_CHAT)
                    handleRemoveAction()
                else
                    handleMoveToRubbishAction(playingHandle)
        }
        videoPlayerViewModel.updateClickedMenuAction(null)
    }

    private fun handleFileInfoAction(launchSource: Int, playingHandle: Long) {
        lifecycleScope.launch {
            Analytics.tracker.trackEvent(VideoPlayerInfoMenuItemEvent)
            val fileInfoIntent = when (launchSource) {
                OFFLINE_ADAPTER ->
                    Intent(
                        this@VideoPlayerComposeActivity,
                        OfflineFileInfoActivity::class.java
                    ).apply {
                        putExtra(HANDLE, playingHandle.toString())
                    }

                else -> {
                    val nodeName = videoPlayerViewModel.uiState.value.currentPlayingItemName
                    Intent(this@VideoPlayerComposeActivity, FileInfoActivity::class.java).apply {
                        putExtra(HANDLE, playingHandle)
                        putExtra(NAME, nodeName)
                    }.apply {
                        val fromIncoming = launchSource in listOf(SEARCH_ADAPTER, RECENTS_ADAPTER)
                                && videoPlayerViewModel.isNodeComesFromIncoming()
                        when {
                            launchSource == INCOMING_SHARES_ADAPTER || fromIncoming -> {
                                putExtra(INTENT_EXTRA_KEY_FROM, FROM_INCOMING_SHARES)
                                putExtra(INTENT_EXTRA_KEY_FIRST_LEVEL, false)
                            }

                            launchSource == BACKUPS_ADAPTER ->
                                putExtra(INTENT_EXTRA_KEY_FROM, FROM_BACKUPS)
                        }
                    }
                }
            }
            startActivity(fileInfoIntent)
        }
    }

    private fun handleChatImportAction() {
        selectImportFolderLauncher.launch(
            Intent(this, FileExplorerActivity::class.java).apply {
                this.action = FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER
            }
        )
    }

    private fun handleSendToChatAction(playingHandle: Long) {
        Analytics.tracker.trackEvent(VideoPlayerSendToChatMenuToolbarEvent)
        val ids = listOf(NodeId(playingHandle))
        nodeAttachmentViewModel.startAttachNodes(ids)
    }

    private fun handleSaveForOfflineAction() {
        if (getStorageState() == StorageState.PayWall) {
            AlertsAndWarnings.showOverDiskQuotaPaywallWarning()
        } else {
            videoPlayerViewModel.saveChatNodeToOffline()
        }
    }

    private fun handleHideAction(playingHandle: Long) {
        Analytics.tracker.trackEvent(HideNodeMultiSelectMenuItemEvent)
        var isPaid: Boolean
        var isHiddenNodesOnboarded: Boolean
        var isBusinessAccountExpired: Boolean


        with(videoPlayerViewModel.uiState.value) {
            isPaid = this.accountType?.isPaid == true
            isHiddenNodesOnboarded = this.isHiddenNodesOnboarded
            isBusinessAccountExpired = this.isBusinessAccountExpired
        }

        if (!isPaid || isBusinessAccountExpired) {
            val intent = HiddenNodesOnboardingActivity.createScreen(
                context = this,
                isOnboarding = false,
            )
            hiddenNodesOnboardingLauncher.launch(intent)
            overridePendingTransition(0, 0)
        } else if (isHiddenNodesOnboarded) {
            videoPlayerViewModel.hideOrUnhideNodes(
                nodeIds = listOf(NodeId(playingHandle)),
                hide = true,
            )
        } else {
            showHiddenNodesOnboarding()
        }
    }

    private fun showHiddenNodesOnboarding() {
        videoPlayerViewModel.setHiddenNodesOnboarded()

        val intent = HiddenNodesOnboardingActivity.createScreen(
            context = this,
            isOnboarding = true,
        )
        hiddenNodesOnboardingLauncher.launch(intent)
        overridePendingTransition(0, 0)
    }

    private fun handleUnhideAction(playingHandle: Long) {
        videoPlayerViewModel.hideOrUnhideNodes(
            nodeIds = listOf(NodeId(playingHandle)),
            hide = false,
        )
    }

    private fun handleMoveAction(playingHandle: Long) {
        selectFolderToMoveLauncher.launch(
            Intent(this, FileExplorerActivity::class.java).apply {
                action = FileExplorerActivity.ACTION_PICK_MOVE_FOLDER
                putExtra(INTENT_EXTRA_KEY_MOVE_FROM, longArrayOf(playingHandle))
            }
        )
    }

    private fun handleCopyAction(playingHandle: Long) {
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

    private fun handleRemoveAction() {
        MaterialAlertDialogBuilder(this@VideoPlayerComposeActivity)
            .setMessage(getString(R.string.confirmation_delete_one_attachment))
            .setPositiveButton(
                getString(R.string.context_remove)
            ) { _, _ ->
                lifecycleScope.launch {
                    runCatching { videoPlayerViewModel.deleteMessageFromChat() }
                        .onSuccess { finish() }
                        .onFailure { Timber.e(it) }
                }
            }
            .setNegativeButton(getString(sharedR.string.general_dialog_cancel_button), null)
            .show()
    }

    private fun handleMoveToRubbishAction(playingHandle: Long) {
        MegaNodeDialogUtil.moveToRubbishOrRemove(
            handle = playingHandle,
            activity = this,
            snackbarShower = this
        )
    }

    private fun handleAddToAction(playingHandle: Long) {
        val intent = Intent(this, AddToAlbumActivity::class.java).apply {
            val ids = listOf(playingHandle).toTypedArray()
            putExtra("ids", ids)
            putExtra("type", 1)
        }
        addToAlbumLauncher.launch(intent)
    }

    private fun handleAddToAlbumResult(result: ActivityResult) {
        if (result.resultCode != RESULT_OK) return
        val message = result.data?.getStringExtra("message") ?: return

        videoPlayerViewModel.updateSnackBarMessage(message)
    }

    private fun initMediaSession() {
        audioManager = (getSystemService(AUDIO_SERVICE) as AudioManager)
        audioFocusRequest = getRequest(audioFocusListener, AUDIOFOCUS_DEFAULT)
        mediaSessionHelper = MediaSessionHelper(
            applicationContext,
            onPlayPauseClicked = {
                mediaPlayerGateway.setPlayWhenReady(!mediaPlayerGateway.getPlayWhenReady())
            },
            onNextClicked = { mediaPlayerGateway.playNext() },
            onPreviousClicked = { mediaPlayerGateway.playPrev() }
        )
        audioFocusRequest?.let {
            if (audioManager?.requestAudioFocus(it) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mediaSessionHelper.setupMediaSession()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        videoPlayerViewModel.updatePlaybackStateWithReplay(false)
    }

    override fun onStart() {
        super.onStart()
        handleAutoReplayIfPaused()
    }

    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(rotationContentObserver)
        mediaPlayerGateway.playerStop()
        mediaPlayerGateway.playerRelease()
        AudioPlayerService.resumeAudioPlayer(this)
        unregisterReceiver(headsetPlugReceiver)
        if (audioManager != null) {
            ChatUtil.abandonAudioFocus(audioFocusListener, audioManager, audioFocusRequest)
        }
        mediaSessionHelper.releaseMediaSession()
    }

    companion object {
        private const val INTENT_KEY_STATE = "state"
        private const val STATE_HEADSET_UNPLUGGED = 0
    }
}