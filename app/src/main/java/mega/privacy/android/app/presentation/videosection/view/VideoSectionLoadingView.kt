package mega.privacy.android.app.presentation.videosection.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.skeleton.ListItemLoadingSkeleton
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.utils.shimmerEffect

@Composable
internal fun VideoSectionLoadingView(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.testTag(VIDEO_SECTION_LOADING_VIEW_TEST_TAG),
    ) {
        item {
            Row(
                modifier = modifier
                    .height(40.dp)
                    .padding(top = 8.dp, start = 12.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(
                    modifier = Modifier
                        .height(20.dp)
                        .width(100.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .shimmerEffect()
                )
            }
        }
        items(count = 20) {
            ListItemLoadingSkeleton()
        }
    }
}

/**
 * Test tag for the video section loading view.
 */
const val VIDEO_SECTION_LOADING_VIEW_TEST_TAG = "video_section:loading_view"

@CombinedThemePreviews
@Composable
private fun RecentLoadingViewListPreview(
    @PreviewParameter(BooleanProvider::class) parameter: Boolean,
) {
    VideoSectionLoadingView()
}