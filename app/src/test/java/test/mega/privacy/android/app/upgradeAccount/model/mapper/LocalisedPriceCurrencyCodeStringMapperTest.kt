package test.mega.privacy.android.app.upgradeAccount.model.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.account.CurrencyAmount
import org.junit.Test
import java.util.Locale

class LocalisedPriceCurrencyCodeStringMapperTest {
    private val underTest = LocalisedPriceCurrencyCodeStringMapper()

    @Test
    fun `test that mapper returns correctly pair of formatted price and currency code strings`() {
        val expectedResult = Pair("€4.99", "EUR")
        val currencyAmount = CurrencyAmount(4.99.toFloat(), Currency("EUR"))
        assertThat(underTest(currencyAmount, Locale.US)).isEqualTo(expectedResult)
    }
}