package mega.privacy.android.legacy.core.ui.controls.lists

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.utils.shimmerEffect
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme


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
                .shimmerEffect()
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
                    .shimmerEffect()
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
            .padding(top = 8.dp, start = 12.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .height(20.dp)
                .width(60.dp)
                .clip(RoundedCornerShape(100.dp))
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.weight(1f))
        Spacer(
            modifier = Modifier
                .height(20.dp)
                .width(20.dp)
                .shimmerEffect()
        )
    }
}

@CombinedThemePreviews
@Composable
private fun NodeLoadingGridViewItemPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        NodeLoadingGridViewItem()
    }
}

@CombinedThemePreviews
@Composable
private fun LoadingHeaderViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        LoadingHeaderView()
    }
}