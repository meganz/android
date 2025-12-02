package mega.privacy.android.feature.photos.presentation.albums.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.android.core.ui.modifiers.shimmerEffect

private val gap = 1.dp

/**
 * Shimmer loading state for AlbumContentHighlightStart layout
 * Mimics the layout with a large photo on left and 2 small photos stacked on right
 */
@Composable
internal fun AlbumContentHighlightStartShimmer(
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        // Large photo on left
        Spacer(
            modifier = Modifier
                .width(size * 2)
                .height(size * 2)
                .shimmerEffect(RectangleShape)
        )

        // Two small photos stacked on right
        Column(verticalArrangement = Arrangement.SpaceBetween) {
            Spacer(
                modifier = Modifier
                    .size(size)
                    .shimmerEffect(RectangleShape)
            )
            Spacer(modifier = Modifier.height(gap))
            Spacer(
                modifier = Modifier
                    .size(size)
                    .shimmerEffect(RectangleShape)
            )
        }
    }
}

/**
 * Shimmer loading state for AlbumContentUniform layout
 * Mimics the layout with 3 equal-sized photos in a row
 */
@Composable
internal fun AlbumContentUniformShimmer(
    size: Dp,
    modifier: Modifier = Modifier,
    itemCount: Int = 3,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        repeat(itemCount) {
            Spacer(
                modifier = Modifier
                    .size(size)
                    .shimmerEffect(RectangleShape)
            )
        }
        // Add spacer if less than 3 items
        if (itemCount < 3) {
            repeat(3 - itemCount) {
                Spacer(modifier = Modifier.size(size))
            }
        }
    }
}

/**
 * Shimmer loading state for AlbumContentHighlightEnd layout
 * Mimics the layout with 2 small photos stacked on left and large photo on right
 */
@Composable
internal fun AlbumContentHighlightEndShimmer(
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        // Two small photos stacked on left
        Column(verticalArrangement = Arrangement.SpaceBetween) {
            Spacer(
                modifier = Modifier
                    .size(size)
                    .shimmerEffect(RectangleShape)
            )
            Spacer(modifier = Modifier.height(gap))
            Spacer(
                modifier = Modifier
                    .size(size)
                    .shimmerEffect(RectangleShape)
            )
        }

        // Large photo on right
        Spacer(
            modifier = Modifier
                .width(size * 2)
                .height(size * 2)
                .shimmerEffect(RectangleShape)
        )
    }
}

