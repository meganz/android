package mega.privacy.android.shared.original.core.ui.controls.skeleton

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.utils.shimmerEffect

/**
 * A shimmering skeleton Composable that is displayed while loading card items
 *
 * @param modifier [Modifier]
 */
@Composable
fun CardItemLoadingSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .testTag(TEST_TAG_CARD_ITEM_LOADING_SKELETON)
            .background(
                color = MegaOriginalTheme.colors.background.surface1,
                shape = RoundedCornerShape(6.dp),
            )
    ) {
        Row(
            modifier = Modifier
                .padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 68.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(
                modifier = Modifier
                    .size(48.dp)
                    .clip(shape = RoundedCornerShape(8.dp))
                    .shimmerEffect()
            )
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .fillMaxWidth()
            ) {
                Spacer(
                    modifier = Modifier
                        .clip(shape = RoundedCornerShape(12.dp))
                        .height(16.dp)
                        .width(80.dp)
                        .shimmerEffect()
                )
                Spacer(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(shape = RoundedCornerShape(12.dp))
                        .height(16.dp)
                        .width(143.dp)
                        .shimmerEffect()
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun CardItemLoadingPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        CardItemLoadingSkeleton()
    }
}

/**
 * Test Tag for the List Item Loading Skeleton
 */
internal const val TEST_TAG_CARD_ITEM_LOADING_SKELETON = "card_item_loading_skeleton"