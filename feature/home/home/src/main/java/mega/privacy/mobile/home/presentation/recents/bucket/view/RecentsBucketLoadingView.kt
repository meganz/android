package mega.privacy.mobile.home.presentation.recents.bucket.view

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.nodecomponents.list.NodeListViewItemSkeleton
import mega.privacy.mobile.home.presentation.recents.view.RecentsDateHeaderSkeleton

@Composable
internal fun RecentsBucketListLoadingView(
    modifier: Modifier = Modifier,
    itemCount: Int = 10,
) {
    LazyColumn(
        modifier = modifier.testTag(RECENTS_LIST_LOADING_TEST_TAG),
        userScrollEnabled = false
    ) {
        item {
            RecentsDateHeaderSkeleton(
                modifier = Modifier
                    .padding(bottom = 1.dp)
            )
        }

        items(itemCount) {
            NodeListViewItemSkeleton()
        }
    }
}

@Composable
internal fun RecentsBucketMediaGridLoadingView(
    modifier: Modifier = Modifier,
    itemCount: Int = 12,
) {
    val configuration = LocalConfiguration.current
    val isInLandscapeMode = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LazyVerticalGrid(
        modifier = modifier.testTag(RECENTS_MEDIA_GRID_LOADING_TEST_TAG),
        state = rememberLazyGridState(),
        columns = GridCells.Fixed(if (isInLandscapeMode) 6 else 3),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        userScrollEnabled = false
    ) {
        item(
            span = { GridItemSpan(maxLineSpan) }
        ) {
            RecentsDateHeaderSkeleton(
                modifier = Modifier
                    .padding(bottom = 1.dp)
            )
        }
        items(itemCount) {
            Spacer(
                modifier = Modifier
                    .aspectRatio(1f)
                    .shimmerEffect(shape = RoundedCornerShape(0.dp)),
            )
        }
    }
}

internal const val RECENTS_LIST_LOADING_TEST_TAG = "recents_list_bucket:loading"
internal const val RECENTS_MEDIA_GRID_LOADING_TEST_TAG = "recents_media_grid_bucket:loading"

@CombinedThemePreviews
@Composable
private fun RecentsBucketListLoadingViewPreview() {
    AndroidThemeForPreviews {
        RecentsBucketListLoadingView()
    }
}


@CombinedThemePreviews
@Composable
private fun RecentsBucletMediaGridLoadingViewPreview() {
    AndroidThemeForPreviews {
        RecentsBucketMediaGridLoadingView()
    }
}

