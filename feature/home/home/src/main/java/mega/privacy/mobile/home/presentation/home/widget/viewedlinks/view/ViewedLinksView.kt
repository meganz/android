package mega.privacy.mobile.home.presentation.home.widget.viewedlinks.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import mega.android.core.ui.modifiers.shimmerEffect

@Composable
internal fun ViewedLinkListLoadingItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(VIEWED_LINK_LOADING_ITEM_TEST_TAG),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .shimmerEffect()
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
                .shimmerEffect()
                .align(Alignment.CenterVertically)
        )
    }
}

@Composable
internal fun ViewedLinkGridLoadingItem() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(VIEWED_LINK_GRID_LOADING_ITEM_TEST_TAG),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(5f / 4f)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )
        Box(
            modifier = Modifier
                .padding(start = 8.dp, top = 6.dp, bottom = 6.dp)
                .fillMaxWidth(fraction = 0.7f)
                .height(14.dp)
                .shimmerEffect()
        )
    }
}

internal const val VIEWED_LINK_LOADING_ITEM_TEST_TAG = "viewed_links:loading_item"
internal const val VIEWED_LINK_GRID_LOADING_ITEM_TEST_TAG = "viewed_links:grid_loading_item"