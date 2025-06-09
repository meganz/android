package mega.privacy.android.app.mediaplayer.videoplayer.view

import android.annotation.SuppressLint
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.graphics.Bitmap
import android.os.Environment.DIRECTORY_DCIM
import android.os.Environment.getExternalStoragePublicDirectory
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ScaffoldState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.graphics.scale
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
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.VideoPlayerPlayerViewBinding
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerController
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModel
import mega.privacy.android.app.presentation.videoplayer.model.MediaPlaybackState
import mega.privacy.android.app.presentation.videoplayer.model.PlaybackPositionStatus
import mega.privacy.android.app.presentation.videoplayer.model.SubtitleSelectedStatus
import mega.privacy.android.app.presentation.videoplayer.view.AddSubtitlesDialog
import mega.privacy.android.app.presentation.videoplayer.view.VideoPlayerTopBar
import mega.privacy.android.app.utils.Constants.AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.sheets.MegaBottomSheetLayout
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.mobile.analytics.event.AddSubtitlesOptionPressedEvent
import mega.privacy.mobile.analytics.event.AutoMatchSubtitleOptionPressedEvent
import mega.privacy.mobile.analytics.event.LoopButtonPressedEvent
import mega.privacy.mobile.analytics.event.SnapshotButtonPressedEvent
import kotlin.math.roundToInt

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialNavigationApi::class)
@Composable
internal fun VideoPlayerScreen(
    bottomSheetNavigator: BottomSheetNavigator,
    scaffoldState: ScaffoldState,
    viewModel: VideoPlayerViewModel,
    player: ExoPlayer?,
    playQueueButtonClicked: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation

    var videoPlayerController by remember { mutableStateOf<VideoPlayerController?>(null) }

    val systemUiController = rememberSystemUiController()
    var isControllerViewVisible by rememberSaveable { mutableStateOf(true) }

    val navigationBarHeight =
        getNavigationBarHeight(orientation, density, LocalLayoutDirection.current)
    val navigationBarHeightPx = with(density) { navigationBarHeight.toPx().toInt() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var playbackState by rememberSaveable { mutableIntStateOf(STATE_IDLE) }
    val playerEventListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            playbackState = state
        }
    }

    val coroutineScope = rememberCoroutineScope()
    var autoHideJob by remember { mutableStateOf<Job?>(null) }

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    var resizedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isScreenshotVisible by remember { mutableStateOf(false) }
    val scale = remember { Animatable(1f) }

    val screenWidth = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }

    var playerView by remember { mutableStateOf<PlayerView?>(null) }

    LaunchedEffect(isScreenshotVisible) {
        if (isScreenshotVisible) {
            scale.snapTo(1f)
            scale.animateTo(if (orientation == ORIENTATION_LANDSCAPE) 0.3f else 0.4f, tween(1000))
            coroutineScope.launch {
                scaffoldState.snackbarHostState.showAutoDurationSnackbar(context.getString(R.string.media_player_video_snackbar_screenshot_saved))
            }
            delay(1000)
            resizedBitmap?.recycle()
            resizedBitmap = null
            isScreenshotVisible = false
        }
    }

    LaunchedEffect(uiState.snackBarMessage) {
        uiState.snackBarMessage?.let { message ->
            scaffoldState.snackbarHostState.showAutoDurationSnackbar(message)
            viewModel.updateSnackBarMessage(null)
        }
    }

    LaunchedEffect(uiState.showPlaybackDialog, uiState.showSubtitleDialog) {
        if (uiState.showPlaybackDialog || uiState.showSubtitleDialog) {
            autoHideJob?.cancel()
        } else {
            if (isControllerViewVisible) {
                autoHideJob?.cancel()
                autoHideJob = coroutineScope.launch {
                    delay(AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS)
                    isControllerViewVisible = false
                    systemUiController.isSystemBarsVisible = false
                    playerView?.hideController()
                }
            }
        }
    }

    LaunchedEffect(uiState.items) {
        if (uiState.items.isNotEmpty()) {
            videoPlayerController?.togglePlayQueueEnabled(uiState.items.size)
        }
    }

    LaunchedEffect(uiState.isFullscreen) {
        videoPlayerController?.updateFullscreenButtonIcon(uiState.isFullscreen)
    }

    LaunchedEffect(uiState.isLocked) {
        videoPlayerController?.updateLockView(uiState.isLocked)
    }

    LaunchedEffect(uiState.currentSpeedPlayback) {
        videoPlayerController?.updateSpeedPlaybackButtonIcon(uiState.currentSpeedPlayback.iconId)
    }

    LaunchedEffect(uiState.subtitleSelectedStatus) {
        videoPlayerController?.updateSubtitleButtonUI(uiState.subtitleSelectedStatus)
    }

    LaunchedEffect(uiState.metadata) {
        videoPlayerController?.displayMetadata(uiState.metadata)
    }

    LaunchedEffect(uiState.repeatToggleMode) {
        videoPlayerController?.updateRepeatToggleButtonUI(context, uiState.repeatToggleMode)
    }

    LaunchedEffect(uiState.mediaPlaybackState) {
        videoPlayerController?.updatePlaybackState(uiState.mediaPlaybackState)
    }

    DisposableEffect(Unit) {
        playbackState = player?.playbackState ?: STATE_IDLE

        player?.addListener(playerEventListener)
        onDispose {
            player?.removeListener(playerEventListener)
        }
    }

    MegaBottomSheetLayout(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                testTagsAsResourceId = true
            },
        bottomSheetNavigator = bottomSheetNavigator,
    ) {
        MegaScaffold(
            modifier = Modifier.fillMaxSize(),
            scaffoldState = scaffoldState,
        ) { paddingValues ->
            key(orientation) {
                AndroidViewBinding(
                    modifier = Modifier.fillMaxSize(),
                    factory = { inflater, parent, attachToParent ->
                        VideoPlayerPlayerViewBinding.inflate(inflater, parent, attachToParent)
                            .apply {
                                playerView = playerComposeView
                                fun updateResizeMode(isFullscreen: Boolean) {
                                    playerComposeView.resizeMode = if (isFullscreen) {
                                        RESIZE_MODE_ZOOM
                                    } else {
                                        RESIZE_MODE_FIT
                                    }
                                }

                                videoPlayerController = VideoPlayerController(
                                    context = context,
                                    uiState = uiState,
                                    container = root,
                                    isShowSubtitleIcon = viewModel.isShowSubtitleIcon(),
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
                                        viewModel.updateIsVideoOptionPopupShown(value)
                                    },
                                    updateIsSpeedPopupShown = { value ->
                                        viewModel.updateIsSpeedPopupShown(value)
                                    },
                                    speedPlaybackItemSelected = { item ->
                                        viewModel.updateCurrentSpeedPlaybackItem(item)
                                    },
                                    updateLockStatus = { isLock ->
                                        viewModel.updateLockStatus(isLock)
                                    },
                                    showSubtitleDialog = {
                                        viewModel.updateShowSubtitleDialog(true)
                                    },
                                    fullscreenClickedCallback = { isFullscreen ->
                                        viewModel.updateFullscreen(isFullscreen)
                                        updateResizeMode(isFullscreen)
                                    },
                                    lockStateChanged = {
                                        autoHideJob?.cancel()
                                        autoHideJob = coroutineScope.launch {
                                            systemUiController.isSystemBarsVisible = true
                                            playerComposeView.showController()
                                            delay(AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS)
                                            systemUiController.isSystemBarsVisible = false
                                            playerComposeView.hideController()
                                        }
                                    },
                                    playQueueButtonClicked = {
                                        autoHideJob?.cancel()
                                        playQueueButtonClicked()
                                    },
                                    playerViewClicked = {
                                        val visible = !isControllerViewVisible
                                        autoHideJob?.cancel()
                                        isControllerViewVisible = visible
                                        systemUiController.isSystemBarsVisible = visible
                                        if (visible) {
                                            playerComposeView.showController()
                                        } else {
                                            playerComposeView.hideController()
                                        }
                                    }
                                ) {
                                    val rootPath =
                                        getExternalStoragePublicDirectory(DIRECTORY_DCIM).absolutePath
                                    playerComposeView.videoSurfaceView?.let { view ->
                                        viewModel.screenshotWhenVideoPlaying(
                                            rootPath = rootPath,
                                            captureView = view
                                        ) { bitmap ->
                                            Analytics.tracker.trackEvent(SnapshotButtonPressedEvent)
                                            val (width, height) =
                                                if (orientation == ORIENTATION_LANDSCAPE && bitmap.height > bitmap.width) {
                                                    (screenHeight * bitmap.width / bitmap.height) to screenHeight
                                                } else {
                                                    screenWidth to (screenWidth * bitmap.height / bitmap.width)
                                                }

                                            resizedBitmap =
                                                bitmap.scale(width.toInt(), height.toInt(), false)
                                            isScreenshotVisible = true
                                            bitmap.recycle()
                                        }
                                    }
                                }.also {
                                    playerComposeView.tag = it
                                }

                                playerComposeView.setControllerVisibilityListener(
                                    object : PlayerView.ControllerVisibilityListener {
                                        override fun onVisibilityChanged(visibility: Int) {
                                            if (visibility == View.VISIBLE && !isControllerViewVisible) {
                                                autoHideJob?.cancel()
                                                autoHideJob = coroutineScope.launch {
                                                    delay(AUDIO_PLAYER_TOOLBAR_INIT_HIDE_DELAY_MS)
                                                    playerComposeView.hideController()
                                                }
                                            }
                                        }
                                    }
                                )

                                playerComposeView.player = player
                                playerComposeView.controllerShowTimeoutMs = 0
                                updateResizeMode(uiState.isFullscreen)

                                autoHideJob?.cancel()
                                isControllerViewVisible = true
                                systemUiController.isSystemBarsVisible = true
                                playerComposeView.showController()

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
                        if (uiState.isVideoOptionPopupShown) {
                            viewModel.updateIsVideoOptionPopupShown(false)
                        }
                    }
                ) {
                    val controllerView = root.findViewById<View>(R.id.controls_view)
                    playerComposeView.keepScreenOn =
                        uiState.mediaPlaybackState == MediaPlaybackState.Playing

                    updateControllerViewPadding(controllerView, orientation, navigationBarHeightPx)
                    root.findViewById<View>(R.id.navigation_bar_bg).isVisible =
                        orientation != ORIENTATION_PORTRAIT

                    root.findViewById<ProgressBar>(R.id.loading_video_player_controller_view).isVisible =
                        playbackState <= STATE_BUFFERING

                    root.findViewById<View>(R.id.track_name).isVisible =
                        orientation == ORIENTATION_PORTRAIT
                }

                if (isControllerViewVisible && !uiState.isLocked) {
                    VideoPlayerTopBar(
                        modifier = Modifier.padding(
                            end = if (orientation == ORIENTATION_PORTRAIT)
                                0.dp
                            else
                                navigationBarHeight
                        ),
                        title = if (orientation == ORIENTATION_PORTRAIT) {
                            ""
                        } else {
                            uiState.metadata.title ?: uiState.metadata.nodeName
                        },
                        menuActions = uiState.menuActions,
                        onBackPressed = { backDispatcher?.onBackPressed() },
                        onMenuActionClicked = viewModel::updateClickedMenuAction,
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

                if (uiState.showPlaybackDialog) {
                    MegaAlertDialog(
                        title = stringResource(R.string.video_playback_position_dialog_title),
                        body = stringResource(
                            R.string.video_playback_position_dialog_message,
                            uiState.currentPlayingItemName ?: "",
                            formatSecondsToString(uiState.playbackPosition ?: 0)
                        ),
                        confirmButtonText = stringResource(R.string.video_playback_position_dialog_restart_button),
                        cancelButtonText = stringResource(R.string.video_playback_position_dialog_resume_button),
                        onConfirm = {
                            viewModel.updatePlaybackPositionStatus(PlaybackPositionStatus.Restart)
                        },
                        onCancel = {
                            viewModel.updatePlaybackPositionStatus(PlaybackPositionStatus.Resume)
                        },
                        onDismiss = {
                            viewModel.updatePlaybackPositionStatus(PlaybackPositionStatus.Initial)
                        },
                    )
                }

                AddSubtitlesDialog(
                    isShown = uiState.showSubtitleDialog,
                    selectOptionState = uiState.subtitleSelectedStatus.id,
                    matchedSubtitleFileUpdate = {
                        viewModel.getMatchedSubtitleFileInfo()
                    },
                    subtitleFileName = uiState.addedSubtitleInfo?.name,
                    onOffClicked = {
                        viewModel.updateSubtitleSelectedStatus(SubtitleSelectedStatus.Off)
                    },
                    onAddedSubtitleClicked = {
                        viewModel.updateSubtitleSelectedStatus(SubtitleSelectedStatus.AddSubtitleItem)
                    },
                    onAutoMatch = { info ->
                        if (info.url == null) {
                            viewModel.updateSnackBarMessage(
                                context.getString(R.string.media_player_video_message_adding_subtitle_failed)
                            )
                            return@AddSubtitlesDialog
                        }
                        Analytics.tracker.trackEvent(AutoMatchSubtitleOptionPressedEvent)
                        viewModel.updateSubtitleSelectedStatus(
                            SubtitleSelectedStatus.SelectMatchedItem,
                            info
                        )
                    },
                    onDismissRequest = {
                        viewModel.updateShowSubtitleDialog(false)
                    },
                    onToSelectSubtitle = {
                        Analytics.tracker.trackEvent(AddSubtitlesOptionPressedEvent)
                        viewModel.navigateToSelectSubtitle()
                    })
            }
        }
    }
}

@Composable
private fun getNavigationBarHeight(
    orientation: Int,
    density: Density,
    layoutDirection: LayoutDirection,
) = if (orientation == ORIENTATION_LANDSCAPE) {
    with(density) { WindowInsets.navigationBars.getRight(density, layoutDirection).toDp() }
} else {
    with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
}

private fun updateControllerViewPadding(controllerView: View, orientation: Int, padding: Int) {
    val layoutParams =
        controllerView.layoutParams as ViewGroup.MarginLayoutParams
    if (orientation == ORIENTATION_PORTRAIT) {
        controllerView.setPadding(0, 0, 0, padding)
        layoutParams.bottomMargin = 0
    } else {
        controllerView.setPadding(0, 0, 0, 0)
        layoutParams.marginEnd = padding
    }
    controllerView.layoutParams = layoutParams
}

@SuppressLint("DefaultLocale")
private fun formatSecondsToString(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000.0).roundToInt()
    val hours = totalSeconds / 3600
    val remSeconds = totalSeconds % 3600
    val minutes = remSeconds / 60
    val seconds = remSeconds % 60

    return if (hours >= 1) {
        String.format("%2d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}