package mega.privacy.android.feature.documentscanner.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * Bottom-left deck of captured page thumbnails, fanned out like a hand of cards.
 * The newest page is on top; a count badge shows the total.
 *
 * Only the most recent thumbnails are fanned, so the caller need only pass a small
 * recent window rather than every captured page. [count] is the running total shown
 * on the badge, which may exceed the number of thumbnails in [pages].
 *
 * @param pages Most recent captured page thumbnails, oldest first.
 * @param count Total number of pages captured so far.
 * @param modifier Modifier for the deck.
 */
@Composable
fun CapturedPagesDeck(
    pages: List<ImageBitmap>,
    count: Int,
    modifier: Modifier = Modifier,
) {
    if (pages.isEmpty()) return

    val fanned = pages.takeLast(MAX_FANNED)
    Box(modifier = modifier.size(96.dp), contentAlignment = Alignment.BottomStart) {
        fanned.forEachIndexed { index, page ->
            // Oldest fanned card leans most; the top (newest) card sits upright.
            val depth = fanned.lastIndex - index
            Image(
                bitmap = page,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .rotate(-FAN_DEGREES * depth)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, DSTokens.colors.icon.onColor, RoundedCornerShape(6.dp)),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(24.dp)
                .clip(CircleShape)
                .background(DSTokens.colors.background.blur),
            contentAlignment = Alignment.Center,
        ) {
            MegaText(
                text = "×$count",
                textColor = TextColor.OnColor,
                style = AppTheme.typography.labelSmall,
                modifier = Modifier.padding(1.dp),
            )
        }
    }
}

private const val MAX_FANNED = 4
private const val FAN_DEGREES = 6f
