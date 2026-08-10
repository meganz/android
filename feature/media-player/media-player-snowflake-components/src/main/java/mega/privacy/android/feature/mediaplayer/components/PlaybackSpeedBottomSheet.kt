package mega.privacy.android.feature.mediaplayer.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR
import kotlin.math.abs
import kotlin.math.roundToInt


private const val PLAYBACK_MIN_SPEED = 0.5f
private const val PLAYBACK_MAX_SPEED = 2.0f
private const val PLAYBACK_SPEED_STEP = 0.05f
private const val PLAYBACK_TOTAL_STEPS = 30 // (2.0 - 0.5) / 0.05

private val PLAYBACK_PRESET_SPEEDS = listOf(1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

/**
 * Bottom sheet for selecting playback speed, shown when the speed indicator is tapped
 * in the audio player's music mode.
 *
 * The ruler spans 0.5× to 2.0× in increments of 0.05 (30 steps). Dragging the
 * red indicator snaps to the nearest tick on release. Five preset buttons offer
 * quick access to 1×, 1.25×, 1.5×, 1.75×, and 2×.
 *
 * @param currentSpeed The currently active playback speed.
 * @param onSpeedSelected Invoked with the new speed when the user commits a selection.
 * @param onDismiss Invoked when the sheet should be dismissed.
 * @param isDark Forces dark or light theme when non-null; falls back to the system theme when null.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSpeedBottomSheet(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
    isDark: Boolean? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // displaySpeed tracks the live drag position so the text updates in real-time while dragging.
    var displaySpeed by remember { mutableFloatStateOf(currentSpeed) }
    LaunchedEffect(currentSpeed) { displaySpeed = currentSpeed }
    val snappedDisplaySpeed = remember(displaySpeed) { snapToNearestTick(displaySpeed) }

    OriginalTheme(isDark = isDark ?: isSystemInDarkTheme()) {
        MegaModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
            ) {
                MegaText(
                    text = stringResource(sharedR.string.audio_player_playback_speed_title),
                    textColor = TextColor.Primary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )

                Spacer(modifier = Modifier.height(8.dp))

                MegaText(
                    text = formatPlaybackSpeed(snappedDisplaySpeed),
                    textColor = if (snappedDisplaySpeed == 1f) TextColor.Primary else TextColor.Brand,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                SpeedRuler(
                    currentSpeed = currentSpeed,
                    onSpeedChanged = onSpeedSelected,
                    onSpeedDragging = { displaySpeed = it },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PLAYBACK_PRESET_SPEEDS.forEach { speed ->
                        PresetSpeedButton(
                            speed = speed,
                            isSelected = abs(currentSpeed - speed) < 0.001f,
                            onClick = { onSpeedSelected(speed) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedRuler(
    currentSpeed: Float,
    onSpeedChanged: (Float) -> Unit,
    onSpeedDragging: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDragging by remember { mutableStateOf(false) }
    var rawDragSpeed by remember { mutableFloatStateOf(currentSpeed) }

    LaunchedEffect(currentSpeed) {
        if (!isDragging) rawDragSpeed = currentSpeed
    }

    // After the finger lifts the indicator snaps to the nearest tick. Animate that snap.
    // During drag rawDragSpeed is used directly so the indicator follows the finger with zero lag.
    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeed,
        animationSpec = spring(stiffness = 600f),
        label = "rulerSpeed",
    )
    val renderSpeed = if (isDragging) rawDragSpeed else animatedSpeed

    val textMeasurer = rememberTextMeasurer()
    val brandColor = DSTokens.colors.brand.default
    val secondaryColor = DSTokens.colors.text.secondary

    Canvas(
        modifier = modifier
            .height(64.dp)
            .pointerInput(Unit) {
                // Horizontal padding keeps the first/last tick inset from the canvas edge,
                // matching the label centering in drawSpeedRuler.
                val paddingPx = 12.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isDragging = true
                    rawDragSpeed = offsetToSpeed(down.position.x, size.width.toFloat(), paddingPx)
                    onSpeedDragging(rawDragSpeed)

                    while (true) {
                        val event = awaitPointerEvent()
                        if (!event.changes.any { it.pressed }) break
                        val x = event.changes
                            .maxByOrNull { it.id.value }
                            ?.position?.x
                            ?.coerceIn(0f, size.width.toFloat())
                            ?: break
                        rawDragSpeed = offsetToSpeed(x, size.width.toFloat(), paddingPx)
                        onSpeedDragging(rawDragSpeed)
                        event.changes.forEach { it.consume() }
                    }

                    isDragging = false
                    onSpeedChanged(snapToNearestTick(rawDragSpeed))
                }
            },
    ) {
        drawSpeedRuler(renderSpeed, textMeasurer, brandColor, secondaryColor)
    }
}

private fun DrawScope.drawSpeedRuler(
    currentSpeed: Float,
    textMeasurer: TextMeasurer,
    brandColor: Color,
    secondaryColor: Color,
) {
    val width = size.width

    // Horizontal padding: first/last tick inset so their labels can be centered without clipping.
    val horizontalPadding = 12.dp.toPx()
    val drawWidth = width - 2 * horizontalPadding

    // Tick sizes: every 0.1 step (even i) → 24 dp; every 0.05 non-0.1 (odd i) → 20 dp.
    val majorTickHeight = 24.dp.toPx()
    val minorTickHeight = 20.dp.toPx()
    val tickStrokeWidth = 1.5.dp.toPx()
    val dotRadius = 3.dp.toPx()
    val dotTickGap = 5.dp.toPx()
    val labelGap = 4.dp.toPx()

    // Layout (top → bottom): topPadding · dot · dotTickGap · ticks (top-aligned) · labelGap · labels
    val topPadding = 4.dp.toPx()
    val dotCenterY = topPadding + dotRadius
    val tickTopY = dotCenterY + dotRadius + dotTickGap

    val indicatorX = speedToOffset(currentSpeed, width, horizontalPadding)
    val snappedCurrent = snapToNearestTick(currentSpeed)

    // Red dot — drawn before ticks so ticks render in front
    drawCircle(
        color = brandColor,
        radius = dotRadius,
        center = Offset(indicatorX, dotCenterY),
    )

    // Ticks: all top-aligned at tickTopY, extending downward
    for (i in 0..PLAYBACK_TOTAL_STEPS) {
        val tickSpeed = PLAYBACK_MIN_SPEED + i * PLAYBACK_SPEED_STEP
        val x = horizontalPadding + i.toFloat() / PLAYBACK_TOTAL_STEPS * drawWidth

        val tickHeight = if (i % 2 == 0) majorTickHeight else minorTickHeight

        val isSelected = abs(tickSpeed - snappedCurrent) < 0.001f
        val tickColor = if (isSelected) brandColor else secondaryColor

        drawLine(
            color = tickColor,
            start = Offset(x, tickTopY),
            end = Offset(x, tickTopY + tickHeight),
            strokeWidth = tickStrokeWidth,
        )
    }

    // Labels at 0.5, 1, 1.5, 2 — drawn below major ticks, centered on the corresponding tick
    val labelTopY = tickTopY + majorTickHeight + labelGap
    val labelStyle = TextStyle(
        color = secondaryColor,
        fontSize = 12.sp,
    )
    val labelData = listOf(0 to "0.5", 10 to "1", 20 to "1.5", 30 to "2")
    for ((tickIndex, label) in labelData) {
        val x = horizontalPadding + tickIndex.toFloat() / PLAYBACK_TOTAL_STEPS * drawWidth
        val measured = textMeasurer.measure(label, labelStyle)
        val textX = (x - measured.size.width / 2f).coerceIn(0f, width - measured.size.width)
        drawText(measured, topLeft = Offset(textX, labelTopY))
    }
}

@Composable
private fun PresetSpeedButton(
    speed: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = DSTokens.colors.neutral.containerDefault,
        modifier = modifier.size(56.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            MegaText(
                text = formatPresetLabel(speed),
                textColor = if (isSelected) TextColor.Brand else TextColor.Primary,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
            )
        }
    }
}

private fun offsetToSpeed(x: Float, totalWidth: Float, horizontalPadding: Float): Float {
    val drawWidth = totalWidth - 2 * horizontalPadding
    val adjustedX = (x - horizontalPadding).coerceIn(0f, drawWidth)
    return (adjustedX / drawWidth * (PLAYBACK_MAX_SPEED - PLAYBACK_MIN_SPEED) + PLAYBACK_MIN_SPEED)
        .coerceIn(PLAYBACK_MIN_SPEED, PLAYBACK_MAX_SPEED)
}

private fun speedToOffset(speed: Float, totalWidth: Float, horizontalPadding: Float): Float {
    val drawWidth = totalWidth - 2 * horizontalPadding
    return horizontalPadding + (speed - PLAYBACK_MIN_SPEED) / (PLAYBACK_MAX_SPEED - PLAYBACK_MIN_SPEED) * drawWidth
}

internal fun snapToNearestTick(speed: Float): Float {
    val steps = ((speed - PLAYBACK_MIN_SPEED) / PLAYBACK_SPEED_STEP).roundToInt()
        .coerceIn(0, PLAYBACK_TOTAL_STEPS)
    // Round to 2 decimal places to eliminate floating-point accumulation errors
    return ((PLAYBACK_MIN_SPEED + steps * PLAYBACK_SPEED_STEP) * 100).roundToInt() / 100f
}

internal fun formatPlaybackSpeed(snappedSpeed: Float): String =
    if (snappedSpeed % 1f == 0f) "${snappedSpeed.toInt()}x" else "${snappedSpeed}x"

/** Label for the preset buttons — no "x" suffix. */
private fun formatPresetLabel(speed: Float): String {
    val rounded = snapToNearestTick(speed)
    return if (rounded % 1f == 0f) "${rounded.toInt()}" else "$rounded"
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1E1E)
@Composable
private fun PreviewPlaybackSpeedBottomSheet() {
    PlaybackSpeedBottomSheet(
        currentSpeed = 1.65f,
        isDark = true,
        onSpeedSelected = {},
        onDismiss = {},
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1E1E)
@Composable
private fun PreviewPlaybackSpeedBottomSheetNormal() {
    PlaybackSpeedBottomSheet(
        currentSpeed = 1f,
        isDark = true,
        onSpeedSelected = {},
        onDismiss = {},
    )
}
