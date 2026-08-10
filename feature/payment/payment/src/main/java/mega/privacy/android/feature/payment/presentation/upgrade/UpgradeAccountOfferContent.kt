package mega.privacy.android.feature.payment.presentation.upgrade

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.badge.Badge
import mega.android.core.ui.components.badge.BadgeType
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.OfferPeriod
import mega.privacy.android.feature.payment.components.BillingPeriodSelector
import mega.privacy.android.feature.payment.components.OfferCountdown
import mega.privacy.android.feature.payment.components.OfferPriceCard
import mega.privacy.android.feature.payment.components.upgradeAccountSkeleton
import mega.privacy.android.feature.payment.model.LocalisedSubscription
import mega.privacy.android.feature.payment.model.OfferHighlight
import mega.privacy.android.feature.payment.model.UpgradeAccountState
import mega.privacy.android.feature.payment.model.extensions.toUIAccountType
import mega.privacy.android.shared.resources.R as sharedR
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

/**
 * The [LocalisedSubscription]s that carry an active discount for [isMonthly] and are not the user's
 * current plan. See [offerHighlight] for how the count of offers across periods selects the layout.
 */
internal fun UpgradeAccountState.offerPlansFor(
    isMonthly: Boolean,
    isUpgradeAccount: Boolean,
): List<LocalisedSubscription> = localisedSubscriptionsList.filter {
    it.getSubscription(isMonthly)?.discountedAmountMonthly != null &&
            !isCurrentPlan(this, it.accountType, isMonthly, isUpgradeAccount)
}

/**
 * Classifies active offers: more than one discounted plan in either period -> [OfferHighlight.Multiple]
 * (persists across the Monthly/Yearly toggle); a single plan for [isMonthly] -> [OfferHighlight.Single];
 * none -> [OfferHighlight.None].
 */
internal fun UpgradeAccountState.offerHighlight(
    isMonthly: Boolean,
    isUpgradeAccount: Boolean,
): OfferHighlight {
    val offerTypesAnyPeriod = (offerPlansFor(isMonthly = true, isUpgradeAccount) +
            offerPlansFor(isMonthly = false, isUpgradeAccount))
        .map { it.accountType }
        .distinct()
    if (offerTypesAnyPeriod.size > 1) {
        val plans = localisedSubscriptionsList.filter { it.accountType in offerTypesAnyPeriod }
        return OfferHighlight.Multiple(plans)
    }
    return with(offerPlansFor(isMonthly, isUpgradeAccount)) {
        when (size) {
            0 -> OfferHighlight.None
            else -> OfferHighlight.Single(first())
        }
    }
}

/**
 * Content of the single-offer subscription page: a promotional header
 * (badge, title, campaign name, countdown), a featured discounted [OfferPriceCard], then the shared
 * "Why go Pro?" card, current plan card, billing-period selector and the remaining (non-featured)
 * plans as regular price cards.
 */
internal fun LazyListScope.subscriptionOfferContent(
    uiState: UpgradeAccountState,
    offerSubscription: LocalisedSubscription,
    isMonthly: Boolean,
    onMonthlyChange: (Boolean) -> Unit,
    locale: Locale,
    context: Context,
    isUpgradeAccount: Boolean,
    onInAppCheckoutClick: (Subscription) -> Unit,
    onSubscriptionUnavailableLearnMoreClick: () -> Unit,
    onPricingPageClick: () -> Unit,
) {
    item("offer_header") {
        val subscription = offerSubscription.getSubscription(isMonthly)
        OfferHeader(
            campaignText = getCampaignName(
                context = context,
                discountName = subscription?.discountName,
                discountPercentage = subscription?.discountedPercentage ?: 0,
            ),
            offerValidUntil = uiState.offerValidUntil,
            locale = locale,
        )
    }

    item("offer_featured_card") {
        OfferPlanCardItem(
            offerSubscription = offerSubscription,
            isMonthly = isMonthly,
            context = context,
            locale = locale,
            onInAppCheckoutClick = onInAppCheckoutClick,
            featured = true,
        )
    }

    whyGoProItem(uiState)

    currentPlanItem(uiState, locale, isUpgradeAccount, onPricingPageClick)

    if (uiState.isSubscriptionFeatureAvailable == false) {
        subscriptionUnavailableContent(onLearnMoreClick = onSubscriptionUnavailableLearnMoreClick)
        return
    }

    item("offer_billing_period") {
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
        val remainingPlans = uiState.localisedSubscriptionsList
            .filter { it.hasSubscriptionFor(isMonthly) }
            .filterNot { it.accountType == offerSubscription.accountType }
            .filterNot { subscription ->
                isCurrentRecurringPlan(
                    uiState = uiState,
                    subscriptionAccountType = subscription.accountType,
                    isMonthly = isMonthly,
                    isUpgradeAccount = isUpgradeAccount,
                )
            }
        itemsIndexed(
            remainingPlans,
            key = { _, subscription -> subscription.accountType.name }
        ) { index, subscription ->
            PlanPriceCardItem(
                uiState = uiState,
                subscription = subscription,
                index = index,
                isMonthly = isMonthly,
                locale = locale,
                isUpgradeAccount = isUpgradeAccount,
                onInAppCheckoutClick = onInAppCheckoutClick,
            )
        }
    }
}

/**
 * Multiple-offer subscription page (Figma 10286-9598 / 10311-24214): a promotional header followed by
 * every plan inline — none featured on top or excluded. Each plan is a discounted [OfferPriceCard]
 * when the selected period has a discount, otherwise a plain [PlanPriceCard].
 */
internal fun LazyListScope.subscriptionMultipleOfferContent(
    uiState: UpgradeAccountState,
    isMonthly: Boolean,
    onMonthlyChange: (Boolean) -> Unit,
    locale: Locale,
    context: Context,
    isUpgradeAccount: Boolean,
    onInAppCheckoutClick: (Subscription) -> Unit,
    onSubscriptionUnavailableLearnMoreClick: () -> Unit,
    onPricingPageClick: () -> Unit,
) {
    item("offer_header") {
        val discountedSubscriptions = uiState.localisedSubscriptionsList
            .flatMap { listOfNotNull(it.monthlySubscription, it.yearlySubscription) }
            .filter { it.discountedPercentage != null }
        val maxPercentage =
            discountedSubscriptions.maxOfOrNull { it.discountedPercentage ?: 0 } ?: 0
        val campaignName = discountedSubscriptions.firstNotNullOfOrNull { it.discountName }
        OfferHeader(
            campaignText = getAggregateCampaignName(
                context = context,
                discountName = campaignName,
                maxPercentage = maxPercentage,
            ),
            offerValidUntil = uiState.offerValidUntil,
            locale = locale,
        )
    }

    whyGoProItem(uiState)

    currentPlanItem(uiState, locale, isUpgradeAccount, onPricingPageClick)

    if (uiState.isSubscriptionFeatureAvailable == false) {
        subscriptionUnavailableContent(onLearnMoreClick = onSubscriptionUnavailableLearnMoreClick)
        return
    }

    item("offer_billing_period") {
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
        val plans = uiState.localisedSubscriptionsList
            .filter { it.hasSubscriptionFor(isMonthly) }
            .filterNot { subscription ->
                isCurrentRecurringPlan(
                    uiState = uiState,
                    subscriptionAccountType = subscription.accountType,
                    isMonthly = isMonthly,
                    isUpgradeAccount = isUpgradeAccount,
                )
            }
        itemsIndexed(
            plans,
            key = { _, subscription -> subscription.accountType.name }
        ) { index, subscription ->
            val hasOffer = subscription.getSubscription(isMonthly)?.discountedAmountMonthly != null
            if (hasOffer) {
                OfferPlanCardItem(
                    offerSubscription = subscription,
                    isMonthly = isMonthly,
                    context = context,
                    locale = locale,
                    onInAppCheckoutClick = onInAppCheckoutClick,
                )
            } else {
                PlanPriceCardItem(
                    uiState = uiState,
                    subscription = subscription,
                    index = index,
                    isMonthly = isMonthly,
                    locale = locale,
                    isUpgradeAccount = isUpgradeAccount,
                    onInAppCheckoutClick = onInAppCheckoutClick,
                )
            }
        }
    }
}

@Composable
private fun OfferHeader(
    campaignText: String,
    offerValidUntil: Long?,
    locale: Locale,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Badge(
            badgeType = BadgeType.Mega,
            text = stringResource(sharedR.string.subscription_offer_special_offer_badge),
            modifier = Modifier.testTag(TEST_TAG_OFFER_HEADER_BADGE),
        )
        MegaText(
            text = stringResource(sharedR.string.subscription_revamp_title),
            style = MaterialTheme.typography.headlineMedium,
            textColor = TextColor.Primary,
            modifier = Modifier.testTag(TEST_TAG_OFFER_HEADER_TITLE),
        )
        MegaText(
            text = campaignText,
            style = MaterialTheme.typography.headlineSmall,
            textColor = TextColor.Primary,
            modifier = Modifier.testTag(TEST_TAG_OFFER_HEADER_CAMPAIGN),
        )
        OfferCountdownSection(validUntil = offerValidUntil, locale = locale)
    }
}

/**
 * Renders the offer countdown driven by [validUntil] (epoch seconds). Renders nothing while
 * [validUntil] is null or already elapsed; the remaining time is recomputed once a minute.
 */
@Composable
private fun OfferCountdownSection(validUntil: Long?, locale: Locale) {
    if (validUntil == null) return
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(validUntil) {
        while (true) {
            now = System.currentTimeMillis()
            if (validUntil * 1000L - now <= 0L) break
            delay(30.seconds)
        }
    }
    val remainingMillis = validUntil * 1000L - now
    if (remainingMillis <= 0L) return
    val totalMinutes = remainingMillis / 60_000L
    val days = totalMinutes / (60L * 24L)
    val hours = totalMinutes / 60L % 24L
    val minutes = totalMinutes % 60L
    OfferCountdown(
        validUntilText = stringResource(
            sharedR.string.subscription_offer_countdown_valid_until,
            DateFormat.getDateInstance(DateFormat.LONG, locale).format(Date(validUntil * 1000L)),
        ),
        days = days.toString().padStart(2, '0'),
        hours = hours.toString().padStart(2, '0'),
        minutes = minutes.toString().padStart(2, '0'),
        daysLabel = pluralStringResource(
            sharedR.plurals.subscription_offer_countdown_days,
            days.toInt()
        ),
        hoursLabel = pluralStringResource(
            sharedR.plurals.subscription_offer_countdown_hours,
            hours.toInt()
        ),
        minutesLabel = pluralStringResource(
            sharedR.plurals.subscription_offer_countdown_minutes,
            minutes.toInt()
        ),
    )
}

/**
 * Renders [offerSubscription] as a discounted [OfferPriceCard]. Used for the single-offer featured
 * card and the inline multiple-offer cards.
 */
@Composable
private fun OfferPlanCardItem(
    offerSubscription: LocalisedSubscription,
    isMonthly: Boolean,
    context: Context,
    locale: Locale,
    onInAppCheckoutClick: (Subscription) -> Unit,
    featured: Boolean = false,
) {
    val subscription = offerSubscription.getSubscription(isMonthly) ?: return
    val planName = stringResource(offerSubscription.accountType.toUIAccountType().textValue)

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

    val discountedMonthly =
        offerSubscription.localiseDiscountedPriceMonthlyCurrencyCode(
            locale,
            isMonthly
        )?.price.orEmpty()
    val discountedYearly =
        offerSubscription.localiseDiscountedPriceYearlyCurrencyCode(
            locale,
            isMonthly
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

    OfferPriceCard(
        planName = planName,
        priceText = priceText,
        originalPriceText = originalPrice,
        discountDescriptionText = billedDescription(
            offerPeriod = subscription.offerPeriod,
            isMonthly = isMonthly,
            discountedPrice = billedDiscountedPrice,
            originalPrice = billedOriginalPrice,
        ),
        discountBadgeText = getCampaignName(
            context = context,
            discountName = subscription.discountName,
            discountPercentage = subscription.discountedPercentage ?: 0,
        ),
        storageText = storageText,
        transferText = transferText,
        buyButtonText = stringResource(
            sharedR.string.subscription_revamp_get_plan_button,
            planName
        ),
        onBuyClick = { onInAppCheckoutClick(subscription) },
        monthlyPriceText = monthlyPriceText,
        useBrandButton = featured,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/**
 * Discount explanation, e.g. "Billed at €59.88 for the first year, €120 charged yearly after". The
 * wording is tailored to the billing period ([isMonthly]) and the offer duration; a 12-month offer
 * reads as "the first year".
 */
@Composable
internal fun billedDescription(
    offerPeriod: OfferPeriod?,
    isMonthly: Boolean,
    discountedPrice: String,
    originalPrice: String,
): String {
    val (count, inYears) = normalizeOfferPeriod(offerPeriod) ?: return ""
    val plural = when {
        isMonthly && inYears -> sharedR.plurals.subscription_offer_billed_monthly_years
        isMonthly -> sharedR.plurals.subscription_offer_billed_monthly_months
        inYears -> sharedR.plurals.subscription_offer_billed_yearly_years
        else -> sharedR.plurals.subscription_offer_billed_yearly_months
    }
    return pluralStringResource(plural, count, count, discountedPrice, originalPrice)
}

/**
 * Normalises an [OfferPeriod] to a (count, inYears) pair, collapsing whole-year month spans (e.g.
 * 12 months) to years so a one-year offer reads as "year" rather than "12 months". Null for no offer.
 */
internal fun normalizeOfferPeriod(offerPeriod: OfferPeriod?): Pair<Int, Boolean>? =
    when (offerPeriod) {
        is OfferPeriod.Year -> offerPeriod.value to true
        is OfferPeriod.Month ->
            if (offerPeriod.value % 12 == 0) (offerPeriod.value / 12) to true
            else offerPeriod.value to false

        null -> null
    }

/**
 * Test tag for the single-offer header badge
 */
internal const val TEST_TAG_OFFER_HEADER_BADGE = "subscription_offer:header_badge"

/**
 * Test tag for the single-offer header title
 */
internal const val TEST_TAG_OFFER_HEADER_TITLE = "subscription_offer:header_title"

/**
 * Test tag for the single-offer header campaign name
 */
internal const val TEST_TAG_OFFER_HEADER_CAMPAIGN = "subscription_offer:header_campaign"
