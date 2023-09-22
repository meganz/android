package mega.privacy.android.app.presentation.search.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.controls.lists.LoadingHeaderView
import mega.privacy.android.core.ui.controls.lists.NodeLoadingGridViewItem
import mega.privacy.android.core.ui.controls.lists.NodeLoadingListViewItem
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews

/**
 * Loading state view for NodesView
 * @param modifier [Modifier]
 * @param isList if current view type is list or Grid
 */
@Composable
fun LoadingStateView(
    isList: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        if (isList) {
            LazyColumn(content = {
                item {
                    LoadingHeaderView()
                }
                items(count = 20) {
                    NodeLoadingListViewItem()
                }
            })
        } else {
            LazyVerticalGrid(
                modifier = Modifier.padding(horizontal = 4.dp),
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                content = {
                    item(span = { GridItemSpan(2) }) {
                        LoadingHeaderView()
                    }
                    items(count = 40) {
                        NodeLoadingGridViewItem()
                    }
                })
        }
    }
}


@CombinedThemePreviews
@Composable
private fun LoadingStateViewListPreview(
    @PreviewParameter(BooleanProvider::class) parameter: Boolean,
) {
    LoadingStateView(isList = parameter)
}