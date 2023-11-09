package mega.privacy.android.legacy.core.ui.controls.lists

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import mega.privacy.android.legacy.core.ui.controls.modifier.skeletonEffect
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012

/**
 * Loading item for list for node
 * @param modifier [Modifier]
 */
@Composable
fun NodeLoadingListViewItem(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .size(40.dp)
                .skeletonEffect()
        )
        Column(
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Spacer(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(100.dp))
                    .height(20.dp)
                    .width(152.dp)
                    .skeletonEffect()
            )

            Spacer(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(shape = RoundedCornerShape(100.dp))
                    .height(20.dp)
                    .width(120.dp)
                    .skeletonEffect()
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Spacer(
            modifier = Modifier
                .size(28.dp)
                .skeletonEffect()
        )
    }
}

/**
 * Loading item for grid for node
 */
@Composable
fun NodeLoadingGridViewItem(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(horizontal = 8.dp)
    ) {
        Spacer(
            modifier = Modifier
                .height(172.dp)
                .fillMaxWidth()
                .skeletonEffect()
        )
        Divider(
            color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
            modifier = Modifier.height(1.dp)
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .width(76.dp)
                    .height(20.dp)
                    .skeletonEffect()
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(
                modifier = Modifier
                    .size(20.dp)
                    .skeletonEffect()
            )
        }
    }
}

/**
 * Loading item for Header
 * @param modifier [Modifier]
 */
@Composable
fun LoadingHeaderView(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(40.dp)
            .padding(top = 8.dp, start = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .height(20.dp)
                .width(40.dp)
                .clip(RoundedCornerShape(100.dp))
                .skeletonEffect()
        )
        Spacer(modifier = Modifier.weight(1f))
        Spacer(
            modifier = Modifier
                .height(20.dp)
                .width(20.dp)
                .skeletonEffect()
        )
    }
}

@CombinedThemePreviews
@Composable
private fun NodeLoadingViewItemPreview() {
    NodeLoadingListViewItem()
}

@CombinedThemePreviews
@Composable
private fun NodeLoadingGridViewItemPreview() {
    NodeLoadingGridViewItem()
}

@CombinedThemePreviews
@Composable
private fun LoadingHeaderViewPreview() {
    LoadingHeaderView()
}