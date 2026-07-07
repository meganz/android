package mega.privacy.android.feature.payment.presentation.upgrade

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.feature.payment.components.BillingPeriodSelector
import mega.privacy.android.feature.payment.components.CurrentPlanCard
import mega.privacy.android.feature.payment.components.PlanPriceCard
import mega.privacy.android.feature.payment.components.WhyGoProCard
import mega.privacy.android.feature.payment.components.upgradeAccountSkeleton
import mega.privacy.android.feature.payment.model.UpgradeAccountState
import mega.privacy.android.feature.payment.model.extensions.toUIAccountType
import mega.privacy.android.shared.resources.R as sharedR
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Content of the redesigned ("no-offer") subscription page: title, "Why go Pro?" card, current plan
 * card, billing period selector, and per-plan cards with their own buy buttons.
 */
internal fun LazyListScope.subscriptionRevampContent(
    uiState: UpgradeAccountState,
    isMonthly: Boolean,
    onMonthlyChange: (Boolean) -> Unit,
    locale: Locale,
    isUpgradeAccount: Boolean,
    onInAppCheckoutClick: (Subscription) -> Unit,
    onSubscriptionUnavailableLearnMoreClick: () -> Unit,
) {
    item("revamp_title") {
        MegaText(
            text = stringResource(sharedR.string.subscription_revamp_title),
            style = MaterialTheme.typography.headlineSmall,
            textColor = TextColor.Primary,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag(TEST_TAG_REVAMP_TITLE),
        )
    }

    item("revamp_why_go_pro") {
        val storageText = uiState.localisedSubscriptionsList
            .maxByOrNull { it.storage }
            ?.let {
                val formatted = it.formatStorageSize()
                stringResource(formatted.unit, formatted.size)
            }
        val transferText = uiState.localisedSubscriptionsList
            .filter { it.yearlySubscription != null }
            .maxByOrNull { it.yearlySubscription?.transfer ?: 0 }
            ?.let {
                val formatted = it.formatTransferSize(isMonthly = false)
                stringResource(formatted.unit, formatted.size)
            }
        WhyGoProCard(
            title = stringResource(sharedR.string.subscription_revamp_why_go_pro_title),
            storageText = stringResource(
                sharedR.string.subscription_revamp_why_go_pro_storage,
                storageText ?: DEFAULT_MAX_STORAGE
            ),
            transferText = stringResource(
                sharedR.string.subscription_revamp_why_go_pro_transfer,
                transferText ?: DEFAULT_MAX_TRANSFER
            ),
            vpnText = stringResource(sharedR.string.subscription_revamp_why_go_pro_vpn),
            passText = stringResource(sharedR.string.subscription_revamp_why_go_pro_pass),
        )
    }

    if (isUpgradeAccount &&
        uiState.currentSubscriptionPlan != null &&
        uiState.currentSubscriptionPlan != AccountType.FREE
    ) {
        item("revamp_current_plan") {
            val cycleText = stringResource(
                if (uiState.subscriptionCycle == AccountSubscriptionCycle.MONTHLY) {
                    sharedR.string.subscription_revamp_current_plan_cycle_monthly
                } else {
                    sharedR.string.subscription_revamp_current_plan_cycle_yearly
                }
            )
            val date = currentPlanDate(uiState, locale)
            val helpText = date?.let {
                if (uiState.isCurrentSubscriptionRenewing) {
                    stringResource(sharedR.string.subscription_revamp_current_plan_renews, it)
                } else {
                    stringResource(sharedR.string.subscription_revamp_current_plan_expires, it)
                }
            }
            CurrentPlanCard(
                currentPlanLabel = stringResource(sharedR.string.account_upgrade_account_pro_plan_info_current_plan_label),
                planName = stringResource(uiState.currentSubscriptionPlan.toUIAccountType().textValue),
                cycleText = cycleText,
                helpText = helpText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    if (uiState.isSubscriptionFeatureAvailable == false) {
        subscriptionUnavailableContent(onLearnMoreClick = onSubscriptionUnavailableLearnMoreClick)
        return
    }

    item("revamp_billing_period") {
        BillingPeriodSelector(
            isMonthly = isMonthly,
            onPeriodSelected = onMonthlyChange,
            monthlyLabel = stringResource(sharedR.string.subscription_type_monthly),
            yearlyLabel = stringResource(sharedR.string.subscription_type_yearly),
            saveLabel = stringResource(sharedR.string.subscription_revamp_save_label),
        )
    }

    if (uiState.localisedSubscriptionsList.isEmpty() || uiState.isSubscriptionFeatureAvailable != true) {
        upgradeAccountSkeleton(itemCount = 3)
    } else {
        val subscriptionsForPeriod = uiState.localisedSubscriptionsList.filter {
            it.hasSubscriptionFor(isMonthly)
        }
        itemsIndexed(
            subscriptionsForPeriod,
            key = { _, subscription -> subscription.accountType.name }
        ) { index, subscription ->
            val uiAccountType = subscription.accountType.toUIAccountType()
            val planName = stringResource(uiAccountType.textValue)

            val storageFormatted = subscription.formatStorageSize()
            val transferFormatted = subscription.formatTransferSize(isMonthly)
            val storageString = stringResource(
                sharedR.string.choose_account_screen_storage_label,
                stringResource(storageFormatted.unit, storageFormatted.size)
            )
            val transferString = stringResource(
                sharedR.string.choose_account_screen_transfer_quota_label,
                stringResource(transferFormatted.unit, transferFormatted.size)
            )

            val perMonthPrice = if (isMonthly) {
                subscription.localisePriceCurrencyCode(locale, true).price
            } else {
                subscription.localisePriceOfYearlyAmountPerMonth(locale)?.price
                    ?: subscription.localisePriceCurrencyCode(locale, false).price
            }
            val monthlyPriceText =
                stringResource(sharedR.string.subscription_revamp_price_per_month, perMonthPrice)
            val yearlyTotalText = if (!isMonthly) {
                stringResource(
                    sharedR.string.subscription_revamp_charged_yearly,
                    subscription.localisePriceCurrencyCode(locale, false).price
                )
            } else null

            val isRecommended =
                uiState.cheapestSubscriptionAvailable?.accountType == subscription.accountType
            val isCurrentPlan = isCurrentPlan(
                uiState = uiState,
                subscriptionAccountType = subscription.accountType,
                isMonthly = isMonthly,
                isUpgradeAccount = isUpgradeAccount,
            )

            PlanPriceCard(
                planName = planName,
                monthlyPriceText = monthlyPriceText,
                yearlyTotalText = yearlyTotalText,
                storageText = storageString,
                transferText = transferString,
                buyButtonText = stringResource(
                    sharedR.string.subscription_revamp_get_plan_button,
                    planName
                ),
                onBuyClick = {
                    subscription.getSubscription(isMonthly)?.let(onInAppCheckoutClick)
                },
                isRecommended = isRecommended && !isCurrentPlan,
                recommendedLabel = stringResource(sharedR.string.account_upgrade_account_pro_plan_info_recommended_label),
                isCurrentPlan = isCurrentPlan,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .testTag("$TEST_TAG_REVAMP_PLAN_CARD$index"),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Returns the formatted renewal/expiry date for the current plan, or null if none is available.
 * Uses the renewal time when the subscription is renewing, otherwise the Pro expiration time.
 */
private fun currentPlanDate(uiState: UpgradeAccountState, locale: Locale): String? {
    val timeInSeconds = if (uiState.isCurrentSubscriptionRenewing) {
        uiState.subscriptionRenewTime
    } else {
        uiState.proExpirationTime
    } ?: return null
    return DateFormat.getDateInstance(DateFormat.LONG, locale).format(Date(timeInSeconds * 1000))
}

private const val DEFAULT_MAX_STORAGE = "20 TB"
private const val DEFAULT_MAX_TRANSFER = "240 TB"

/**
 * Test tag for the redesigned subscription page title
 */
internal const val TEST_TAG_REVAMP_TITLE = "subscription_revamp:title"

/**
 * Test tag prefix for each plan card on the redesigned subscription page (append index)
 */
internal const val TEST_TAG_REVAMP_PLAN_CARD = "subscription_revamp:plan_card_"
