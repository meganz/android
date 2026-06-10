package mega.privacy.android.feature.videoeditor.presentation.editor.render

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import mega.privacy.android.feature.videoeditor.components.FreeFormCropOverlay
import mega.privacy.android.feature.videoeditor.presentation.editor.engine.ToolRegistry
import mega.privacy.android.feature.videoeditor.presentation.editor.engine.buildMediaItem
import mega.privacy.android.feature.videoeditor.presentation.editor.engine.createPreviewPlayer
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorAction
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorState
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.BuiltInToolIds
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.crop.CropAction
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.crop.CropPreset
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

/**
 * Main preview composable. Hosts the [androidx.media3.exoplayer.ExoPlayer]
 * via a Compose-native [PlayerSurface] (texture-view variant), syncs editor
 * state ↔ player, and renders the Compose tree with crop / rotate / flip /
 * scale transforms.
 *
 * Built-in transforms (Crop, Rotate, Flip) are applied via Compose rather than
 * Media3 player effects, keeping the TextureView surface stable across tool
 * transitions. Each active tool can draw its own [PreviewOverlay] above the
 * surface.
 *
 * Design notes on layer stability — the [PlayerSurface]'s modifier chain is
 * kept constant across the Crop-tool ↔ cropped-preview transition (the
 * conditional crop clip is implemented as a [Modifier.drawWithContent] +
 * `clipRect` rather than a `Modifier.clip(shape)` that would add/remove a
 * graphicsLayer); the surface itself never resizes. Both avoid TextureView
 * surface re-attach artefacts on a long high-bitrate source.
 */
@UnstableApi
@Composable
fun EditorPreview(
    state: EditorState,
    registry: ToolRegistry,
    onAction: (EditorAction) -> Unit,
    onBufferingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val player = remember(context) { createPreviewPlayer(context) }

    val onActionState = rememberUpdatedState(onAction)
    val onBufferingState = rememberUpdatedState(onBufferingChange)
    // Capture the source's natural size only once per video. Media3 reports
    // post-effect dims on later onVideoSizeChanged callbacks; we don't want
    // those overwriting the original.
    val sourceSizeCaptured = remember(state.source.uri) { mutableStateOf(false) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                onBufferingState.value(playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_ENDED) {
                    onActionState.value(EditorAction.SetPlaying(false))
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (sourceSizeCaptured.value) return
                if (videoSize.width <= 0 || videoSize.height <= 0) return
                // VideoSize.width/height in Media3 ≥1.x are already
                // post-rotation; the old `unappliedRotationDegrees` field
                // is deprecated and effectively always 0. Pixel ratio still
                // needs to be folded in for non-square-pixel sources.
                val effectiveWidth = (videoSize.width * videoSize.pixelWidthHeightRatio).toInt()
                if (effectiveWidth <= 0) return
                onActionState.value(
                    EditorAction.SourceSizeChanged(effectiveWidth, videoSize.height),
                )
                sourceSizeCaptured.value = true
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // (Re)prepare the player when the source loads or the trim changes. Trim
    // updates stream in on every handle-drag tick, and each prepare tears down
    // the decoder and re-buffers the clipping window — so trim changes are
    // debounced via collectLatest until the drag settles. The first emission
    // (source load) prepares immediately.
    val stateLive = rememberUpdatedState(state)
    LaunchedEffect(state.source.uri, state.source.durationMs) {
        if (state.source.uri == null || state.source.durationMs <= 0L) return@LaunchedEffect
        var prepared = false
        snapshotFlow { stateLive.value.trim }
            .distinctUntilChanged()
            .collectLatest {
                if (prepared) delay(TRIM_REPREPARE_DEBOUNCE_MS)
                player.setMediaItem(buildMediaItem(stateLive.value))
                player.prepare()
                prepared = true
            }
    }

    // Speed is applied via setPlaybackSpeed, NOT via SpeedChangeEffect — running
    // both would double-apply (the effect remaps timestamps, the player then
    // consumes those frames N× as fast on top), choking the effect pipeline at
    // 2×+. Export still composes the full effect list including SpeedChangeEffect.
    LaunchedEffect(state.speed) { player.setPlaybackSpeed(state.speed.speed) }

    LaunchedEffect(state.volume) {
        player.volume = state.volume.volume.coerceIn(MIN_PREVIEW_VOLUME, MAX_PREVIEW_VOLUME)
    }

    LaunchedEffect(state.playback.isPlaying) {
        if (state.playback.isPlaying && player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0L)
        }
        player.playWhenReady = state.playback.isPlaying
    }

    LaunchedEffect(state.playback.playheadMs, state.activeTool) {
        val targetInClip = state.playback.playheadMs - state.trim.startMs
        if (abs(player.currentPosition - targetInClip) > SEEK_CORRECTION_THRESHOLD_MS) {
            player.seekTo(targetInClip.coerceAtLeast(0L))
        }
    }

    // Trim values read through rememberUpdatedState so a trim edit made WHILE
    // playing affects the next polling iteration. Re-keying the effect on
    // trim itself would tear down + restart the loop on every drag tick,
    // wasteful — the loop runs forever as long as isPlaying.
    val trimStartLive = rememberUpdatedState(state.trim.startMs)
    val trimEndLive = rememberUpdatedState(state.trim.endMs)
    LaunchedEffect(state.playback.isPlaying, state.source.uri) {
        while (state.playback.isPlaying) {
            val positionMs = player.currentPosition + trimStartLive.value
            onActionState.value(
                EditorAction.SetPlayhead(
                    positionMs.coerceIn(trimStartLive.value, trimEndLive.value),
                ),
            )
            delay(PLAYHEAD_POLL_INTERVAL_MS)
        }
    }

    val sourceAspectRatio = if (state.source.aspectRatio > 0f) state.source.aspectRatio else 1f
    val cropRect = state.crop.rect
    val cropRectWidth = cropRect.right - cropRect.left
    val cropRectHeight = cropRect.bottom - cropRect.top
    val isCropTool = state.activeTool == BuiltInToolIds.Crop
    val showCroppedPreview = !isCropTool &&
            (cropRectWidth < FULL_FRAME_MIN_FRACTION || cropRectHeight < FULL_FRAME_MIN_FRACTION)

    // Continuous rotation, animated. ViewModel keeps the running total (no mod 360)
    // so animateFloatAsState interpolates in the same direction across wraps.
    val animatedRotation by animateFloatAsState(
        targetValue = state.rotate.degrees.toFloat(),
        animationSpec = tween(
            durationMillis = TRANSFORM_ANIMATION_DURATION_MS,
            easing = FastOutSlowInEasing,
        ),
        label = "rotation",
    )
    // Animate flip too — scaleX interpolates from ±1 to ∓1, passing through 0
    // (zero-width), which reads as a card-flip / fold.
    val animatedFlipSign by animateFloatAsState(
        targetValue = if (state.rotate.flipHorizontal) -1f else 1f,
        animationSpec = tween(
            durationMillis = TRANSFORM_ANIMATION_DURATION_MS,
            easing = FastOutSlowInEasing,
        ),
        label = "flip",
    )

    val showCroppedRef = rememberUpdatedState(showCroppedPreview)
    val cropRectRef = rememberUpdatedState(cropRect)

    // Crop-tool scratch state: pan/zoom of the raw source while framing the
    // crop. Local to this composable so it doesn't survive tool exit; the crop
    // overlay reads/writes it through callbacks, and the inner graphicsLayer
    // below reads it while the Crop tool is active.
    var videoPan by remember(state.source.uri) { mutableStateOf(Offset.Zero) }
    var videoScale by remember(state.source.uri) { mutableFloatStateOf(1f) }

    BoxWithConstraints(
        modifier = modifier
            .background(Color.Black)
            .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        val canvasWidthPx = constraints.maxWidth.toFloat()
        val canvasHeightPx = constraints.maxHeight.toFloat()
        val geometry = computePreviewGeometry(
            canvasWidthPx, canvasHeightPx, sourceAspectRatio, showCroppedPreview, cropRect,
        )
        val previewSize = IntSize(geometry.boxW.toInt(), geometry.boxH.toInt())

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val outerScale = computeOuterScale(
                        geometry, canvasWidthPx, canvasHeightPx, animatedRotation,
                    )
                    scaleX = animatedFlipSign * outerScale
                    scaleY = outerScale
                    rotationZ = animatedRotation
                },
            contentAlignment = Alignment.Center,
        ) {
            // Wrapper Box: aspectRatio sizing + draw-time clipRect for the
            // cropped slice. Stable modifier chain across tool transitions.
            Box(
                modifier = Modifier
                    .aspectRatio(sourceAspectRatio, matchHeightConstraintsFirst = false)
                    .drawWithContent {
                        if (showCroppedRef.value) {
                            val width = size.width
                            val height = size.height
                            val rect = cropRectRef.value
                            val croppedWidth = (rect.right - rect.left) * width
                            val croppedHeight = (rect.bottom - rect.top) * height
                            val offsetX = (width - croppedWidth) / 2f
                            val offsetY = (height - croppedHeight) / 2f
                            clipRect(
                                offsetX,
                                offsetY,
                                offsetX + croppedWidth,
                                offsetY + croppedHeight,
                            ) {
                                this@drawWithContent.drawContent()
                            }
                        } else {
                            drawContent()
                        }
                    },
            ) {
                PlayerSurface(
                    player = player,
                    surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (showCroppedRef.value) {
                                // Translate cropRect's centre to surface centre.
                                val width = size.width
                                val height = size.height
                                val rect = cropRectRef.value
                                val cropCenterX = ((rect.left + rect.right) / 2f) * width
                                val cropCenterY = ((rect.top + rect.bottom) / 2f) * height
                                translationX = width / 2f - cropCenterX
                                translationY = height / 2f - cropCenterY
                            } else if (isCropTool) {
                                // Crop tool free pan/zoom on the raw source.
                                translationX = videoPan.x
                                translationY = videoPan.y
                                scaleX = videoScale
                                scaleY = videoScale
                            }
                        },
                )
            }

            // Crop tool's overlay needs read+write access to videoPan/Scale,
            // which lives in this composable — so it's rendered here directly
            // rather than via tool.PreviewOverlay.
            if (isCropTool && state.source.widthPx > 0 && state.source.heightPx > 0 &&
                state.crop.freeForm
            ) {
                val rawSourceAspectRatio =
                    state.source.widthPx.toFloat() / state.source.heightPx.toFloat()
                val effectiveAspectLock = state.crop.aspectLock
                    ?: rawSourceAspectRatio.takeIf { state.crop.selectedPreset == CropPreset.ORIGINAL }
                val normalizedAspectLock = effectiveAspectLock?.let { it / rawSourceAspectRatio }
                FreeFormCropOverlay(
                    cropRect = state.crop.rect,
                    sourceWidth = state.source.widthPx,
                    sourceHeight = state.source.heightPx,
                    videoPan = videoPan,
                    videoScale = videoScale,
                    aspectLock = normalizedAspectLock,
                    presetKey = state.crop.selectedPreset,
                    onCornerResize = { onAction(EditorAction.DispatchTool(CropAction.SetRect(it))) },
                    onPanPinch = { newPan, newScale, newCropRect ->
                        videoPan = newPan
                        videoScale = newScale
                        onAction(EditorAction.DispatchTool(CropAction.SetRect(newCropRect)))
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Other tools inject their overlay via PreviewOverlay (Crop is
            // handled inline above).
            state.activeTool?.let { toolId ->
                if (toolId != BuiltInToolIds.Crop) {
                    registry[toolId]?.PreviewOverlay(
                        state = state,
                        onAction = { action -> onAction(EditorAction.DispatchTool(action)) },
                        previewSize = previewSize,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

/**
 * A crop dimension at or above this fraction of the full frame is treated as
 * uncropped — guards against float dust leaving a hair-thin clip on a rect the
 * user dragged back to the edges.
 */
private const val FULL_FRAME_MIN_FRACTION = 0.999f

/** Duration of the rotate / flip transform animations. */
private const val TRANSFORM_ANIMATION_DURATION_MS = 300

/**
 * Only re-seek the player when it has drifted further than this from the
 * requested playhead — avoids fighting normal playback advance with seeks.
 */
private const val SEEK_CORRECTION_THRESHOLD_MS = 250L

/** Poll interval for pushing the player position back into editor state while playing. */
private const val PLAYHEAD_POLL_INTERVAL_MS = 80L

/**
 * Quiet period after the last trim change before the clipped media item is
 * rebuilt and re-prepared — long enough to coalesce a handle drag, short enough
 * that the preview frame updates promptly once the drag settles.
 */
private const val TRIM_REPREPARE_DEBOUNCE_MS = 200L

/**
 * Preview-volume clamp. `ExoPlayer.volume` can't amplify, so the preview is
 * capped at unity even when the export volume is set higher.
 */
private const val MIN_PREVIEW_VOLUME = 0f
private const val MAX_PREVIEW_VOLUME = 1f
