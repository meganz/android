package mega.privacy.android.app.mediaplayer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import kotlin.math.pow

private const val BAR_LENGTH_RATIO = 0.24f
private const val BAR_WIDTH_RATIO = 0.12f
private const val BAR_START_RATIO = 0.42f
private const val ALPHA_DECAY_EXPONENT = 1.8f

/**
 * Media player loading indicator.
 *
 * Draws [barCount] (default 8) radially arranged rounded bars whose opacity cycles continuously,
 * producing a throbber effect identical to the platform spinner without rotating the composable
 * itself.
 */
@Composable
internal fun MediaPlayerLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    barCount: Int = 8,
    durationMillis: Int = 1000,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "media_player_loading")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = barCount.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
        ),
        label = "bar_progress",
    )

    Canvas(modifier = modifier.semantics { contentDescription = "Loading" }) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.minDimension / 2f
        val barLength = outerRadius * BAR_LENGTH_RATIO
        val barWidth = outerRadius * BAR_WIDTH_RATIO
        val barStart = outerRadius * BAR_START_RATIO

        for (i in 0 until barCount) {
            // Bar 0 sits at 12 o'clock; bars advance clockwise.
            val angleDeg = i * (360f / barCount) - 90f

            // How far behind the current lit position this bar is (0 = head, ≈barCount = just ahead).
            val distance = (progress - i + barCount) % barCount

            // Exponential decay: head → alpha 1.0, tail → alpha ~0.07.
            val alpha = (1f - distance / barCount)
                .pow(ALPHA_DECAY_EXPONENT)
                .coerceIn(0.07f, 1f)

            rotate(angleDeg, pivot = center) {
                drawRoundRect(
                    color = color,
                    alpha = alpha,
                    topLeft = Offset(
                        x = center.x - barWidth / 2f,
                        y = center.y - barStart - barLength,
                    ),
                    size = Size(barWidth, barLength),
                    cornerRadius = CornerRadius(barWidth / 2f),
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewMediaPlayerLoadingIndicator() {
    OriginalTheme(isDark = true) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(Color(21, 22, 22)),
            contentAlignment = Alignment.Center,
        ) {
            MediaPlayerLoadingIndicator(modifier = Modifier.size(48.dp))
        }
    }
}
