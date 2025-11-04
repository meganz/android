package mega.privacy.android.feature.payment.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.formatter.mapper.FormattedSizeMapper
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.feature.payment.model.LocalisedSubscription
import mega.privacy.android.feature.payment.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.feature.payment.model.mapper.LocalisedSubscriptionMapper
import org.junit.Test

class LocalisedSubscriptionMapperTest {
    private val localisedPriceCurrencyCodeStringMapper = LocalisedPriceCurrencyCodeStringMapper()
    private val formattedSizeMapper = FormattedSizeMapper()
    private val underTest = LocalisedSubscriptionMapper(
        localisedPriceCurrencyCodeStringMapper,
        formattedSizeMapper,
    )

    @Test
    fun `test that mapper returns correctly LocalisedSubscription object`() {
        val subscriptionProIMonthly = Subscription(
            sku = "pro_i_monthly",
            accountType = AccountType.PRO_I,
            handle = 1560943707714440503,
            storage = 2048,
            transfer = 2048,
            amount = CurrencyAmount(9.99.toFloat(), Currency("EUR")),
            offerId = null,
            discountedAmountMonthly = null,
            discountedPercentage = null,
            offerPeriod = null
        )

        val subscriptionProIYearly = Subscription(
            sku = "pro_i_yearly",
            accountType = AccountType.PRO_I,
            handle = 7472683699866478542,
            storage = 2048,
            transfer = 24576,
            amount = CurrencyAmount(99.99.toFloat(), Currency("EUR")),
            offerId = null,
            discountedAmountMonthly = null,
            discountedPercentage = null,
            offerPeriod = null
        )

        val localisedSubscription = LocalisedSubscription(
            monthlySubscription = subscriptionProIMonthly,
            yearlySubscription = subscriptionProIYearly,
            localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
            formattedSize = formattedSizeMapper,
        )
        assertThat(underTest(subscriptionProIMonthly, subscriptionProIYearly)).isEqualTo(
            localisedSubscription
        )
    }
}