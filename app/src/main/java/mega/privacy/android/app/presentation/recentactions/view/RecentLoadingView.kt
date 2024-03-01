package mega.privacy.android.app.presentation.recentactions.view


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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.legacy.core.ui.controls.lists.NodeLoadingListViewItem
import mega.privacy.android.legacy.core.ui.controls.modifier.skeletonEffect

/**
 * Loading state view for Recent Actions
 * @param modifier [Modifier]
 */
@Composable
fun RecentLoadingView(
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        item {
            Row(
                modifier = modifier
                    .height(40.dp)
                    .padding(top = 8.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(
                    modifier = Modifier
                        .height(20.dp)
                        .width(100.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .skeletonEffect()
                )
            }
        }
        items(count = 10) {
            NodeLoadingListViewItem()
        }
    }
}


@CombinedThemePreviews
@Composable
private fun RecentLoadingViewListPreview(
    @PreviewParameter(BooleanProvider::class) parameter: Boolean,
) {
    RecentLoadingView()
}