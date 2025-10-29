package mega.privacy.android.feature.payment.presentation.upgrade

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.privacy.android.core.formatter.mapper.FormattedSizeMapper
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.feature.payment.model.ChooseAccountState
import mega.privacy.android.feature.payment.model.LocalisedSubscription
import mega.privacy.android.feature.payment.model.mapper.LocalisedPriceCurrencyCodeStringMapper

internal class ChooseAccountPreviewProvider :
    PreviewParameterProvider<ChooseAccountState> {
    override val values: Sequence<ChooseAccountState>
        get() = sequenceOf(
            ChooseAccountState(
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
            ),
            yearlySubscription = Subscription(
                accountType = AccountType.PRO_I,
                handle = 1560943707714440503,
                storage = 2048,
                transfer = 24576,
                amount = CurrencyAmount(99.99F, Currency("EUR")),
            ),
            localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
            formattedSize = formattedSizeMapper,
        )

        val subscriptionProII = LocalisedSubscription(
            monthlySubscription = Subscription(
                accountType = AccountType.PRO_II,
                handle = 1560943707714440504,
                storage = 8192,
                transfer = 8192,
                amount = CurrencyAmount(19.99F, Currency("EUR")),
            ),
            yearlySubscription = Subscription(
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
                accountType = AccountType.PRO_III,
                handle = 1560943707714440505,
                storage = 16384,
                transfer = 16384,
                amount = CurrencyAmount(29.99F, Currency("EUR")),
            ),
            yearlySubscription = Subscription(
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
                accountType = AccountType.PRO_LITE,
                handle = 1560943707714440506,
                storage = 400,
                transfer = 1024,
                amount = CurrencyAmount(4.99F, Currency("EUR")),
            ),
            yearlySubscription = Subscription(
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
    }
}