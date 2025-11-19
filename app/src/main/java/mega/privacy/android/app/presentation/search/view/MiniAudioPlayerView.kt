package mega.privacy.android.app.presentation.search.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.MiniAudioPlayerBinding
import mega.privacy.android.app.mediaplayer.miniplayer.MiniAudioPlayerController


/**
 * Mini audio player view
 *
 * @param modifier Modifier
 * @param lifecycle Lifecycle
 */
@Composable
fun MiniAudioPlayerView(modifier: Modifier, lifecycle: Lifecycle) {
    var audioPlayerInitialized by remember {
        mutableStateOf(false)
    }
    var audioPlayerHeight by remember {
        mutableIntStateOf(0)
    }
    val scope = rememberCoroutineScope()
    AndroidViewBinding(
        modifier = modifier.height(audioPlayerHeight.dp),
        factory = MiniAudioPlayerBinding::inflate,
    ) {
        if (audioPlayerInitialized.not()) {
            val audioPlayerController =
                MiniAudioPlayerController(playerView = this.miniAudioPlayer).apply {
                    shouldVisible = true
                }
            lifecycle.addObserver(audioPlayerController)
            scope.launch {
                audioPlayerController.audioBinding.collectLatest {
                    audioPlayerHeight = if (it) 64 else 0
                }
            }
        }
        audioPlayerInitialized = true
    }
}

/**
 * Mini audio player with slide in/out animation.
 *
 * Note: We do NOT use AnimatedVisibility here.
 * PlayerView requires a stable, non-zero height for correct measurement,
 * but AnimatedVisibility removes its child (height = 0) when invisible.
 * This breaks PlayerView layout and prevents the controller from initializing.
 *
 * Instead we keep a fixed-height container and animate its vertical offset,
 * collapsing the height only after the slide-out animation completes.
 */
@Composable
fun MiniAudioPlayerView(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
    val miniPlayerHeight = dimensionResource(R.dimen.audio_player_mini_controller_height)

    var isVisible by remember { mutableStateOf(false) }
    var isFullyHidden by remember { mutableStateOf(true) }

    // Slide: 0.dp when visible, miniPlayerHeight when hidden (moves down)
    val animatedOffset by animateDpAsState(
        targetValue = if (isVisible) 0.dp else miniPlayerHeight,
        label = "miniPlayerOffset",
        finishedListener = { endValue ->
            if (!isVisible && endValue == miniPlayerHeight) {
                isFullyHidden = true
            }
        },
    )

    // Height is full when visible OR animating, 0 when completely hidden
    val containerHeight = if (isFullyHidden) 0.dp else miniPlayerHeight

    var audioPlayerController by remember {
        mutableStateOf<MiniAudioPlayerController?>(null)
    }

    Box(
        modifier = modifier
            .height(containerHeight)
            .clipToBounds(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AndroidViewBinding(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = animatedOffset),
            factory = MiniAudioPlayerBinding::inflate,
        ) {
            if (audioPlayerController == null) {
                audioPlayerController =
                    MiniAudioPlayerController(playerView = miniAudioPlayer).apply {
                        shouldVisible = true
                    }
            }
        }
    }

    DisposableEffect(lifecycleOwner, audioPlayerController) {
        audioPlayerController?.let {
            lifecycleOwner.lifecycle.addObserver(it)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(it)
            }
        } ?: onDispose { }
    }

    LaunchedEffect(audioPlayerController) {
        audioPlayerController?.audioBinding?.collectLatest { visible ->
            if (visible) {
                isFullyHidden = false
            }
            isVisible = visible
        }
    }
}
