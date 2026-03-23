package mega.privacy.android.feature.payment.presentation.upgrade

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.badge.Badge
import mega.android.core.ui.components.badge.BadgeType
import mega.android.core.ui.components.banner.InlineErrorBanner
import mega.android.core.ui.components.chip.MegaChip
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.feature.payment.components.ProPlanCard
import mega.privacy.android.feature.payment.components.TEST_TAG_PRO_PLAN_CARD
import mega.privacy.android.feature.payment.components.upgradeAccountSkeleton
import mega.privacy.android.feature.payment.model.UpgradeAccountState
import mega.privacy.android.feature.payment.model.extensions.toUIAccountType
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import java.util.Locale

/**
 * Content shown when Google Play subscriptions are not available in the user's region.
 */
internal fun LazyListScope.subscriptionUnavailableContent(
    onLearnMoreClick: () -> Unit,
) {
    item("subscription_unavailable_banner") {
        InlineErrorBanner(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag(TEST_TAG_SUBSCRIPTION_UNAVAILABLE_BANNER),
            body = stringResource(sharedR.string.choose_account_screen_subscriptions_not_available_via_google_play),
            actionButtonText = stringResource(sharedR.string.general_learn_more),
            showCancelButton = false,
            onActionButtonClick = onLearnMoreClick,
        )
    }
}

/**
 * Content shown when subscription feature is available: period chips, save badge, and plan cards.
 */
internal fun LazyListScope.subscriptionAvailableContent(
    uiState: UpgradeAccountState,
    isMonthly: Boolean,
    onMonthlyChange: (Boolean) -> Unit,
    chosenPlan: AccountType?,
    onPlanSelected: (AccountType) -> Unit,
    hasDiscount: Boolean,
    context: Context,
    locale: Locale,
    isUpgradeAccount: Boolean,
) {
    item("subscription_period") {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MegaChip(
                modifier = Modifier.testTag(TEST_TAG_MONTHLY_CHIP),
                selected = isMonthly,
                onClick = { onMonthlyChange(true) },
                content = stringResource(id = sharedR.string.subscription_type_monthly),
                leadingPainter = if (isMonthly) {
                    rememberVectorPainter(IconPack.Medium.Thin.Outline.Check)
                } else null
            )

            MegaChip(
                modifier = Modifier.testTag(TEST_TAG_YEARLY_CHIP),
                selected = !isMonthly,
                onClick = { onMonthlyChange(false) },
                content = stringResource(id = sharedR.string.subscription_type_yearly),
                leadingPainter = if (!isMonthly) {
                    rememberVectorPainter(IconPack.Medium.Thin.Outline.Check)
                } else null
            )
        }
    }

    item("save_up_to_badge_${hasDiscount}") {
        val label = if (hasDiscount) {
            stringResource(sharedR.string.account_upgrade_account_label_save_at_least)
        } else {
            stringResource(sharedR.string.account_upgrade_account_label_save_up_to)
        }
        val badgeType = if (hasDiscount) {
            BadgeType.MegaSecondary
        } else {
            BadgeType.Mega
        }
        Badge(
            badgeType = badgeType,
            text = label,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag(TEST_TAG_SAVE_UP_TO_BADGE)
        )
    }

    item("pro_plans_top_space") {
        Spacer(modifier = Modifier.height(8.dp))
    }

    if (uiState.localisedSubscriptionsList.isEmpty() || uiState.isSubscriptionFeatureAvailable != true) {
        upgradeAccountSkeleton(itemCount = 3)
    } else {
        val subscriptionsForPeriod = uiState.localisedSubscriptionsList.filter {
            it.hasSubscriptionFor(isMonthly)
        }
        itemsIndexed(subscriptionsForPeriod) { index, subscription ->
            val isRecommended = !hasDiscount
                    && uiState.cheapestSubscriptionAvailable?.accountType == subscription.accountType
            val storageFormattedSize = subscription.formatStorageSize()
            val transferFormattedSize = subscription.formatTransferSize(isMonthly)

            val uiAccountType = subscription.accountType.toUIAccountType()

            val storageString = stringResource(
                id = sharedR.string.choose_account_screen_storage_label,
                stringResource(id = storageFormattedSize.unit, storageFormattedSize.size)
            )
            val transferString = stringResource(
                id = sharedR.string.choose_account_screen_transfer_quota_label,
                stringResource(id = transferFormattedSize.unit, transferFormattedSize.size)
            )
            val totalPrice =
                subscription.localisePriceCurrencyCode(locale, isMonthly)

            val yearlyPricePerMonth = if (!isMonthly) {
                subscription.localisePriceOfYearlyAmountPerMonth(locale)
            } else null

            val currentSubscription = subscription.getSubscription(isMonthly)!!
            val discountPercentage = currentSubscription.discountedPercentage
            val offerPeriod = currentSubscription.offerPeriod
            val discountedPriceMonthly =
                subscription.localiseDiscountedPriceMonthlyCurrencyCode(locale, isMonthly)
            val discountedPriceYearly =
                subscription.localiseDiscountedPriceYearlyCurrencyCode(locale, isMonthly)

            val isCurrentPlan = isCurrentPlan(
                uiState = uiState,
                subscriptionAccountType = subscription.accountType,
                isMonthly = isMonthly,
                isUpgradeAccount = isUpgradeAccount
            )

            val yearlyBillingInfo = if (!isMonthly) {
                if (!isCurrentPlan && discountedPriceYearly != null
                    && discountPercentage != null && offerPeriod != null
                ) {
                    "[A]${totalPrice.price}[/A] ${
                        getOfferPeriodLabel(
                            discountedPriceYearly.price,
                            offerPeriod
                        )
                    }"
                } else {
                    stringResource(
                        sharedR.string.choose_account_screen_billed_yearly,
                        totalPrice.price
                    )
                }
            } else null

            ProPlanCard(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .testTag("$TEST_TAG_PRO_PLAN_CARD$index"),
                planName = stringResource(id = uiAccountType.textValue),
                isRecommended = isRecommended,
                isSelected = chosenPlan == subscription.accountType && !isCurrentPlan,
                storage = storageString,
                transfer = transferString,
                price = yearlyPricePerMonth?.price ?: totalPrice.price,
                yearlyBillingInfo = yearlyBillingInfo,
                offerName = discountPercentage?.takeIf { !isCurrentPlan }?.let {
                    getCampaignName(
                        context = context,
                        offerId = currentSubscription.offerId,
                        discountPercentage = discountPercentage
                    )
                },
                discountedPrice = discountedPriceMonthly?.price?.takeIf { !isCurrentPlan },
                isCurrentPlan = isCurrentPlan,
                onSelected = { onPlanSelected(subscription.accountType) },
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}