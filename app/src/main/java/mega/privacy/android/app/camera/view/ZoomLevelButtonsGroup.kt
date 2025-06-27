package mega.privacy.android.app.camera.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.floor


@Composable
internal fun ZoomLevelButtonsGroup(
    availableZoomRange: ClosedFloatingPointRange<Float>,
    currentZoomRatio: Float,
    onZoomLevelSelected: (Float) -> Unit,
    modifier: Modifier = Modifier,
    rotationDegree: Float = 0f,
) {
    val zoomButtons = remember(availableZoomRange) {
        val minZoom = availableZoomRange.start
        val maxZoom = availableZoomRange.endInclusive
        if (minZoom == maxZoom)
            return@remember emptyList<Float>()
        val candidates = listOf(minZoom, 1.0f, 2.0f, 3.0f, maxZoom)
            .filter { it in availableZoomRange }
            .distinct()
            .sorted()
            .take(4)
        if (candidates.size < 4) {
            (candidates + List(4 - candidates.size) { maxZoom }).take(4)
        } else {
            candidates
        }
    }
    var tappedZoom by remember { mutableStateOf<Float?>(null) }

    if (zoomButtons.isNotEmpty()) {
        val highlightIndex = if (tappedZoom != null) {
            zoomButtons.indexOf(tappedZoom)
        } else {
            zoomButtons.indexOfLast { it <= currentZoomRatio }.coerceAtLeast(0)
        }

        // Reset highlight after a delay when tappedZoom changes
        LaunchedEffect(tappedZoom) {
            if (tappedZoom != null) {
                kotlinx.coroutines.delay(350)
                tappedZoom = null
            }
        }

        Row(
            modifier = modifier
                .background(Color.Black.copy(alpha = 0.35f), shape = RoundedCornerShape(32.dp))
                .padding(horizontal = 7.dp)
                .height(40.dp)
                .testTag(TEST_TAG_ZOOM_BUTTONS_ROW),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            zoomButtons.forEachIndexed { idx, buttonZoom ->
                val isClosest = idx == highlightIndex
                val nextButtonValue = zoomButtons.getOrNull(idx + 1)
                val displayValue = when {
                    tappedZoom != null && isClosest -> buttonZoom
                    isClosest && nextButtonValue != null && currentZoomRatio >= nextButtonValue -> (nextButtonValue - 0.1f).roundDown1Decimal()
                    isClosest -> currentZoomRatio.roundDown1Decimal()
                    else -> buttonZoom
                }
                val displayText = if (isClosest) {
                    if (displayValue == displayValue.toInt().toFloat()) "${displayValue.toInt()}x"
                    else String.format(Locale.US, "%.1fx", displayValue)
                } else {
                    if (buttonZoom == buttonZoom.toInt().toFloat()) buttonZoom.toInt().toString()
                    else String.format(Locale.US, "%.1f", buttonZoom)
                }
                val interactionSource = remember { MutableInteractionSource() }
                val pressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    when {
                        pressed -> 0.85f
                        isClosest -> 1.25f
                        else -> 1f
                    },
                    label = "zoomBtnScale"
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .background(
                            color = Color.Black.copy(alpha = if (isClosest) 0.8f else 0.5f),
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            tappedZoom = buttonZoom
                            onZoomLevelSelected(buttonZoom)
                        }
                        .testTag(TEST_TAG_ZOOM_BUTTON_PREFIX + idx),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayText,
                        color = if (isClosest) Color(0xFF05BAF1) else Color.White,
                        fontSize = if (isClosest) 9.sp else 10.sp,
                        fontWeight = if (isClosest) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.rotate(rotationDegree)
                    )
                }
            }
        }
    }
}

private fun Float.roundDown1Decimal(): Float = floor(this * 10) / 10f


internal const val TEST_TAG_ZOOM_BUTTONS_ROW = "zoom_buttons:row"
internal const val TEST_TAG_ZOOM_BUTTON_PREFIX = "zoom_buttons:button:"


@Preview(showBackground = true, name = "Default Zoom Buttons")
@Composable
fun PreviewZoomLevelButtons_Default() {
    ZoomLevelButtonsGroup(
        availableZoomRange = 0.6f..5.0f,
        currentZoomRatio = 1.0f,
        onZoomLevelSelected = {},
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(showBackground = true, name = "Zoomed Between 2x and 3x")
@Composable
fun PreviewZoomLevelButtons_Between() {
    ZoomLevelButtonsGroup(
        availableZoomRange = 0.5f..3.0f,
        currentZoomRatio = 2.7f,
        onZoomLevelSelected = {},
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(showBackground = true, name = "Zoomed at Max")
@Composable
fun PreviewZoomLevelButtons_Max() {
    ZoomLevelButtonsGroup(
        availableZoomRange = 0.5f..5.0f,
        currentZoomRatio = 5.0f,
        onZoomLevelSelected = {},
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(showBackground = true, name = "Rotated")
@Composable
fun PreviewZoomLevelButtons_Rotated() {
    ZoomLevelButtonsGroup(
        availableZoomRange = 0.5f..5.0f,
        currentZoomRatio = 5.0f,
        onZoomLevelSelected = {},
        rotationDegree = 90f,
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(showBackground = true, name = "Unavailable zoom")
@Composable
fun PreviewZoomLevelButtons_Unavailable() {
    ZoomLevelButtonsGroup(
        availableZoomRange = 1f..1f,
        currentZoomRatio = 5.0f,
        onZoomLevelSelected = {},
        modifier = Modifier.padding(16.dp)
    )
}