package mega.privacy.android.feature.mediaplayer.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.controls.images.MegaIcon
import kotlin.math.roundToInt

enum class GestureSliderType { Brightness, Volume }

internal const val SLIDER_CONTENT_DESC_BRIGHTNESS = "Brightness"
internal const val SLIDER_CONTENT_DESC_VOLUME_MUTED = "Volume muted"
internal const val SLIDER_CONTENT_DESC_VOLUME_LOW = "Volume low"
internal const val SLIDER_CONTENT_DESC_VOLUME_MEDIUM = "Volume medium"
internal const val SLIDER_CONTENT_DESC_VOLUME_HIGH = "Volume high"

@Composable
fun VideoPlayerGestureSlider(
    value: Float,
    type: GestureSliderType,
    modifier: Modifier = Modifier,
) {
    val (icon, iconContentDescription) = when (type) {
        GestureSliderType.Brightness ->
            IconPack.Medium.Thin.Outline.Sun to SLIDER_CONTENT_DESC_BRIGHTNESS

        GestureSliderType.Volume -> when {
            value == 0f -> IconPack.Medium.Thin.Outline.VolumeX to SLIDER_CONTENT_DESC_VOLUME_MUTED
            value < 0.2f -> IconPack.Medium.Thin.Outline.Volume to SLIDER_CONTENT_DESC_VOLUME_LOW
            value < 0.5f -> IconPack.Medium.Thin.Outline.VolumeMin to SLIDER_CONTENT_DESC_VOLUME_MEDIUM
            else -> IconPack.Medium.Thin.Outline.VolumeMax to SLIDER_CONTENT_DESC_VOLUME_HIGH
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val percentText = "${(value * 100).roundToInt()}%"
        MegaText(
            text = percentText,
            textColor = TextColor.InverseAccent,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(DSTokens.colors.background.inverse, RoundedCornerShape(12.dp))
                .width(48.dp)
                .padding(vertical = 4.dp),
        )

        val trackBackground = DSTokens.colors.background.surfaceTransparent
        val trackFill = DSTokens.colors.components.selectionControl
        Canvas(
            modifier = Modifier
                .size(width = 48.dp, height = 200.dp)
                .semantics { contentDescription = "$iconContentDescription $percentText" }
        ) {
            val radius = CornerRadius(x = 10.dp.toPx(), y = 10.dp.toPx())
            drawRoundRect(color = trackBackground, cornerRadius = radius)
            val filledHeight = size.height * value
            if (filledHeight > 0f) {
                clipRect(top = size.height - filledHeight) {
                    drawRoundRect(color = trackFill, cornerRadius = radius)
                }
            }
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .background(DSTokens.colors.button.primary, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            MegaIcon(
                painter = rememberVectorPainter(icon),
                contentDescription = iconContentDescription,
                tint = IconColor.InverseAccent,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun VideoPlayerGestureSliderBrightnessPreview() {
    VideoPlayerGestureSlider(value = 0.6f, type = GestureSliderType.Brightness)
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun VideoPlayerGestureSliderVolumePreview() {
    VideoPlayerGestureSlider(value = 0.4f, type = GestureSliderType.Volume)
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun VideoPlayerGestureSliderVolumeMutedPreview() {
    VideoPlayerGestureSlider(value = 0f, type = GestureSliderType.Volume)
}
