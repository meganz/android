package mega.privacy.android.feature.videoeditor.presentation.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Play/pause button + scrub bar overlay shown over the video. Decoupled from
 * editor state — the caller passes the playback position and trim window.
 *
 * These are media-overlay controls drawn on the (always dark) video frame, so
 * they use fixed light colours rather than theme tokens. `isBuffering` is
 * debounced so brief seek flickers don't replace the icon with a spinner; pass
 * `hideScrub = true` from tools (Trim) that own the timeline.
 */
@Composable
fun PreviewControls(
    isPlaying: Boolean,
    playheadMs: Long,
    trimStartMs: Long,
    trimEndMs: Long,
    hideScrub: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isBuffering: Boolean = false,
) {
    val durationMs = (trimEndMs - trimStartMs).coerceAtLeast(1L)
    val relMs = (playheadMs - trimStartMs).coerceIn(0L, durationMs)
    val durationInSecondsTextMapper = remember { DurationInSecondsTextMapper() }

    var showSpinner by remember { mutableStateOf(false) }
    LaunchedEffect(isBuffering) {
        if (isBuffering) {
            delay(BUFFERING_DEBOUNCE_MS)
            showSpinner = true
        } else {
            showSpinner = false
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                // On-video scrim — a translucent black circle, not a theme surface.
                .background(Color.Black.copy(alpha = 0.55f))
                .then(if (showSpinner) Modifier else Modifier.clickable(onClick = onPlayPause)),
            contentAlignment = Alignment.Center,
        ) {
            if (showSpinner) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                MegaIcon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    tint = IconColor.OnColor,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (!hideScrub) {
            MegaText(
                text = durationInSecondsTextMapper(relMs.milliseconds),
                style = AppTheme.typography.labelMedium,
                textColor = TextColor.OnColor,
            )
            ScrubBar(
                progress = relMs.toFloat() / durationMs,
                onSeek = { p -> onSeek(trimStartMs + (p * durationMs).toLong()) },
                modifier = Modifier.weight(1f),
            )
            MegaText(
                text = durationInSecondsTextMapper(durationMs.milliseconds),
                style = AppTheme.typography.labelMedium,
                textColor = TextColor.OnColor,
            )
        }
    }
}

@Composable
private fun ScrubBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var widthPx by remember { mutableStateOf(0) }
    val onSeekState = rememberUpdatedState(onSeek)
    Box(
        modifier = modifier
            .height(20.dp)
            .onSizeChanged { widthPx = it.width }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { offset ->
                    if (widthPx > 0) onSeekState.value((offset.x / widthPx).coerceIn(0f, 1f))
                })
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, _ ->
                        if (widthPx > 0) {
                            onSeekState.value((change.position.x / widthPx).coerceIn(0f, 1f))
                            change.consume()
                        }
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        // On-video scrub bar — fixed light colours against the video frame.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Color.White.copy(alpha = 0.25f)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(3.dp)
                .background(Color.White),
        )
        val handleOffsetDp = with(LocalDensity.current) {
            (widthPx * progress.coerceIn(0f, 1f)).toDp() - 7.dp
        }
        Box(
            modifier = Modifier
                .padding(start = handleOffsetDp.coerceAtLeast(0.dp))
                .size(14.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

private const val BUFFERING_DEBOUNCE_MS = 250L
