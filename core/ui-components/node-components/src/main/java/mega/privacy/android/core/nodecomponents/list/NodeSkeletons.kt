package mega.privacy.android.core.nodecomponents.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.list.GenericListItem
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * A skeleton view that mimics the cloud drive content layout with shimmer effects.
 * Shows placeholder items for files/folders while loading.
 * Supports both list and grid view types with exact matching layouts.
 */
@Composable
fun NodesViewSkeleton(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    isListView: Boolean = true,
) {
    if (isListView) {
        NodeListViewSkeleton(modifier = modifier, contentPadding = contentPadding)
    } else {
        NodeGridViewSkeleton(modifier = modifier, contentPadding = contentPadding)
    }
}

/**
 * List view skeleton for cloud drive content.
 * Matches exact NodeListViewItem layout and spacing.
 */
@Composable
private fun NodeListViewSkeleton(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        userScrollEnabled = false
    ) {
        // Header skeleton (matches NodeHeaderItem)
        item {
            NodeHeaderItemSkeleton(
                modifier = Modifier
                    .padding(start = DSTokens.spacings.s2, end = DSTokens.spacings.s3)
                    .padding(bottom = DSTokens.spacings.s3)
            )
        }

        // List items skeleton (matches NodeListViewItem)
        items(20) { index ->
            NodeListViewItemSkeleton()
        }
    }
}

/**
 * Grid view skeleton for cloud drive content.
 * Matches exact NodeGridViewItem layout and spacing.
 */
@Composable
private fun NodeGridViewSkeleton(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            contentPadding = contentPadding,
            columns = GridCells.Fixed(2), // Matches NodeGridViewItem span count
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = DSTokens.spacings.s3)
                .semantics { testTagsAsResourceId = true },
            horizontalArrangement = Arrangement.spacedBy(DSTokens.spacings.s3),
            verticalArrangement = Arrangement.spacedBy(DSTokens.spacings.s3),
        ) {
            // Header skeleton (spans full width)
            item(span = { GridItemSpan(2) }) {
                NodeHeaderItemSkeleton()
            }

            // Grid items skeleton (matches NodeGridViewItem)
            items(40) { index ->
                NodeGridViewItemSkeleton()
            }
        }
    }
}

/**
 * Header skeleton that matches NodeHeaderItem exactly.
 */
@Composable
private fun NodeHeaderItemSkeleton(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sort order skeleton
        Box(
            contentAlignment = Alignment.Center,
        ) {
            Text(
                style = AppTheme.typography.titleSmall,
                text = " ",
                modifier = Modifier.padding(DSTokens.spacings.s3)
            )
            Spacer(
                modifier = Modifier
                    .padding(start = DSTokens.spacings.s3)
                    .width(80.dp)
                    .height(16.dp)
                    .shimmerEffect()
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // View type toggle skeleton
        Spacer(
            modifier = Modifier
                .padding(DSTokens.spacings.s3)
                .size(16.dp)
                .shimmerEffect(shape = RoundedCornerShape(6.dp))
        )
    }
}

/**
 * List skeleton item that exactly matches NodeListViewItem layout.
 * Uses exact spacing: horizontal = DSTokens.spacings.s4 (16.dp), vertical = DSTokens.spacings.s3 (12.dp)
 */
@Composable
private fun NodeListViewItemSkeleton() {
    GenericListItem(
        contentPadding = PaddingValues(
            horizontal = DSTokens.spacings.s4,
            vertical = DSTokens.spacings.s3
        ),
        leadingElement = {
            Spacer(
                modifier = Modifier
                    .size(32.dp)
                    .shimmerEffect(RoundedCornerShape(6.dp))
            )
        },
        title = {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = " ",
                    overflow = TextOverflow.MiddleEllipsis,
                    maxLines = 1,
                    style = AppTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                )
                Spacer(
                    modifier = Modifier
                        .height(18.dp)
                        .fillMaxWidth(0.6f)
                        .shimmerEffect(),
                )
            }
            Spacer(Modifier.height(DSTokens.spacings.s1))
        },
        subtitle = {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = " ",
                    overflow = TextOverflow.Clip,
                    style = AppTheme.typography.bodySmall,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                )
                Spacer(
                    modifier = Modifier
                        .height(16.dp)
                        .fillMaxWidth(0.4f)
                        .shimmerEffect(),
                )
            }
        }
    )
}

/**
 * Grid skeleton item that exactly matches NodeGridViewItem layout.
 * Uses exact spacing: DSTokens.spacings.s3 (12.dp) for arrangement
 */
@Composable
private fun NodeGridViewItemSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Thumbnail area skeleton (5:4 aspect ratio as per NodeGridViewItem)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(5f / 4f)
                .shimmerEffect(DSTokens.shapes.extraSmall)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 44.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = "",
                style = AppTheme.typography.bodySmall,
                overflow = TextOverflow.MiddleEllipsis,
                modifier = Modifier
                    .testTag(NODE_TITLE_TEXT_TEST_TAG),
            )

            // Name text skeleton
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .shimmerEffect(RoundedCornerShape(4.dp))
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun NodesListViewSkeletonPreview() {
    AndroidThemeForPreviews {
        NodesViewSkeleton(
            contentPadding = PaddingValues(16.dp),
            isListView = true
        )
    }
}

@CombinedThemePreviews
@Composable
private fun NodesGridViewSkeletonPreview() {
    AndroidThemeForPreviews {
        NodesViewSkeleton(
            contentPadding = PaddingValues(16.dp),
            isListView = false
        )
    }
}