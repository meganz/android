package mega.privacy.android.feature.photos.presentation.mediadiscovery.view

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.nodes.components.NodeHeaderItem
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration

@Composable
internal fun MediaDiscoveryLoadingView(
    modifier: Modifier = Modifier,
    itemCount: Int = 12,
    onChangeViewType: () -> Unit = {},
) {
    val configuration = LocalConfiguration.current
    val isInLandscapeMode = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LazyVerticalGrid(
        modifier = modifier,
        state = rememberLazyGridState(),
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        userScrollEnabled = false
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            NodeHeaderItem(
                modifier = Modifier
                    .padding(horizontal = 8.dp),
                onSortOrderClick = {},
                onChangeViewTypeClick = onChangeViewType,
                onEnterMediaDiscoveryClick = {},
                sortConfiguration = NodeSortConfiguration.default,
                isListView = false,
                showSortOrder = false,
                showChangeViewType = true,
                showMediaDiscoveryButton = false,
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(
                    modifier = Modifier
                        .width(120.dp)
                        .height(16.dp)
                        .shimmerEffect(shape = RoundedCornerShape(4.dp))
                )
            }
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

@CombinedThemePreviews
@Composable
private fun MediaDiscoveryLoadingViewPreview() {
    AndroidThemeForPreviews {
        MediaDiscoveryLoadingView()
    }
}
