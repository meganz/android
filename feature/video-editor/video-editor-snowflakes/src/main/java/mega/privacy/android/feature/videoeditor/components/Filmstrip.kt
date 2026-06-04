package mega.privacy.android.feature.videoeditor.components

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.tokens.theme.DSTokens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

private const val THUMB_COUNT = 10

/**
 * Trim filmstrip: thumbnail strip + draggable in/out handles + playhead. Tap or
 * drag on any non-handle part seeks the playhead inside the trim window.
 */
@Composable
fun Filmstrip(
    sourceUri: Uri?,
    durationMs: Long,
    trimStartMs: Long,
    trimEndMs: Long,
    playheadMs: Long,
    onTrimChange: (Long, Long) -> Unit,
    modifier: Modifier = Modifier,
    onSeek: (Long) -> Unit = {},
) {
    var widthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val onTrimState = rememberUpdatedState(onTrimChange)
    val onSeekState = rememberUpdatedState(onSeek)

    val safeDuration = durationMs.coerceAtLeast(1L)
    val startFrac = trimStartMs.toFloat() / safeDuration
    val endFrac = trimEndMs.toFloat() / safeDuration
    val playFrac = playheadMs.toFloat() / safeDuration

    val startFracState = rememberUpdatedState(startFrac)
    val endFracState = rememberUpdatedState(endFrac)
    val trimStartMsState = rememberUpdatedState(trimStartMs)
    val trimEndMsState = rememberUpdatedState(trimEndMs)

    val thumbnails = rememberFilmstripThumbnails(sourceUri, durationMs)
    val brand = DSTokens.colors.brand.default
    val handleWidthDp = 14.dp
    val handleHalf = handleWidthDp / 2

    BoxSurface(
        surfaceColor = SurfaceColor.Surface2,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .onSizeChanged { widthPx = it.width }
            .systemGestureExclusion()
            .pointerInput(widthPx, safeDuration) {
                detectTapGestures(onTap = { offset ->
                    if (widthPx > 0) {
                        val frac = (offset.x / widthPx).coerceIn(0f, 1f)
                        val ms = (frac * safeDuration).toLong()
                        onSeekState.value(ms.coerceIn(trimStartMsState.value, trimEndMsState.value))
                    }
                })
            }
            .pointerInput(widthPx, safeDuration) {
                detectDragGestures(onDrag = { change, _ ->
                    if (widthPx > 0) {
                        val frac = (change.position.x / widthPx).coerceIn(0f, 1f)
                        val ms = (frac * safeDuration).toLong()
                        onSeekState.value(ms.coerceIn(trimStartMsState.value, trimEndMsState.value))
                        change.consume()
                    }
                })
            },
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            repeat(THUMB_COUNT) { i ->
                val bm = thumbnails.getOrNull(i)
                if (bm != null) {
                    Image(
                        bitmap = bm.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(placeholderColor(i)),
                    )
                }
            }
        }

        if (widthPx > 0) {
            val widthDp = with(density) { widthPx.toDp() }
            val startDp = widthDp * startFrac
            val endDp = widthDp * endFrac
            val playDp = widthDp * playFrac

            Box(
                modifier = Modifier
                    .width(startDp.coerceAtLeast(0.dp))
                    .fillMaxHeight()
                    .background(Color.Black.copy(alpha = 0.6f)),
            )
            Box(
                modifier = Modifier
                    .offset(x = endDp)
                    .width((widthDp - endDp).coerceAtLeast(0.dp))
                    .fillMaxHeight()
                    .background(Color.Black.copy(alpha = 0.6f)),
            )

            Box(
                modifier = Modifier
                    .offset(x = startDp)
                    .width((endDp - startDp).coerceAtLeast(0.dp))
                    .height(3.dp)
                    .background(brand),
            )
            Box(
                modifier = Modifier
                    .offset(x = startDp, y = 53.dp)
                    .width((endDp - startDp).coerceAtLeast(0.dp))
                    .height(3.dp)
                    .background(brand),
            )

            TrimHandle(
                xOffset = (startDp - handleHalf).coerceAtLeast(-handleHalf),
                widthDp = handleWidthDp,
                brand = brand,
                onDrag = { dx ->
                    val newStartFrac = (startFracState.value + dx / widthPx)
                        .coerceIn(0f, endFracState.value - 0.02f)
                    onTrimState.value((newStartFrac * safeDuration).toLong(), trimEndMsState.value)
                },
            )
            TrimHandle(
                xOffset = endDp - handleHalf,
                widthDp = handleWidthDp,
                brand = brand,
                onDrag = { dx ->
                    val newEndFrac = (endFracState.value + dx / widthPx)
                        .coerceIn(startFracState.value + 0.02f, 1f)
                    onTrimState.value(trimStartMsState.value, (newEndFrac * safeDuration).toLong())
                },
            )

            Box(
                modifier = Modifier
                    .offset(x = playDp - 1.dp)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color.White),
            )
        }
    }
}

@Composable
private fun TrimHandle(
    xOffset: Dp,
    widthDp: Dp,
    brand: Color,
    onDrag: (Float) -> Unit,
) {
    val onDragState = rememberUpdatedState(onDrag)
    Box(
        modifier = Modifier
            .offset(x = xOffset)
            .width(widthDp)
            .fillMaxHeight()
            .systemGestureExclusion()
            .background(brand, RoundedCornerShape(4.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val dx = change.position.x - change.previousPosition.x
                        if (dx != 0f) onDragState.value(dx)
                        change.consume()
                    }
                }
            },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 4.dp)
                .width(2.dp)
                .height(18.dp)
                .background(Color.White, RoundedCornerShape(1.dp)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-4).dp)
                .width(2.dp)
                .height(18.dp)
                .background(Color.White, RoundedCornerShape(1.dp)),
        )
    }
}

@Composable
private fun rememberFilmstripThumbnails(uri: Uri?, durationMs: Long): List<Bitmap?> {
    val context = LocalContext.current
    val thumbs = remember(uri, durationMs) {
        mutableStateListOf<Bitmap?>().apply { repeat(THUMB_COUNT) { add(null) } }
    }
    LaunchedEffect(uri, durationMs) {
        if (uri == null || durationMs <= 0L) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                for (i in 0 until THUMB_COUNT) {
                    ensureActive()
                    val timeUs = (durationMs * 1000L * i / THUMB_COUNT).coerceAtLeast(0L)
                    val bm = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                            retriever.getScaledFrameAtTime(
                                timeUs,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                                THUMB_TARGET_PX,
                                THUMB_TARGET_PX,
                            )
                        } else {
                            retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        }
                    } catch (_: Throwable) {
                        null
                    }
                    if (bm != null) thumbs[i] = bm
                }
            } catch (c: CancellationException) {
                throw c
            } catch (_: Throwable) {
                // Leave remaining slots null — UI shows placeholder colours.
            } finally {
                runCatching { retriever.release() }
            }
        }
    }
    return thumbs
}

private const val THUMB_TARGET_PX = 240

private fun placeholderColor(index: Int): Color {
    val palette = listOf(
        Color(0xFF2A1340), Color(0xFF3B1D58), Color(0xFF5B2A6D),
        Color(0xFF8A3F73), Color(0xFFB14E73), Color(0xFFD16968),
        Color(0xFFE38866), Color(0xFFE0A267), Color(0xFFC78C5A), Color(0xFF6B4A48),
    )
    return palette[index.coerceIn(0, palette.lastIndex)]
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun FilmstripPreview() {
    AndroidThemeForPreviews {
        Filmstrip(
            sourceUri = null,
            durationMs = 32_000L,
            trimStartMs = 6_000L,
            trimEndMs = 26_000L,
            playheadMs = 14_000L,
            onTrimChange = { _, _ -> },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp),
        )
    }
}
