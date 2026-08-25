package mega.privacy.android.feature.payment.presentation.offer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.feature.payment.components.SubscriptionOfferScreenContent
import mega.privacy.android.feature.payment.components.SubscriptionOfferScreenError
import mega.privacy.android.feature.payment.components.SubscriptionOfferScreenSkeleton
import mega.privacy.android.feature.payment.components.rememberOfferExpired
import mega.privacy.android.feature.payment.model.LocalisedSubscription
import mega.privacy.android.feature.payment.model.extensions.toUIAccountType
import mega.privacy.android.feature.payment.presentation.upgrade.billedDescription
import mega.privacy.android.feature.payment.presentation.upgrade.getCampaignName
import mega.privacy.android.shared.resources.R as sharedR
import java.text.DateFormat
import java.util.Date

/**
 * Full-screen promo for the recommended discounted plan (DSN-3130 offer landing screen). Shows the
 * campaign artwork, the offer countdown, the discounted plan card and a pinned buy CTA.
 *
 * Shows the "Couldn't connect" state while the device is offline or the offer failed to load, and a
 * full-screen skeleton while [SubscriptionOfferState.offerSubscription] is still null.
 *
 * When more than one plan carries the campaign, a "View all plans" text button is shown below the
 * buy CTA so the other discounted plans stay reachable.
 *
 * @param uiState the offer to promote
 * @param onBuyClick called with the promoted [Subscription] when the buy CTA is tapped while the
 * offer is still running
 * @param onDismiss called when the dismiss (X) icon is tapped
 * @param onViewAllPlansClick called when the "View all plans" text button is tapped, and when the
 * buy CTA is tapped after the offer has expired — both open the regular upgrade screen
 * @param onRetryClick called when the error state's "Try again" button is tapped
 * @param modifier
 */
@Composable
internal fun SubscriptionOfferScreen(
    uiState: SubscriptionOfferState,
    onBuyClick: (Subscription) -> Unit,
    onDismiss: () -> Unit,
    onViewAllPlansClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val offerSubscription = uiState.offerSubscription
    val subscription = offerSubscription?.getSubscription(uiState.isMonthly)
    when {
        !uiState.isConnected || uiState.hasLoadError -> SubscriptionOfferScreenError(
            onRetryClick = onRetryClick,
            onDismissClick = onDismiss,
            modifier = modifier.fillMaxSize(),
        )

        offerSubscription == null || subscription == null ->
            SubscriptionOfferScreenSkeleton(
                onDismissClick = onDismiss,
                modifier = modifier.fillMaxSize(),
            )

        else -> SubscriptionOfferLoadedContent(
            uiState = uiState,
            offerSubscription = offerSubscription,
            subscription = subscription,
            onBuyClick = onBuyClick,
            onDismiss = onDismiss,
            onViewAllPlansClick = onViewAllPlansClick,
            modifier = modifier,
        )
    }
}

/**
 * The loaded offer: resolves the localised plan name, benefits and discount wording for
 * [offerSubscription] on the promoted billing period and hands them to [SubscriptionOfferScreenContent].
 *
 * @param uiState the offer to promote
 * @param offerSubscription the discounted plan to promote
 * @param subscription the promoted [offerSubscription] on the billing period the offer applies to
 * @param onBuyClick called with [subscription] when the buy CTA is tapped while the offer is running
 * @param onDismiss called when the dismiss (X) icon is tapped
 * @param onViewAllPlansClick called when the "View all plans" text button is tapped, and when the
 * buy CTA is tapped after the offer has expired
 * @param modifier
 */
@Composable
private fun SubscriptionOfferLoadedContent(
    uiState: SubscriptionOfferState,
    offerSubscription: LocalisedSubscription,
    subscription: Subscription,
    onBuyClick: (Subscription) -> Unit,
    onDismiss: () -> Unit,
    onViewAllPlansClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isMonthly = uiState.isMonthly
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val planName = stringResource(offerSubscription.accountType.toUIAccountType().textValue)

    // The discount UI stays after the deal ends (DSN-3130), so the CTA is what stops honouring it.
    val isOfferEnded = rememberOfferExpired(uiState.offerValidUntil ?: 0L)

    val storageFormatted = offerSubscription.formatStorageSize()
    val transferFormatted = offerSubscription.formatTransferSize(isMonthly)
    val storageText = stringResource(
        sharedR.string.choose_account_screen_storage_label,
        stringResource(storageFormatted.unit, storageFormatted.size)
    )
    val transferText = stringResource(
        sharedR.string.choose_account_screen_transfer_quota_label,
        stringResource(transferFormatted.unit, transferFormatted.size)
    )

    val discountedMonthly = offerSubscription.localiseDiscountedPriceMonthlyCurrencyCode(
        locale,
        isMonthly,
    )?.price.orEmpty()
    val discountedYearly = offerSubscription.localiseDiscountedPriceYearlyCurrencyCode(
        locale,
        isMonthly,
    )?.price.orEmpty()
    val originalPrice = offerSubscription.localisePriceCurrencyCode(locale, isMonthly).price

    val priceText: String
    val monthlyPriceText: String?
    val billedDiscountedPrice: String
    val billedOriginalPrice: String
    if (isMonthly) {
        priceText =
            stringResource(sharedR.string.subscription_revamp_price_per_month, discountedMonthly)
        monthlyPriceText = null
        billedDiscountedPrice = priceText
        billedOriginalPrice =
            stringResource(sharedR.string.subscription_revamp_price_per_month, originalPrice)
    } else {
        priceText =
            stringResource(sharedR.string.subscription_revamp_price_per_year, discountedYearly)
        monthlyPriceText =
            stringResource(sharedR.string.subscription_revamp_price_per_month, discountedMonthly)
        billedDiscountedPrice = discountedYearly
        billedOriginalPrice = originalPrice
    }

    SubscriptionOfferScreenContent(
        campaignText = getCampaignName(
            context = context,
            discountName = subscription.discountName,
            discountPercentage = subscription.discountedPercentage ?: 0,
        ),
        validUntil = uiState.offerValidUntil,
        validUntilText = uiState.offerValidUntil?.let {
            stringResource(
                sharedR.string.subscription_offer_countdown_valid_until,
                DateFormat.getDateInstance(DateFormat.LONG, locale).format(Date(it * 1000L)),
            )
        },
        planName = planName,
        priceText = priceText,
        originalPriceText = originalPrice,
        discountDescriptionText = billedDescription(
            offerPeriod = subscription.offerPeriod,
            isMonthly = isMonthly,
            discountedPrice = billedDiscountedPrice,
            originalPrice = billedOriginalPrice,
        ),
        storageText = storageText,
        transferText = transferText,
        buyButtonText = stringResource(
            sharedR.string.subscription_revamp_get_plan_button,
            planName,
        ),
        onBuyClick = {
            if (isOfferEnded) onViewAllPlansClick() else onBuyClick(subscription)
        },
        onDismissClick = onDismiss,
        modifier = modifier,
        monthlyPriceText = monthlyPriceText,
        viewAllPlansText = stringResource(sharedR.string.subscription_quota_view_all_plans)
            .takeIf { uiState.hasMultipleOffers },
        onViewAllPlansClick = onViewAllPlansClick,
    )
}
