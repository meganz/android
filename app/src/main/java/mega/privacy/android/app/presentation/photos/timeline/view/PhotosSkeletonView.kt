package mega.privacy.android.app.presentation.photos.timeline.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.fade
import com.google.accompanist.placeholder.placeholder
import mega.privacy.android.core.ui.theme.extensions.grey_020_grey_800
import mega.privacy.android.core.ui.theme.extensions.grey_050_grey_900

@Composable
internal fun PhotosSkeletonView() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize(),
    ) {
        items(
            count = 21, // make sure the shimmer effect shows at least a screen
            key = { index ->
                index
            },
            span = { index ->
                if (index % 7 == 0) {
                    GridItemSpan(maxLineSpan)
                } else {
                    GridItemSpan(1)
                }
            }
        ) { index ->
            if (index % 7 == 0) {
                Box {
                    Text(
                        text = "Place holder",
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(start = 16.dp, top = 14.dp, bottom = 14.dp)
                            .placeholder(
                                color = MaterialTheme.colors.grey_020_grey_800,
                                shape = RoundedCornerShape(4.dp),
                                highlight = PlaceholderHighlight.fade(MaterialTheme.colors.grey_050_grey_900),
                                visible = true,
                            ),
                    )
                }
            } else {
                Spacer(
                    modifier = Modifier
                        .padding(all = 1.5.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .placeholder(
                            color = MaterialTheme.colors.grey_020_grey_800,
                            shape = RoundedCornerShape(4.dp),
                            highlight = PlaceholderHighlight.fade(MaterialTheme.colors.grey_050_grey_900),
                            visible = true,
                        ),
                )
            }
        }
    }
}