package mega.privacy.android.feature.documentscanner.components

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
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
 * on the badge, which may exceed the number of thumbnails in [thumbnailUris].
 *
 * @param thumbnailUris URIs of the most recent captured page thumbnails, oldest first.
 * @param count Total number of pages captured so far.
 * @param modifier Modifier for the deck.
 */
@Composable
fun CapturedPagesDeck(
    thumbnailUris: List<String>,
    count: Int,
    modifier: Modifier = Modifier,
) {
    if (thumbnailUris.isEmpty()) return

    val fanned = thumbnailUris.takeLast(MAX_FANNED)
    Box(modifier = modifier.size(96.dp), contentAlignment = Alignment.BottomStart) {
        fanned.forEachIndexed { index, uri ->
            // Oldest fanned card leans most; the top (newest) card sits upright.
            val depth = fanned.lastIndex - index
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = PAGE_CARD_WIDTH, height = PAGE_CARD_HEIGHT)
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

// Portrait, page-shaped cards (≈0.72 aspect) so captured pages read as documents, not squares.
private val PAGE_CARD_WIDTH = 52.dp
private val PAGE_CARD_HEIGHT = 72.dp
