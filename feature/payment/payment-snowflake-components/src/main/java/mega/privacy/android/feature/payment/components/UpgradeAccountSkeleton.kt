package mega.privacy.android.feature.payment.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * A skeleton view that mimics the choose account screen layout with shimmer effects.
 * Shows placeholder items for ProPlanCard while loading subscriptions.
 */
fun LazyListScope.upgradeAccountSkeleton(itemCount: Int = 3) {
    items(itemCount) { index ->
        ProPlanCardSkeleton(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp)
                .testTag("${TEST_TAG_PRO_PLAN_CARD_SKELETON}$index")
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Skeleton item that exactly matches ProPlanCard layout.
 * Uses exact spacing and dimensions to match the real component.
 */
@Composable
fun ProPlanCardSkeleton(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .border(
                width = 1.dp,
                color = DSTokens.colors.border.strong,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    ) {
        // Header row skeleton (matches ProPlanCard header)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 10.dp),
        ) {
            // Plan name skeleton
            Spacer(
                modifier = Modifier
                    .width(60.dp)
                    .height(20.dp)
                    .shimmerEffect(RoundedCornerShape(4.dp))
            )

            Spacer(Modifier.weight(1f))

            // Radio button skeleton
            Spacer(
                modifier = Modifier
                    .size(20.dp)
                    .shimmerEffect(RoundedCornerShape(10.dp))
            )
        }

        // Content row skeleton (matches ProPlanCard content)
        Row(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left column: Storage and Transfer
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Storage text skeleton
                Spacer(
                    modifier = Modifier
                        .width(120.dp)
                        .height(20.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )
                // Transfer text skeleton
                Spacer(
                    modifier = Modifier
                        .width(130.dp)
                        .height(20.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )
            }

            // Right column: Price info
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Price value skeleton
                Spacer(
                    modifier = Modifier
                        .width(64.dp)
                        .height(24.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )
                // Billing info skeleton (optional)
                Spacer(
                    modifier = Modifier
                        .width(140.dp)
                        .height(16.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

/**
 * Full-page skeleton for the redesigned subscription page. Replaces the whole screen content while
 * subscriptions are loading: a title line, a shorter subtitle line, then placeholder plan cards.
 */
fun LazyListScope.upgradeAccountRevampSkeleton(itemCount: Int = 3) {
    item(TEST_TAG_REVAMP_SKELETON_HEADER) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp)
                .testTag(TEST_TAG_REVAMP_SKELETON_HEADER),
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .shimmerEffect(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(24.dp))
            Spacer(
                modifier = Modifier
                    .width(112.dp)
                    .height(16.dp)
                    .shimmerEffect(RoundedCornerShape(12.dp))
            )
        }
    }
    items(itemCount) { index ->
        PlanPriceCardSkeleton(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .testTag("$TEST_TAG_PLAN_PRICE_CARD_SKELETON$index")
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Skeleton item matching [PlanPriceCard] layout: name line, price line, two benefit lines, and a
 * full-width buy-button placeholder.
 */
@Composable
fun PlanPriceCardSkeleton(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = DSTokens.colors.border.strong,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(
            modifier = Modifier
                .width(48.dp)
                .height(16.dp)
                .shimmerEffect(RoundedCornerShape(12.dp))
        )
        Spacer(
            modifier = Modifier
                .width(90.dp)
                .height(16.dp)
                .shimmerEffect(RoundedCornerShape(12.dp))
        )
        Spacer(
            modifier = Modifier
                .width(230.dp)
                .height(16.dp)
                .shimmerEffect(RoundedCornerShape(12.dp))
        )
        Spacer(
            modifier = Modifier
                .width(230.dp)
                .height(16.dp)
                .shimmerEffect(RoundedCornerShape(12.dp))
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .shimmerEffect(RoundedCornerShape(8.dp))
        )
    }
}

/**
 * Test tag for the ProPlanCard skeleton
 */
internal const val TEST_TAG_PRO_PLAN_CARD_SKELETON = "upgrade_account:pro_plan_card_skeleton"

/**
 * Test tag for the redesigned subscription page skeleton header (title + subtitle lines)
 */
const val TEST_TAG_REVAMP_SKELETON_HEADER = "upgrade_account:revamp_skeleton_header"

/**
 * Test tag prefix for each PlanPriceCard skeleton (append index)
 */
const val TEST_TAG_PLAN_PRICE_CARD_SKELETON = "upgrade_account:plan_price_card_skeleton"

@CombinedThemePreviews
@Composable
private fun UpgradeAccountSkeletonPreview() {
    AndroidThemeForPreviews {
        LazyColumn {
            upgradeAccountSkeleton(itemCount = 3)
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ProPlanCardSkeletonPreview() {
    AndroidThemeForPreviews {
        ProPlanCardSkeleton(
            modifier = Modifier.padding(16.dp)
        )
    }
}

@CombinedThemePreviews
@Composable
private fun UpgradeAccountRevampSkeletonPreview() {
    AndroidThemeForPreviews {
        LazyColumn {
            upgradeAccountRevampSkeleton(itemCount = 3)
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PlanPriceCardSkeletonPreview() {
    AndroidThemeForPreviews {
        PlanPriceCardSkeleton(
            modifier = Modifier.padding(16.dp)
        )
    }
}
