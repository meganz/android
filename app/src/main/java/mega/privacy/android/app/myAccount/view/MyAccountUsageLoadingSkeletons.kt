package mega.privacy.android.app.myAccount.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.divider.StrongDivider
import mega.android.core.ui.modifiers.shimmerEffect

@Composable
internal fun UsageMeterShimmerLayout(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UsageMeterSingleSideShimmer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(10.dp))
        // Right side is intentionally blank during shimmer: transfer section is only shown for paid accounts.
        Spacer(modifier = Modifier.weight(1f))
    }
}

/** One circular quota bar + text column, matching the left (storage) side of [RegularUsageLayout]. */
@Composable
private fun UsageMeterSingleSideShimmer(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .shimmerEffect(shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        // No weight on Column: intrinsic width only (fixed-dp bars) so shimmer never sees width jumps.
        Column(verticalArrangement = Arrangement.Center) {
            Spacer(
                modifier = Modifier
                    .size(width = 128.dp, height = 16.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .shimmerEffect(shape = RoundedCornerShape(100.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Spacer(
                modifier = Modifier
                    .size(width = 128.dp, height = 16.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .shimmerEffect(shape = RoundedCornerShape(100.dp))
            )
        }
    }
}

@Composable
internal fun StorageBreakdownLoadingSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(16.dp))
        Spacer(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect(shape = RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.height(5.dp))
        repeat(4) {
            StorageBreakdownRowSkeleton()
        }
    }
}

@Composable
private fun StorageBreakdownRowSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(top = 4.dp, bottom = 4.dp)
    ) {
        Spacer(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(148.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(100.dp))
                .shimmerEffect(shape = RoundedCornerShape(100.dp))
        )
        Spacer(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(80.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(100.dp))
                .shimmerEffect(shape = RoundedCornerShape(100.dp))
        )
        StrongDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )
    }
}
