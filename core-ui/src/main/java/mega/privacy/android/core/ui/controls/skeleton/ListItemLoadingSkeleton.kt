package mega.privacy.android.core.ui.controls.skeleton

import androidx.compose.foundation.isSystemInDarkTheme
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
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.utils.shimmerEffect


/**
 * A shimmering skeleton Composable that is displayed while loading node list items
 */
@Composable
fun ListItemLoadingSkeleton() {
    Row(
        modifier = Modifier
            .testTag(LIST_ITEM_LOADING_SKELETON)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            modifier = Modifier
                .size(48.dp)
                .clip(shape = RoundedCornerShape(10.dp))
                .shimmerEffect()
        )
        Column(
            modifier = Modifier.padding(start = 12.dp).fillMaxWidth()
        ) {
            Spacer(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(100.dp))
                    .height(16.dp)
                    .width(120.dp)
                    .shimmerEffect()
            )
            Spacer(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(shape = RoundedCornerShape(100.dp))
                    .height(16.dp)
                    .width(183.dp)
                    .shimmerEffect()
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewDeviceCenterLoadingItem() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ListItemLoadingSkeleton()
    }
}

/**
 * Test Tag for the List Item Loading Skeleton
 */
internal const val LIST_ITEM_LOADING_SKELETON = "list_item_loading_skeleton:row_loading_item"
