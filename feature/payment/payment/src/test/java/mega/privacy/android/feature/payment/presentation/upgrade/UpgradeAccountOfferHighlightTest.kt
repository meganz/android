package mega.privacy.android.feature.payment.presentation.upgrade

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.formatter.mapper.FormattedSizeMapper
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.SubscriptionStatus
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.domain.entity.account.OfferPeriod
import mega.privacy.android.feature.payment.model.LocalisedSubscription
import mega.privacy.android.feature.payment.model.OfferHighlight
import mega.privacy.android.feature.payment.model.UpgradeAccountState
import mega.privacy.android.feature.payment.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import org.junit.jupiter.api.Test

/**
 * Unit tests for the [offerHighlight] classifier, the single seam the upgrade screen dispatches on
 * to choose between the standard, single-offer and (future) multiple-offer layouts.
 */
class UpgradeAccountOfferHighlightTest {
    private val priceMapper = LocalisedPriceCurrencyCodeStringMapper()
    private val sizeMapper = FormattedSizeMapper()

    private fun subscription(
        accountType: AccountType,
        discounted: Boolean,
    ): LocalisedSubscription = subscription(
        accountType = accountType,
        discountedMonthly = discounted,
        discountedYearly = discounted,
    )

    private fun subscription(
        accountType: AccountType,
        discountedMonthly: Boolean,
        discountedYearly: Boolean,
    ): LocalisedSubscription {
        fun option(transfer: Int, discounted: Boolean) = Subscription(
            sku = "${accountType.name}_$transfer",
            accountType = accountType,
            handle = accountType.ordinal.toLong(),
            storage = 2048,
            transfer = transfer,
            amount = CurrencyAmount(9.99F, Currency("EUR")),
            discountedAmountMonthly = if (discounted) CurrencyAmount(4.99F, Currency("EUR")) else null,
            discountedPercentage = if (discounted) 50 else null,
            offerPeriod = if (discounted) OfferPeriod.Month(12) else null,
            discountName = if (discounted) "Black Friday" else null,
        )
        return LocalisedSubscription(
            monthlySubscription = option(2048, discountedMonthly),
            yearlySubscription = option(24576, discountedYearly),
            localisedPriceCurrencyCode = priceMapper,
            formattedSize = sizeMapper,
        )
    }

    private fun state(vararg subscriptions: LocalisedSubscription) = UpgradeAccountState(
        localisedSubscriptionsList = subscriptions.toList(),
        isSubscriptionFeatureAvailable = true,
    )

    @Test
    fun `test that offerHighlight is None when no plan has a discount`() {
        val result = state(
            subscription(AccountType.PRO_I, discounted = false),
            subscription(AccountType.PRO_II, discounted = false),
        ).offerHighlight(isMonthly = false, isUpgradeAccount = false)

        assertThat(result).isEqualTo(OfferHighlight.None)
    }

    @Test
    fun `test that offerHighlight is Single when exactly one plan has a discount`() {
        val discounted = subscription(AccountType.PRO_I, discounted = true)
        val result = state(
            discounted,
            subscription(AccountType.PRO_II, discounted = false),
        ).offerHighlight(isMonthly = false, isUpgradeAccount = false)

        assertThat(result).isEqualTo(OfferHighlight.Single(discounted))
    }

    @Test
    fun `test that offerHighlight is Multiple when more than one plan has a discount`() {
        val first = subscription(AccountType.PRO_I, discounted = true)
        val second = subscription(AccountType.PRO_II, discounted = true)
        val result = state(first, second)
            .offerHighlight(isMonthly = false, isUpgradeAccount = false)

        assertThat(result).isEqualTo(OfferHighlight.Multiple(listOf(first, second)))
    }

    @Test
    fun `test that offerHighlight is Multiple for both periods when only yearly plans are discounted`() {
        val first = subscription(
            AccountType.PRO_I,
            discountedMonthly = false,
            discountedYearly = true,
        )
        val second = subscription(
            AccountType.PRO_II,
            discountedMonthly = false,
            discountedYearly = true,
        )
        val state = state(first, second)

        assertThat(state.offerHighlight(isMonthly = false, isUpgradeAccount = false))
            .isEqualTo(OfferHighlight.Multiple(listOf(first, second)))
        // Multiple persists across periods so the promotional header stays visible even when the
        // selected period (monthly) has no discounted cards.
        assertThat(state.offerHighlight(isMonthly = true, isUpgradeAccount = false))
            .isEqualTo(OfferHighlight.Multiple(listOf(first, second)))
    }

    @Test
    fun `test that offerHighlight excludes the current recurring plan on its own period`() {
        val current = subscription(AccountType.PRO_I, discounted = true)
        val result = UpgradeAccountState(
            localisedSubscriptionsList = listOf(
                current,
                subscription(AccountType.PRO_II, discounted = false),
            ),
            isSubscriptionFeatureAvailable = true,
            currentSubscriptionPlan = AccountType.PRO_I,
            subscriptionCycle = AccountSubscriptionCycle.YEARLY,
            subscriptionStatus = SubscriptionStatus.VALID,
        ).offerHighlight(isMonthly = false, isUpgradeAccount = true)

        assertThat(result).isEqualTo(OfferHighlight.None)
    }
}
