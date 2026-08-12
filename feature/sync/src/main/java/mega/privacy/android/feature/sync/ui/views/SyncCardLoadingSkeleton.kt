package mega.privacy.android.feature.sync.ui.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.surface.CardSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Placeholder shown in place of a [SyncCard] while the sync list is loading.
 *
 * core-ui has no card-shaped skeleton, so this mirrors the card's own surface and the
 * thumbnail/title/subtitle blocks it will be replaced by.
 */
@Composable
internal fun SyncCardLoadingSkeleton(modifier: Modifier = Modifier) {
    CardSurface(
        surfaceColor = SurfaceColor.Surface1,
        modifier = modifier.testTag(TEST_TAG_SYNC_CARD_LOADING_SKELETON),
    ) {
        Row(
            modifier = Modifier.padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 68.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shimmerEffect()
            )
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .fillMaxWidth()
            ) {
                Spacer(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .height(16.dp)
                        .fillMaxWidth(fraction = 0.5f)
                        .shimmerEffect()
                )
                Spacer(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .height(12.dp)
                        .fillMaxWidth(fraction = 0.3f)
                        .shimmerEffect()
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun SyncCardLoadingSkeletonPreview() {
    AndroidThemeForPreviews {
        SyncCardLoadingSkeleton(modifier = Modifier.padding(16.dp))
    }
}

internal const val TEST_TAG_SYNC_CARD_LOADING_SKELETON = "sync_card:loading_skeleton"
