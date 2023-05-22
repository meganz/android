package test.mega.privacy.android.app.upgradeAccount.model.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.upgradeAccount.model.LocalisedSubscription
import mega.privacy.android.app.upgradeAccount.model.mapper.FormattedSizeMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceStringMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedSubscriptionMapper
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.CurrencyAmount
import org.junit.Test

class LocalisedSubscriptionMapperTest {
    private val localisedPriceStringMapper = LocalisedPriceStringMapper()
    private val localisedPriceCurrencyCodeStringMapper = LocalisedPriceCurrencyCodeStringMapper()
    private val formattedSizeMapper = FormattedSizeMapper()
    private val underTest = LocalisedSubscriptionMapper(
        localisedPriceStringMapper,
        localisedPriceCurrencyCodeStringMapper,
        formattedSizeMapper,
    )

    @Test
    fun `test that mapper returns correctly LocalisedSubscription object`() {
        val subscriptionProIMonthly = Subscription(
            accountType = AccountType.PRO_I,
            handle = 1560943707714440503,
            storage = 2048,
            transfer = 2048,
            amount = CurrencyAmount(9.99.toFloat(), Currency("EUR"))
        )

        val subscriptionProIYearly = Subscription(
            accountType = AccountType.PRO_I,
            handle = 7472683699866478542,
            storage = 2048,
            transfer = 24576,
            amount = CurrencyAmount(99.99.toFloat(), Currency("EUR"))
        )

        val localisedSubscription = LocalisedSubscription(
            accountType = AccountType.PRO_I,
            storage = 2048,
            monthlyTransfer = 2048,
            yearlyTransfer = 24576,
            monthlyAmount = CurrencyAmount(9.99.toFloat(), Currency("EUR")),
            yearlyAmount = CurrencyAmount(
                99.99.toFloat(),
                Currency("EUR")
            ),
            localisedPrice = localisedPriceStringMapper,
            localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
            formattedSize = formattedSizeMapper,
        )
        assertThat(underTest(subscriptionProIMonthly, subscriptionProIYearly)).isEqualTo(
            localisedSubscription
        )
    }
}