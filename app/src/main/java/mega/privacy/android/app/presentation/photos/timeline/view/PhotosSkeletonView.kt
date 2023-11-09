package mega.privacy.android.app.presentation.photos.timeline.view

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.privacy.android.legacy.core.ui.controls.modifier.skeletonEffect

@Composable
internal fun PhotosSkeletonView() {
    val columns =
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
            3
        } else {
            5
        }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier
            .fillMaxSize(),
        userScrollEnabled = false,
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
                        text = "Placeholder",
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(start = 16.dp, top = 14.dp, bottom = 14.dp)
                            .skeletonEffect(),
                    )
                }
            } else {
                Spacer(
                    modifier = Modifier
                        .padding(all = 1.5.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .skeletonEffect(),
                )
            }
        }
    }
}

@Composable
internal fun AlbumListSkeletonView() {
    val columns =
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
            3
        } else {
            4
        }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier
            .fillMaxSize(),
        userScrollEnabled = false,
        contentPadding = PaddingValues(top = 8.dp, start = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(
            count = 21, // make sure the shimmer effect shows at least a screen
            key = { index ->
                index
            },
        ) { index ->

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(
                    modifier = Modifier
                        .padding(all = 1.5.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .skeletonEffect(),
                )
                Text(
                    text = "Album names",
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(top = 10.dp, bottom = 3.dp)
                        .skeletonEffect(),
                )
                Text(
                    text = "number",
                    modifier = Modifier
                        .wrapContentSize()
                        .skeletonEffect(),
                )
            }
        }
    }
}

@Composable
internal fun AlbumContentSkeletonView(
    smallWidth: Dp,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(1.dp),
        userScrollEnabled = false,
    ) {
        items(
            count = 4,
        ) { index ->

            when (index % 3) {
                0 -> AlbumBig2SmallSkeletonView(smallWidth = smallWidth)
                1 -> AlbumSmall3ItemSkeletonView(smallWidth = smallWidth)
                2 -> AlbumSmall2BigSkeletonView(smallWidth = smallWidth)
            }
        }
    }
}

@Composable
private fun AlbumBig2SmallSkeletonView(
    smallWidth: Dp,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(
            modifier = Modifier
                .width(smallWidth * 2)
                .height(smallWidth * 2 + 1.dp)
                .aspectRatio(1f)
                .skeletonEffect(),
        )
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(
                modifier = Modifier
                    .width(smallWidth)
                    .height(smallWidth)
                    .aspectRatio(1f)
                    .skeletonEffect(),
            )
            Spacer(
                modifier = Modifier.height(1.dp)
            )
            Spacer(
                modifier = Modifier
                    .width(smallWidth)
                    .height(smallWidth)
                    .aspectRatio(1f)
                    .skeletonEffect(),
            )
        }
    }
}

@Composable
private fun AlbumSmall3ItemSkeletonView(
    smallWidth: Dp,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(
            modifier = Modifier
                .width(smallWidth)
                .height(smallWidth)
                .aspectRatio(1f)
                .skeletonEffect(),
        )
        Spacer(
            modifier = Modifier
                .width(smallWidth)
                .height(smallWidth)
                .aspectRatio(1f)
                .skeletonEffect(),
        )
        Spacer(
            modifier = Modifier
                .width(smallWidth)
                .height(smallWidth)
                .aspectRatio(1f)
                .skeletonEffect(),
        )
    }
}

@Composable
private fun AlbumSmall2BigSkeletonView(
    smallWidth: Dp,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(
                modifier = Modifier
                    .width(smallWidth)
                    .height(smallWidth)
                    .aspectRatio(1f)
                    .skeletonEffect(),
            )
            Spacer(
                modifier = Modifier.height(1.dp)
            )
            Spacer(
                modifier = Modifier
                    .width(smallWidth)
                    .height(smallWidth)
                    .aspectRatio(1f)
                    .skeletonEffect(),
            )
        }
        Spacer(
            modifier = Modifier
                .width(smallWidth * 2)
                .height(smallWidth * 2 + 1.dp)
                .aspectRatio(1f)
                .skeletonEffect(),
        )
    }
}
