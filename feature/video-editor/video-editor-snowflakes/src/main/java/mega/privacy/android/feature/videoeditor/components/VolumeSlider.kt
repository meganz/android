package mega.privacy.android.feature.videoeditor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.tokens.theme.DSTokens

/** Gain at which the unity tick is drawn (100%) on the 0..[MAX_VOLUME] track. */
private const val UNITY_VOLUME = 1f

/** Maximum gain the slider can reach (200% → 2× amplification at export). */
const val MAX_VOLUME = 2f

/**
 * Volume slider spanning `0..[MAX_VOLUME]` with a tick at unity gain. Reports
 * the new gain (in the same 0..[MAX_VOLUME] range) via [onValueChange].
 */
@Composable
fun VolumeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var widthPx by remember { mutableIntStateOf(0) }
    val onChangeState = rememberUpdatedState(onValueChange)
    val fraction = (value / MAX_VOLUME).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(40.dp)
            .onSizeChanged { widthPx = it.width }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { offset ->
                    if (widthPx > 0) {
                        onChangeState.value((offset.x / widthPx).coerceIn(0f, 1f) * MAX_VOLUME)
                    }
                })
            }
            .pointerInput(Unit) {
                detectDragGestures(onDrag = { change, _ ->
                    if (widthPx > 0) {
                        onChangeState.value((change.position.x / widthPx).coerceIn(0f, 1f) * MAX_VOLUME)
                        change.consume()
                    }
                })
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DSTokens.colors.brand.default.copy(alpha = 0.2f)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DSTokens.colors.brand.default),
        )
        val density = LocalDensity.current
        // Tick at unity gain — white reads against both the brand fill and the
        // faint brand track.
        val unityOffset = with(density) { (widthPx * (UNITY_VOLUME / MAX_VOLUME)).toDp() - 1.dp }
        Box(
            modifier = Modifier
                .padding(start = unityOffset.coerceAtLeast(0.dp))
                .width(2.dp)
                .height(20.dp)
                .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(1.dp)),
        )
        val handleOffset = with(density) { (widthPx * fraction).toDp() - 2.dp }
        Box(
            modifier = Modifier
                .padding(start = handleOffset.coerceAtLeast(0.dp))
                .width(4.dp)
                .height(44.dp)
                .background(DSTokens.colors.brand.default, RoundedCornerShape(2.dp)),
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun VolumeSliderPreview() {
    AndroidThemeForPreviews {
        VolumeSlider(value = 1.5f, onValueChange = {})
    }
}
