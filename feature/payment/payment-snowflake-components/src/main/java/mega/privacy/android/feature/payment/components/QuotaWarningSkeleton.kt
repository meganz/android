package mega.privacy.android.feature.payment.components

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * Shimmer placeholder shown while the quota-warning upsell screen loads its subscriptions and
 * usage. Mirrors the loaded layout: a centred illustration and two title lines, followed by the
 * current-plan card and the recommended-plan card.
 */
@Composable
fun QuotaWarningSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(
                    modifier = Modifier
                        .size(120.dp)
                        .shimmerEffect(RoundedCornerShape(8.dp))
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SkeletonBar(Modifier.fillMaxWidth().height(16.dp))
                    SkeletonBar(Modifier.width(120.dp).height(16.dp))
                }
            }
            QuotaSkeletonCard()
        }
        QuotaSkeletonCard()
    }
}

@Composable
private fun QuotaSkeletonCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = DSTokens.colors.border.strong,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SkeletonBar(Modifier.width(48.dp).height(16.dp))
        SkeletonBar(Modifier.width(90.dp).height(16.dp))
        SkeletonBar(Modifier.width(230.dp).height(16.dp))
        SkeletonBar(Modifier.width(230.dp).height(16.dp))
        SkeletonBar(Modifier.fillMaxWidth().height(32.dp), shape = RoundedCornerShape(8.dp))
    }
}

@Composable
private fun SkeletonBar(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
) {
    Spacer(modifier = modifier.shimmerEffect(shape))
}

@CombinedThemePreviews
@Composable
private fun QuotaWarningSkeletonPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        QuotaWarningSkeleton(modifier = Modifier.padding(16.dp))
    }
}
