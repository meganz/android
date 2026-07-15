package mega.privacy.android.feature.payment.presentation.upgrade

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.privacy.android.core.formatter.mapper.FormattedSizeMapper
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.domain.entity.account.OfferPeriod
import mega.privacy.android.feature.payment.model.UpgradeAccountState
import mega.privacy.android.feature.payment.model.LocalisedSubscription
import mega.privacy.android.feature.payment.model.mapper.LocalisedPriceCurrencyCodeStringMapper

internal class UpgradeAccountPreviewProvider :
    PreviewParameterProvider<UpgradeAccountState> {
    override val values: Sequence<UpgradeAccountState>
        get() = sequenceOf(
            UpgradeAccountState(
                localisedSubscriptionsList = localisedSubscriptionsList
            )
        )

    companion object {
        val localisedPriceCurrencyCodeStringMapper = LocalisedPriceCurrencyCodeStringMapper()
        val formattedSizeMapper = FormattedSizeMapper()

        val subscriptionProI = LocalisedSubscription(
            monthlySubscription = Subscription(
                accountType = AccountType.PRO_I,
                handle = 1560943707714440503,
                storage = 2048,
                transfer = 2048,
                amount = CurrencyAmount(9.99F, Currency("EUR")),
                sku = "mega.android.pro1.onemonth"
            ),
            yearlySubscription = Subscription(
                accountType = AccountType.PRO_I,
                handle = 1560943707714440503,
                storage = 2048,
                transfer = 24576,
                amount = CurrencyAmount(99.99F, Currency("EUR")),
                sku = "mega.android.pro1.oneyear"
            ),
            localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
            formattedSize = formattedSizeMapper,
        )

        val subscriptionProII = LocalisedSubscription(
            monthlySubscription = Subscription(
                sku = "mega.android.pro2.onemonth",
                accountType = AccountType.PRO_II,
                handle = 1560943707714440504,
                storage = 8192,
                transfer = 8192,
                amount = CurrencyAmount(19.99F, Currency("EUR")),
            ),
            yearlySubscription = Subscription(
                sku = "mega.android.pro2.oneyear",
                accountType = AccountType.PRO_II,
                handle = 1560943707714440504,
                storage = 8192,
                transfer = 98304,
                amount = CurrencyAmount(199.99F, Currency("EUR")),
            ),
            localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
            formattedSize = formattedSizeMapper,
        )

        val subscriptionProIII = LocalisedSubscription(
            monthlySubscription = Subscription(
                sku = "mega.android.pro3.onemonth",
                accountType = AccountType.PRO_III,
                handle = 1560943707714440505,
                storage = 16384,
                transfer = 16384,
                amount = CurrencyAmount(29.99F, Currency("EUR")),
            ),
            yearlySubscription = Subscription(
                sku = "mega.android.pro3.oneyear",
                accountType = AccountType.PRO_III,
                handle = 1560943707714440505,
                storage = 16384,
                transfer = 196608,
                amount = CurrencyAmount(299.99F, Currency("EUR")),
            ),
            localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
            formattedSize = formattedSizeMapper,
        )

        val subscriptionProLite = LocalisedSubscription(
            monthlySubscription = Subscription(
                sku = "mega.android.prolite.onemonth",
                accountType = AccountType.PRO_LITE,
                handle = 1560943707714440506,
                storage = 400,
                transfer = 1024,
                amount = CurrencyAmount(4.99F, Currency("EUR")),
            ),
            yearlySubscription = Subscription(
                sku = "mega.android.prolite.oneyear",
                accountType = AccountType.PRO_LITE,
                handle = 1560943707714440506,
                storage = 400,
                transfer = 12288,
                amount = CurrencyAmount(49.99F, Currency("EUR")),
            ),
            localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
            formattedSize = formattedSizeMapper,
        )

        val localisedSubscriptionsList: List<LocalisedSubscription> = listOf(
            subscriptionProLite,
            subscriptionProI,
            subscriptionProII,
            subscriptionProIII
        )

        val subscriptionProIOffer = LocalisedSubscription(
            monthlySubscription = Subscription(
                sku = "mega.android.pro1.onemonth",
                accountType = AccountType.PRO_I,
                handle = 1560943707714440503,
                storage = 2048,
                transfer = 2048,
                amount = CurrencyAmount(9.99F, Currency("EUR")),
                discountedAmountMonthly = CurrencyAmount(4.99F, Currency("EUR")),
                discountedPercentage = 50,
                offerPeriod = OfferPeriod.Month(12),
                discountName = "Black Friday",
            ),
            yearlySubscription = Subscription(
                sku = "mega.android.pro1.oneyear",
                accountType = AccountType.PRO_I,
                handle = 1560943707714440503,
                storage = 2048,
                transfer = 24576,
                amount = CurrencyAmount(99.99F, Currency("EUR")),
                discountedAmountMonthly = CurrencyAmount(4.99F, Currency("EUR")),
                discountedPercentage = 50,
                offerPeriod = OfferPeriod.Month(12),
                discountName = "Black Friday",
            ),
            localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
            formattedSize = formattedSizeMapper,
        )

        val singleOfferSubscriptionsList: List<LocalisedSubscription> = listOf(
            subscriptionProLite,
            subscriptionProIOffer,
            subscriptionProII,
            subscriptionProIII
        )

        private fun LocalisedSubscription.withOffer(
            discountedMonthly: Float,
            percentage: Int,
        ): LocalisedSubscription = copy(
            monthlySubscription = monthlySubscription?.copy(
                discountedAmountMonthly = CurrencyAmount(discountedMonthly, Currency("EUR")),
                discountedPercentage = percentage,
                offerPeriod = OfferPeriod.Month(12),
                discountName = "Mid-year sale",
            ),
            yearlySubscription = yearlySubscription?.copy(
                discountedAmountMonthly = CurrencyAmount(discountedMonthly, Currency("EUR")),
                discountedPercentage = percentage,
                offerPeriod = OfferPeriod.Month(12),
                discountName = "Mid-year sale",
            ),
        )

        val multipleOfferSubscriptionsList: List<LocalisedSubscription> = listOf(
            subscriptionProI.withOffer(discountedMonthly = 3.59F, percentage = 28),
            subscriptionProII.withOffer(discountedMonthly = 6.19F, percentage = 38),
            subscriptionProIII.withOffer(discountedMonthly = 8.32F, percentage = 48),
        )
    }
}
