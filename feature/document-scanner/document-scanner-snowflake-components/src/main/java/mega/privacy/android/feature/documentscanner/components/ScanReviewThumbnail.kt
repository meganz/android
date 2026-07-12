package mega.privacy.android.feature.documentscanner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * A single page thumbnail in the review strip. Shows its 1-based position and a
 * selection border when it is the page currently open in the preview.
 *
 * @param thumbnailUri URI of the page thumbnail.
 * @param pageNumber 1-based position of this page in the strip.
 * @param isSelected Whether this page is the one shown in the preview.
 * @param modifier Modifier (the caller sets the size).
 */
@Composable
fun ScanReviewThumbnail(
    thumbnailUri: String,
    pageNumber: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(DSTokens.colors.background.surface1)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) {
                    DSTokens.colors.border.strongSelected
                } else {
                    DSTokens.colors.border.subtle
                },
                shape = shape,
            ),
    ) {
        AsyncImage(
            model = thumbnailUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        MegaText(
            text = "$pageNumber",
            textColor = TextColor.OnColor,
            style = AppTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(3.dp)
                .clip(CircleShape)
                .background(DSTokens.colors.background.blur)
                .defaultMinSize(minWidth = 16.dp)
                .padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}
