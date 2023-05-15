package test.mega.privacy.android.app.upgradeAccount.model.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.upgradeAccount.model.LocalisedSubscription
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
    private val underTest = LocalisedSubscriptionMapper(
        localisedPriceStringMapper,
        localisedPriceCurrencyCodeStringMapper
    )

    @Test
    fun `test that mapper returns correctly LocalisedSubscription object`() {
        val subscription = Subscription(
            accountType = AccountType.FREE,
            handle = 11,
            storage = 12,
            transfer = 13,
            amount = CurrencyAmount(4.99.toFloat(), Currency("EUR"))
        )

        val localisedSubscription = LocalisedSubscription(
            accountType = AccountType.FREE,
            handle = 11,
            storage = 12,
            transfer = 13,
            amount = CurrencyAmount(4.99.toFloat(), Currency("EUR")),
            localisedPrice = localisedPriceStringMapper,
            localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        )
        assertThat(underTest(subscription)).isEqualTo(localisedSubscription)
    }
}