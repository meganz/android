package mega.privacy.android.app.presentation.videoplayer.view

import android.Manifest
import android.app.Activity
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment.DIRECTORY_DCIM
import android.os.Environment.getExternalStoragePublicDirectory
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.graphics.scale
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
import androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
import androidx.media3.ui.PlayerView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.VideoPlayerRevampPlayerViewBinding
import mega.privacy.android.app.mediaplayer.model.NavigationBarInsets
import mega.privacy.android.app.mediaplayer.model.NavigationBarPosition
import mega.privacy.android.app.mediaplayer.queue.audio.AudioQueueFragment.Companion.SINGLE_PLAYLIST_SIZE
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerController
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModelV2
import mega.privacy.android.app.presentation.videoplayer.model.MediaPlaybackState
import mega.privacy.android.app.presentation.videoplayer.model.SubtitleSelectedStatus
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMoreOption
import mega.privacy.android.app.presentation.videoplayer.model.VideoSpeedPlaybackMenuAction
import mega.privacy.android.app.utils.Constants.AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS
import mega.privacy.android.core.nodecomponents.list.NodeActionListTile
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.shared.original.core.ui.utils.rememberPermissionState
import mega.privacy.mobile.analytics.event.AddSubtitleDialogEvent
import mega.privacy.mobile.analytics.event.AddSubtitlesOptionPressedEvent
import mega.privacy.mobile.analytics.event.AutoMatchSubtitleOptionPressedEvent
import mega.privacy.mobile.analytics.event.LoopButtonPressedEvent
import mega.privacy.mobile.analytics.event.SnapshotButtonPressedEvent
import mega.privacy.mobile.analytics.event.SpeedSelectedDialogEvent

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalPermissionsApi::class,
    ExperimentalMaterial3Api::class,
)
@Composable
internal fun VideoPlayerScreen(
    viewModel: VideoPlayerViewModelV2,
    player: ExoPlayer?,
    playQueueButtonClicked: () -> Unit,
    onMoreActionsClicked: () -> Unit,
) {
    val context = LocalContext.current
    val resource = LocalResources.current
    val density = LocalDensity.current

    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation

    var videoPlayerController by remember { mutableStateOf<VideoPlayerController?>(null) }
    var isSpeedOptionsShown by rememberSaveable { mutableStateOf(false) }

    val systemUiController = rememberSystemUiController()
    var isControllerViewVisible by rememberSaveable { mutableStateOf(true) }

    val view = LocalView.current
    val rootView: View = (context as? Activity)?.window?.decorView ?: view
    val navBarInsets = rememberRevampNavigationBarInsets(rootView, orientation, density)
    val navigationBarHeight = maxOf(navBarInsets.bottom, navBarInsets.right, navBarInsets.left)
    val navigationBarHeightPx = with(density) { navigationBarHeight.toPx().toInt() }

    var navigationBarPosition by remember(navBarInsets) {
        mutableStateOf(
            when {
                navBarInsets.bottom > 0.dp -> NavigationBarPosition.Bottom
                navBarInsets.right > 0.dp -> NavigationBarPosition.Right
                navBarInsets.left > 0.dp -> NavigationBarPosition.Left
                else -> NavigationBarPosition.None
            }
        )
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var subtitleSheetMatchedInfo by remember { mutableStateOf<SubtitleFileInfo?>(null) }

    val subtitleSheetRows = remember(
        uiState.addedSubtitleInfo?.name,
        subtitleSheetMatchedInfo,
    ) {
        buildSubtitleSheetRows(
            uiState.addedSubtitleInfo?.name,
            subtitleSheetMatchedInfo,
        )
    }

    val subtitleSheetState = key(subtitleSheetRows.size) {
        rememberModalBottomSheetState(skipPartiallyExpanded = true)
    }

    val snackBarQueue = rememberSnackBarQueue()

    LaunchedEffect(uiState.showSubTitlesOptions) {
        if (uiState.showSubTitlesOptions) {
            subtitleSheetMatchedInfo = viewModel.getMatchedSubtitleFileInfo()
            Analytics.tracker.trackEvent(AddSubtitleDialogEvent)
        }
    }

    LaunchedEffect(uiState.showSubTitlesOptions, subtitleSheetRows.size) {
        if (uiState.showSubTitlesOptions) {
            subtitleSheetState.show()
        }
    }

    val isShowSubtitleIcon = viewModel.isShowSubtitleIcon()
    val isShowPlaylistOption = uiState.items.size > SINGLE_PLAYLIST_SIZE
    val moreOptionActions = remember(isShowSubtitleIcon, isShowPlaylistOption) {
        buildList {
            add(VideoPlayerMoreOption.Snapshot)
            if (isShowSubtitleIcon) {
                add(VideoPlayerMoreOption.Subtitle)
            }
            if (isShowPlaylistOption) {
                add(VideoPlayerMoreOption.Playlist)
            }
            add(VideoPlayerMoreOption.Lock)
        }
    }

    var playbackState by rememberSaveable { mutableIntStateOf(STATE_IDLE) }
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    val playerEventListener = remember {
        object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
    }

    val moreOptionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val coroutineScope = rememberCoroutineScope()
    var autoHideJob by remember { mutableStateOf<Job?>(null) }

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    // Consume back presses while the player is locked to prevent accidental navigation
    BackHandler(enabled = uiState.isLocked) {}

    var resizedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isScreenshotVisible by remember { mutableStateOf(false) }
    val scale = remember { Animatable(1f) }

    val screenWidth = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }

    var playerView by remember { mutableStateOf<PlayerView?>(null) }

    var snapshotScreen by rememberSaveable { mutableStateOf(false) }
    val writeStoragePermission = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        rememberPermissionState(Manifest.permission.WRITE_EXTERNAL_STORAGE) { granted ->
            snapshotScreen = granted
        }
    } else {
        null
    }
    LaunchedEffect(snapshotScreen) {
        if (snapshotScreen) {
            val rootPath =
                getExternalStoragePublicDirectory(DIRECTORY_DCIM).absolutePath
            playerView?.videoSurfaceView?.let { surfaceView ->
                viewModel.screenshotWhenVideoPlaying(
                    rootPath = rootPath,
                    captureView = surfaceView
                ) { bitmap ->
                    Analytics.tracker.trackEvent(SnapshotButtonPressedEvent)
                    val (width, height) =
                        if (orientation == ORIENTATION_LANDSCAPE && bitmap.height > bitmap.width) {
                            (screenHeight * bitmap.width / bitmap.height) to screenHeight
                        } else {
                            screenWidth to (screenWidth * bitmap.height / bitmap.width)
                        }
                    val scaledBitmap =
                        bitmap.scale(width.toInt(), height.toInt(), false)
                    bitmap.recycle()
                    withContext(Dispatchers.Main.immediate) {
                        resizedBitmap = scaledBitmap
                        isScreenshotVisible = true
                    }
                }
            }
            snapshotScreen = false
        }
    }

    LaunchedEffect(isScreenshotVisible) {
        if (isScreenshotVisible) {
            scale.snapTo(1f)
            scale.animateTo(if (orientation == ORIENTATION_LANDSCAPE) 0.3f else 0.4f, tween(1000))
            coroutineScope.launch {
                snackBarQueue.queueMessage(resource.getString(R.string.media_player_video_snackbar_screenshot_saved))
            }
            delay(1000)
            resizedBitmap?.recycle()
            resizedBitmap = null
            isScreenshotVisible = false
        }
    }

    LaunchedEffect(playbackState, uiState.showSubTitlesOptions, uiState.isMoreOptionShown) {
        if (playbackState <= STATE_BUFFERING ||
            uiState.showSubTitlesOptions ||
            uiState.isMoreOptionShown
        ) {
            autoHideJob?.cancel()
        } else if (isControllerViewVisible) {
            delay(AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS)
            isControllerViewVisible = false
            systemUiController.isSystemBarsVisible = false
            playerView?.hideController()
        }
    }

    LaunchedEffect(uiState.items) {
        videoPlayerController?.updatePlayQueueOverflowMenuItems(uiState.items.size)
    }

    LaunchedEffect(uiState.isFullscreen) {
        videoPlayerController?.updateFullscreenButtonIcon(uiState.isFullscreen)
    }

    LaunchedEffect(uiState.isLocked) {
        videoPlayerController?.updateLockView(uiState.isLocked)
    }

    LaunchedEffect(uiState.currentSpeedPlayback) {
        videoPlayerController?.updateSpeedPlaybackButtonIcon(uiState.currentSpeedPlayback.text)
    }

    LaunchedEffect(uiState.repeatToggleMode) {
        videoPlayerController?.updateRepeatToggleButtonUI(context, uiState.repeatToggleMode)
    }

    LaunchedEffect(uiState.mediaPlaybackState) {
        videoPlayerController?.updatePlaybackState(uiState.mediaPlaybackState)
    }

    DisposableEffect(Unit) {
        playbackState = player?.playbackState ?: STATE_IDLE
        isPlaying = player?.isPlaying ?: false

        player?.addListener(playerEventListener)
        onDispose {
            player?.removeListener(playerEventListener)
        }
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                testTagsAsResourceId = true
            },
    ) { _ ->
        key(orientation) {
            AndroidViewBinding(
                modifier = Modifier.fillMaxSize(),
                factory = { inflater, parent, attachToParent ->
                    VideoPlayerRevampPlayerViewBinding.inflate(inflater, parent, attachToParent)
                        .apply {
                            playerView = playerComposeView
                            fun updateResizeMode(isFullscreen: Boolean) {
                                playerComposeView.resizeMode = if (isFullscreen) {
                                    RESIZE_MODE_ZOOM
                                } else {
                                    RESIZE_MODE_FIT
                                }
                            }

                            fun applyPlayPauseIcon() {
                                playerComposeView.findViewById<ImageButton>(androidx.media3.ui.R.id.exo_play_pause)
                                    ?.setImageDrawable(
                                        ContextCompat.getDrawable(
                                            context,
                                            if (isPlaying) R.drawable.ic_player_pause else R.drawable.ic_player_play
                                        )
                                    )
                            }

                            videoPlayerController = VideoPlayerController(
                                context = context,
                                uiState = uiState,
                                container = root,
                                updateRepeatToggleMode = {
                                    val repeatToggleMode =
                                        uiState.repeatToggleMode.let { repeatToggleMode ->
                                            if (repeatToggleMode == RepeatToggleMode.REPEAT_NONE) {
                                                Analytics.tracker.trackEvent(
                                                    LoopButtonPressedEvent
                                                )
                                                RepeatToggleMode.REPEAT_ONE

                                            } else {
                                                RepeatToggleMode.REPEAT_NONE
                                            }
                                        }
                                    viewModel.setRepeatToggleModeForPlayer(repeatToggleMode)
                                },
                                updateIsVideoOptionPopupShown = { value ->
                                    viewModel.updateIsMoreOptionShown(value)
                                },
                                updateIsSpeedOptionsShown = { value ->
                                    isSpeedOptionsShown = value
                                },
                                updateLockStatus = { isLock ->
                                    viewModel.updateLockStatus(isLock)
                                },
                                fullscreenClickedCallback = { isFullscreen ->
                                    viewModel.updateFullscreen(isFullscreen)
                                    updateResizeMode(isFullscreen)
                                },
                                lockStateChanged = { isLock ->
                                    autoHideJob?.cancel()
                                    if (isLock) {
                                        isControllerViewVisible = true
                                        systemUiController.isSystemBarsVisible = false
                                        playerComposeView.showController()
                                        autoHideJob = coroutineScope.launch {
                                            delay(AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS)
                                            isControllerViewVisible = false
                                            playerComposeView.hideController()
                                        }
                                    } else {
                                        autoHideJob = coroutineScope.launch {
                                            isControllerViewVisible = true
                                            systemUiController.isSystemBarsVisible = true
                                            playerComposeView.showController()
                                            delay(AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS)
                                            isControllerViewVisible = false
                                            systemUiController.isSystemBarsVisible = false
                                            playerComposeView.hideController()
                                        }
                                    }
                                },
                                playerViewClicked = {
                                    val visible = !isControllerViewVisible
                                    autoHideJob?.cancel()
                                    isControllerViewVisible = visible
                                    if (!uiState.isLocked) {
                                        systemUiController.isSystemBarsVisible = visible
                                    }
                                    if (visible) {
                                        playerComposeView.showController()
                                        if (uiState.isLocked) {
                                            autoHideJob = coroutineScope.launch {
                                                delay(AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS)
                                                isControllerViewVisible = false
                                                playerComposeView.hideController()
                                            }
                                        }
                                    } else {
                                        playerComposeView.hideController()
                                    }
                                },
                                onSnapshotSelected = {
                                    writeStoragePermission?.launchPermissionRequest() ?: run {
                                        snapshotScreen = true
                                    }
                                },
                            ).also { controller ->
                                playerComposeView.tag = controller
                            }

                            playerComposeView.setControllerVisibilityListener(
                                PlayerView.ControllerVisibilityListener { visibility ->
                                    if (visibility == View.VISIBLE) {
                                        applyPlayPauseIcon()
                                    }
                                    if (visibility == View.VISIBLE && !isControllerViewVisible && !uiState.isLocked) {
                                        autoHideJob?.cancel()
                                        autoHideJob = coroutineScope.launch {
                                            delay(AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS)
                                            playerComposeView.hideController()
                                        }
                                    }
                                }
                            )

                            playerComposeView.player = player
                            playerComposeView.controllerShowTimeoutMs = 0
                            updateResizeMode(uiState.isFullscreen)

                            autoHideJob?.cancel()
                            if (isControllerViewVisible) {
                                systemUiController.isSystemBarsVisible = true
                                playerComposeView.showController()
                            }

                            playerComposeView.controllerAutoShow = false

                            autoHideJob = coroutineScope.launch {
                                delay(AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS)
                                isControllerViewVisible = false
                                systemUiController.isSystemBarsVisible = false
                                playerComposeView.hideController()
                            }
                        }
                },
                onRelease = {
                    (playerComposeView.tag as? VideoPlayerController)?.release()
                    if (uiState.isMoreOptionShown) {
                        viewModel.updateIsMoreOptionShown(false)
                    }
                }
            ) {
                val controllerView = root.findViewById<View>(R.id.controls_view)

                playerComposeView.keepScreenOn =
                    uiState.mediaPlaybackState == MediaPlaybackState.Playing

                updateControllerViewPadding(
                    controllerView = controllerView,
                    orientation = orientation,
                    padding = navigationBarHeightPx,
                    navigationBarPosition = navigationBarPosition
                )
                root.findViewById<View>(R.id.navigation_bar_bg).isVisible =
                    orientation != ORIENTATION_PORTRAIT

                root.findViewById<ProgressBar>(R.id.loading_video_player_controller_view).isVisible =
                    playbackState <= STATE_BUFFERING

                root.findViewById<View>(R.id.exo_play_pause).isVisible =
                    playbackState > STATE_BUFFERING
                root.findViewById<ImageButton>(androidx.media3.ui.R.id.exo_play_pause)
                    ?.setImageDrawable(
                        ContextCompat.getDrawable(
                            context,
                            if (isPlaying) R.drawable.ic_player_pause else R.drawable.ic_player_play
                        )
                    )
            }

            if (isControllerViewVisible && !uiState.isLocked) {
                val horizontalPadding = when (orientation) {
                    ORIENTATION_LANDSCAPE if navigationBarPosition == NavigationBarPosition.Left ->
                        PaddingValues(start = navigationBarHeight)

                    ORIENTATION_LANDSCAPE if navigationBarPosition == NavigationBarPosition.Right ->
                        PaddingValues(end = navigationBarHeight)

                    else -> PaddingValues(0.dp)
                }
                VideoPlayerTopBar(
                    modifier = Modifier.padding(horizontalPadding),
                    title = uiState.metadata.title ?: uiState.metadata.nodeName,
                    onBackPressed = { backDispatcher?.onBackPressed() },
                    onMoreActionsClicked = onMoreActionsClicked,
                )
            }

            resizedBitmap?.let {
                if (isScreenshotVisible) {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Screenshot Animation",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale.value,
                                scaleY = scale.value,
                                transformOrigin =
                                    if (orientation == ORIENTATION_LANDSCAPE)
                                        TransformOrigin(0.9f, 0.9f)
                                    else {
                                        TransformOrigin(0.9f, 0.8f)
                                    }
                            )
                    )
                }
            }

            if (isSpeedOptionsShown) {
                LaunchedEffect(Unit) {
                    Analytics.tracker.trackEvent(SpeedSelectedDialogEvent)
                }
                val speedSheetState =
                    rememberModalBottomSheetState(skipPartiallyExpanded = false)
                MegaModalBottomSheet(
                    bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
                    sheetState = speedSheetState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    onDismissRequest = { isSpeedOptionsShown = false },
                ) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        VideoSpeedPlaybackMenuAction.entries.forEach { action ->
                            val isSelected = action.playbackItem == uiState.currentSpeedPlayback
                            FlexibleLineListItem(
                                modifier = Modifier.testTag(action.testTag),
                                title = action.getDescription(),
                                trailingElement = {
                                    if (isSelected) {
                                        MegaIcon(
                                            modifier = Modifier.size(24.dp),
                                            painter = rememberVectorPainter(
                                                IconPack.Small.Thin.Outline.Check
                                            ),
                                            contentDescription = null,
                                            tint = IconColor.Secondary,
                                        )
                                    }
                                },
                                onClickListener = {
                                    Analytics.tracker.trackEvent(action.speedOptionPressedEvent)
                                    viewModel.updateCurrentSpeedPlaybackItem(action.playbackItem)
                                    coroutineScope.launch {
                                        speedSheetState.hide()
                                        isSpeedOptionsShown = false
                                    }
                                },
                            )
                        }
                    }
                }
            }

            if (uiState.showSubTitlesOptions) {
                MegaModalBottomSheet(
                    bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
                    sheetState = subtitleSheetState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    windowInsets = WindowInsets.navigationBars,
                    onDismissRequest = {
                        viewModel.updateShowSubtitleDialog(false)
                    },
                ) {
                    VideoPlayerSubtitleBottomSheetContent(
                        rows = subtitleSheetRows,
                        selectOptionState = uiState.subtitleSelectedStatus.id,
                        onOffClicked = {
                            viewModel.updateSubtitleSelectedStatus(SubtitleSelectedStatus.Off)
                        },
                        onAddedSubtitleClicked = {
                            viewModel.updateSubtitleSelectedStatus(SubtitleSelectedStatus.AddSubtitleItem)
                        },
                        onAutoMatch = { info ->
                            if (info.url == null) {
                                coroutineScope.launch {
                                    snackBarQueue.queueMessage(
                                        resource.getString(R.string.media_player_video_message_adding_subtitle_failed)
                                    )
                                }
                            } else {
                                Analytics.tracker.trackEvent(AutoMatchSubtitleOptionPressedEvent)
                                viewModel.updateSubtitleSelectedStatus(
                                    SubtitleSelectedStatus.SelectMatchedItem,
                                    info
                                )
                            }
                        },
                        onToSelectSubtitle = {
                            Analytics.tracker.trackEvent(AddSubtitlesOptionPressedEvent)
                            viewModel.navigateToSelectSubtitle()
                        },
                    )
                }
            }

            if (uiState.isMoreOptionShown) {
                MegaModalBottomSheet(
                    modifier = Modifier.fillMaxWidth(),
                    sheetState = moreOptionsSheetState,
                    bottomSheetBackground = MegaModalBottomSheetBackground.PageBackground,
                    onDismissRequest = {
                        // Called after a swipe/outside-tap gesture — the sheet is already
                        // animating away, so just sync the ViewModel state.
                        viewModel.updateIsMoreOptionShown(false)
                    },
                    content = {
                        moreOptionActions.forEach { action ->
                            NodeActionListTile(
                                modifier = Modifier.testTag(action.testTag),
                                menuAction = action,
                                onActionClicked = {
                                    // Animate the sheet away first, then perform the action
                                    // so the hide animation is not skipped.
                                    coroutineScope.launch {
                                        moreOptionsSheetState.hide()
                                    }.invokeOnCompletion { cause ->
                                        if (cause == null && !moreOptionsSheetState.isVisible) {
                                            when (action) {
                                                VideoPlayerMoreOption.Snapshot ->
                                                    videoPlayerController?.onSnapshotOptionSelected()

                                                VideoPlayerMoreOption.Subtitle ->
                                                    viewModel.updateShowSubtitleDialog(true)

                                                VideoPlayerMoreOption.Playlist -> {
                                                    autoHideJob?.cancel()
                                                    playQueueButtonClicked()
                                                }

                                                VideoPlayerMoreOption.Lock ->
                                                    videoPlayerController?.onLockOptionSelected()
                                            }
                                            viewModel.updateIsMoreOptionShown(false)
                                        }
                                    }
                                },
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun rememberRevampNavigationBarInsets(
    root: View,
    orientation: Int,
    density: Density,
): NavigationBarInsets {
    var navInsets by remember { mutableStateOf(NavigationBarInsets()) }

    DisposableEffect(root, orientation) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val gestures = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
            val tappable = insets.getInsets(WindowInsetsCompat.Type.tappableElement())

            val bottomPx = maxOf(systemBars.bottom, gestures.bottom, tappable.bottom)
            val leftPx = maxOf(systemBars.left, gestures.left, tappable.left)
            val rightPx = maxOf(systemBars.right, gestures.right, tappable.right)

            navInsets = NavigationBarInsets(
                bottom = with(density) { bottomPx.toDp() },
                left = with(density) { leftPx.toDp() },
                right = with(density) { rightPx.toDp() },
            )
            insets
        }
        ViewCompat.requestApplyInsets(root)

        onDispose {
            ViewCompat.setOnApplyWindowInsetsListener(root, null)
        }
    }

    return navInsets
}

private fun updateControllerViewPadding(
    controllerView: View,
    orientation: Int,
    padding: Int,
    navigationBarPosition: NavigationBarPosition,
) {
    val layoutParams = controllerView.layoutParams as ViewGroup.MarginLayoutParams
    if (orientation == ORIENTATION_PORTRAIT || navigationBarPosition == NavigationBarPosition.Bottom) {
        controllerView.setPadding(0, 0, 0, padding)
        layoutParams.bottomMargin = 0
        layoutParams.marginStart = 0
        layoutParams.marginEnd = 0
    } else {
        controllerView.setPadding(0, 0, 0, 0)
        layoutParams.bottomMargin = 0
        when (navigationBarPosition) {
            NavigationBarPosition.Left -> layoutParams.marginStart = padding
            NavigationBarPosition.Right -> layoutParams.marginEnd = padding
            else -> {}
        }
    }
    controllerView.layoutParams = layoutParams
}
